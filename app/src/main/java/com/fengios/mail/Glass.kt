package com.fengios.mail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max

// A pre-blurred copy of whatever is behind the UI (wallpaper or colour blobs).
// Glass panels draw the matching slice of it, so they look like frosted glass over the wallpaper.
class BackdropState(val blurred: ImageBitmap?, val w: Int, val h: Int)

val LocalBackdrop = compositionLocalOf { BackdropState(null, 0, 0) }

// true on screens where bars float over scrolling content: the glass then lets that content show through
val LocalSeeThrough = compositionLocalOf { false }

// User-adjustable look: clarity = how much of the content behind shows through, shine = highlights and rim light
class GlassPrefs(val clarity: Float, val shine: Float)

val LocalGlass = compositionLocalOf { GlassPrefs(0.25f, 0.7f) }

fun makeBackdrop(src: Bitmap?, dark: Boolean, w: Int, h: Int): ImageBitmap {
    val sw = max(24, w / 10)
    val sh = max(24, h / 10)
    val out = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawColor(if (dark) 0xFF000000.toInt() else 0xFFF2F2F7.toInt())
    if (src != null) {
        val scale = max(sw / src.width.toFloat(), sh / src.height.toFloat())
        val dw = src.width * scale
        val dh = src.height * scale
        c.drawBitmap(src, null, RectF((sw - dw) / 2, (sh - dh) / 2, (sw + dw) / 2, (sh + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
        c.drawColor(if (dark) 0x8C000000.toInt() else 0x59FFFFFF)
    }
    var cur = out
    repeat(2) {
        val small = Bitmap.createScaledBitmap(cur, max(6, cur.width / 3), max(6, cur.height / 3), true)
        cur = Bitmap.createScaledBitmap(small, sw, sh, true)
    }
    return cur.asImageBitmap()
}
