package com.safetube.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

/** পুরো পর্দা (ভিডিও) — Android-এ যেটা আড়াআড়ি মোড ছিল */
object WindowCtl {
    var fullscreen by mutableStateOf(false)
}

fun main() {
    Vlc.init()
    AppFont.init()
    NewPipe.init(DownloaderImpl, Localization("bn", "BD"), ContentCountry("BD"))
    application {
        val state = rememberWindowState(size = DpSize(1180.dp, 780.dp), position = WindowPosition(Alignment.Center))
        val vm = remember { MainVm() }
        val icon = remember { BitmapPainter(useResource("app_icon_art.png") { loadImageBitmap(it) }) }
        LaunchedEffect(WindowCtl.fullscreen) {
            state.placement = if (WindowCtl.fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
        }
        Window(
            onCloseRequest = {
                try { Video.player.release(); Audio.player.release() } catch (e: Throwable) { }
                exitApplication()
            },
            title = "মুসলিম টিউব",
            state = state,
            icon = icon,
            onPreviewKeyEvent = { handleKey(it, vm) }
        ) {
            window.minimumSize = Dimension(380, 560)
            SafeTubeTheme(vm.theme) { Root(vm) }
        }
    }
}

/** Esc = পেছনে, Space = চালান/থামান, ←/→ = ৫ সেকেন্ড, ↑/↓ = ভলিউম, F বা F11 = পুরো পর্দা, M = মিউট */
private fun handleKey(e: KeyEvent, vm: MainVm): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    val inPlayer = vm.current != null && !vm.minimized
    when (e.key) {
        Key.Escape -> {
            return when {
                WindowCtl.fullscreen -> { WindowCtl.fullscreen = false; true }
                vm.showThemes -> { vm.showThemes = false; true }
                vm.showProfile -> { vm.showProfile = false; true }
                inPlayer -> { vm.minimizePlayer(); true }
                vm.tab != Tab.HOME -> { vm.tab = Tab.HOME; true }
                else -> false
            }
        }
        Key.F11 -> { WindowCtl.fullscreen = !WindowCtl.fullscreen; return true }
    }
    if (!inPlayer) return false
    val p = Video.player
    return when (e.key) {
        Key.Spacebar -> { p.toggle(); true }
        Key.DirectionLeft -> { p.seekBy(-5_000); true }
        Key.DirectionRight -> { p.seekBy(5_000); true }
        Key.DirectionUp -> { p.setVolumePct(p.volume + 5); true }
        Key.DirectionDown -> { p.setVolumePct(p.volume - 5); true }
        Key.F -> { WindowCtl.fullscreen = !WindowCtl.fullscreen; true }
        Key.M -> { p.toggleMute(); true }
        else -> false
    }
}

@Composable
fun Root(vm: MainVm) {
    val playing = vm.current
    // নতুন ভিডিও চাপলে সেটা সরাসরি বড় হয়ে খোলে
    LaunchedEffect(playing?.url) { vm.minimized = false }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(Modifier.fillMaxSize()) {
            if (playing == null || vm.minimized) {
                MainScreen(vm)
            } else {
                key(playing.url) {
                    PlayerScreen(playing, vm) { vm.minimizePlayer() }
                }
            }
            val msg = Toast.text
            if (msg != null) {
                Text(
                    msg, color = Color.White, fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xE6222222))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}
