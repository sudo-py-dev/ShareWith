package com.share.with

import android.content.Context
import com.share.with.R
import java.io.File
import java.util.Locale

object WebTemplates {
    private var styleCss = ""
    private var loginHtml = ""
    private var waitingHtml = ""
    private var rejectedHtml = ""
    private var filesHtml = ""
    private var loaded = false

    fun loadTemplates(context: Context) {
        if (loaded) return
        try {
            styleCss = context.assets.open("style.css").bufferedReader().use { it.readText() }
            loginHtml = context.assets.open("login.html").bufferedReader().use { it.readText() }
            waitingHtml = context.assets.open("waiting.html").bufferedReader().use { it.readText() }
            rejectedHtml = context.assets.open("rejected.html").bufferedReader().use { it.readText() }
            filesHtml = context.assets.open("files.html").bufferedReader().use { it.readText() }
            loaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getStyleCss(): String {
        return styleCss
    }

    private fun getDirectionHtml(): String {
        return if (AppState.selectedLanguage == "he" || AppState.selectedLanguage == "ar") "dir=\"rtl\"" else "dir=\"ltr\""
    }

    fun loginPage(context: Context, error: String?, csrfToken: String): String {
        val errorHtml = if (error != null) "<div class=\"error-box\">${escapeHtml(error)}</div>" else ""
        return loginHtml
            .replace("{dir}", getDirectionHtml())
            .replace("{title}", escapeHtml(context.getString(R.string.web_login_title)))
            .replace("{appName}", escapeHtml(context.getString(R.string.app_name)))
            .replace("{subtitle}", escapeHtml(context.getString(R.string.web_login_title)))
            .replace("{errorHtml}", errorHtml)
            .replace("{label}", escapeHtml(context.getString(R.string.password_label)))
            .replace("{placeholder}", escapeHtml(context.getString(R.string.web_login_password_placeholder)))
            .replace("{button}", escapeHtml(context.getString(R.string.web_login_button)))
            .replace("{csrfToken}", escapeHtml(csrfToken))
    }

    fun waitingPage(context: Context, token: String): String {
        return waitingHtml
            .replace("{dir}", getDirectionHtml())
            .replace("{title}", escapeHtml(context.getString(R.string.web_waiting_approval_title)))
            .replace("{message}", escapeHtml(context.getString(R.string.web_waiting_approval_message)))
            .replace("{rejectedTitle}", escapeHtml(context.getString(R.string.web_rejected_title)))
            .replace("{rejectedMessage}", escapeHtml(context.getString(R.string.web_rejected_message)))
            .replace("{token}", escapeHtml(token))
    }

    fun rejectedPage(context: Context): String {
        return rejectedHtml
            .replace("{dir}", getDirectionHtml())
            .replace("{title}", escapeHtml(context.getString(R.string.web_rejected_title)))
            .replace("{message}", escapeHtml(context.getString(R.string.web_rejected_message)))
    }

    data class WebFileEntry(
        val name: String,
        val size: Long,
        val isDirectory: Boolean,
        val browseUrl: String?,
        val downloadUrl: String
    )

    fun fileListPage(
        context: Context, 
        files: List<WebFileEntry>, 
        breadcrumbs: List<Pair<String, String>>
    ): String {
        val emptyListLabel = context.getString(R.string.web_file_list_empty)
        val directoryLabel = context.getString(R.string.web_directory_label)
        val fileLabel = context.getString(R.string.web_file_label)
        val downloadFolderNote = context.getString(R.string.web_download_folder_note)
        val typeLabel = context.getString(R.string.web_type_label)
        val sizeLabel = context.getString(R.string.web_size_label)
        val downloadButtonLabel = context.getString(R.string.web_download_button)

        val itemsHtml = StringBuilder()
        if (files.isEmpty()) {
            itemsHtml.append("<div class=\"empty-state\">${escapeHtml(emptyListLabel)}</div>")
        } else {
            files.forEach { file ->
                val isDir = file.isDirectory
                val extension = file.name.substringAfterLast('.', "").lowercase()
                
                // Determine file item layout class and icon
                val (itemClass, icon) = when {
                    isDir -> Pair("dir", "📁")
                    extension in listOf("jpg", "jpeg", "png", "gif", "webp", "svg") -> Pair("image", "🖼️")
                    extension in listOf("mp4", "mkv", "avi", "mov", "webm") -> Pair("video", "🎥")
                    extension in listOf("mp3", "wav", "flac", "ogg", "m4a") -> Pair("audio", "🎵")
                    extension in listOf("zip", "rar", "tar", "gz", "7z", "iso") -> Pair("archive", "📦")
                    extension in listOf("pdf") -> Pair("doc", "📕")
                    extension in listOf("txt", "md", "json", "xml", "html", "js", "ts", "kt", "css") -> Pair("doc", "📝")
                    extension in listOf("apk") -> Pair("doc", "🤖")
                    extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx") -> Pair("doc", "💼")
                    else -> Pair("other", "📄")
                }
                
                val typeName = if (isDir) directoryLabel else fileLabel
                val sizeText = if (isDir) {
                    downloadFolderNote
                } else {
                    formatFileSize(file.size, sizeLabel)
                }

                val actionButton = if (isDir && file.browseUrl != null) {
                    """
                    <div style="display:flex; gap:8px;">
                        <a href="${file.browseUrl}" class="download-btn" style="background:var(--success);">Open</a>
                        <a href="${file.downloadUrl}" class="download-btn">${escapeHtml(downloadButtonLabel)} ZIP</a>
                    </div>
                    """.trimIndent()
                } else {
                    """<a href="${file.downloadUrl}" class="download-btn">${escapeHtml(downloadButtonLabel)}</a>"""
                }

                itemsHtml.append("""
                    <div class="file-item $itemClass">
                        <div class="file-info">
                            <span class="file-icon">$icon</span>
                            <div class="file-details">
                                <div class="file-name" title="${escapeHtml(file.name)}">${escapeHtml(file.name)}</div>
                                <div class="file-meta">
                                    ${escapeHtml(typeLabel)}: ${escapeHtml(typeName)} &bull; ${escapeHtml(sizeLabel)}: ${escapeHtml(sizeText)}
                                </div>
                            </div>
                        </div>
                        $actionButton
                    </div>
                """.trimIndent())
            }
        }

        val breadcrumbsHtml = breadcrumbs.joinToString(" <span>/</span> ") { (name, url) ->
            if (url.isEmpty()) "<span>${escapeHtml(name)}</span>"
            else "<a href=\"$url\">${escapeHtml(name)}</a>"
        }

        val searchBarHtml = if (files.isNotEmpty()) {
            """
            <div class="search-wrapper">
                <span class="search-icon">🔍</span>
                <input type="text" id="searchInput" class="search-input" placeholder="Search files..." onkeyup="filterFiles()">
            </div>
            """.trimIndent()
        } else {
            ""
        }

        return filesHtml
            .replace("{dir}", getDirectionHtml())
            .replace("{title}", escapeHtml(context.getString(R.string.web_file_list_title)))
            .replace("{appName}", escapeHtml(context.getString(R.string.app_name)))
            .replace("{breadcrumbsHtml}", breadcrumbsHtml)
            .replace("{searchBarHtml}", searchBarHtml)
            .replace("{itemsHtml}", itemsHtml.toString())
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;")
    }

    private fun formatFileSize(size: Long, sizeLabel: String): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val formattedValue = String.format(Locale.US, "%.2f", size / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formattedValue ${units[digitGroups]}"
    }
}
