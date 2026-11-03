package com.fengios.mail

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.sun.mail.imap.IMAPFolder
import java.io.File
import javax.mail.event.MessageCountAdapter
import javax.mail.event.MessageCountEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Date
import java.util.Properties
import javax.mail.FetchProfile
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.UIDFolder
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeUtility
import javax.mail.search.BodyTerm
import javax.mail.search.FlagTerm
import javax.mail.search.FromStringTerm
import javax.mail.search.OrTerm
import javax.mail.search.SearchTerm
import javax.mail.search.SubjectTerm

object MailClient {

    private suspend fun <T> io(b: () -> T): T = withContext(Dispatchers.IO) { b() }

    private fun props(a: Account, fetchSize: Int = 16384) = Properties().apply {
        put("mail.imaps.partialfetch", "true")
        put("mail.imaps.connectionpooltimeout", "600000")
        put("mail.imaps.fetchsize", fetchSize.toString())
        put("mail.imaps.host", a.imapHost)
        put("mail.imaps.port", a.imapPort.toString())
        put("mail.imaps.ssl.enable", "true")
        put("mail.imaps.connectiontimeout", "15000")
        put("mail.imaps.timeout", "20000")
        put("mail.smtp.host", a.smtpHost)
        put("mail.smtp.port", a.smtpPort.toString())
        put("mail.smtp.auth", "true")
        if (a.smtpPort == 465) put("mail.smtp.ssl.enable", "true") else put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.connectiontimeout", "15000")
        put("mail.smtp.timeout", "25000")
    }

    // One connected store per account is kept and reused: logging in to Gmail is the slowest step.
    // A store idle for more than 90 s is replaced instead of probed (probing a dead socket can stall for ~20 s).
    private val stores = java.util.concurrent.ConcurrentHashMap<String, javax.mail.Store>()
    private val lastUse = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun closeLater(st: javax.mail.Store?) {
        if (st == null) return
        Thread { try { st.close() } catch (e: Exception) { } }.start()
    }

    private fun obtain(a: Account, fetchSize: Int): javax.mail.Store {
        if (!Guard.ok) throw MessagingException("Connection refused")
        val key = a.email + "|" + fetchSize
        synchronized(key.intern()) {
            val cur = stores[key]
            val now = System.currentTimeMillis()
            if (cur != null && now - (lastUse[key] ?: 0L) < 90_000L) return cur
            closeLater(cur)
            val st = Session.getInstance(props(a, fetchSize)).getStore("imaps")
            st.connect(a.imapHost, a.imapPort, a.email, a.password)
            stores[key] = st
            lastUse[key] = now
            return st
        }
    }

    fun drop(email: String) {
        stores.keys.filter { it.startsWith("$email|") }.forEach { k -> closeLater(stores.remove(k)) }
    }

    private fun <T> withStore(a: Account, fetchSize: Int = 16384, retry: Boolean = true, block: (javax.mail.Store) -> T): T {
        val key = a.email + "|" + fetchSize
        try {
            val r = block(obtain(a, fetchSize))
            lastUse[key] = System.currentTimeMillis()
            return r
        } catch (e: Exception) {
            closeLater(stores.remove(key))
            val retryable = e !is javax.mail.AuthenticationFailedException &&
                (e is MessagingException || e is java.io.IOException || e is IllegalStateException)
            if (retry && retryable) {
                val r = block(obtain(a, fetchSize))
                lastUse[key] = System.currentTimeMillis()
                return r
            }
            throw e
        }
    }

    private fun profile() = FetchProfile().apply {
        add(FetchProfile.Item.ENVELOPE)
        add(FetchProfile.Item.FLAGS)
        add(FetchProfile.Item.CONTENT_INFO)
        add(UIDFolder.FetchProfileItem.UID)
        add("List-Unsubscribe")
    }

    // ---------- Mailboxes ----------
    private fun listBoxes(st: javax.mail.Store): List<Box> {
        val out = mutableListOf<Box>()
        for (f in st.defaultFolder.list("*")) {
            if (f.type and Folder.HOLDS_MESSAGES == 0) continue
            val attrs = (f as? IMAPFolder)?.attributes?.map { it.lowercase() } ?: emptyList()
            val n = f.fullName.lowercase()
            val kind = when {
                n == "inbox" -> "inbox"
                "\\sent" in attrs || n.contains("sent") -> "sent"
                "\\drafts" in attrs || n.contains("draft") -> "drafts"
                "\\junk" in attrs || n.contains("junk") || n.contains("spam") -> "junk"
                "\\trash" in attrs || n.contains("trash") || n.contains("deleted") -> "trash"
                "\\archive" in attrs || "\\all" in attrs || n.contains("archive") || n.contains("all mail") -> "archive"
                else -> "other"
            }
            val title = when (kind) {
                "inbox" -> "Inbox"; "sent" -> "Sent"; "drafts" -> "Drafts"
                "junk" -> "Junk"; "trash" -> "Trash"; "archive" -> "Archive"
                else -> f.name
            }
            out.add(Box(f.fullName, title, kind))
        }
        val order = listOf("inbox", "drafts", "sent", "junk", "trash", "archive", "other")
        return out.sortedBy { order.indexOf(it.kind) }
    }

    suspend fun boxes(a: Account): List<Box> = io { withStore(a) { listBoxes(it) } }

    // ---------- Message list ----------
    suspend fun list(a: Account, box: String, count: Int = 30): List<MailItem> = io {
        withStore(a) { st ->
            val f = st.getFolder(box) as IMAPFolder
            f.open(Folder.READ_ONLY)
            try {
                val total = f.messageCount
                if (total == 0) emptyList() else {
                    val msgs = f.getMessages(maxOf(1, total - count + 1), total)
                    f.fetch(msgs, profile())
                    msgs.reversed().map { toItem(f, it) }
                }
            } finally {
                try { f.close(false) } catch (e: Exception) { }
            }
        }
    }

    suspend fun unread(a: Account): List<MailItem> = io {
        withStore(a) { st ->
            val f = st.getFolder("INBOX") as IMAPFolder
            f.open(Folder.READ_ONLY)
            try {
                val found = f.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
                val msgs = found.takeLast(10).toTypedArray()
                f.fetch(msgs, profile())
                msgs.map { toItem(f, it, true) }
            } finally {
                try { f.close(false) } catch (e: Exception) { }
            }
        }
    }

    suspend fun search(a: Account, box: String, q: String): List<MailItem> = io {
        withStore(a) { st ->
            val f = st.getFolder(box) as IMAPFolder
            f.open(Folder.READ_ONLY)
            try {
                val term = OrTerm(arrayOf<SearchTerm>(SubjectTerm(q), FromStringTerm(q), BodyTerm(q)))
                val msgs = f.search(term).takeLast(60).toTypedArray()
                f.fetch(msgs, profile())
                val now = System.currentTimeMillis()
                msgs.map { toItem(f, it) }.sortedByDescending { score(it, q, now) }
            } finally {
                try { f.close(false) } catch (e: Exception) { }
            }
        }
    }

    // Relevance ranking: subject > sender > preview, boosted by unread and recency
    private fun score(i: MailItem, q: String, now: Long): Double {
        val ql = q.lowercase()
        var s = 0.0
        if (i.subject.lowercase().contains(ql)) s += 5
        if (i.fromName.lowercase().contains(ql) || i.fromAddr.lowercase().contains(ql)) s += 3
        if (i.preview.lowercase().contains(ql)) s += 2
        if (i.unread) s += 0.5
        val days = (now - i.date) / 86_400_000.0
        return s + 2.0 / (1 + days / 30)
    }

    private fun toItem(f: IMAPFolder, m: Message, withPreview: Boolean = false): MailItem {
        val from = m.from?.firstOrNull() as? InternetAddress
        val name = from?.personal?.takeIf { it.isNotBlank() } ?: from?.address ?: "Unknown"
        val subject = m.subject ?: "(No Subject)"
        val preview = if (withPreview) previewOf(m) else ""
        val bulk = try { m.getHeader("List-Unsubscribe") != null } catch (e: Exception) { false }
        return MailItem(
            uid = f.getUID(m), box = f.fullName, fromName = name, fromAddr = from?.address ?: "",
            subject = subject, preview = preview,
            date = (m.sentDate ?: m.receivedDate)?.time ?: 0L,
            unread = !m.isSet(Flags.Flag.SEEN), flagged = m.isSet(Flags.Flag.FLAGGED),
            category = categorize(from?.address ?: "", subject, preview, bulk),
            hasAttachment = try { m.isMimeType("multipart/mixed") } catch (e: Exception) { false }
        )
    }


    // ---------- Fast previews: read only the first ~2 KB of the text part ----------
    private fun findText(p: Part): Part? {
        if (p.isMimeType("multipart/*")) {
            val mp = p.content as Multipart
            var html: Part? = null
            for (i in 0 until mp.count) {
                val r = findText(mp.getBodyPart(i)) ?: continue
                if (r.isMimeType("text/plain")) return r
                if (html == null) html = r
            }
            return html
        }
        return if (p.isMimeType("text/plain") || p.isMimeType("text/html")) p else null
    }

    private fun previewOf(m: Message): String {
        try {
            val part = findText(m) ?: return ""
            val buf = ByteArray(2000)
            val n = part.inputStream.use { it.read(buf) }
            if (n <= 0) return ""
            val cs = try {
                javax.mail.internet.ContentType(part.contentType).getParameter("charset")
                    ?.let { java.nio.charset.Charset.forName(it) } ?: Charsets.UTF_8
            } catch (e: Exception) { Charsets.UTF_8 }
            var t = String(buf, 0, n, cs)
            if (part.isMimeType("text/html")) {
                t = t.replace(Regex("(?is)<(style|script).*?(</\\1>|\\z)"), " ")
                    .replace(Regex("<[^>]*>?"), " ").replace("&nbsp;", " ")
            }
            return t.replace(Regex("\\s+"), " ").trim().take(140)
        } catch (e: Exception) {
            return ""
        }
    }

    // Loads previews in the background after the list is already on screen
    suspend fun previews(a: Account, box: String, uids: List<Long>, onBatch: (Map<Long, String>) -> Unit) = io {
        withStore(a, 2048) { st ->
            val f = st.getFolder(box) as IMAPFolder
            f.open(Folder.READ_ONLY)
            try {
                val msgs = f.getMessagesByUID(uids.toLongArray())
                f.fetch(msgs, FetchProfile().apply { add(FetchProfile.Item.CONTENT_INFO) })
                var batch = mutableMapOf<Long, String>()
                for (m in msgs) {
                    if (m == null) continue
                    batch[f.getUID(m)] = previewOf(m)
                    if (batch.size >= 5) { onBatch(batch); batch = mutableMapOf() }
                }
                if (batch.isNotEmpty()) onBatch(batch)
            } finally {
                try { f.close(false) } catch (e: Exception) { }
            }
        }
    }

    fun categorize(from: String, subj: String, preview: String, bulk: Boolean): Category {
        val t = "$subj $preview".lowercase()
        val f = from.lowercase()
        val trans = listOf("receipt", "invoice", "your order", "payment", "your ticket", "boarding", "booking",
            "reservation", "tagihan", "pesanan", "pembayaran", "pengiriman", "shipped", "delivery", "verification code", "otp")
        val promo = listOf("sale", "% off", "discount", "diskon", "promo", "voucher", "deal", "newsletter", "limited time")
        return when {
            trans.any { t.contains(it) } -> Category.Transactions
            bulk || promo.any { t.contains(it) } -> Category.Promotions
            listOf("noreply", "no-reply", "notification", "alert", "update").any { f.contains(it) } -> Category.Updates
            else -> Category.Primary
        }
    }

    // ---------- Message body ----------
    private class Parts { var plain = ""; var html = ""; val att = mutableListOf<String>() }

    private fun text(p: Part): String {
        val c = p.content
        return when (c) {
            is String -> c
            is InputStream -> c.readBytes().toString(Charsets.UTF_8)
            else -> ""
        }
    }

    private fun walk(p: Part, o: Parts) {
        when {
            p.isMimeType("multipart/*") -> {
                val mp = p.content as Multipart
                for (i in 0 until mp.count) walk(mp.getBodyPart(i), o)
            }
            Part.ATTACHMENT.equals(p.disposition, true) ->
                p.fileName?.let { o.att.add(MimeUtility.decodeText(it)) }
            p.isMimeType("text/plain") -> if (o.plain.isEmpty()) o.plain = text(p)
            p.isMimeType("text/html") -> if (o.html.isEmpty()) o.html = text(p)
            else -> p.fileName?.let { o.att.add(MimeUtility.decodeText(it)) }
        }
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    private fun addrs(a: Array<javax.mail.Address>?) = a?.joinToString(", ") { (it as? InternetAddress)?.address ?: it.toString() } ?: ""

    suspend fun body(a: Account, box: String, uid: Long): MailBody = io {
        withStore(a) { st ->
            val f = st.getFolder(box) as IMAPFolder
            f.open(Folder.READ_WRITE)
            try {
                val m = f.getMessageByUID(uid) ?: throw MessagingException("Message not found")
                val o = Parts(); walk(m, o)
                m.setFlag(Flags.Flag.SEEN, true)
                val html = if (o.html.isNotEmpty()) o.html
                else "<pre style='white-space:pre-wrap;font-family:sans-serif'>${esc(o.plain)}</pre>"
                MailBody(html, addrs(m.getRecipients(Message.RecipientType.TO)),
                    addrs(m.getRecipients(Message.RecipientType.CC)), o.att,
                    m.getHeader("Message-ID")?.firstOrNull() ?: "")
            } finally {
                try { f.close(false) } catch (e: Exception) { }
            }
        }
    }

    // ---------- Actions ----------
    suspend fun setFlag(a: Account, box: String, uid: Long, flag: Flags.Flag, on: Boolean) = io {
        withStore(a) { st ->
            val f = st.getFolder(box) as IMAPFolder
            f.open(Folder.READ_WRITE)
            try { f.getMessageByUID(uid)?.setFlag(flag, on) } finally { try { f.close(false) } catch (e: Exception) { } }
        }
    }

    suspend fun moveTo(a: Account, box: String, uid: Long, destKind: String, permanent: Boolean) = io {
        withStore(a, retry = false) { st ->
            val dest = if (permanent) null else listBoxes(st).firstOrNull { it.kind == destKind }
            val f = st.getFolder(box) as IMAPFolder
            f.open(Folder.READ_WRITE)
            try {
                val m = f.getMessageByUID(uid)
                if (m != null) {
                    if (dest != null) f.copyMessages(arrayOf(m), st.getFolder(dest.path))
                    m.setFlag(Flags.Flag.DELETED, true)
                    f.expunge()
                }
            } finally {
                try { f.close(false) } catch (e: Exception) { }
            }
        }
    }

    // ---------- Send ----------
    suspend fun send(a: Account, to: String, cc: String, subject: String, body: String, inReplyTo: String?) = io {
        val s = Session.getInstance(props(a))
        val m = MimeMessage(s)
        m.setFrom(InternetAddress(a.email, a.name))
        m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.replace(";", ",")))
        if (cc.isNotBlank()) m.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc.replace(";", ",")))
        m.setSubject(subject, "UTF-8")
        m.setText(body, "UTF-8")
        m.sentDate = Date()
        if (!inReplyTo.isNullOrBlank()) {
            m.setHeader("In-Reply-To", inReplyTo)
            m.setHeader("References", inReplyTo)
        }
        val t = s.getTransport("smtp")
        t.connect(a.smtpHost, a.smtpPort, a.email, a.password)
        try { t.sendMessage(m, m.allRecipients) } finally { t.close() }
        // Gmail saves sent mail itself; other providers need a copy in Sent
        if (!a.imapHost.contains("gmail")) {
            try {
                withStore(a) { st ->
                    val sent = listBoxes(st).firstOrNull { it.kind == "sent" }
                    if (sent != null) {
                        val f = st.getFolder(sent.path)
                        f.open(Folder.READ_WRITE)
                        m.setFlag(Flags.Flag.SEEN, true)
                        f.appendMessages(arrayOf<Message>(m))
                        f.close(false)
                    }
                }
            } catch (e: Exception) { }
        }
    }

    // ---------- Attachments ----------
    private fun findPart(p: Part, name: String): Part? {
        if (p.isMimeType("multipart/*")) {
            val mp = p.content as Multipart
            for (i in 0 until mp.count) { val r = findPart(mp.getBodyPart(i), name); if (r != null) return r }
            return null
        }
        val fn = p.fileName?.let { MimeUtility.decodeText(it) }
        return if (fn == name) p else null
    }

    suspend fun saveAttachment(ctx: Context, a: Account, box: String, uid: Long, name: String): String = io {
        withStore(a) { st ->
            val f = st.getFolder(box) as IMAPFolder
            f.open(Folder.READ_ONLY)
            try {
                val m = f.getMessageByUID(uid) ?: throw MessagingException("Message not found")
                val part = findPart(m, name) ?: throw MessagingException("Attachment not found")
                val mime = part.contentType?.substringBefore(';')?.trim() ?: "application/octet-stream"
                val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                if (Build.VERSION.SDK_INT >= 29) {
                    val v = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safe)
                        put(MediaStore.Downloads.MIME_TYPE, mime)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                        ?: throw MessagingException("Cannot create file")
                    ctx.contentResolver.openOutputStream(uri)?.use { out -> part.inputStream.use { it.copyTo(out) } }
                } else {
                    val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: ctx.filesDir
                    File(dir, safe).outputStream().use { out -> part.inputStream.use { it.copyTo(out) } }
                }
                safe
            } finally {
                try { f.close(false) } catch (e: Exception) { }
            }
        }
    }

    // ---------- Instant notifications (IMAP IDLE) ----------
    // Blocks the calling thread. Ends with an exception when the connection drops or the store is closed.
    fun idleLoop(a: Account, onStore: (javax.mail.Store) -> Unit, onNew: (List<MailItem>) -> Unit) {
        val p = props(a).apply { remove("mail.imaps.timeout") } // IDLE must not hit the read timeout
        val st = Session.getInstance(p).getStore("imaps")
        st.connect(a.imapHost, a.imapPort, a.email, a.password)
        onStore(st)
        try {
            val f = st.getFolder("INBOX") as IMAPFolder
            f.open(Folder.READ_ONLY)
            f.addMessageCountListener(object : MessageCountAdapter() {
                override fun messagesAdded(e: MessageCountEvent) {
                    try {
                        val msgs = e.messages
                        f.fetch(msgs, profile())
                        onNew(msgs.map { toItem(f, it, true) })
                    } catch (ex: Exception) { }
                }
            })
            while (true) f.idle()
        } finally {
            try { st.close() } catch (e: Exception) { }
        }
    }
}
