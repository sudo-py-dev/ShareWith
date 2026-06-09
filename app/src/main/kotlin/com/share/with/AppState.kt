package com.share.with

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PendingConnection(
    val token: String,
    val ipAddress: String,
    val userAgent: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActiveSession(
    val token: String,
    val ipAddress: String,
    val userAgent: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)

data class SharedItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val uriString: String,
    val size: Long,
    val isDirectory: Boolean
)

object AppState {
    val blockedIps = mutableStateListOf<String>()

    fun blockIp(ip: String) {
        synchronized(lock) {
            if (!blockedIps.contains(ip)) {
                blockedIps.add(ip)
            }
            pendingApprovals.removeAll { it.ipAddress == ip }
            activeSessions.removeAll { it.ipAddress == ip }
        }
    }

    fun unblockIp(ip: String) {
        synchronized(lock) {
            blockedIps.remove(ip)
        }
    }

    fun clearBlockedIps() {
        synchronized(lock) {
            blockedIps.clear()
        }
    }
    // UI Theme & Lang
    var selectedTheme by mutableStateOf("System")
    var selectedLanguage by mutableStateOf("en")
    
    // Server State
    var serverRunning by mutableStateOf(false)
    var portInput by mutableStateOf("8080")
    var serverPort by mutableIntStateOf(8080)
    var securityMode by mutableStateOf(SecurityMode.MANUAL_APPROVAL)
    var password by mutableStateOf("")
    
    var localIp by mutableStateOf<String?>(null)

    // Dynamic Lists (Thread-Safe Wrapper Methods)
    val sharedItems = mutableStateListOf<SharedItem>()
    val logs = mutableStateListOf<String>()

    fun addSharedItem(file: java.io.File, isDirectory: Boolean) {
        synchronized(lock) {
            val item = SharedItem(
                name = file.name,
                uriString = file.absolutePath,
                size = if (isDirectory) 0L else file.length(),
                isDirectory = isDirectory
            )
            sharedItems.add(item)
            addLog("Added ${if (isDirectory) "folder" else "file"}: ${item.name}")
        }
    }
    val pendingApprovals = mutableStateListOf<PendingConnection>()
    val activeSessions = mutableStateListOf<ActiveSession>()

    // Thread Locks
    private val lock = Any()

    init {
        detectLocalIp()
    }

    fun detectLocalIp() {
        localIp = getLocalIpAddress()
    }

    fun addLog(message: String) {
        val sanitizedMessage = message.replace('\r', ' ').replace('\n', ' ')
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val formattedLog = "[$time] $sanitizedMessage"
        synchronized(lock) {
            logs.add(0, formattedLog) // Add to top of logs
            // Keep logs capped at 1000 items to prevent memory issues
            if (logs.size > 1000) {
                logs.removeAt(logs.size - 1)
            }
        }
    }

    fun clearLogs() {
        synchronized(lock) {
            logs.clear()
        }
    }

    fun addPending(conn: PendingConnection) {
        synchronized(lock) {
            pendingApprovals.removeAll { it.ipAddress == conn.ipAddress }
            pendingApprovals.add(conn)
        }
    }

    fun removePending(token: String) {
        synchronized(lock) {
            pendingApprovals.removeAll { it.token == token }
        }
    }

    fun approvePending(token: String) {
        synchronized(lock) {
            val conn = pendingApprovals.find { it.token == token }
            if (conn != null) {
                pendingApprovals.remove(conn)
                activeSessions.removeAll { it.ipAddress == conn.ipAddress }
                activeSessions.add(ActiveSession(token, conn.ipAddress, conn.userAgent))
            }
        }
    }

    fun addActive(token: String, ipAddress: String, userAgent: String) {
        synchronized(lock) {
            activeSessions.removeAll { it.ipAddress == ipAddress }
            activeSessions.add(ActiveSession(token, ipAddress, userAgent))
        }
    }

    fun updateSessionActivity(token: String, ipAddress: String) {
        synchronized(lock) {
            val sessionIndex = activeSessions.indexOfFirst { it.token == token && it.ipAddress == ipAddress }
            if (sessionIndex != -1) {
                val old = activeSessions[sessionIndex]
                activeSessions[sessionIndex] = old.copy(lastSeen = System.currentTimeMillis())
            }
        }
    }

    fun cleanExpiredSessions(expiryMs: Long) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            activeSessions.removeAll { now - it.lastSeen > expiryMs }
            pendingApprovals.removeAll { now - it.timestamp > expiryMs }
        }
    }

    fun removeActive(ipAddress: String) {
        synchronized(lock) {
            activeSessions.removeAll { it.ipAddress == ipAddress }
        }
    }

    fun clearActiveAndPending() {
        synchronized(lock) {
            activeSessions.clear()
            pendingApprovals.clear()
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            val candidateIps = mutableListOf<Pair<String, String>>() // Interface Name to IP
            
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (host != null) {
                            candidateIps.add(Pair(iface.name.lowercase(), host))
                        }
                    }
                }
            }

            // Prioritize Wi-Fi and Ethernet
            for (candidate in candidateIps) {
                val name = candidate.first
                if (name.contains("wlan") || name.contains("wlp") || name.contains("eth") || name.contains("enp") || name.contains("wl")) {
                    return candidate.second
                }
            }
            
            // Return first found IPv4 address as fallback
            return candidateIps.firstOrNull()?.second
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }
}
