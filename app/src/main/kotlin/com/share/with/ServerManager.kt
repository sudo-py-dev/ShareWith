package com.share.with

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.request.header
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ServerManager {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var appContext: Context? = null
    private var scheduler: java.util.concurrent.ScheduledExecutorService? = null
    private val csrfTokens = ConcurrentHashMap<String, String>()
    private val loginAttempts = ConcurrentHashMap<String, Int>()
    private val lastAttemptTime = ConcurrentHashMap<String, Long>()

    val rejectedTokens = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private const val SESSION_COOKIE_NAME = "sharewith_session"
    private const val MAX_LOGIN_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 60_000L
    private const val SESSION_EXPIRY_MS = 3_600_000L // 1 hour
    private const val MAX_ZIP_DEPTH = 10

    internal fun isValidUuid(str: String): Boolean {
        return try {
            UUID.fromString(str)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isClientAuthorized(
        call: ApplicationCall,
        ip: String,
    ): Boolean {
        if (AppState.blockedIps.contains(ip)) return false

        return when (AppState.securityMode) {
            SecurityMode.NONE -> true
            SecurityMode.PASSWORD, SecurityMode.MANUAL_APPROVAL -> {
                val token = call.request.cookies[SESSION_COOKIE_NAME]
                if (token != null && isValidUuid(token)) {
                    val authorized = AppState.activeSessions.any { it.token == token && it.ipAddress == ip }
                    if (authorized) {
                        AppState.updateSessionActivity(token, ip)
                    }
                    authorized
                } else {
                    false
                }
            }
        }
    }

    fun start(
        context: Context,
        port: Int,
        onError: (Throwable) -> Unit,
    ) {
        AppState.serverStarting = true
        Thread {
            try {
                WebTemplates.loadTemplates(context)
                scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                scheduler?.scheduleWithFixedDelay({
                    AppState.cleanExpiredSessions(SESSION_EXPIRY_MS)
                    val now = System.currentTimeMillis()
                    val it1 = lastAttemptTime.entries.iterator()
                    while (it1.hasNext()) {
                        if (now - it1.next().value > LOCKOUT_DURATION_MS * 10) {
                            it1.remove()
                        }
                    }
                    val it2 = loginAttempts.entries.iterator()
                    while (it2.hasNext()) {
                        if (!lastAttemptTime.containsKey(it2.next().key)) {
                            it2.remove()
                        }
                    }
                }, 1, 1, java.util.concurrent.TimeUnit.MINUTES)

                appContext = context.applicationContext

                val cioServer =
                    embeddedServer(CIO, configure = {
                        connector {
                            this.port = port
                        }

                        if (AppState.isHttpsEnabled) {
                            val keyStore = loadKeyStore()
                            if (keyStore != null) {
                                sslConnector(
                                    keyStore = keyStore,
                                    keyAlias = keyStore.aliases().nextElement(),
                                    keyStorePassword = { AppState.keystorePassword.toCharArray() },
                                    privateKeyPassword = { AppState.keystorePassword.toCharArray() },
                                ) {
                                    this.port = AppState.httpsPort
                                }
                                AppState.addLog("HTTPS enabled on port ${AppState.httpsPort}")
                            } else {
                                AppState.addLog("HTTPS enabled but KeyStore failed to load. Falling back to HTTP only.")
                            }
                        }
                    }) {
                        install(io.ktor.server.plugins.defaultheaders.DefaultHeaders) {
                            header("X-Content-Type-Options", "nosniff")
                            header("X-Frame-Options", "DENY")
                            header("X-XSS-Protection", "1; mode=block")
                            header(
                                "Content-Security-Policy",
                                "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:;",
                            )
                        }

                        routing {
                            get("/style.css") {
                                call.respondText(WebTemplates.getStyleCss(), ContentType.Text.CSS)
                            }
                            get("/") {
                                val ctx = appContext
                                if (ctx == null) {
                                    call.respond(HttpStatusCode.InternalServerError, "Server Context is unavailable")
                                    return@get
                                }
                                val ip = call.request.local.remoteHost
                                if (AppState.blockedIps.contains(ip)) {
                                    call.respondText(WebTemplates.rejectedPage(ctx), ContentType.Text.Html)
                                    return@get
                                }
                                val userAgent = call.request.headers["User-Agent"] ?: "Unknown Device"

                                if (isClientAuthorized(call, ip)) {
                                    val idStr = call.request.queryParameters["id"]
                                    val subPath = call.request.queryParameters["subPath"]

                                    val webFiles = mutableListOf<WebTemplates.WebFileEntry>()
                                    val breadcrumbs = mutableListOf<Pair<String, String>>()
                                    breadcrumbs.add(Pair(ctx.getString(R.string.app_name), "/"))

                                    val query = call.request.queryParameters["q"]

                                    if (idStr.isNullOrEmpty()) {
                                        if (!query.isNullOrEmpty()) {
                                            for (item in AppState.sharedItems) {
                                                val rootFile = File(item.uriString)
                                                if (rootFile.exists()) {
                                                    searchFilesRecursively(rootFile, query, rootFile, item.id, webFiles)
                                                }
                                            }
                                        } else {
                                            for (item in AppState.sharedItems) {
                                                val browseUrl = if (item.isDirectory) "/?id=${item.id}" else null
                                                val downloadUrl = "/download?id=${item.id}"
                                                webFiles.add(
                                                    WebTemplates.WebFileEntry(
                                                        name = item.name,
                                                        size = item.size,
                                                        isDirectory = item.isDirectory,
                                                        browseUrl = browseUrl,
                                                        downloadUrl = downloadUrl,
                                                    ),
                                                )
                                            }
                                        }
                                    } else {
                                        val sharedItem = AppState.sharedItems.find { it.id == idStr }
                                        if (sharedItem == null || !sharedItem.isDirectory) {
                                            call.respond(HttpStatusCode.NotFound, "Shared directory not found")
                                            return@get
                                        }

                                        val rootFile = File(sharedItem.uriString)
                                        if (!rootFile.exists() || !rootFile.isDirectory) {
                                            call.respond(HttpStatusCode.NotFound, "Shared directory does not exist")
                                            return@get
                                        }

                                        breadcrumbs.add(Pair(sharedItem.name, "/?id=${sharedItem.id}"))

                                        val targetDir =
                                            if (subPath.isNullOrEmpty()) {
                                                rootFile
                                            } else {
                                                if (subPath.startsWith("/") || subPath.startsWith("\\") || subPath.contains("..")) {
                                                    AppState.addLog(
                                                        "Security alert! Directory traversal blocked from $ip seeking: $subPath",
                                                    )
                                                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                                                    return@get
                                                }
                                                val resolved = File(rootFile, subPath).canonicalFile
                                                val rootPath = rootFile.canonicalPath
                                                val normalizedRoot =
                                                    if (rootPath.endsWith(
                                                            File.separator,
                                                        )
                                                    ) {
                                                        rootPath
                                                    } else {
                                                        rootPath + File.separator
                                                    }
                                                val isChild =
                                                    resolved.canonicalPath.startsWith(normalizedRoot) ||
                                                        resolved.canonicalPath == rootPath
                                                if (!isChild || !resolved.isDirectory) {
                                                    AppState.addLog(
                                                        "Security alert! Directory traversal blocked from $ip seeking: $subPath",
                                                    )
                                                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                                                    return@get
                                                }

                                                val parts = subPath.split("/").filter { it.isNotEmpty() }
                                                var currentPath = ""
                                                for (part in parts) {
                                                    currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                                                    val encodedPath = java.net.URLEncoder.encode(currentPath, "UTF-8")
                                                    breadcrumbs.add(Pair(part, "/?id=${sharedItem.id}&subPath=$encodedPath"))
                                                }

                                                resolved
                                            }

                                        if (!query.isNullOrEmpty()) {
                                            searchFilesRecursively(targetDir, query, rootFile, sharedItem.id, webFiles)
                                        } else {
                                            val children = targetDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
                                            for (child in children) {
                                                val relativePath =
                                                    child.canonicalPath.removePrefix(
                                                        rootFile.canonicalPath,
                                                    ).removePrefix(File.separator)
                                                val urlPath = relativePath.replace(File.separatorChar, '/')
                                                val encodedPath = java.net.URLEncoder.encode(urlPath, "UTF-8")

                                                val browseUrl = if (child.isDirectory) "/?id=${sharedItem.id}&subPath=$encodedPath" else null
                                                val downloadUrl = "/download?id=${sharedItem.id}&subPath=$encodedPath"

                                                webFiles.add(
                                                    WebTemplates.WebFileEntry(
                                                        name = child.name,
                                                        size = if (child.isDirectory) 0L else child.length(),
                                                        isDirectory = child.isDirectory,
                                                        browseUrl = browseUrl,
                                                        downloadUrl = downloadUrl,
                                                    ),
                                                )
                                            }
                                        }
                                    }

                                    call.respondText(
                                        WebTemplates.fileListPage(ctx, webFiles, breadcrumbs, query),
                                        ContentType.Text.Html,
                                    )
                                    return@get
                                }

                                when (AppState.securityMode) {
                                    SecurityMode.PASSWORD -> {
                                        val csrfToken = UUID.randomUUID().toString()
                                        csrfTokens[ip] = csrfToken
                                        call.respondText(WebTemplates.loginPage(ctx, null, csrfToken), ContentType.Text.Html)
                                    }
                                    SecurityMode.MANUAL_APPROVAL -> {
                                        val tokenParam = call.request.queryParameters["token"]
                                        if (tokenParam != null && isValidUuid(tokenParam)) {
                                            if (rejectedTokens.contains(tokenParam)) {
                                                call.respondText(WebTemplates.rejectedPage(ctx), ContentType.Text.Html)
                                                return@get
                                            }
                                            val approvedSession = AppState.activeSessions.find { it.token == tokenParam && it.ipAddress == ip }
                                            if (approvedSession != null) {
                                                call.response.cookies.append(
                                                    SESSION_COOKIE_NAME,
                                                    tokenParam,
                                                    path = "/",
                                                    httpOnly = true,
                                                )
                                                call.respondRedirect("/")
                                                return@get
                                            }
                                            call.respondText(WebTemplates.waitingPage(ctx, tokenParam), ContentType.Text.Html)
                                        } else {
                                            val newToken = UUID.randomUUID().toString()
                                            AppState.addPending(PendingConnection(newToken, ip, userAgent))
                                            AppState.addLog("Access request from $ip ($userAgent) - Pending approval")
                                            showPendingConnectionNotification(ctx, ip, userAgent)
                                            call.respondText(WebTemplates.waitingPage(ctx, newToken), ContentType.Text.Html)
                                        }
                                    }
                                    else -> {
                                        call.respond(HttpStatusCode.Forbidden, "Access Denied")
                                    }
                                }
                            }

                            post("/login") {
                                val ctx = appContext
                                if (ctx == null) {
                                    call.respond(HttpStatusCode.InternalServerError, "Server Context is unavailable")
                                    return@post
                                }
                                val ip = call.request.local.remoteHost
                                val userAgent = call.request.headers["User-Agent"] ?: "Unknown Device"

                                val now = System.currentTimeMillis()
                                val attempts = loginAttempts[ip] ?: 0
                                val lastTime = lastAttemptTime[ip] ?: 0L

                                if (attempts >= MAX_LOGIN_ATTEMPTS && now - lastTime < LOCKOUT_DURATION_MS) {
                                    AppState.addLog("Rate limit exceeded for $ip")
                                    call.respond(HttpStatusCode.TooManyRequests, "Too many failed attempts. Please try again later.")
                                    return@post
                                }

                                val params = call.receiveParameters()
                                val pwd = params["password"]
                                val csrfIn = params["csrf_token"]

                                if (csrfIn == null || csrfIn != csrfTokens[ip]) {
                                    AppState.addLog("Security alert! CSRF mismatch from $ip")
                                    call.respond(HttpStatusCode.Forbidden, "Invalid request session")
                                    return@post
                                }
                                csrfTokens.remove(ip)

                                val passwordMatch =
                                    if (pwd != null && AppState.password.isNotEmpty()) {
                                        java.security.MessageDigest.isEqual(
                                            pwd.toByteArray(Charsets.UTF_8),
                                            AppState.password.toByteArray(Charsets.UTF_8),
                                        )
                                    } else {
                                        pwd == AppState.password
                                    }

                                if (passwordMatch) {
                                    loginAttempts.remove(ip)
                                    lastAttemptTime.remove(ip)
                                    val newToken = UUID.randomUUID().toString()
                                    AppState.addActive(newToken, ip, userAgent)
                                    AppState.addLog("Authentication successful for $ip ($userAgent)")
                                    call.response.cookies.append(SESSION_COOKIE_NAME, newToken, path = "/", httpOnly = true)
                                    call.respondRedirect("/")
                                } else {
                                    loginAttempts[ip] = attempts + 1
                                    lastAttemptTime[ip] = now
                                    AppState.addLog("Authentication failed for $ip ($userAgent) - Invalid password")
                                    val errorMsg = ctx.getString(R.string.web_login_error)
                                    val nextCsrf = UUID.randomUUID().toString()
                                    csrfTokens[ip] = nextCsrf
                                    call.respondText(
                                        WebTemplates.loginPage(ctx, errorMsg, nextCsrf),
                                        ContentType.Text.Html,
                                    )
                                }
                            }

                            get("/approve-check") {
                                val token = call.request.queryParameters["token"]
                                val ip = call.request.local.remoteHost
                                val status =
                                    when {
                                        token == null || !isValidUuid(token) -> "invalid"
                                        AppState.activeSessions.any { it.token == token && it.ipAddress == ip } -> {
                                            call.response.cookies.append(SESSION_COOKIE_NAME, token, path = "/", httpOnly = true)
                                            "approved"
                                        }
                                        AppState.blockedIps.contains(ip) -> "rejected"
                                        rejectedTokens.contains(token) -> "rejected"
                                        else -> "pending"
                                    }
                                call.respondText("{\"status\":\"$status\"}", ContentType.Application.Json)
                            }

                            get("/download") {
                                val ip = call.request.local.remoteHost
                                val ctx = appContext
                                if (ctx == null) {
                                    call.respond(HttpStatusCode.InternalServerError, "Service Context is unavailable")
                                    return@get
                                }

                                if (!isClientAuthorized(call, ip)) {
                                    call.respond(HttpStatusCode.Forbidden, "Access Denied")
                                    return@get
                                }

                                val idStr = call.request.queryParameters["id"]
                                val subPath = call.request.queryParameters["subPath"]
                                val isPreview = call.request.queryParameters["preview"] == "true"
                                if (idStr.isNullOrEmpty()) {
                                    call.respond(HttpStatusCode.BadRequest, "Missing or invalid file ID")
                                    return@get
                                }

                                val sharedItem = AppState.sharedItems.find { it.id == idStr }
                                if (sharedItem == null) {
                                    call.respond(HttpStatusCode.NotFound, "Shared file not found")
                                    return@get
                                }

                                val rootFile = File(sharedItem.uriString)
                                if (!rootFile.exists()) {
                                    call.respond(HttpStatusCode.NotFound, "Shared item does not exist")
                                    return@get
                                }

                                val targetFile =
                                    if (subPath.isNullOrEmpty()) {
                                        rootFile
                                    } else {
                                        if (subPath.startsWith("/") || subPath.startsWith("\\") || subPath.contains("..")) {
                                            AppState.addLog("Security alert! Directory traversal blocked from $ip seeking: $subPath")
                                            call.respond(HttpStatusCode.Forbidden, "Access Denied")
                                            return@get
                                        }
                                        val resolved = File(rootFile, subPath).canonicalFile
                                        val rootPath = rootFile.canonicalPath
                                        val normalizedRoot =
                                            if (rootPath.endsWith(
                                                    File.separator,
                                                )
                                            ) {
                                                rootPath
                                            } else {
                                                rootPath + File.separator
                                            }
                                        val isChild = resolved.canonicalPath.startsWith(normalizedRoot) || resolved.canonicalPath == rootPath
                                        if (!isChild) {
                                            AppState.addLog("Security alert! Directory traversal blocked from $ip seeking: $subPath")
                                            call.respond(HttpStatusCode.Forbidden, "Access Denied")
                                            return@get
                                        }
                                        resolved
                                    }

                                if (!targetFile.exists()) {
                                    call.respond(HttpStatusCode.NotFound, "File does not exist")
                                    return@get
                                }

                                if (targetFile.isFile) {
                                    AppState.addLog("File ${if (isPreview) "previewed" else "downloaded"}: ${targetFile.name} by $ip")
                                    val encodedName = java.net.URLEncoder.encode(targetFile.name, "UTF-8").replace("+", "%20")
                                    val disposition = if (isPreview) "inline" else "attachment"
                                    call.response.header(
                                        HttpHeaders.ContentDisposition,
                                        "$disposition; filename=\"$encodedName\"; filename*=UTF-8''$encodedName",
                                    )
                                    call.respondFile(targetFile)
                                } else if (targetFile.isDirectory) {
                                    AppState.addLog("Directory download (as ZIP) started: ${targetFile.name} by $ip")
                                    val encodedName = java.net.URLEncoder.encode("${targetFile.name}.zip", "UTF-8").replace("+", "%20")
                                    call.response.header(
                                        HttpHeaders.ContentDisposition,
                                        "attachment; filename=\"$encodedName\"; filename*=UTF-8''$encodedName",
                                    )
                                    call.respondOutputStream(ContentType.Application.Zip) {
                                        val fileCount = java.util.concurrent.atomic.AtomicInteger(0)
                                        ZipOutputStream(this).use { zos ->
                                            zipDirectory(targetFile, "", zos, 0, fileCount, rootFile)
                                        }
                                    }
                                    AppState.addLog("Directory download (as ZIP) completed: ${targetFile.name} by $ip")
                                }
                            }
                        }
                    }
                cioServer.start(wait = false)
                server = cioServer
                AppState.serverRunning = true
                AppState.serverPort = port
                AppState.addLog("Server started on port $port")
            } catch (e: Throwable) {
                AppState.serverRunning = false
                onError(e)
            } finally {
                AppState.serverStarting = false
            }
        }.start()
    }

    private fun loadKeyStore(): KeyStore? {
        val uri = AppState.keystoreUri
        val password = AppState.keystorePassword
        if (uri.isEmpty()) return null

        return try {
            val keyStore = KeyStore.getInstance("PKCS12")
            appContext?.contentResolver?.openInputStream(uri.toUri())?.use {
                keyStore.load(it, password.toCharArray())
            }
            keyStore
        } catch (e: Exception) {
            AppState.addLog("Failed to load KeyStore: ${e.message}")
            null
        }
    }

    fun cancelRequestNotification() {
        val notificationManager = appContext?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(2002)
    }

    fun rejectPending(token: String) {
        val conn = AppState.pendingApprovals.find { it.token == token }
        if (conn != null) {
            AppState.blockIp(conn.ipAddress)
        }

        val notificationManager = appContext?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(2002)

        rejectedTokens.add(token)
        AppState.removePending(token)

        scheduler?.schedule({
            rejectedTokens.remove(token)
        }, 60, java.util.concurrent.TimeUnit.SECONDS)
    }

    fun stop() {
        AppState.serverStopping = true
        Thread {
            try {
                scheduler?.shutdownNow()
                scheduler = null
                server?.stop(1000, 2000)
                server = null
                AppState.serverRunning = false
                AppState.clearActiveAndPending()
                rejectedTokens.clear()
                csrfTokens.clear()
                loginAttempts.clear()
                lastAttemptTime.clear()
                appContext = null
                AppState.addLog("Server stopped")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                AppState.serverStopping = false
            }
        }.start()
    }

    private fun showPendingConnectionNotification(
        context: Context,
        ip: String,
        userAgent: String,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager == null) return

        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                2,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val channelId = "sharewith_request_channel"
        val channel =
            NotificationChannel(
                channelId,
                context.getString(R.string.notif_channel_request_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_request_desc)
            }
        notificationManager.createNotificationChannel(channel)

        val notification =
            NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.notif_request_title, ip))
                .setContentText(context.getString(R.string.notif_request_desc, userAgent))
                .setSmallIcon(android.R.drawable.ic_menu_help)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

        notificationManager.notify(2002, notification)
    }

    private fun searchFilesRecursively(
        dir: File,
        query: String,
        rootFile: File,
        sharedItemId: String,
        results: MutableList<WebTemplates.WebFileEntry>,
        depth: Int = 0,
    ) {
        if (depth > 20 || results.size >= 500) return
        val files =
            dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: return

        val rootPath = rootFile.canonicalPath
        val normalizedRoot = if (rootPath.endsWith(File.separator)) rootPath else rootPath + File.separator

        for (file in files) {
            if (results.size >= 500) break

            // Skip symbolic links and files outside root for absolute safety
            val canonical =
                try {
                    file.canonicalPath
                } catch (e: Exception) {
                    continue
                }
            if (java.nio.file.Files.isSymbolicLink(file.toPath())) continue
            if (!canonical.startsWith(normalizedRoot) && canonical != rootPath) continue

            if (file.name.contains(query, ignoreCase = true)) {
                val relativePath = canonical.removePrefix(rootPath).removePrefix(File.separator)
                val urlPath = relativePath.replace(File.separatorChar, '/')
                val encodedPath = java.net.URLEncoder.encode(urlPath, "UTF-8")

                val isDir = file.isDirectory
                val browseUrl = if (isDir) "/?id=$sharedItemId&subPath=$encodedPath" else null
                val downloadUrl = "/download?id=$sharedItemId&subPath=$encodedPath"

                val parent = file.parentFile
                val displayPath =
                    if (parent != null &&
                        parent.canonicalPath != rootPath
                    ) {
                        parent.canonicalPath.removePrefix(rootPath)
                            .removePrefix(File.separator).replace(File.separatorChar, '/')
                    } else {
                        ""
                    }

                results.add(
                    WebTemplates.WebFileEntry(
                        name = file.name,
                        size = if (isDir) 0L else file.length(),
                        isDirectory = isDir,
                        browseUrl = browseUrl,
                        downloadUrl = downloadUrl,
                        path = displayPath,
                    ),
                )
            }

            if (file.isDirectory) {
                searchFilesRecursively(file, query, rootFile, sharedItemId, results, depth + 1)
            }
        }
    }

    private const val MAX_ZIP_FILES = 5000

    private fun zipDirectory(
        dir: File,
        relativePrefix: String,
        zos: ZipOutputStream,
        depth: Int,
        fileCount: java.util.concurrent.atomic.AtomicInteger,
        rootFile: File,
    ) {
        if (depth > MAX_ZIP_DEPTH || fileCount.get() > MAX_ZIP_FILES) return
        val files = dir.listFiles() ?: return
        val buffer = ByteArray(64 * 1024)

        val rootPath = rootFile.canonicalPath
        val normalizedRoot = if (rootPath.endsWith(File.separator)) rootPath else rootPath + File.separator

        for (file in files) {
            if (fileCount.incrementAndGet() > MAX_ZIP_FILES) break

            // Security checks: canonical path must be within root, skip symlinks
            val canonical =
                try {
                    file.canonicalPath
                } catch (e: Exception) {
                    continue
                }
            if (java.nio.file.Files.isSymbolicLink(file.toPath())) continue
            if (!canonical.startsWith(normalizedRoot) && canonical != rootPath) continue

            val entryName = if (relativePrefix.isEmpty()) file.name else "$relativePrefix/${file.name}"
            if (file.isDirectory) {
                zipDirectory(file, entryName, zos, depth + 1, fileCount, rootFile)
            } else {
                try {
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    file.inputStream().use { fis ->
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            zos.write(buffer, 0, bytesRead)
                        }
                    }
                    zos.closeEntry()
                } catch (e: Exception) {
                    // Skip files that cannot be read
                }
            }
        }
    }
}
