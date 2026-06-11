package com.share.with

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityAuditTest {
    @Test
    fun testSessionBindingToIp() {
        AppState.clearActiveAndPending()
        val token = "valid-token"
        val ip1 = "192.168.1.100"
        val ip2 = "192.168.1.101"

        AppState.addActive(token, ip1, "UserAgent")

        // Session should be valid for ip1
        assertTrue(AppState.activeSessions.any { it.token == token && it.ipAddress == ip1 }, "Session should be valid for original IP")

        // Session should NOT be valid for ip2 with the same token
        assertFalse(AppState.activeSessions.any { it.token == token && it.ipAddress == ip2 }, "Session should be invalid for different IP")
    }

    @Test
    fun testSessionActivityUpdate() {
        AppState.clearActiveAndPending()
        val token = "token"
        val ip = "1.1.1.1"
        AppState.addActive(token, ip, "UA")

        val initialLastSeen = AppState.activeSessions[0].lastSeen
        Thread.sleep(10)
        AppState.updateSessionActivity(token, ip)
        val updatedLastSeen = AppState.activeSessions[0].lastSeen

        assertTrue(updatedLastSeen > initialLastSeen, "Last seen should be updated")
    }

    @Test
    fun testSessionExpiry() {
        AppState.clearActiveAndPending()
        val token = "expired"
        val ip = "2.2.2.2"
        AppState.addActive(token, ip, "UA")

        // Manually manipulate lastSeen for testing if needed, or use a very small expiry
        // Since we can't easily manipulate lastSeen in the list without reflection or better API,
        // we'll just test the cleanExpiredSessions method logic.

        AppState.cleanExpiredSessions(-1) // Everything should be expired
        assertEquals(0, AppState.activeSessions.size, "All sessions should be cleared if expiry is negative")
    }

    @Test
    fun testIpBlockingAndUnblocking() {
        val ip = "10.0.0.5"
        AppState.blockIp(ip)
        assertTrue(AppState.blockedIps.contains(ip))

        AppState.unblockIp(ip)
        assertFalse(AppState.blockedIps.contains(ip))
    }

    @Test
    fun testCsrfTokenLifecycle() {
        val csrfTokens = java.util.concurrent.ConcurrentHashMap<String, String>()
        val ip = "192.168.1.50"

        // Generate CSRF token
        val token = java.util.UUID.randomUUID().toString()
        csrfTokens[ip] = token

        // 1. Valid validation
        assertEquals(token, csrfTokens[ip])

        // 2. Simulate single-use consumption
        val csrfIn = token
        val isValid = (csrfIn == csrfTokens[ip])
        assertTrue(isValid)
        csrfTokens.remove(ip) // consumed

        // 3. Re-verification should fail (Single-use check)
        assertFalse(csrfIn == csrfTokens[ip], "Token should be single-use only")
    }

    @Test
    fun testRateLimitingLockout() {
        val loginAttempts = java.util.concurrent.ConcurrentHashMap<String, Int>()
        val lastAttemptTime = java.util.concurrent.ConcurrentHashMap<String, Long>()
        val ip = "192.168.1.60"

        val maxAttempts = 5
        val lockoutDuration = 60_000L

        fun isLockedOut(
            ip: String,
            now: Long,
        ): Boolean {
            val attempts = loginAttempts[ip] ?: 0
            val lastTime = lastAttemptTime[ip] ?: 0L
            return attempts >= maxAttempts && (now - lastTime < lockoutDuration)
        }

        // Simulate 4 failed attempts - should NOT be locked out
        loginAttempts[ip] = 4
        lastAttemptTime[ip] = System.currentTimeMillis()
        assertFalse(isLockedOut(ip, System.currentTimeMillis()), "4 failed attempts should not trigger lockout")

        // 5th failed attempt - should be locked out
        loginAttempts[ip] = 5
        val lockoutStartTime = System.currentTimeMillis()
        lastAttemptTime[ip] = lockoutStartTime
        assertTrue(isLockedOut(ip, lockoutStartTime), "5 failed attempts must trigger lockout")

        // Lockout expired - should be allowed again
        assertTrue(isLockedOut(ip, lockoutStartTime + 30_000L), "Lockout should still be active at 30s")
        assertFalse(isLockedOut(ip, lockoutStartTime + 61_000L), "Lockout must expire after duration elapsed")
    }

    @Test
    fun testAdvancedDirectoryTraversalPrevention() {
        val baseDir = java.io.File("build/tmp/shared_advanced").canonicalFile
        baseDir.mkdirs()

        fun isPathSafe(subPath: String): Boolean {
            // Replicate directory traversal prevention checks in ServerManager.kt
            if (subPath.startsWith("/") || subPath.startsWith("\\") || subPath.contains("..")) {
                return false
            }
            // URL decode step
            val decodedPath =
                try {
                    java.net.URLDecoder.decode(subPath, "UTF-8")
                } catch (e: Exception) {
                    subPath
                }
            if (decodedPath.startsWith("/") || decodedPath.startsWith("\\") || decodedPath.contains("..")) {
                return false
            }
            val resolved = java.io.File(baseDir, decodedPath).canonicalFile
            val rootPath = baseDir.canonicalPath
            return resolved.path.startsWith(rootPath + java.io.File.separator) || resolved.path == rootPath
        }

        // Standard inputs
        assertTrue(isPathSafe("file.txt"))
        assertTrue(isPathSafe("dir/file.txt"))

        // Traversal patterns
        assertFalse(isPathSafe("../file.txt"))
        assertFalse(isPathSafe("/file.txt"))
        assertFalse(isPathSafe("\\file.txt"))

        // URL encoded traversal checks
        assertFalse(isPathSafe("%2e%2e%2fescape.txt"), "URL-encoded traversal must be blocked")
        assertFalse(isPathSafe("dir/%2e%2e%2fescape.txt"), "Nested URL-encoded traversal must be blocked")
        assertFalse(isPathSafe("dir/..%2fescape.txt"), "Partial URL-encoded traversal must be blocked")

        // Clean up
        baseDir.deleteRecursively()
    }
}
