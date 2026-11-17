package com.safetube.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.stream.StreamInfo

@Composable
fun PlayerScreen(video: VideoItem, vm: MainVm, onBack: () -> Unit) {
    val t = LocalYt.current
    val player = Video.player
    val clipboard = LocalClipboardManager.current
    var attempt by remember { mutableIntStateOf(0) }
    var retryable by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("লোড হচ্ছে…") }
    var ready by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf("") }
    var dlUrl by remember { mutableStateOf<String?>(null) }
    var related by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var info by remember { mutableStateOf<StreamInfo?>(null) }
    var likeCount by remember { mutableStateOf(-1L) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var commentsMsg by remember { mutableStateOf("মন্তব্য আসছে…") }
    var commentInfo by remember { mutableStateOf<CommentsInfo?>(null) }
    var commentNext by remember { mutableStateOf<Page?>(null) }
    var showAllComments by remember { mutableStateOf(false) }
    var moreLoading by remember { mutableStateOf(false) }
    var channelName by remember { mutableStateOf(video.channel) }
    var subs by remember { mutableStateOf(-1L) }
    var viewCount by remember { mutableStateOf(video.views) }
    var qualityLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var qualityIdx by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val okComment = { c: CommentItem -> c.text.isNotBlank() && !ContentFilter.isStrongBad(c.text + " " + c.author) }
    val full = WindowCtl.fullscreen

    val saved = vm.isSaved(video)
    val liked = vm.isLiked(video)
    val dl = vm.downloads.firstOrNull { it.video.url == video.url }
    val dlPct: Int? = dl?.let { vm.dlProgress[it.dlId] }

    LaunchedEffect(video.url, attempt) {
        Audio.pause()
        if (!Vlc.available) {
            status = Vlc.problem
            return@LaunchedEffect
        }
        // একই ভিডিওতে ফিরলে নতুন করে শুরু না করে যেখানে ছিল সেখান থেকেই চলে
        val resuming = attempt == 0 && Video.currentUrl == video.url && player.hasMedia
        if (!resuming) {
            Video.runner?.stop()
            Video.runner = null
        }
        status = "লোড হচ্ছে…"
        retryable = false
        try {
            val local = vm.localFile(video)
            if (local != null) {
                if (!resuming) {
                    player.playFile(local)
                    Video.currentUrl = video.url
                    vm.addHistory(video)
                }
                qualityLabels = emptyList()
                ready = true
                return@LaunchedEffect
            }
            val si = withContext(Dispatchers.IO) { Repo.streamInfo(video.url) }
            val d = try { si.description?.content ?: "" } catch (e: Exception) { "" }
            val verdict = Screening.check(si, video, d, vm.screenStrict(video))
            if (!verdict.ok) {
                status = "🚫 এই ভিডিও চালানো যাবে না।\nকারণ: " + verdict.reason
                return@LaunchedEffect
            }
            info = si
            likeCount = si.likeCount
            channelName = si.uploaderName ?: video.channel
            subs = si.uploaderSubscriberCount
            if (si.viewCount > 0) viewCount = si.viewCount
            desc = d
            dlUrl = Repo.muxedUrl(si)
            val cands = Repo.candidates(si)
            qualityLabels = cands.map { it.label }
            if (cands.isEmpty()) {
                status = "এই ভিডিওর চালানোর মতো লিংক পাওয়া যায়নি।"
                retryable = true
                return@LaunchedEffect
            }
            val failCb = { msg: String ->
                ready = false
                status = "ভিডিও চালানো যায়নি ($msg)"
                retryable = true
            }
            val existing = Video.runner
            if (resuming && existing != null) {
                existing.onFail = failCb
                existing.onIndex = { i -> qualityIdx = i }
                qualityIdx = existing.index
            } else {
                val r = SourceRunner(player, cands, failCb) { i -> qualityIdx = i }
                Video.runner = r
                Video.currentUrl = video.url
                r.start()
                vm.addHistory(video)
            }
            ready = true
            related = withContext(Dispatchers.IO) { Repo.related(si) }.filter { vm.keep(it) }
        } catch (e: Exception) {
            status = "সমস্যা হয়েছে: " + (e.message ?: e.javaClass.simpleName)
            retryable = true
        }
    }

    LaunchedEffect(info) {
        val i = info ?: return@LaunchedEffect
        try {
            val cp = withContext(Dispatchers.IO) { Repo.comments(i) }
            comments = cp.items.filter(okComment)
            commentInfo = cp.info
            commentNext = cp.next
            commentsMsg = if (comments.isEmpty()) "কোনো মন্তব্য পাওয়া যায়নি।" else ""
        } catch (e: Exception) {
            commentsMsg = "মন্তব্য বন্ধ আছে বা আনা যায়নি।"
        }
    }

    val box: @Composable (Modifier) -> Unit = { m ->
        PlayerBox(
            player, ready, status, retryable, qualityLabels, qualityIdx, full, t,
            onRetry = { Repo.invalidate(video.url); attempt++ },
            onPick = { i -> Video.runner?.switchTo(i) },
            modifier = m
        )
    }

    val details: LazyListScope.() -> Unit = {
        item {
            Column {
                Text(video.title, color = t.text, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp)
                if (viewCount > 0) {
                    Text("${fmtCount(viewCount)} বার দেখা", color = t.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp))
                }
                val name = channelName.ifEmpty { video.channel }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(t.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name.trimStart('@').take(1).ifEmpty { "?" }, color = t.onAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            name.ifEmpty { "চ্যানেলের নাম পাওয়া যায়নি" },
                            color = t.text, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (subs >= 0) {
                            Text("${fmtCount(subs)} সাবস্ক্রাইবার", color = t.muted, fontSize = 12.sp)
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip(if (likeCount >= 0) "👍 ${fmtCount(likeCount)}" else "👍 পছন্দ", liked, t) { vm.toggleLike(video) }
                    ActionChip("↗ লিংক কপি", false, t) {
                        clipboard.setText(AnnotatedString(video.title + "\n" + video.url))
                        Toast.show("লিংক কপি হয়েছে")
                    }
                    ActionChip(if (saved) "🔖 সংরক্ষিত" else "🔖 সংরক্ষণ", saved, t) { vm.toggleSave(video) }
                    val label = when {
                        dl == null -> "⬇ ডাউনলোড"
                        dlPct == null || dlPct in 0..99 -> "⏳ নামছে ${dlPct ?: 0}%"
                        dlPct == 100 -> "✅ ডাউনলোড হয়েছে"
                        else -> "⚠ ব্যর্থ, আবার করুন"
                    }
                    ActionChip(label, dlPct == 100, t) {
                        if (dl == null) {
                            val u = dlUrl
                            if (u == null) {
                                Toast.show("এই ভিডিওর ডাউনলোড লিংক পাওয়া যায়নি")
                            } else {
                                vm.startDownload(video, u)
                                Toast.show("ডাউনলোড শুরু হয়েছে")
                            }
                        } else if (dlPct != null && dlPct < 0) {
                            vm.deleteDownload(dl)
                            val u = dlUrl
                            if (u != null) vm.startDownload(video, u)
                        }
                    }
                }
            }
        }
        item {
            CommentsBox(
                t, comments, commentsMsg, showAllComments, moreLoading, commentNext != null,
                onToggle = { showAllComments = !showAllComments },
                onMore = {
                    val ci = commentInfo
                    val pg = commentNext
                    if (ci != null && pg != null && !moreLoading) {
                        scope.launch {
                            moreLoading = true
                            try {
                                val cp = withContext(Dispatchers.IO) { Repo.moreComments(ci, pg) }
                                comments = comments + cp.items.filter(okComment)
                                commentNext = cp.next
                            } catch (e: Exception) {
                                commentNext = null
                            } finally {
                                moreLoading = false
                            }
                        }
                    }
                }
            )
        }
    }

    val relatedItems: LazyListScope.() -> Unit = {
        if (related.isNotEmpty()) {
            item { Text("আরও দেখুন", color = t.text, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        }
        items(related) { r -> VideoCard(r, t) { vm.current = r } }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(t.bg)) {
        val wide = maxWidth >= 940.dp
        val maxBoxH = maxHeight * 0.70f
        if (full) {
            box(Modifier.fillMaxSize())
        } else if (wide) {
            Column(Modifier.fillMaxSize()) {
                YtHeader(t, onBack = onBack)
                Row(Modifier.weight(1f)) {
                    Column(Modifier.weight(1f)) {
                        box(Modifier.fillMaxWidth().heightIn(max = maxBoxH).aspectRatio(16f / 9f))
                        LazyColumn(
                            Modifier.weight(1f),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = details
                        )
                    }
                    LazyColumn(
                        Modifier.width(400.dp),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = relatedItems
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                YtHeader(t, onBack = onBack)
                box(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    details()
                    relatedItems()
                }
            }
        }
    }
}

/** ভিডিওর জানালা: ফ্রেম + নিজস্ব কন্ট্রোল (চালান/থামান, সিক, ভলিউম, গতি, মান, পুরো পর্দা) */
@Composable
private fun PlayerBox(
    p: VPlayer,
    ready: Boolean,
    status: String,
    retryable: Boolean,
    qualityLabels: List<String>,
    qualityIdx: Int,
    full: Boolean,
    t: YtTheme,
    onRetry: () -> Unit,
    onPick: (Int) -> Unit,
    modifier: Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    var lastMove by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCtl by remember { mutableStateOf(true) }
    LaunchedEffect(lastMove, p.isPlaying) {
        showCtl = true
        if (p.isPlaying) {
            delay(3000)
            showCtl = false
            menuOpen = false
        }
    }
    Box(
        modifier
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.type == PointerEventType.Move || e.type == PointerEventType.Press) lastMove = System.currentTimeMillis()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (ready) {
            VideoSurface(
                p,
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { WindowCtl.fullscreen = !WindowCtl.fullscreen },
                        onTap = { menuOpen = false; p.toggle() }
                    )
                }
            )
            if (p.buffering) CircularProgressIndicator(color = Color.White)
            if (showCtl || !p.isPlaying) {
                // ⚙ : মান, ধরন ও গতি
                Column(Modifier.align(Alignment.TopEnd).padding(6.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        "⚙", color = Color.White, fontSize = 18.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                            .clickable { menuOpen = !menuOpen }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    if (menuOpen) {
                        Column(
                            Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xE6202020))
                                .padding(vertical = 6.dp)
                        ) {
                            if (qualityLabels.isNotEmpty()) {
                                Text(
                                    "ভিডিওর মান ও ধরন", color = Color(0xFFBBBBBB), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                                qualityLabels.forEachIndexed { i, label ->
                                    MenuLine(label, i == qualityIdx) { menuOpen = false; onPick(i) }
                                }
                            }
                            Text(
                                "গতি", color = Color(0xFFBBBBBB), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                            for (r in listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) {
                                MenuLine(if (r == 1f) "স্বাভাবিক" else "${r}x", p.rate == r) { menuOpen = false; p.setSpeed(r) }
                            }
                        }
                    }
                }
                Controls(p, full, Modifier.align(Alignment.BottomCenter))
            }
        } else if (status == "লোড হচ্ছে…") {
            CircularProgressIndicator(color = t.accent)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(status, color = Color.White)
                if (!Vlc.available) {
                    Text(
                        "⬇ VLC নামান (videolan.org)", color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x55FFFFFF))
                            .clickable { openUrl("https://www.videolan.org/vlc/") }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (retryable) {
                    Text(
                        "🔄 আবার চেষ্টা করুন", color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x55FFFFFF))
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuLine(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label + if (on) "  ✓" else "",
        color = if (on) Color(0xFFFDE68A) else Color.White,
        fontSize = 14.sp,
        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun CtlBtn(label: String, onClick: () -> Unit) {
    Text(
        label, color = Color.White, fontSize = 20.sp,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun Controls(p: VPlayer, full: Boolean, modifier: Modifier) {
    var dragging by remember { mutableStateOf(false) }
    var dragVal by remember { mutableFloatStateOf(0f) }
    val len = p.lengthMs
    val frac = if (len > 0) (p.positionMs.toFloat() / len).coerceIn(0f, 1f) else 0f
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
            .padding(horizontal = 10.dp)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Slider(
            value = if (dragging) dragVal else frac,
            onValueChange = { dragging = true; dragVal = it },
            onValueChangeFinished = {
                if (len > 0) p.seekTo((dragVal * len).toLong())
                dragging = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF0000),
                activeTrackColor = Color(0xFFFF0000),
                inactiveTrackColor = Color(0x66FFFFFF)
            )
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            CtlBtn(if (p.isPlaying) "⏸" else "▶") { p.toggle() }
            CtlBtn("⏪") { p.seekBy(-10_000) }
            CtlBtn("⏩") { p.seekBy(10_000) }
            Text(
                fmtClock(if (dragging) (dragVal * len).toLong() else p.positionMs) + " / " + fmtClock(len),
                color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp)
            )
            Spacer(Modifier.weight(1f))
            CtlBtn(if (p.muted || p.volume == 0) "🔇" else "🔊") { p.toggleMute() }
            Slider(
                value = p.volume / 100f,
                onValueChange = { p.setVolumePct((it * 100).toInt()); if (p.muted) p.toggleMute() },
                modifier = Modifier.width(90.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x66FFFFFF)
                )
            )
            CtlBtn(if (full) "🗗" else "⛶") { WindowCtl.fullscreen = !WindowCtl.fullscreen }
        }
    }
}

@Composable
private fun ActionChip(label: String, on: Boolean, t: YtTheme, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        label,
        color = if (on) t.onAccent else t.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(shape)
            .background(if (on) t.chipOn else t.panel)
            .border(1.dp, if (on) t.chipOn else t.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun CommentsBox(
    t: YtTheme,
    comments: List<CommentItem>,
    msg: String,
    showAll: Boolean,
    moreLoading: Boolean,
    hasMore: Boolean,
    onToggle: () -> Unit,
    onMore: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(t.panel)
            .border(1.dp, t.line, shape)
            .padding(12.dp)
    ) {
        // লেখায় চাপলে মন্তব্য খোলে, আবার চাপলে লুকিয়ে যায়
        Text(
            if (showAll) "💬 মন্তব্য লুকান ▴" else "💬 মন্তব্য দেখুন ▾",
            color = t.text, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp)
        )
        if (showAll) {
            if (msg.isNotEmpty()) {
                Text(msg, color = t.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            for (c in comments) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(t.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(c.author.trimStart('@').take(1).ifEmpty { "?" }, color = t.onAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            c.author + if (c.date.isNotEmpty()) "  ·  " + c.date else "",
                            color = t.muted, fontSize = 11.5.sp, maxLines = 1
                        )
                        Text(
                            c.text, color = t.text, fontSize = 13.5.sp,
                            maxLines = 12,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (c.likes > 0) {
                            Text("👍 ${c.likes}", color = t.muted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
            if (comments.isNotEmpty() && hasMore) {
                TextButton(onClick = onMore, modifier = Modifier.padding(top = 6.dp)) {
                    Text(if (moreLoading) "আসছে…" else "আরও মন্তব্য", color = t.chipOn)
                }
            }
        }
    }
}
