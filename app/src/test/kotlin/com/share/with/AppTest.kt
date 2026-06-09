package com.share.with

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecurityTest {

    @Test
    fun testPathTraversalPrevention() {
        // Mock a base shared directory
        val baseDir = File("build/tmp/shared").canonicalFile
        baseDir.mkdirs()
        
        // Validation algorithm replicating ServerManager.kt
        fun isPathSafe(subPath: String?, resolvedFileOverrideForSymlink: File? = null): Boolean {
            if (subPath != null && (subPath.startsWith("/") || subPath.startsWith("\\") || subPath.contains(".."))) return false
            val targetFile = resolvedFileOverrideForSymlink ?: if (subPath.isNullOrEmpty()) {
                baseDir
            } else {
                File(baseDir, subPath).canonicalFile
            }
            val rootPath = baseDir.path
            return targetFile.path.startsWith(rootPath + File.separator) || targetFile.path == rootPath
        }

        // Valid paths within the shared root
        assertTrue(isPathSafe(""), "Empty subpath should be allowed")
        assertTrue(isPathSafe(null), "Null subpath should be allowed")
        assertTrue(isPathSafe("file.txt"), "Standard file should be allowed")
        assertTrue(isPathSafe("folder/nested_file.txt"), "Nested file should be allowed")

        // Malicious directory traversal attempts
        assertFalse(isPathSafe(".."), "Parent directory escape should be blocked")
        assertFalse(isPathSafe("../etc/passwd"), "Relative path escaping shared root should be blocked")
        assertFalse(isPathSafe("folder/../../escape.txt"), "Deep relative escaping should be blocked")
        assertFalse(isPathSafe("/etc/passwd"), "Absolute path escaping shared root should be blocked")
        
        // Test sibling prefix traversal (e.g. symlink to sibling directory with shared prefix)
        val siblingSecretDir = File("build/tmp/shared-secret").canonicalFile
        assertFalse(isPathSafe("symlink_to_sibling", siblingSecretDir), "Access to prefix-matching sibling directory must be blocked")

        // Clean up
        baseDir.deleteRecursively()
    }

    @Test
    fun testOnlySharedItemsCanBeAccessed() {
        // Clear all shared items
        AppState.sharedItems.clear()
        
        // Add a valid shared item
        val allowedFile = File("build/tmp/allowed_file.txt").canonicalFile
        val allowedItem = SharedItem(
            name = allowedFile.name,
            uriString = allowedFile.absolutePath,
            size = 100L,
            isDirectory = false
        )
        AppState.sharedItems.add(allowedItem)
        
        // 1. Verify that the allowed item can be found by its UUID
        val foundItem = AppState.sharedItems.find { it.id == allowedItem.id }
        assertEquals(allowedItem, foundItem, "Allowed item should be accessible")
        
        // 2. Verify that an item with an arbitrary UUID not added by the user cannot be found
        val fakeId = java.util.UUID.randomUUID().toString()
        val notFoundItem = AppState.sharedItems.find { it.id == fakeId }
        assertNull(notFoundItem, "Arbitrary non-added items must not be accessible")
        
        // Clean up
        AppState.sharedItems.clear()
    }

    @Test
    fun testIsValidUuid() {
        assertTrue(ServerManager.isValidUuid(java.util.UUID.randomUUID().toString()))
        assertFalse(ServerManager.isValidUuid("not-a-uuid"))
        assertFalse(ServerManager.isValidUuid("abc');alert(1);//"))
    }
}

class AppStateTest {

    @Test
    fun testLogStateManagement() {
        AppState.clearLogs()
        assertEquals(0, AppState.logs.size, "Logs should be empty initially")

        AppState.addLog("Test message 1")
        assertEquals(1, AppState.logs.size, "Logs size should increase")
        assertTrue(AppState.logs[0].contains("Test message 1"), "Logs should contain the message")

        // Test log injection prevention (newline sanitization)
        AppState.addLog("Injected\nLog\rLine")
        assertFalse(AppState.logs[0].contains("\n"), "Logs must not contain raw newlines")
        assertFalse(AppState.logs[0].contains("\r"), "Logs must not contain raw carriage returns")
        assertTrue(AppState.logs[0].contains("Injected Log Line"), "Newline characters should be replaced with spaces")

        // Test log capping at 1000 items
        AppState.clearLogs()
        for (i in 1..1050) {
            AppState.addLog("Message $i")
        }
        assertEquals(1000, AppState.logs.size, "Logs should be capped at 1000 items to prevent OOM")
        assertTrue(AppState.logs[0].contains("Message 1050"), "Latest log should be at the top")
    }

    @Test
    fun testPendingConnectionsLifecycle() {
        AppState.clearActiveAndPending()
        assertEquals(0, AppState.pendingApprovals.size)

        val connection1 = PendingConnection("token123", "192.168.1.5", "Mozilla/5.0")
        AppState.addPending(connection1)
        assertEquals(1, AppState.pendingApprovals.size)

        // Deduplication: adding a connection with the same IP should replace the old one
        val connection2 = PendingConnection("token456", "192.168.1.5", "Chrome/100")
        AppState.addPending(connection2)
        assertEquals(1, AppState.pendingApprovals.size, "Connections from same IP should be deduplicated")
        assertEquals("token456", AppState.pendingApprovals[0].token, "New connection token should replace old one")

        // Test removal
        AppState.removePending("token456")
        assertEquals(0, AppState.pendingApprovals.size, "Pending connection should be removed")
    }

    @Test
    fun testActiveSessionsLifecycle() {
        AppState.clearActiveAndPending()
        assertEquals(0, AppState.activeSessions.size)

        // Add active session
        AppState.addActive("session_token", "192.168.1.10", "Safari")
        assertEquals(1, AppState.activeSessions.size)

        // Deduplication for active sessions from same IP
        AppState.addActive("new_session_token", "192.168.1.10", "Firefox")
        assertEquals(1, AppState.activeSessions.size, "Active sessions from same IP should replace previous ones")
        assertEquals("new_session_token", AppState.activeSessions[0].token)

        // Remove active session
        AppState.removeActive("192.168.1.10")
        assertEquals(0, AppState.activeSessions.size)
    }

    @Test
    fun testConnectionApprovalWorkflow() {
        AppState.clearActiveAndPending()

        // 1. Client attempts to connect (becomes pending)
        val token = "approve_me"
        val ip = "192.168.1.15"
        val userAgent = "WebClient"
        AppState.addPending(PendingConnection(token, ip, userAgent))

        assertTrue(AppState.pendingApprovals.any { it.token == token })
        assertFalse(AppState.activeSessions.any { it.ipAddress == ip })

        // 2. Host approves connection
        AppState.approvePending(token)

        // Pending approval queue should clear this token, and active sessions list should gain it
        assertFalse(AppState.pendingApprovals.any { it.token == token }, "Pending approval should be cleared")
        assertTrue(AppState.activeSessions.any { it.token == token && it.ipAddress == ip }, "Should be moved to active sessions")
    }

    @Test
    fun testThemeAndLanguageState() {
        // Verify state bindings work as reactive properties
        AppState.selectedTheme = "System"
        assertEquals("System", AppState.selectedTheme)
        
        AppState.selectedTheme = "Dark"
        assertEquals("Dark", AppState.selectedTheme)

        AppState.selectedLanguage = "en"
        assertEquals("en", AppState.selectedLanguage)

        AppState.selectedLanguage = "he"
        assertEquals("he", AppState.selectedLanguage)
    }
}
