package com.safetube.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/* ================= শর্টস ================= */

@Composable
fun ShortsTab(vm: MainVm, t: YtTheme) {
    val list = vm.shorts
    LaunchedEffect(Unit) { if (list.isEmpty()) vm.loadShorts() }

    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (vm.shortsLoading) CircularProgressIndicator(color = t.accent)
            else Text("শর্ট ভিডিও আনা যায়নি। অন্য ট্যাবে গিয়ে ফিরে আসুন।", color = t.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
        return
    }

    val pager = rememberPagerState(pageCount = { list.size })
    val player = remember { VPlayer(true).apply { onEnded = { replay() } } }
    val scope = rememberCoroutineScope()
    val runnerHolder = remember { arrayOfNulls<SourceRunner>(1) }
    var attemptS by remember { mutableIntStateOf(0) }
    var retryable by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        onDispose {
            runnerHolder[0]?.stop()
            player.release()
        }
    }

    LaunchedEffect(pager.settledPage, list.size, attemptS) {
        val v = list.getOrNull(pager.settledPage) ?: return@LaunchedEffect
        Audio.pause()
        runnerHolder[0]?.stop()
        ready = false
        status = ""
        retryable = false
        player.stop()
        if (!Vlc.available) {
            status = "🚫 " + Vlc.problem
            return@LaunchedEffect
        }
        try {
            val info = withContext(Dispatchers.IO) { Repo.streamInfo(v.url) }
            val d = try { info.description?.content ?: "" } catch (e: Exception) { "" }
            val verdict = Screening.check(info, v, d, vm.screenStrict(v))
            if (!verdict.ok) {
                status = "🚫 " + verdict.reason
                return@LaunchedEffect
            }
            val cands = Repo.candidates(info)
            if (cands.isEmpty()) {
                status = "চালানো যায়নি — চাপলে আবার চেষ্টা হবে"
                retryable = true
                return@LaunchedEffect
            }
            val r = SourceRunner(player, cands, onFail = { msg ->
                ready = false
                status = "চালানো যায়নি ($msg) — চাপলে আবার চেষ্টা হবে"
                retryable = true
            })
            runnerHolder[0] = r
            r.start()
            ready = true
            vm.addHistory(v)
            // পরের দুটো শর্টের তথ্য আগে থেকে এনে রাখে, যাতে স্লাইড করলেই চলে
            vm.prefetch(list.drop(pager.settledPage + 1).take(2))
        } catch (e: Exception) {
            status = "সমস্যা: " + (e.message ?: e.javaClass.simpleName) + " — চাপলে আবার চেষ্টা হবে"
            retryable = true
        }
    }
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage >= list.size - 3) vm.loadShorts()
    }

    VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        val v = list[page]
        val current = page == pager.settledPage
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable {
                    if (current && ready) {
                        player.toggle()
                    } else if (current && retryable) {
                        Repo.invalidate(v.url)
                        attemptS++
                    }
                }
        ) {
            AsyncImage(
                model = v.thumb, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
            )
            if (current && ready) {
                VideoSurface(player, Modifier.fillMaxSize())
            }
            if (current && !ready && status.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
            if (current && status.isNotEmpty()) {
                Text(
                    status, color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            }
            // নিচের লেখা
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(start = 14.dp, end = 70.dp, top = 40.dp, bottom = 16.dp)
            ) {
                Text(v.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(v.channel, color = Color(0xFFDDDDDD), fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
            // ডানের বোতাম
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RoundAction("▲", false) { if (pager.currentPage > 0) scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
                RoundAction("▼", false) { if (pager.currentPage < list.size - 1) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
                RoundAction("👍", vm.isLiked(v)) { vm.toggleLike(v) }
                RoundAction("🔖", vm.isSaved(v)) { vm.toggleSave(v) }
            }
        }
    }
}

@Composable
private fun RoundAction(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (on) Color(0xFFE53935) else Color(0x66000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 20.sp) }
}

/* ================= মিউজিক (অডিও) ================= */

@Composable
fun MusicTab(vm: MainVm, t: YtTheme) {
    LaunchedEffect(Unit) { if (vm.music.isEmpty() && !vm.musicLoading) vm.pickMusic(vm.musicChip) }
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            Modifier.fillMaxWidth().background(t.panel),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MUSIC_TOPICS) { name ->
                ThemedChip(name, vm.musicChip == name, t) { vm.pickMusic(name) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))

        val state = rememberLazyListState()
        LaunchedEffect(state, vm.music.size) {
            snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                .collect { last ->
                    val total = state.layoutInfo.totalItemsCount
                    if (total > 0 && last >= total - 3) vm.loadMoreMusic()
                }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = state,
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(vm.music) { i, v ->
                    val playing = vm.audioItem?.url == v.url
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(if (playing) t.accent.copy(alpha = 0.14f) else t.panel)
                            .border(1.dp, if (playing) t.accent else t.line, shape)
                            .clickable { vm.playAudio(vm.music.toList(), i) }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = v.thumb, contentDescription = null,
                            modifier = Modifier.width(96.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(Modifier.weight(1f)) {
                            Text(v.title, color = t.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val dur = fmtDuration(v.durationSec)
                            Text(
                                v.channel + if (dur.isNotEmpty()) " · $dur" else "",
                                color = t.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        Text(if (playing && vm.audioPlaying) "🔊" else "▶", fontSize = 18.sp, color = t.accent)
                    }
                }
                if (vm.musicLoading && vm.music.isNotEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = t.accent)
                        }
                    }
                }
            }
            if (vm.music.isEmpty()) {
                if (vm.musicLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = t.accent)
                } else {
                    Text(
                        vm.musicMessage ?: "অডিওর তালিকা আসছে…", color = t.muted, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                }
            }
        }
    }
}

/** নিচের ছোট প্লেয়ার: অডিও চললে সব ট্যাবে দেখায় */
@Composable
fun MiniPlayer(vm: MainVm, t: YtTheme) {
    val item = vm.audioItem ?: return
    val d = Audio.player.lengthMs
    val progress = if (d > 0) (Audio.player.positionMs.toFloat() / d).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().background(t.panel)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = t.accent,
            trackColor = t.line
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.thumb, contentDescription = null,
                modifier = Modifier.size(width = 56.dp, height = 36.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = t.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (vm.audioLoading) "লোড হচ্ছে…" else item.channel,
                    color = t.muted, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            MiniBtn("⏮") { vm.audioPrev() }
            MiniBtn(if (vm.audioPlaying) "⏸" else "▶") { vm.toggleAudio() }
            MiniBtn("⏭") { vm.audioNext() }
            MiniBtn("✕") { vm.stopAudio() }
        }
    }
}

@Composable
private fun MiniBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 18.sp) }
}

/* ================= লাইব্রেরি: সংরক্ষিত, পছন্দের, ডাউনলোড, ইতিহাস ================= */

@Composable
fun LibraryTab(vm: MainVm, t: YtTheme) {
    var sub by remember { mutableIntStateOf(0) }
    val labels = listOf("🔖 সংরক্ষিত", "👍 পছন্দের", "⬇ ডাউনলোড", "🕘 ইতিহাস")
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            Modifier.fillMaxWidth().background(t.panel),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(labels) { i, label ->
                ThemedChip(label, sub == i, t) { sub = i }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))
        Box(Modifier.weight(1f)) {
            when (sub) {
                0 -> VideoGrid(vm, t, vm.saved, false, null, "কিছু সংরক্ষণ করা হয়নি।\nভিডিও চালিয়ে 🔖 চাপলে এখানে জমা হবে।", null)
                1 -> VideoGrid(vm, t, vm.liked, false, null, "কোনো ভিডিওতে 👍 দেওয়া হয়নি।", null)
                2 -> DownloadsTab(vm, t)
                else -> VideoGrid(vm, t, vm.history, false, null, "এখনো কিছু দেখা হয়নি।", "🗑 ইতিহাস মুছুন" to { vm.clearHistory() })
            }
        }
    }
}
