package com.fengios.mail

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.mail.Flags

enum class Screen { Boxes, List, Read, Compose }

class MailVM(private val app: Application) : AndroidViewModel(app) {
    var accounts by mutableStateOf(Vault.all(app))
    var account by mutableStateOf(Vault.active(app))
    var adding by mutableStateOf(false)
    var instant by mutableStateOf(Vault.instant(app))
    private val ui = app.getSharedPreferences("ui", Context.MODE_PRIVATE)
    var theme by mutableStateOf(ui.getString("theme", "system") ?: "system")
    var wallpaperUri by mutableStateOf(ui.getString("wall", null))
    var wallpaper by mutableStateOf<ImageBitmap?>(null)
    var glassClarity by mutableStateOf(ui.getFloat("clarity", 0.25f))
    var glassShine by mutableStateOf(ui.getFloat("shine", 0.7f))
    var alertTitle by mutableStateOf<String?>(null)
    var alertMsg by mutableStateOf("")
    var alertLink by mutableStateOf<String?>(null)
    var confirmRemove by mutableStateOf(false)
    var showCredit by mutableStateOf(false)
    var avatars by mutableStateOf<Map<String, ImageBitmap>>(emptyMap())
    var selecting by mutableStateOf(false)
    var selected by mutableStateOf<Set<Long>>(emptySet())
    var screen by mutableStateOf(Screen.Boxes)
    var boxes by mutableStateOf<List<Box>>(emptyList())
    var box by mutableStateOf<Box?>(null)
    var items by mutableStateOf<List<MailItem>>(emptyList())
    var loading by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    var category by mutableStateOf<Category?>(null)
    var unreadOnly by mutableStateOf(false)
    var flaggedOnly by mutableStateOf(false)
    var query by mutableStateOf("")
    var opened by mutableStateOf<MailItem?>(null)
    var body by mutableStateOf<MailBody?>(null)
    var draft by mutableStateOf(Draft())
    var updated by mutableStateOf(0L)
    private var cameFrom = Screen.Boxes

    val shown: List<MailItem>
        get() = items.filter {
            (category == null || it.category == category) &&
                (!unreadOnly || it.unread) && (!flaggedOnly || it.flagged)
        }

    init {
        loadWallpaper()
        loadAvatars()
        if (account != null) { Scheduler.start(app); IdleService.sync(app); loadBoxes() }
    }

    private fun isNetwork(msg: String): Boolean {
        val m = msg.lowercase()
        return listOf("timed out", "bye", "unable to connect", "unknownhost", "connection", "socket", "unreachable", "reset").any { m.contains(it) }
    }

    // quiet = background refresh: show a small banner instead of an alert
    private fun run(quiet: Boolean = false, block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: Exception) {
                val msg = e.message ?: e.javaClass.simpleName
                if (quiet) toast = "!" + (if (isNetwork(msg)) "Can't reach the mail server" else msg.take(80)) else showError(msg)
                loading = false; busy = false
            }
        }
    }

    private fun resetView() {
        boxes = emptyList(); items = emptyList(); box = null; opened = null; body = null
        screen = Screen.Boxes; query = ""
    }

    private fun showError(msg: String) {
        if (msg.contains("Application-specific password", true) || msg.contains("185833")) {
            alertTitle = "App Password Needed"
            alertMsg = "Google doesn't allow your regular password in mail apps. Create an App Password, then paste it here."
            alertLink = "https://myaccount.google.com/apppasswords"
        } else if (isNetwork(msg)) {
            alertTitle = "Connection Problem"
            alertMsg = "Couldn't reach the mail server. Check your internet connection and try again."
            alertLink = null
        } else if (msg.contains("AUTHENTICATIONFAILED", true) || msg.contains("Invalid credentials", true) || msg.contains("authenticat", true)) {
            alertTitle = "Sign In Failed"
            alertMsg = "Check your email and password."
            alertLink = null
        } else {
            alertTitle = "Something Went Wrong"
            alertMsg = msg
            alertLink = null
        }
    }

    fun updateClarity(x: Float) { glassClarity = x; ui.edit().putFloat("clarity", x).apply() }
    fun updateShine(x: Float) { glassShine = x; ui.edit().putFloat("shine", x).apply() }

    // ---------- Profile photos ----------
    private fun avatarFile(email: String) = java.io.File(app.filesDir, "avatar_" + email.lowercase().hashCode() + ".png")

    private fun loadAvatars() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val m = mutableMapOf<String, ImageBitmap>()
                Vault.all(app).forEach { acc ->
                    val f = avatarFile(acc.email)
                    if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { m[acc.email] = it.asImageBitmap() }
                }
                avatars = m
            } catch (e: Exception) { }
        }
    }

    private fun squareOf(src: android.graphics.Bitmap): android.graphics.Bitmap {
        val s = minOf(src.width, src.height)
        val cropped = android.graphics.Bitmap.createBitmap(src, (src.width - s) / 2, (src.height - s) / 2, s, s)
        return android.graphics.Bitmap.createScaledBitmap(cropped, 384, 384, true)
    }

    private fun storeAvatar(email: String, bmp: android.graphics.Bitmap) {
        val sq = squareOf(bmp)
        avatarFile(email).outputStream().use { sq.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        avatars = avatars + (email to sq.asImageBitmap())
    }

    fun chooseAvatar(uri: String) {
        val a = account ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cr = app.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var s = 1
                while (bounds.outWidth / s > 1200 || bounds.outHeight / s > 1200) s *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = s }
                val bmp = cr.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it, null, opts) }
                if (bmp != null) storeAvatar(a.email, bmp)
            } catch (e: Exception) { toast = "!Couldn't use that photo" }
        }
    }

    fun removeAvatar() {
        val a = account ?: return
        try { avatarFile(a.email).delete() } catch (e: Exception) { }
        avatars = avatars - a.email
    }

    // Looks up a public profile photo for the address (Gravatar). Google does not expose its photo to mail apps.
    private fun detectGravatar(a: Account) {
        if (avatarFile(a.email).exists()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val md = java.security.MessageDigest.getInstance("MD5")
                val hash = md.digest(a.email.trim().lowercase().toByteArray()).joinToString("") { "%02x".format(it) }
                val conn = java.net.URL("https://www.gravatar.com/avatar/$hash?s=384&d=404").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                if (conn.responseCode == 200) {
                    val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                    if (bmp != null) storeAvatar(a.email, bmp)
                }
                conn.disconnect()
            } catch (e: Exception) { }
        }
    }

    fun chooseTheme(t: String) { theme = t; ui.edit().putString("theme", t).apply() }

    fun setWallpaper(uri: String?) {
        wallpaperUri = uri
        ui.edit().putString("wall", uri).apply()
        loadWallpaper()
    }

    private fun loadWallpaper() {
        val u = wallpaperUri
        if (u == null) { wallpaper = null; return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cr = app.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(Uri.parse(u))?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var s = 1
                while (bounds.outWidth / s > 1400 || bounds.outHeight / s > 1400) s *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = s }
                val bmp = cr.openInputStream(Uri.parse(u))?.use { BitmapFactory.decodeStream(it, null, opts) }
                wallpaper = bmp?.asImageBitmap()
            } catch (e: Exception) { wallpaper = null }
        }
    }

    fun login(a: Account) {
        busy = true
        run {
            MailClient.boxes(a)
            Vault.add(app, a)
            Scheduler.start(app)
            accounts = Vault.all(app)
            account = a
            adding = false
            busy = false
            showCredit = true
            detectGravatar(a)
            resetView()
            IdleService.sync(app)
            loadBoxes()
        }
    }

    fun switchTo(a: Account) {
        if (a.email == account?.email) return
        Vault.setActive(app, a.email)
        account = a
        resetView()
        loadBoxes()
    }

    // Removes the active account; switches to another one if any remain
    fun logout() {
        val cur = account ?: return
        Vault.remove(app, cur.email)
        MailClient.drop(cur.email)
        Cache.clear(app)
        accounts = Vault.all(app)
        val next = accounts.firstOrNull()
        resetView()
        if (next == null) {
            Scheduler.stop(app); account = null
        } else {
            account = next; Vault.setActive(app, next.email); loadBoxes()
        }
        IdleService.sync(app)
    }

    fun toggleSelecting() { selecting = !selecting; selected = emptySet() }

    fun toggleSelect(i: MailItem) { selected = if (i.uid in selected) selected - i.uid else selected + i.uid }

    fun bulkRead() {
        val a = account ?: return
        val ids = selected
        val list = items.filter { it.uid in ids }
        items = items.map { if (it.uid in ids) it.copy(unread = false) else it }
        selecting = false; selected = emptySet()
        run { list.forEach { MailClient.setFlag(a, it.box, it.uid, Flags.Flag.SEEN, true) } }
    }

    fun bulkRemove(destKind: String) {
        val a = account ?: return
        val ids = selected
        val list = items.filter { it.uid in ids }
        val permanent = box?.kind == "trash" || (box?.kind == "junk" && destKind == "trash")
        items = items.filter { it.uid !in ids }
        list.firstOrNull()?.let { Cache.save(app, a.email, it.box, items) }
        selecting = false; selected = emptySet()
        toast = if (permanent) "Deleted" else if (destKind == "archive") "Archived" else "Moved to Trash"
        run { list.forEach { MailClient.moveTo(a, it.box, it.uid, destKind, permanent) } }
    }

    fun toggleInstant(on: Boolean) {
        instant = on
        Vault.setInstant(app, on)
        IdleService.sync(app)
    }

    fun downloadAttachment(name: String) {
        val a = account ?: return
        val i = opened ?: return
        toast = "Downloading $name..."
        run {
            val saved = MailClient.saveAttachment(app, a, i.box, i.uid, name)
            toast = "Saved to Downloads: $saved"
        }
    }

    fun loadBoxes() {
        val a = account ?: return
        if (boxes.isEmpty()) boxes = Cache.loadBoxes(app, a.email)
        run(quiet = true) {
            boxes = MailClient.boxes(a)
            Cache.saveBoxes(app, a.email, boxes)
        }
    }

    fun openBox(b: Box) {
        box = b; items = account?.let { Cache.load(app, it.email, b.path) } ?: emptyList(); query = ""; category = null
        unreadOnly = false; flaggedOnly = false; screen = Screen.List
        refresh()
    }

    fun refresh() {
        val a = account ?: return
        val b = box ?: return
        loading = true
        run(quiet = true) {
            val fresh = if (query.isBlank()) MailClient.list(a, b.path) else MailClient.search(a, b.path, query)
            val old = items.associateBy { it.uid }
            items = fresh.map { x -> old[x.uid]?.let { x.copy(preview = it.preview, category = it.category) } ?: x }
            loading = false
            updated = System.currentTimeMillis()
            if (query.isBlank()) Cache.save(app, a.email, b.path, items)
            launchPreviews(a, b.path)
        }
    }

    private fun launchPreviews(a: Account, path: String) {
        val uids = items.filter { it.preview.isEmpty() }.map { it.uid }
        if (uids.isEmpty()) return
        viewModelScope.launch {
            try {
                MailClient.previews(a, path, uids) { batch ->
                    items = items.map {
                        val p = batch[it.uid]
                        if (p == null) it else it.copy(
                            preview = p,
                            category = MailClient.categorize(it.fromAddr, it.subject, p, it.category == Category.Promotions)
                        )
                    }
                }
                if (query.isBlank()) Cache.save(app, a.email, path, items)
            } catch (e: Exception) { }
        }
    }

    fun poll() {
        val a = account ?: return
        val b = box ?: return
        if (screen != Screen.List || b.kind != "inbox" || query.isNotBlank() || loading) return
        viewModelScope.launch {
            try {
                val n = MailClient.list(a, b.path)
                val known = items.map { it.uid }.toSet()
                val fresh = n.filter { it.uid !in known && it.unread }
                val old = items.associateBy { it.uid }
                items = n.map { x -> old[x.uid]?.let { x.copy(preview = it.preview, category = it.category) } ?: x }
                updated = System.currentTimeMillis()
                launchPreviews(a, b.path)
                if (fresh.isNotEmpty()) toast = "New mail from ${fresh.first().fromName}"
            } catch (e: Exception) { }
        }
    }

    fun open(i: MailItem) {
        val a = account ?: return
        opened = i; body = null; screen = Screen.Read
        items = items.map { if (it.uid == i.uid) it.copy(unread = false) else it }
        run { body = MailClient.body(a, i.box, i.uid) }
    }

    fun toggleRead(i: MailItem) {
        val a = account ?: return
        val nowUnread = !i.unread
        items = items.map { if (it.uid == i.uid) it.copy(unread = nowUnread) else it }
        run { MailClient.setFlag(a, i.box, i.uid, Flags.Flag.SEEN, !nowUnread) }
    }

    fun toggleFlag(i: MailItem) {
        val a = account ?: return
        val on = !i.flagged
        items = items.map { if (it.uid == i.uid) it.copy(flagged = on) else it }
        if (opened?.uid == i.uid) opened = opened?.copy(flagged = on)
        run { MailClient.setFlag(a, i.box, i.uid, Flags.Flag.FLAGGED, on) }
    }

    fun remove(i: MailItem, destKind: String) {
        val a = account ?: return
        val permanent = box?.kind == "trash" || box?.kind == "junk" && destKind == "trash"
        items = items.filter { it.uid != i.uid }
        Cache.save(app, a.email, i.box, items)
        if (screen == Screen.Read) { screen = Screen.List; opened = null; body = null }
        toast = when {
            permanent -> "Deleted"
            destKind == "archive" -> "Archived"
            else -> "Moved to Trash"
        }
        run { MailClient.moveTo(a, i.box, i.uid, destKind, permanent) }
    }

    fun startCompose(d: Draft) {
        cameFrom = screen; draft = d; screen = Screen.Compose
    }

    fun reply() {
        val i = opened ?: return
        startCompose(Draft(
            to = i.fromAddr,
            subject = if (i.subject.startsWith("Re:", true)) i.subject else "Re: ${i.subject}",
            body = "\n\nOn ${fmtDate(i.date)}, ${i.fromName} wrote:\n> ${i.preview}",
            inReplyTo = body?.messageId
        ))
    }

    fun forward() {
        val i = opened ?: return
        startCompose(Draft(
            subject = if (i.subject.startsWith("Fwd:", true)) i.subject else "Fwd: ${i.subject}",
            body = "\n\n---------- Forwarded message ----------\nFrom: ${i.fromName} <${i.fromAddr}>\nSubject: ${i.subject}\n\n${i.preview}"
        ))
    }

    fun sendDraft() {
        val a = account ?: return
        val d = draft
        if (!d.to.contains("@")) { toast = "Add a recipient"; return }
        busy = true
        run {
            MailClient.send(a, d.to, d.cc, d.subject.ifBlank { "(No Subject)" }, d.body, d.inReplyTo)
            busy = false
            toast = "Sent"
            back()
        }
    }

    fun back() {
        when (screen) {
            Screen.Compose -> screen = cameFrom
            Screen.Read -> { screen = Screen.List; opened = null; body = null }
            Screen.List -> screen = Screen.Boxes
            Screen.Boxes -> { }
        }
    }
}
