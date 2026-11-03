package com.fengios.mail

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Account(
    val email: String, val password: String, val name: String,
    val imapHost: String, val imapPort: Int, val smtpHost: String, val smtpPort: Int
)

enum class Category(val label: String) {
    Primary("Primary"), Transactions("Transactions"), Updates("Updates"), Promotions("Promotions")
}

data class Box(val path: String, val title: String, val kind: String)

data class MailItem(
    val uid: Long, val box: String, val fromName: String, val fromAddr: String,
    val subject: String, val preview: String, val date: Long,
    val unread: Boolean, val flagged: Boolean, val category: Category, val hasAttachment: Boolean
)

data class MailBody(val html: String, val to: String, val cc: String, val attachments: List<String>, val messageId: String)

data class Draft(
    val to: String = "", val cc: String = "", val subject: String = "",
    val body: String = "", val inReplyTo: String? = null
)

data class Provider(val name: String, val imap: String, val imapPort: Int, val smtp: String, val smtpPort: Int, val hint: String)

val Providers = listOf(
    Provider("Gmail", "imap.gmail.com", 993, "smtp.gmail.com", 465,
        "Gmail needs an App Password: Google Account > Security > 2-Step Verification > App passwords."),
    Provider("iCloud", "imap.mail.me.com", 993, "smtp.mail.me.com", 587,
        "Use an app-specific password from appleid.apple.com."),
    Provider("Yahoo", "imap.mail.yahoo.com", 993, "smtp.mail.yahoo.com", 465,
        "Use an app password from Yahoo Account Security."),
    Provider("Other", "", 993, "", 465, "Enter your IMAP and SMTP servers.")
)

object Vault {
    private var cached: SharedPreferences? = null

    // One shared instance: creating encrypted prefs repeatedly (and from several threads) is slow and can fail
    @Synchronized
    private fun p(c: Context): SharedPreferences {
        cached?.let { return it }
        val ctx = c.applicationContext
        fun make(): SharedPreferences = EncryptedSharedPreferences.create(
            ctx, "mail_secure",
            MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val sp = try { make() } catch (e: Exception) {
            // Unreadable keys: reset the secure store (the user signs in again) instead of crashing
            try { ctx.deleteSharedPreferences("mail_secure") } catch (x: Exception) { }
            try {
                val ks = java.security.KeyStore.getInstance("AndroidKeyStore")
                ks.load(null)
                ks.deleteEntry("_androidx_security_master_key_")
            } catch (x: Exception) { }
            make()
        }
        cached = sp
        return sp
    }

    private fun <T> safe(d: T, f: () -> T): T = try { f() } catch (e: Exception) { d }

    private fun toJson(a: Account) = JSONObject().put("e", a.email).put("p", a.password).put("n", a.name)
        .put("ih", a.imapHost).put("ip", a.imapPort).put("sh", a.smtpHost).put("sp", a.smtpPort)

    private fun fromJson(j: JSONObject) = Account(j.getString("e"), j.getString("p"), j.getString("n"),
        j.getString("ih"), j.getInt("ip"), j.getString("sh"), j.getInt("sp"))

    fun all(c: Context): List<Account> = safe(emptyList()) {
        val s = p(c).getString("accs", null)
        if (s == null) emptyList() else {
            val arr = JSONArray(s)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }
    }

    private fun saveAll(c: Context, list: List<Account>) = safe(Unit) {
        val arr = JSONArray(); list.forEach { arr.put(toJson(it)) }
        p(c).edit().putString("accs", arr.toString()).apply()
    }

    fun activeEmail(c: Context): String? = safe(null) { p(c).getString("active", null) }
    fun active(c: Context): Account? { val l = all(c); return l.firstOrNull { it.email == activeEmail(c) } ?: l.firstOrNull() }
    fun setActive(c: Context, email: String) = safe(Unit) { p(c).edit().putString("active", email).apply() }

    fun add(c: Context, a: Account) {
        saveAll(c, all(c).filter { it.email != a.email } + a)
        setActive(c, a.email)
    }

    fun remove(c: Context, email: String) {
        val rest = all(c).filter { it.email != email }
        saveAll(c, rest)
        safe(Unit) { p(c).edit().remove("last_$email").apply() }
        if (activeEmail(c) == email) rest.firstOrNull()?.let { setActive(c, it.email) }
    }

    fun lastUid(c: Context, email: String): Long = safe(0L) { p(c).getLong("last_$email", 0L) }
    fun setLastUid(c: Context, email: String, v: Long) = safe(Unit) { p(c).edit().putLong("last_$email", v).apply() }
    fun instant(c: Context): Boolean = safe(true) { p(c).getBoolean("instant", true) }
    fun setInstant(c: Context, on: Boolean) = safe(Unit) { p(c).edit().putBoolean("instant", on).apply() }
}

// Last known mail is kept on disk so screens open instantly while fresh data loads
object Cache {
    private fun file(c: Context, key: String) = java.io.File(c.filesDir, "c_" + key.hashCode() + ".json")

    fun save(c: Context, email: String, box: String, items: List<MailItem>) {
        try {
            val arr = JSONArray()
            items.forEach {
                arr.put(JSONObject().put("u", it.uid).put("b", it.box).put("fn", it.fromName).put("fa", it.fromAddr)
                    .put("s", it.subject).put("p", it.preview).put("d", it.date).put("un", it.unread)
                    .put("fl", it.flagged).put("c", it.category.name).put("at", it.hasAttachment))
            }
            file(c, "$email|$box").writeText(arr.toString())
        } catch (e: Exception) { }
    }

    fun load(c: Context, email: String, box: String): List<MailItem> = try {
        val f = file(c, "$email|$box")
        if (!f.exists()) emptyList() else {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map {
                val j = arr.getJSONObject(it)
                MailItem(j.getLong("u"), j.getString("b"), j.getString("fn"), j.getString("fa"), j.getString("s"),
                    j.getString("p"), j.getLong("d"), j.getBoolean("un"), j.getBoolean("fl"),
                    Category.valueOf(j.getString("c")), j.getBoolean("at"))
            }
        }
    } catch (e: Exception) { emptyList() }

    fun saveBoxes(c: Context, email: String, list: List<Box>) {
        try {
            val arr = JSONArray()
            list.forEach { arr.put(JSONObject().put("p", it.path).put("t", it.title).put("k", it.kind)) }
            file(c, "boxes|$email").writeText(arr.toString())
        } catch (e: Exception) { }
    }

    fun loadBoxes(c: Context, email: String): List<Box> = try {
        val f = file(c, "boxes|$email")
        if (!f.exists()) emptyList() else {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { val j = arr.getJSONObject(it); Box(j.getString("p"), j.getString("t"), j.getString("k")) }
        }
    } catch (e: Exception) { emptyList() }

    fun clear(c: Context) { c.filesDir.listFiles()?.filter { it.name.startsWith("c_") }?.forEach { it.delete() } }
}

fun fmtDate(t: Long): String {
    val now = Calendar.getInstance()
    val c = Calendar.getInstance().apply { timeInMillis = t }
    val sameYear = now.get(Calendar.YEAR) == c.get(Calendar.YEAR)
    val pattern = when {
        sameYear && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR) -> "h:mm a"
        sameYear -> "MMM d"
        else -> "M/d/yy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(t))
}
