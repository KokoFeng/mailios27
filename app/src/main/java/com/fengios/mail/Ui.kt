package com.fengios.mail

import android.content.Intent
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

val Blue = Color(0xFF007AFF)
val Red = Color(0xFFFF3B30)
val Orange = Color(0xFFFF9500)

val LocalDark = compositionLocalOf { false }

@Composable
fun MailTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme(primary = Blue, background = Color.Black, surface = Color(0xFF1C1C1E))
            else lightColorScheme(primary = Blue, background = Color(0xFFF2F2F7), surface = Color.White),
            content = content
        )
    }
}

// ---------- Liquid glass ----------
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), seeThrough: Boolean = LocalSeeThrough.current): Modifier {
    val dark = LocalDark.current
    val bd = LocalBackdrop.current
    val gp = LocalGlass.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    val tint = if (dark) Color(0x3D1C1C1E) else Color(0x40FFFFFF)
    val sat = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.45f) }) }
    val sh = gp.shine
    // how much of what is behind the glass shows through (the frosted layer gets thinner)
    val copyAlpha = if (seeThrough) 1f - gp.clarity * 0.95f else 1f
    val rim = Brush.linearGradient(listOf(
        Color.White.copy(alpha = (if (dark) 0.65f else 1f) * (0.35f + 0.65f * sh)),
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = (if (dark) 0.15f else 0.25f) * (0.4f + 0.6f * sh)),
        Color.White.copy(alpha = (if (dark) 0.5f else 0.85f) * (0.35f + 0.65f * sh))
    ))
    return this
        .shadow(18.dp, shape, ambientColor = Color(0x22000000), spotColor = Color(0x33000000))
        .clip(shape)
        .onGloballyPositioned { pos = it.positionInRoot() }
        .drawBehind {
            val img = bd.blurred
            if (img != null && bd.w > 0) {
                drawImage(img, dstOffset = IntOffset((-pos.x).roundToInt(), (-pos.y).roundToInt()),
                    dstSize = IntSize(bd.w, bd.h), alpha = copyAlpha, colorFilter = sat)
            } else {
                drawRect(if (dark) Color(0xFF1C1C1E) else Color.White, alpha = copyAlpha)
            }
            drawRect(tint)
            drawRect(Brush.verticalGradient(
                listOf(Color.White.copy(alpha = (if (dark) 0.22f else 0.55f) * sh), Color.Transparent),
                endY = size.height * 0.55f))
            // soft light pooling at the bottom edge, like a lens
            drawRect(Brush.verticalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = (if (dark) 0.10f else 0.22f) * sh)),
                startY = size.height * 0.6f, endY = size.height))
        }
        .border(1.2.dp, rim, shape)
}

@Composable
fun Modifier.bounceClick(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "press"
    )
    return this.scale(s).clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
fun GlassIcon(icon: ImageVector, desc: String, tint: Color = Blue, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).bounceClick(onClick).glass(CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint)
    }
}

@Composable
fun Backdrop(vm: MailVM, bd: BackdropState) {
    val dark = LocalDark.current
    val wp = vm.wallpaper
    Box(Modifier.fillMaxSize().background(if (dark) Color.Black else Color(0xFFF2F2F7))) {
        if (wp != null) {
            Image(wp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(if (dark) Color(0x8C000000) else Color(0x59FFFFFF)))
        }
    }
}

@Composable
fun Avatar(name: String, size: Dp = 44.dp, photo: ImageBitmap? = null) {
    if (photo != null) {
        Image(photo, null, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
        return
    }
    val colors = listOf(0xFF5AC8FA, 0xFFFF9500, 0xFF34C759, 0xFFAF52DE, 0xFFFF2D55, 0xFF5856D6, 0xFFFFCC00)
    val c = Color(colors[(name.hashCode().and(0x7fffffff)) % colors.size])
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.verticalGradient(listOf(c.copy(alpha = 0.75f), c))),
        contentAlignment = Alignment.Center
    ) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.42f).sp)
    }
}

@Composable
fun topInset() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
fun bottomInset() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

// ---------- Root ----------
@Composable
fun MailApp(vm: MailVM) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val dark = LocalDark.current
    val blurred by produceState<ImageBitmap?>(null, vm.wallpaper, dark, size) {
        if (size.width > 0) value = withContext(Dispatchers.Default) {
            makeBackdrop(vm.wallpaper?.asAndroidBitmap(), dark, size.width, size.height)
        }
    }
    val bd = BackdropState(blurred, size.width, size.height)
    CompositionLocalProvider(LocalBackdrop provides bd, LocalGlass provides GlassPrefs(vm.glassClarity, vm.glassShine)) {
    Box(Modifier.fillMaxSize().onSizeChanged { size = it }) {
        Backdrop(vm, bd)
        if (vm.account == null || vm.adding) {
            if (vm.adding) BackHandler { vm.adding = false }
            LoginScreen(vm)
        } else {
            BackHandler(enabled = vm.screen != Screen.Boxes) { vm.back() }
            val owner = LocalLifecycleOwner.current
            LaunchedEffect(vm.account) {
                owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) { delay(30_000); vm.poll() }
                }
            }
            AnimatedContent(
                targetState = vm.screen,
                transitionSpec = {
                    if (targetState == Screen.Compose) {
                        (slideInVertically(tween(380)) { it } + fadeIn()) togetherWith fadeOut(tween(200))
                    } else if (initialState == Screen.Compose) {
                        fadeIn(tween(200)) togetherWith (slideOutVertically(tween(320)) { it } + fadeOut())
                    } else if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(tween(350)) { it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally(tween(350)) { -it / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally(tween(350)) { -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally(tween(350)) { it / 3 } + fadeOut())
                    }
                },
                label = "screens"
            ) { s ->
                CompositionLocalProvider(LocalSeeThrough provides (s == Screen.List || s == Screen.Read)) {
                    when (s) {
                        Screen.Boxes -> BoxesScreen(vm)
                        Screen.List -> ListScreen(vm)
                        Screen.Read -> ReadScreen(vm)
                        Screen.Compose -> ComposeScreen(vm)
                    }
                }
            }
        }
        Toast(vm)
        AlertHost(vm)
    }
    }
}

@Composable
fun Toast(vm: MailVM) {
    val msg = vm.toast
    var last by remember { mutableStateOf("") }
    if (msg != null) last = msg
    LaunchedEffect(msg) { if (msg != null) { delay(2600); vm.toast = null } }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = msg != null,
            enter = slideInVertically(spring(0.55f, 380f)) { -it * 2 } + scaleIn(spring(0.6f), initialScale = 0.8f) + fadeIn(),
            exit = slideOutVertically { -it * 2 } + scaleOut(targetScale = 0.9f) + fadeOut()
        ) {
            Row(Modifier.glass(RoundedCornerShape(30.dp)).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (last.startsWith("!")) Icons.Filled.Warning else Icons.Filled.CheckCircle, null, tint = if (last.startsWith("!")) Orange else Color(0xFF34C759), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(last.removePrefix("!"), fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

data class AlertAction(val label: String, val role: Int = 0, val onClick: () -> Unit)
data class IosMenuItem(val label: String, val icon: ImageVector, val checked: Boolean = false, val destructive: Boolean = false, val onClick: () -> Unit)

@Composable
fun AlertButton(a: AlertAction, modifier: Modifier) {
    Box(modifier.clickable { a.onClick() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(a.label, fontSize = 17.sp, fontWeight = if (a.role == 1) FontWeight.Bold else FontWeight.Normal,
            color = if (a.role == 2) Red else Blue, maxLines = 1)
    }
}

// iOS-style alert: centered glass card, spring scale-in, hairline dividers
@Composable
fun IosAlert(title: String, message: String, actions: List<AlertAction>) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scrim by animateColorAsState(if (shown) Color(0x59000000) else Color.Transparent, label = "scrim")
    Box(Modifier.fillMaxSize().background(scrim)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center) {
        AnimatedVisibility(shown, enter = scaleIn(spring(0.62f, 380f), initialScale = 1.15f) + fadeIn(tween(150))) {
            Column(Modifier.width(290.dp).glass(RoundedCornerShape(32.dp))) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(message, fontSize = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
                if (actions.size == 2) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        AlertButton(actions[0], Modifier.weight(1f))
                        VerticalDivider(thickness = 0.5.dp)
                        AlertButton(actions[1], Modifier.weight(1f))
                    }
                } else {
                    actions.forEachIndexed { i, a ->
                        AlertButton(a, Modifier.fillMaxWidth())
                        if (i < actions.lastIndex) HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertHost(vm: MailVM) {
    val ctx = LocalContext.current
    val t = vm.alertTitle
    if (t != null) {
        val link = vm.alertLink
        val acts = buildList {
            if (link != null) add(AlertAction("Create App Password", 1) {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))); vm.alertTitle = null
            })
            add(AlertAction("OK", if (link != null) 0 else 1) { vm.alertTitle = null })
        }
        IosAlert(t, vm.alertMsg, acts)
    } else if (vm.showCredit) {
        IosAlert("Mail iOS 27", "Aplikasi ini dibuat oleh FengiOSonDroid / Feng-Dev.", listOf(
            AlertAction("Support Developer", 1) {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sociabuzz.com/feng_ios/tribe"))); vm.showCredit = false
            },
            AlertAction("Tutup") { vm.showCredit = false }
        ))
    } else if (vm.confirmRemove) {
        IosAlert("Remove Account?", "${vm.account?.email ?: ""} will be removed from this app. Your emails stay in your mailbox.",
            listOf(
                AlertAction("Cancel") { vm.confirmRemove = false },
                AlertAction("Remove", 2) { vm.confirmRemove = false; vm.logout() }
            ))
    }
}

// iOS-style context menu: glass card that springs out from the top-right button
@Composable
fun BoxScope.IosMenu(visible: Boolean, onDismiss: () -> Unit, items: List<IosMenuItem>) {
    if (visible) Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() })
    AnimatedVisibility(
        visible, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 66.dp, end = 12.dp),
        enter = scaleIn(spring(0.7f, 420f), transformOrigin = TransformOrigin(1f, 0f)) + fadeIn(tween(120)),
        exit = scaleOut(tween(140), transformOrigin = TransformOrigin(1f, 0f)) + fadeOut(tween(120))
    ) {
        Column(Modifier.width(250.dp).glass(RoundedCornerShape(26.dp), seeThrough = false).padding(vertical = 6.dp)) {
            items.forEachIndexed { i, m ->
                val c = if (m.destructive) Red else MaterialTheme.colorScheme.onBackground
                Row(Modifier.fillMaxWidth().clickable { m.onClick(); onDismiss() }.padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(m.label, Modifier.weight(1f), fontSize = 17.sp, color = c)
                    Icon(if (m.checked) Icons.Filled.Check else m.icon, null, tint = if (m.checked) Blue else c)
                }
                if (i < items.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 18.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(text, Modifier.bounceClick(onClick)
        .then(if (selected) Modifier.clip(RoundedCornerShape(16.dp)).background(Blue) else Modifier.glass(RoundedCornerShape(16.dp)))
        .padding(horizontal = 14.dp, vertical = 7.dp),
        maxLines = 1, softWrap = false, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onBackground)
}

@Composable
fun plainFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
)

// ---------- Login ----------
private fun detect(email: String): Provider {
    val d = email.substringAfter('@', "").lowercase().trim()
    return when {
        d == "gmail.com" || d == "googlemail.com" -> Providers[0]
        d in listOf("icloud.com", "me.com", "mac.com") -> Providers[1]
        d.startsWith("yahoo.") || d == "ymail.com" -> Providers[2]
        else -> Provider("Other", "imap.$d", 993, "smtp.$d", 465, "Use your email password. If sign in fails, open Server Settings.")
    }
}

@Composable
fun LoginScreen(vm: MailVM) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf(false) }
    var imap by remember { mutableStateOf("") }
    var smtp by remember { mutableStateOf("") }
    val prov = detect(email)
    val onBg = MaterialTheme.colorScheme.onBackground
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (vm.account != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text("Cancel", Modifier.bounceClick { vm.adding = false }.glass(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Blue, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(24.dp))
        Image(painterResource(R.drawable.app_icon), "Mail",
            Modifier.size(104.dp).shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x33000000), spotColor = Color(0x33000000)))
        Spacer(Modifier.height(16.dp))
        Text("Mail", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = onBg)
        Text("Sign in with your email account", color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))) {
            TextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), placeholder = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = plainFieldColors())
            HorizontalDivider(thickness = 0.5.dp)
            TextField(pass, { pass = it }, Modifier.fillMaxWidth(), placeholder = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), colors = plainFieldColors())
            if (manual) {
                HorizontalDivider(thickness = 0.5.dp)
                TextField(imap, { imap = it.trim() }, Modifier.fillMaxWidth(), placeholder = { Text(prov.imap.ifBlank { "IMAP server" }) }, singleLine = true, colors = plainFieldColors())
                HorizontalDivider(thickness = 0.5.dp)
                TextField(smtp, { smtp = it.trim() }, Modifier.fillMaxWidth(), placeholder = { Text(prov.smtp.ifBlank { "SMTP server" }) }, singleLine = true, colors = plainFieldColors())
            }
        }
        Spacer(Modifier.height(12.dp))
        if (prov.name == "Gmail") {
            Text("Google only lets mail apps sign in with an App Password. Tap here to create one.", fontSize = 13.sp, color = Blue,
                textAlign = TextAlign.Center,
                modifier = Modifier.bounceClick { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"))) })
        } else if (prov.hint.isNotBlank()) {
            Text(prov.hint, fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
        if (prov.name == "Other" && email.contains("@")) {
            Text(if (manual) "Hide Server Settings" else "Server Settings", color = Blue, fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp).bounceClick { manual = !manual })
        }
        Spacer(Modifier.height(20.dp))
        val ok = email.contains("@") && email.substringAfter('@').contains('.') && pass.isNotBlank()
        Box(
            Modifier.fillMaxWidth().height(52.dp).bounceClick {
                if (ok && !vm.busy) {
                    vm.login(Account(email, pass.replace(" ", ""), email.substringBefore('@'),
                        imap.ifBlank { prov.imap }, prov.imapPort, smtp.ifBlank { prov.smtp }, prov.smtpPort))
                }
            }.clip(RoundedCornerShape(26.dp)).background(if (ok) Blue else Color.Gray.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Sign In", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}

// ---------- Mailboxes ----------
@Composable
fun boxIcon(kind: String) = when (kind) {
    "inbox" -> Icons.Filled.Inbox; "sent" -> Icons.Filled.Send; "drafts" -> Icons.Filled.Drafts
    "junk" -> Icons.Filled.Report; "trash" -> Icons.Filled.Delete; "archive" -> Icons.Filled.Archive
    else -> Icons.Filled.Folder
}

@Composable
fun BoxesScreen(vm: MailVM) {
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            vm.setWallpaper(uri.toString())
        }
    }
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.chooseAvatar(uri.toString())
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = topInset() + 16.dp, bottom = bottomInset() + 110.dp, start = 16.dp, end = 16.dp)) {
            Text("Mailboxes", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp, bottom = 14.dp))
            Text("Accounts", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))) {
                vm.accounts.forEach { acc ->
                    val sel = acc.email == vm.account?.email
                    Row(Modifier.fillMaxWidth().bounceClick { vm.switchTo(acc) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Avatar(acc.name, 34.dp, vm.avatars[acc.email])
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(acc.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text(acc.email, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (sel) Icon(Icons.Filled.Check, null, tint = Blue)
                    }
                    HorizontalDivider(Modifier.padding(start = 62.dp), thickness = 0.5.dp)
                }
                Text("Choose Profile Photo...", Modifier.fillMaxWidth().bounceClick { pickAvatar.launch(arrayOf("image/*")) }
                    .padding(horizontal = 16.dp, vertical = 14.dp), color = Blue, fontSize = 16.sp)
                HorizontalDivider(thickness = 0.5.dp)
                if (vm.account != null && vm.avatars.containsKey(vm.account?.email)) {
                    Text("Remove Profile Photo", Modifier.fillMaxWidth().bounceClick { vm.removeAvatar() }
                        .padding(horizontal = 16.dp, vertical = 14.dp), color = Red, fontSize = 16.sp)
                    HorizontalDivider(thickness = 0.5.dp)
                }
                Text("Add Account...", Modifier.fillMaxWidth().bounceClick { vm.adding = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                    color = Blue, fontSize = 16.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text("Mailboxes", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))) {
                if (vm.boxes.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                vm.boxes.forEachIndexed { idx, b ->
                    Row(Modifier.fillMaxWidth().bounceClick { vm.openBox(b) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(boxIcon(b.kind), null, tint = Blue)
                        Spacer(Modifier.width(14.dp))
                        Text(b.title, Modifier.weight(1f), fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                        Icon(Icons.Filled.KeyboardArrowLeft, null, tint = Color.Gray, modifier = Modifier.scale(-1f, 1f))
                    }
                    if (idx < vm.boxes.lastIndex) HorizontalDivider(Modifier.padding(start = 54.dp), thickness = 0.5.dp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Appearance", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Auto", vm.theme == "system") { vm.chooseTheme("system") }
                    Pill("Light", vm.theme == "light") { vm.chooseTheme("light") }
                    Pill("Dark", vm.theme == "dark") { vm.chooseTheme("dark") }
                }
                HorizontalDivider(thickness = 0.5.dp)
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Liquid Glass: Transparency", fontSize = 14.sp, color = Color.Gray)
                    Slider(vm.glassClarity, { vm.updateClarity(it) },
                        colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue))
                    Text("Liquid Glass: Shine", fontSize = 14.sp, color = Color.Gray)
                    Slider(vm.glassShine, { vm.updateShine(it) },
                        colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue))
                }
                HorizontalDivider(thickness = 0.5.dp)
                Text("Choose Wallpaper from Photos", Modifier.fillMaxWidth().bounceClick { pick.launch(arrayOf("image/*")) }
                    .padding(horizontal = 16.dp, vertical = 14.dp), color = Blue, fontSize = 16.sp)
                if (vm.wallpaperUri != null) {
                    HorizontalDivider(thickness = 0.5.dp)
                    Text("Remove Wallpaper", Modifier.fillMaxWidth().bounceClick { vm.setWallpaper(null) }
                        .padding(horizontal = 16.dp, vertical = 14.dp), color = Red, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Instant Notifications", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("Keeps a connection open for push. Shows a quiet status notification.", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(vm.instant, { vm.toggleInstant(it) })
            }
            Spacer(Modifier.height(20.dp))
            Text("Support", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))) {
                Row(Modifier.fillMaxWidth().bounceClick {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sociabuzz.com/feng_ios/tribe")))
                }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, null, tint = Color(0xFFFF2D55))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Support Developer", fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text("Donate via Sociabuzz", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = Color.Gray)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Remove ${vm.account?.email ?: ""}", color = Red, modifier = Modifier.padding(start = 8.dp).bounceClick { vm.confirmRemove = true })
            Spacer(Modifier.height(28.dp))
            Text("Developer : FengiOSonDroid", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.Gray)
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(20.dp), horizontalArrangement = Arrangement.End) {
            GlassIcon(Icons.Filled.Edit, "Compose") { vm.startCompose(Draft()) }
        }
    }
}

// ---------- Message list ----------
fun catIcon(c: Category): ImageVector = when (c) {
    Category.Primary -> Icons.Filled.Person
    Category.Transactions -> Icons.Filled.ShoppingCart
    Category.Updates -> Icons.Filled.ChatBubble
    Category.Promotions -> Icons.Filled.Campaign
}

// Icon-only circle that widens to show its name when selected (iOS 27 categories)
@Composable
fun CategoryChip(c: Category, selected: Boolean, onClick: () -> Unit) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Row(
        Modifier.bounceClick(onClick)
            .animateContentSize(spring(0.75f, 500f))
            .then(if (selected) Modifier.clip(CircleShape).background(Blue) else Modifier.glass(CircleShape))
            .height(42.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(catIcon(c), c.label, tint = if (selected) Color.White else onBg, modifier = Modifier.size(20.dp))
        if (selected) {
            Text(c.label, Modifier.padding(start = 7.dp), color = Color.White, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, maxLines = 1, softWrap = false)
        }
    }
}

private fun updatedLabel(vm: MailVM): String = when {
    vm.loading -> "Updating..."
    vm.updated == 0L -> ""
    System.currentTimeMillis() - vm.updated < 60_000 -> "Updated Just Now"
    else -> "Updated " + fmtDate(vm.updated)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListScreen(vm: MailVM) {
    val b = vm.box ?: return
    val state = rememberLazyListState()
    val collapsed by remember { derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 60 } }
    val shown = vm.shown
    var menu by remember { mutableStateOf(false) }
    val onBg = MaterialTheme.colorScheme.onBackground
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topInset() + 68.dp, bottom = bottomInset() + 120.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(b.title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = onBg)
                    val sub = listOf(vm.account?.email ?: "", updatedLabel(vm)).filter { it.isNotBlank() }.joinToString(" · ")
                    Text(sub, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (b.kind == "inbox") item {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.values().forEach { c ->
                        CategoryChip(c, vm.category == c) { vm.category = if (vm.category == c) null else c }
                    }
                }
            }
            if (vm.loading && shown.isEmpty()) item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            items(shown, key = { it.uid }) { m -> SwipeRow(vm, m, Modifier.animateItemPlacement()) }
            if (!vm.loading && shown.isEmpty()) item {
                Text("No Mail", Modifier.fillMaxWidth().padding(48.dp), textAlign = TextAlign.Center, fontSize = 22.sp, color = Color.Gray)
            }
        }
        // top bar: back, Select, more
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(Icons.Filled.KeyboardArrowLeft, "Back") { vm.back() }
            AnimatedVisibility(collapsed, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut()) {
                Text(b.title, Modifier.glass(RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Bold, color = onBg)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (vm.selecting) "Done" else "Select",
                    Modifier.height(44.dp).bounceClick { vm.toggleSelecting() }.glass(RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp).wrapContentHeight(Alignment.CenterVertically),
                    color = Blue, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                GlassIcon(Icons.Filled.MoreHoriz, "More") { menu = !menu }
            }
        }
        IosMenu(menu, { menu = false }, listOf(
            IosMenuItem("Show All Mail", Icons.Filled.Inbox, checked = vm.category == null && !vm.unreadOnly && !vm.flaggedOnly) {
                vm.category = null; vm.unreadOnly = false; vm.flaggedOnly = false
            },
            IosMenuItem("Unread Only", Icons.Filled.MarkEmailUnread, checked = vm.unreadOnly) { vm.unreadOnly = !vm.unreadOnly },
            IosMenuItem("Flagged Only", Icons.Filled.Flag, checked = vm.flaggedOnly) { vm.flaggedOnly = !vm.flaggedOnly },
            IosMenuItem("Refresh", Icons.Filled.Refresh) { vm.refresh() }
        ))
        // bottom: filter, search, compose (or bulk actions while selecting)
        if (vm.selecting) {
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp)
                .glass(RoundedCornerShape(32.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).bounceClick { vm.bulkRead() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.MarkEmailRead, "Read", tint = Blue) }
                Text("${vm.selected.size} Selected", fontSize = 14.sp, color = onBg)
                Box(Modifier.size(48.dp).bounceClick { vm.bulkRemove("archive") }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Archive, "Archive", tint = Blue) }
                Box(Modifier.size(48.dp).bounceClick { vm.bulkRemove("trash") }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Delete, "Delete", tint = Red) }
            }
        } else {
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).bounceClick { vm.unreadOnly = !vm.unreadOnly }
                    .then(if (vm.unreadOnly) Modifier.clip(CircleShape).background(Blue) else Modifier.glass(CircleShape)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FilterList, "Filter", tint = if (vm.unreadOnly) Color.White else Blue)
                }
                Row(Modifier.weight(1f).height(50.dp).glass(RoundedCornerShape(25.dp)), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = Color.Gray, modifier = Modifier.padding(start = 14.dp))
                    BasicTextField(
                        value = vm.query, onValueChange = { vm.query = it; if (it.isBlank()) vm.refresh() },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp), singleLine = true,
                        textStyle = TextStyle(color = onBg, fontSize = 17.sp), cursorBrush = SolidColor(Blue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.refresh() }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (vm.query.isEmpty()) Text("Search", color = Color.Gray, fontSize = 17.sp)
                                inner()
                            }
                        }
                    )
                }
                Box(Modifier.size(50.dp).bounceClick { vm.startCompose(Draft()) }.glass(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Edit, "Compose", tint = Blue)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRow(vm: MailVM, item: MailItem, modifier: Modifier) {
    val st = rememberSwipeToDismissBoxState(confirmValueChange = {
        when (it) {
            SwipeToDismissBoxValue.StartToEnd -> { vm.toggleRead(item); false }
            SwipeToDismissBoxValue.EndToStart -> { vm.remove(item, "trash"); false }
            else -> false
        }
    })
    val onBg = MaterialTheme.colorScheme.onBackground
    SwipeToDismissBox(state = st, modifier = modifier,
        enableDismissFromStartToEnd = !vm.selecting, enableDismissFromEndToStart = !vm.selecting,
        backgroundContent = {
            val dir = st.dismissDirection
            val c by animateColorAsState(when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> Blue
                SwipeToDismissBoxValue.EndToStart -> Red
                else -> Color.Transparent
            }, label = "swipe")
            Box(Modifier.fillMaxSize().background(c).padding(horizontal = 24.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) {
                Icon(if (dir == SwipeToDismissBoxValue.StartToEnd) (if (item.unread) Icons.Filled.MarkEmailRead else Icons.Filled.MarkEmailUnread) else Icons.Filled.Delete,
                    null, tint = Color.White)
            }
        }) {
        Column(Modifier.background(
            if (vm.wallpaper != null) (if (LocalDark.current) Color(0x66000000) else Color(0x66FFFFFF)) else MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth()
                .bounceClick { if (vm.selecting) vm.toggleSelect(item) else vm.open(item) }
                .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
                Box(Modifier.width(22.dp).padding(top = 16.dp)) {
                    if (vm.selecting) {
                        val sel = item.uid in vm.selected
                        Box(Modifier.size(20.dp).then(if (sel) Modifier.clip(CircleShape).background(Blue) else Modifier.border(1.5.dp, Color.Gray, CircleShape)),
                            contentAlignment = Alignment.Center) {
                            if (sel) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    } else if (item.unread) {
                        Box(Modifier.size(10.dp).background(Blue, CircleShape))
                    }
                }
                Avatar(item.fromName)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.fromName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onBg)
                        if (item.flagged) Icon(Icons.Filled.Flag, null, tint = Orange, modifier = Modifier.size(15.dp))
                        Text(fmtDate(item.date), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 6.dp))
                        Icon(Icons.Filled.KeyboardArrowRight, null, tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                    Text(item.subject, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, color = onBg,
                        fontWeight = if (item.unread) FontWeight.SemiBold else FontWeight.Normal)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(item.preview, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, color = Color.Gray)
                        if (item.hasAttachment) Icon(Icons.Filled.AttachFile, null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }
            }
            HorizontalDivider(Modifier.padding(start = 88.dp), thickness = 0.5.dp)
        }
    }
}

// ---------- Reading ----------
// Only the text is kept: every email background is removed and the text colour follows the theme
private fun wrapHtml(h: String, dark: Boolean): String {
    val text = if (dark) "#F2F2F7" else "#111111"
    val link = if (dark) "#4DA3FF" else "#007AFF"
    val line = if (dark) "rgba(255,255,255,0.22)" else "rgba(0,0,0,0.18)"
    val css = "body{font-family:sans-serif;word-wrap:break-word;margin:0;padding:14px 16px 140px 16px}" +
        "img,video{max-width:100%;height:auto}table{max-width:100%}" +
        "html,body,*{background:transparent !important;background-image:none !important;box-shadow:none !important;" +
        "color:$text !important;border-color:$line !important}" +
        "a,a *{color:$link !important}"
    return "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>$css</style></head><body>$h</body></html>"
}

@Composable
fun ReadScreen(vm: MailVM) {
    val i = vm.opened ?: return
    var remote by remember { mutableStateOf(false) }
    val dark = LocalDark.current
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = topInset() + 68.dp)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(i.fromName, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(i.fromName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("To: ${vm.body?.to?.ifBlank { vm.account?.email } ?: vm.account?.email ?: ""}", fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(fmtDate(i.date), fontSize = 13.sp, color = Color.Gray)
            }
            Text(i.subject, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            val b = vm.body
            if (b != null && b.attachments.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    b.attachments.forEach { n ->
                        Row(Modifier.fillMaxWidth().bounceClick { vm.downloadAttachment(n) }.glass(RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AttachFile, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Text(n, Modifier.weight(1f).padding(start = 6.dp), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground)
                            Icon(Icons.Filled.KeyboardArrowDown, "Download", tint = Blue)
                        }
                    }
                }
            }
            if (b == null) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (!remote) {
                    Text("Load Remote Images", Modifier.padding(horizontal = 16.dp, vertical = 4.dp).bounceClick { remote = true },
                        color = Blue, fontSize = 14.sp)
                }
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(0)
                            settings.javaScriptEnabled = false
                            settings.blockNetworkImage = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                        }
                    },
                    update = { w ->
                        val key = b.html + remote + dark
                        if (w.tag != key) {
                            w.tag = key
                            w.settings.blockNetworkImage = !remote
                            w.loadDataWithBaseURL(null, wrapHtml(b.html, dark), "text/html", "utf-8", null)
                        }
                    }
                )
            }
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            GlassIcon(Icons.Filled.KeyboardArrowLeft, "Back") { vm.back() }
            GlassIcon(Icons.Filled.Flag, "Flag", tint = if (i.flagged) Orange else Blue) { vm.toggleFlag(i) }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp)
            .glass(RoundedCornerShape(32.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).bounceClick { vm.remove(i, "trash") }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Delete, "Delete", tint = Blue) }
            Box(Modifier.size(48.dp).bounceClick { vm.remove(i, "archive") }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Archive, "Archive", tint = Blue) }
            Box(Modifier.size(48.dp).bounceClick { vm.reply() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Reply, "Reply", tint = Blue) }
            Box(Modifier.size(48.dp).bounceClick { vm.forward() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.ArrowForward, "Forward", tint = Blue) }
            Box(Modifier.size(48.dp).bounceClick { vm.startCompose(Draft()) }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Edit, "Compose", tint = Blue) }
        }
    }
}

// ---------- Compose ----------
@Composable
fun ComposeScreen(vm: MailVM) {
    val d = vm.draft
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Cancel", Modifier.bounceClick { vm.back() }.glass(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 12.dp), color = Blue, fontWeight = FontWeight.Medium)
            Text("New Message", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
            val ready = d.to.contains("@") && !vm.busy
            Box(Modifier.size(44.dp).bounceClick { if (ready) vm.sendDraft() }.clip(CircleShape).background(if (ready) Blue else Color.Gray.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Filled.Send, "Send", tint = Color.White)
            }
        }
        Column(Modifier.fillMaxSize().glass(RoundedCornerShape(24.dp))) {
            TextField(d.to, { vm.draft = d.copy(to = it) }, Modifier.fillMaxWidth(), prefix = { Text("To:  ", color = Color.Gray) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = plainFieldColors())
            HorizontalDivider(thickness = 0.5.dp)
            TextField(d.cc, { vm.draft = d.copy(cc = it) }, Modifier.fillMaxWidth(), prefix = { Text("Cc:  ", color = Color.Gray) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = plainFieldColors())
            HorizontalDivider(thickness = 0.5.dp)
            TextField(d.subject, { vm.draft = d.copy(subject = it) }, Modifier.fillMaxWidth(), prefix = { Text("Subject:  ", color = Color.Gray) }, singleLine = true, colors = plainFieldColors())
            HorizontalDivider(thickness = 0.5.dp)
            Text("From: ${vm.account?.email ?: ""}", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Color.Gray, fontSize = 14.sp)
            HorizontalDivider(thickness = 0.5.dp)
            TextField(d.body, { vm.draft = d.copy(body = it) }, Modifier.fillMaxSize(), colors = plainFieldColors())
        }
    }
}
