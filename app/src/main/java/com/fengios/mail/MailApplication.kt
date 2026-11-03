package com.fengios.mail

import android.app.Application
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

// Saves the stack trace of a crash so the next launch can show what went wrong
class MailApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Guard.verify(this)
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                File(filesDir, "crash.txt").writeText("[" + t.name + "] " + sw.toString().lines().take(14).joinToString("\n"))
            } catch (x: Throwable) { }
            old?.uncaughtException(t, e)
        }
    }
}
