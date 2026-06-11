package com.share.with

import android.content.Context
import android.content.Intent
import android.os.Process
import kotlin.system.exitProcess

class GlobalExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        try {
            val intent =
                Intent(context, CrashActivity::class.java).apply {
                    putExtra("error_message", throwable.localizedMessage ?: "Unknown Error")
                    val sw = java.io.StringWriter()
                    throwable.printStackTrace(java.io.PrintWriter(sw))
                    putExtra("stack_trace", sw.toString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            context.startActivity(intent)

            // Kill the current process
            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (e: Exception) {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun initialize(context: Context) {
            val handler = GlobalExceptionHandler(context.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }
    }
}
