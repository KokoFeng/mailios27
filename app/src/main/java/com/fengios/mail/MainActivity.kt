package com.fengios.mail

import android.Manifest
import android.app.AlertDialog
import java.io.File
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: MailVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Guard.verify(this)) {
            AlertDialog.Builder(this)
                .setTitle("App Modified")
                .setMessage("This copy of Mail is not the original and will not open. Please install the original from the developer (FengiOSonDroid).")
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ -> finishAffinity() }
                .show()
            return
        }
        enableEdgeToEdge()
        Notifier.ensure(this)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // If the app crashed last time, show the error first (plain dialog, before any UI can crash again)
        val crash = File(filesDir, "crash.txt")
        if (crash.exists()) {
            val txt = try { crash.readText().take(1200) } catch (e: Exception) { "unreadable" }
            crash.delete()
            AlertDialog.Builder(this)
                .setTitle("Mail crashed last time")
                .setMessage("Please screenshot this and send it to the developer:\n\n$txt")
                .setCancelable(false)
                .setPositiveButton("Continue") { _, _ -> begin(savedInstanceState) }
                .show()
        } else {
            begin(savedInstanceState)
        }
    }

    private fun begin(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) handle(intent)
        setContent {
            val sys = isSystemInDarkTheme()
            val dark = when (vm.theme) { "dark" -> true; "light" -> false; else -> sys }
            LaunchedEffect(dark) {
                val clear = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear)
                )
            }
            MailTheme(vm.theme) { MailApp(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    // Lets this app act as an email handler (mailto: links and "Share" > Mail)
    private fun handle(i: Intent?) {
        if (i == null) return
        when (i.action) {
            Intent.ACTION_SENDTO, Intent.ACTION_VIEW -> {
                val u: Uri = i.data ?: return
                if (u.scheme != "mailto") return
                val ssp = u.schemeSpecificPart
                val to = Uri.decode(ssp.substringBefore('?'))
                val params = ssp.substringAfter('?', "").split('&').filter { it.contains('=') }
                    .associate { it.substringBefore('=').lowercase() to Uri.decode(it.substringAfter('=')) }
                vm.startCompose(Draft(to = to, cc = params["cc"] ?: "", subject = params["subject"] ?: "", body = params["body"] ?: ""))
            }
            Intent.ACTION_SEND -> {
                vm.startCompose(Draft(
                    to = i.getStringArrayExtra(Intent.EXTRA_EMAIL)?.joinToString(", ") ?: "",
                    subject = i.getStringExtra(Intent.EXTRA_SUBJECT) ?: "",
                    body = i.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                ))
            }
        }
    }
}
