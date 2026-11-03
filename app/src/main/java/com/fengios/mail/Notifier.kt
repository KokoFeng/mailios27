package com.fengios.mail

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

object Notifier {
    private const val CH = "mail"
    const val SYNC = "sync"

    fun ensure(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "New Mail", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(SYNC, "Mail Sync", NotificationManager.IMPORTANCE_MIN))
    }

    fun show(c: Context, i: MailItem, acct: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensure(c)
        val pi = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_mail)
            .setContentTitle(i.fromName)
            .setContentText(i.subject)
            .setSubText(acct)
            .setStyle(NotificationCompat.BigTextStyle().bigText(i.subject + "\n" + i.preview))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        c.getSystemService(NotificationManager::class.java).notify(i.uid.toInt() + acct.hashCode(), n)
    }
}

// Notify only for mail newer than the last one we already told the user about
fun notifyFresh(c: Context, a: Account, items: List<MailItem>): List<MailItem> {
    val last = Vault.lastUid(c, a.email)
    val fresh = items.filter { it.uid > last && it.unread }
    if (last != 0L) fresh.forEach { Notifier.show(c, it, a.email) }
    if (items.isNotEmpty()) Vault.setLastUid(c, a.email, maxOf(last, items.maxOf { it.uid }))
    return fresh
}

class MailWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result {
        if (!Guard.ok) return Result.success()
        var failed = false
        for (a in Vault.all(applicationContext)) {
            try { notifyFresh(applicationContext, a, MailClient.unread(a)) } catch (e: Throwable) { failed = true }
        }
        return if (failed) Result.retry() else Result.success()
    }
}

object Scheduler {
    fun start(c: Context) {
        val req = PeriodicWorkRequestBuilder<MailWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(c).enqueueUniquePeriodicWork("mail_check", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun stop(c: Context) {
        WorkManager.getInstance(c).cancelUniqueWork("mail_check")
    }
}

// Foreground service that keeps an IMAP IDLE connection open per account for instant push
class IdleService : Service() {
    private var scope: CoroutineScope? = null
    private val stores = CopyOnWriteArrayList<javax.mail.Store>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        try { start() } catch (e: Throwable) { stopSelf(); START_NOT_STICKY }

    private fun start(): Int {
        if (!Guard.ok) { stopSelf(); return START_NOT_STICKY }
        Notifier.ensure(this)
        val n = NotificationCompat.Builder(this, Notifier.SYNC)
            .setSmallIcon(R.drawable.ic_mail)
            .setContentTitle("Mail")
            .setContentText("Listening for new mail")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        try {
            ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        stop()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = s
        for (a in Vault.all(this)) {
            s.launch {
                while (isActive) {
                    try {
                        MailClient.idleLoop(a, { stores.add(it) }) { items -> notifyFresh(applicationContext, a, items) }
                    } catch (e: Throwable) {
                        if (isActive) delay(30_000)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun stop() {
        scope?.cancel()
        val list = stores.toList()
        stores.clear()
        Thread { list.forEach { try { it.close() } catch (e: Exception) { } } }.start()
    }

    override fun onDestroy() { stop(); super.onDestroy() }

    companion object {
        // Starts or stops the service depending on accounts and the user's toggle
        fun sync(c: Context) {
            val i = Intent(c, IdleService::class.java)
            if (Vault.all(c).isNotEmpty() && Vault.instant(c)) ContextCompat.startForegroundService(c, i)
            else c.stopService(i)
        }
    }
}
