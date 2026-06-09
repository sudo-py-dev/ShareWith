package com.share.with

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
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
    private const val PREFS_NAME = "sharewith_settings"
    private var prefs: SharedPreferences? = null

    val blockedIps = mutableStateListOf<String>()

    fun blockIp(ip: String) {
        synchronized(lock) {
            if (!blockedIps.contains(ip)) {
                blockedIps.add(ip)
                saveBlockedIps()
            }
            pendingApprovals.removeAll { it.ipAddress == ip }
            activeSessions.removeAll { it.ipAddress == ip }
        }
    }

    fun unblockIp(ip: String) {
        synchronized(lock) {
            if (blockedIps.remove(ip)) {
                saveBlockedIps()
            }
        }
    }

    fun clearBlockedIps() {
        synchronized(lock) {
            blockedIps.clear()
            saveBlockedIps()
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

    fun updateTheme(theme: String) {
        selectedTheme = theme
        prefs?.edit()?.putString("theme", theme)?.apply()
    }

    fun updateLanguage(lang: String) {
        selectedLanguage = lang
        prefs?.edit()?.putString("language", lang)?.apply()
    }

    fun updatePortInput(port: String) {
        portInput = port
        prefs?.edit()?.putString("port_input", port)?.apply()
    }

    fun updateServerPort(port: Int) {
        serverPort = port
        prefs?.edit()?.putInt("server_port", port)?.apply()
    }

    fun updateSecurityMode(mode: SecurityMode) {
        securityMode = mode
        prefs?.edit()?.putString("security_mode", mode.name)?.apply()
    }

    fun updatePassword(pass: String) {
        password = pass
        prefs?.edit()?.putString("password", pass)?.apply()
    }

    fun addSharedItem(file: java.io.File, isDirectory: Boolean) {
        synchronized(lock) {
            val item = SharedItem(
                name = file.name,
                uriString = file.absolutePath,
                size = if (isDirectory) 0L else file.length(),
                isDirectory = isDirectory
            )
            sharedItems.add(item)
            saveSharedItems()
            addLog("Added ${if (isDirectory) "folder" else "file"}: ${item.name}")
        }
    }
    
    fun removeSharedItem(item: SharedItem) {
        synchronized(lock) {
            if (sharedItems.remove(item)) {
                saveSharedItems()
            }
        }
    }

    fun clearSharedItems() {
        synchronized(lock) {
            sharedItems.clear()
            saveSharedItems()
        }
    }

    val pendingApprovals = mutableStateListOf<PendingConnection>()
    val activeSessions = mutableStateListOf<ActiveSession>()

    // Thread Locks
    private val lock = Any()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSettings()
        detectLocalIp()
    }

    private fun loadSettings() {
        prefs?.let { p ->
            selectedTheme = p.getString("theme", "System") ?: "System"
            selectedLanguage = p.getString("language", "en") ?: "en"
            portInput = p.getString("port_input", "8080") ?: "8080"
            serverPort = p.getInt("server_port", 8080)
            
            val modeStr = p.getString("security_mode", SecurityMode.MANUAL_APPROVAL.name)
            securityMode = SecurityMode.entries.find { it.name == modeStr } ?: SecurityMode.MANUAL_APPROVAL
            
            password = p.getString("password", "") ?: ""
            
            // Blocked IPs
            try {
                val array = JSONArray(p.getString("blocked_ips", "[]"))
                blockedIps.clear()
                for (i in 0 until array.length()) {
                    blockedIps.add(array.getString(i))
                }
            } catch (e: Exception) {}

            // Shared Items
            try {
                val array = JSONArray(p.getString("shared_items", "[]"))
                sharedItems.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    sharedItems.add(SharedItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        uriString = obj.getString("uriString"),
                        size = obj.getLong("size"),
                        isDirectory = obj.getBoolean("isDirectory")
                    ))
                }
            } catch (e: Exception) {}
        }
    }

    private fun saveBlockedIps() {
        val array = JSONArray()
        blockedIps.forEach { array.put(it) }
        prefs?.edit()?.putString("blocked_ips", array.toString())?.apply()
    }

    private fun saveSharedItems() {
        val array = JSONArray()
        sharedItems.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("uriString", item.uriString)
            obj.put("size", item.size)
            obj.put("isDirectory", item.isDirectory)
            array.put(obj)
        }
        prefs?.edit()?.putString("shared_items", array.toString())?.apply()
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
