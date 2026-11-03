package com.fengios.mail

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

// Anti-tamper: the app only runs when it carries the developer's signature and is not a debuggable repack.
// A re-signed or patched copy has a different signature and refuses to start.
object Guard {
    @Volatile
    var ok = true

    private val part = listOf(
        "b9033c5461bb6535", "f23c9a8d4444f7f2",
        "f64d8c98ddecc384", "520954815f91b779"
    )

    fun verify(ctx: Context): Boolean {
        val good = try { signatureOk(ctx) && !debuggable(ctx) } catch (e: Throwable) { false }
        ok = good
        return good
    }

    private fun debuggable(ctx: Context): Boolean =
        (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun signatureOk(ctx: Context): Boolean {
        val pm = ctx.packageManager
        val sigs: Array<Signature> = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners ?: return false
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES).signatures ?: return false
        }
        if (sigs.size != 1) return false
        val hash = MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray())
        val hex = hash.joinToString("") { "%02x".format(it) }
        return hex == part.joinToString("")
    }
}
