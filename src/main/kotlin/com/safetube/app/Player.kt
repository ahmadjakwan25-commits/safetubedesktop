package com.safetube.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

/** ExoPlayer-এর বদলে VLC (libvlc) ইঞ্জিন */
object Vlc {
    var available = false
        private set
    var problem: String = ""
        private set
    var factory: MediaPlayerFactory? = null
        private set

    /** সব VLC কমান্ড একটা আলাদা থ্রেডে চলে, তাই UI আটকায় না আর ডেডলকও হয় না */
    val exec: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "vlc-control").apply { isDaemon = true } }

    private fun bundledDir(): File? {
        val roots = listOfNotNull(
            System.getProperty("compose.application.resources.dir")?.let { File(it) },
            File("resources/windows-x64"),
            File("resources/common")
        )
        for (r in roots) {
            val d = File(r, "vlc")
            if (File(d, "libvlc.dll").exists() || File(d, "libvlc.so").exists() || File(d, "libvlc.dylib").exists()) return d
        }
        return null
    }

    fun init() {
        val args = mutableListOf("--no-video-title-show", "--no-osd", "--quiet", "--no-snapshot-preview")
        try {
            val b = bundledDir()
            if (b != null) {
                System.setProperty("jna.library.path", b.absolutePath)
                args += "--plugin-path=" + File(b, "plugins").absolutePath
                factory = MediaPlayerFactory(*args.toTypedArray())
                available = true
            } else if (NativeDiscovery().discover()) {
                factory = MediaPlayerFactory(*args.toTypedArray())
                available = true
            } else {
                problem = "VLC পাওয়া যায়নি"
            }
        } catch (e: Throwable) {
            available = false
            problem = "VLC চালু করা যায়নি: " + (e.message ?: e.javaClass.simpleName)
        }
    }
}

/** চালানোর একটা উপায়: সরাসরি লিংক, অথবা আলাদা ভিডিও + আলাদা অডিও জোড়া লাগিয়ে */
class Candidate(val label: String, val url: String, val audioUrl: String? = null)

class VPlayer(private val withVideo: Boolean) {
    private var mp: MediaPlayer? = null

    var frame by mutableStateOf<ImageBitmap?>(null)
    var isPlaying by mutableStateOf(false)
    var buffering by mutableStateOf(false)
    var hasMedia by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var lengthMs by mutableLongStateOf(0L)
    var videoRatio by mutableFloatStateOf(16f / 9f)
    var volume by mutableIntStateOf(Prefs.getInt("volume", 80))
    var muted by mutableStateOf(false)
    var rate by mutableFloatStateOf(1f)
    var onError: (String) -> Unit = {}
    var onEnded: () -> Unit = {}

    private var gen = 0
    private var started = false
    private var last: Candidate? = null

    init {
        val f = Vlc.factory
        if (f != null) {
            try {
                if (withVideo) {
                    val e: EmbeddedMediaPlayer = f.mediaPlayers().newEmbeddedMediaPlayer()
                    val sink = object : VlcSurface.Sink {
                        override fun format(srcWidth: Int, srcHeight: Int) {
                            if (srcWidth > 0 && srcHeight > 0) videoRatio = srcWidth.toFloat() / srcHeight
                        }

                        override fun frame(width: Int, height: Int, bgra: ByteArray) {
                            val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
                            frame = org.jetbrains.skia.Image.makeRaster(info, bgra, width * 4).toComposeImageBitmap()
                        }
                    }
                    val s = VlcSurface(sink)
                    e.videoSurface().set(f.videoSurfaces().newVideoSurface(s, s, true))
                    mp = e
                } else {
                    mp = f.mediaPlayers().newMediaPlayer()
                }
                mp?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                    override fun playing(m: MediaPlayer) { isPlaying = true; buffering = false; started = true }
                    override fun paused(m: MediaPlayer) { isPlaying = false }
                    override fun stopped(m: MediaPlayer) { isPlaying = false }
                    override fun finished(m: MediaPlayer) { isPlaying = false; onEnded() }
                    override fun error(m: MediaPlayer) { isPlaying = false; onError("VLC") }
                    override fun timeChanged(m: MediaPlayer, newTime: Long) { positionMs = newTime }
                    override fun lengthChanged(m: MediaPlayer, newLength: Long) { lengthMs = newLength }
                    override fun buffering(m: MediaPlayer, newCache: Float) { buffering = newCache < 100f }
                })
            } catch (e: Throwable) {
                mp = null
            }
        }
    }

    val ready: Boolean get() = mp != null

    fun play(c: Candidate, startMs: Long = 0L) {
        val g = ++gen
        last = c
        started = false
        hasMedia = true
        buffering = true
        positionMs = startMs
        lengthMs = 0L
        Vlc.exec.execute {
            val m = mp ?: return@execute
            val opts = mutableListOf(":http-user-agent=$USER_AGENT", ":network-caching=1500")
            if (c.audioUrl != null) opts += ":input-slave=${c.audioUrl}"
            if (startMs > 1000) opts += ":start-time=${startMs / 1000.0}"
            try {
                m.media().play(c.url, *opts.toTypedArray())
                m.audio().setVolume(volume)
                m.audio().setMute(muted)
                if (rate != 1f) m.controls().setRate(rate)
            } catch (e: Throwable) {
                onError(e.message ?: "play")
            }
        }
        // ২০ সেকেন্ডে চালু না হলে ব্যর্থ ধরা হয়, যাতে পরের উপায় চেষ্টা হয়
        Thread {
            Thread.sleep(20_000)
            if (g == gen && !started && hasMedia) onError("সময় শেষ")
        }.apply { isDaemon = true }.start()
    }

    fun replay() {
        last?.let { play(it) }
    }

    fun playFile(path: String) = play(Candidate("ডাউনলোড করা", File(path).absolutePath))

    fun pause() = Vlc.exec.execute { try { mp?.controls()?.setPause(true) } catch (e: Throwable) { } }

    fun resume() = Vlc.exec.execute { try { mp?.controls()?.play() } catch (e: Throwable) { } }

    fun toggle() {
        if (isPlaying) pause() else resume()
    }

    fun seekTo(ms: Long) {
        positionMs = ms
        Vlc.exec.execute { try { mp?.controls()?.setTime(ms) } catch (e: Throwable) { } }
    }

    fun seekBy(deltaMs: Long) {
        val target = (positionMs + deltaMs).coerceIn(0L, if (lengthMs > 0) lengthMs else Long.MAX_VALUE)
        seekTo(target)
    }

    fun setVolumePct(v: Int) {
        volume = v.coerceIn(0, 100)
        Prefs.putInt("volume", volume)
        Vlc.exec.execute { try { mp?.audio()?.setVolume(volume) } catch (e: Throwable) { } }
    }

    fun toggleMute() {
        muted = !muted
        Vlc.exec.execute { try { mp?.audio()?.setMute(muted) } catch (e: Throwable) { } }
    }

    fun setSpeed(r: Float) {
        rate = r
        Vlc.exec.execute { try { mp?.controls()?.setRate(r) } catch (e: Throwable) { } }
    }

    fun stop() {
        gen++
        hasMedia = false
        isPlaying = false
        buffering = false
        frame = null
        positionMs = 0L
        lengthMs = 0L
        Vlc.exec.execute { try { mp?.controls()?.stop() } catch (e: Throwable) { } }
    }

    fun release() {
        gen++
        Vlc.exec.execute { try { mp?.release() } catch (e: Throwable) { } }
    }
}

/** মিউজিক ট্যাবের অডিও প্লেয়ার। ট্যাব বদলালেও থামে না। */
object Audio {
    val player: VPlayer by lazy { VPlayer(false) }
    fun pause() {
        try { player.pause() } catch (e: Exception) { }
    }
}

/** সাধারণ ভিডিওর প্লেয়ার। ছোট করলেও (মিনি) চলতে থাকে। */
object Video {
    val player: VPlayer by lazy { VPlayer(true) }
    var currentUrl: String? = null
    var runner: SourceRunner? = null

    fun stop() {
        runner?.stop()
        runner = null
        currentUrl = null
        try { player.stop() } catch (e: Exception) { }
    }
}

/** একটা উপায়ে না চললে নিজে থেকে পরের উপায় চেষ্টা করে; মান বদলালে আগের জায়গা থেকেই চলে */
class SourceRunner(
    private val player: VPlayer,
    private val cands: List<Candidate>,
    var onFail: (String) -> Unit,
    var onIndex: (Int) -> Unit = {}
) {
    var index = 0
        private set
    private var active = true

    fun start() {
        player.onError = { handleError() }
        tryIdx(0, 0L)
    }

    fun switchTo(i: Int) = tryIdx(i, player.positionMs)

    private fun tryIdx(i: Int, pos: Long) {
        index = i
        onIndex(i)
        val c = cands.getOrNull(i)
        if (c == null) {
            onFail("চালানোর মতো কোনো লিংক পাওয়া যায়নি")
            return
        }
        player.play(c, pos)
    }

    private fun handleError() {
        if (!active) return
        if (index + 1 < cands.size) tryIdx(index + 1, player.positionMs) else onFail("VLC")
    }

    fun stop() {
        active = false
    }
}

/** ভিডিওর ফ্রেম আঁকে (সবসময় সঠিক অনুপাতে, কালো পটে) */
@Composable
fun VideoSurface(p: VPlayer, modifier: Modifier = Modifier) {
    val f = p.frame
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (f != null) Image(f, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

fun fmtClock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
