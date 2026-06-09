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
}
