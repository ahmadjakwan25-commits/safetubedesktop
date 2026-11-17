package com.safetube.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val TOPICS = listOf("কুরআন তিলাওয়াত", "ওয়াজ", "দোয়া", "ইসলামিক জ্ঞান", "হাদিস", "সীরাত", "নাত", "রমজান")
private val GOLD_LINE = listOf(Color.Transparent, Color(0xFFFDE68A), Color(0xFFF6C445), Color(0xFFFDE68A), Color.Transparent)

fun fmtDuration(sec: Long): String {
    if (sec <= 0) return ""
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/* ---------- হেডার: চ্যাট অ্যাপের ইউটিউবের মতো সোনালি ব্যাজ + গ্রেডিয়েন্ট ---------- */

@Composable
fun HeaderBtn(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(38.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.2f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 17.sp, color = Color.White) }
}

@Composable
fun YtHeader(t: YtTheme, onBack: (() -> Unit)? = null, onTheme: (() -> Unit)? = null, onSettings: (() -> Unit)? = null) {
    if (t.headerStyle == HeaderStyle.YOUTUBE) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(t.panel)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Text(
                    "←", color = t.text, fontSize = 22.sp,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Box(
                Modifier
                    .size(width = 32.dp, height = 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF0000)),
                contentAlignment = Alignment.Center
            ) { Text("▶", color = Color.White, fontSize = 11.sp) }
            Spacer(Modifier.width(6.dp))
            Text("মুসলিম টিউব", color = t.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (onTheme != null) {
                Text(
                    "🎨", fontSize = 20.sp,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onTheme).padding(10.dp)
                )
            }
            if (onSettings != null) {
                Text(
                    "⚙️", fontSize = 20.sp,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onSettings).padding(10.dp)
                )
            }
        }
        return
    }
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(t.header))
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    HeaderBtn("←", onBack)
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFFFFBE0), Color(0xFFF6C445), Color(0xFFC98D0B)))),
                    contentAlignment = Alignment.Center
                ) { Text("🕌", fontSize = 22.sp) }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("মুসলিম টিউব", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Box(
                        Modifier
                            .padding(top = 3.dp)
                            .size(width = 50.dp, height = 2.dp)
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFDE68A), Color.Transparent)))
                    )
                }
                if (onTheme != null) {
                    HeaderBtn("🎨", onTheme)
                    Spacer(Modifier.width(6.dp))
                }
                if (onSettings != null) HeaderBtn("⚙️", onSettings)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(GOLD_LINE))
        )
    }
}

/* ---------- সব ট্যাবে একই ধরনের চিপ (ইউটিউব থিমে ইউটিউবের মতো) ---------- */

@Composable
fun ThemedChip(label: String, on: Boolean, t: YtTheme, avatar: String? = null, onClick: () -> Unit) {
    if (t.headerStyle == HeaderStyle.YOUTUBE) {
        val bg = if (on) t.text else (if (t.dark) Color(0xFF272727) else Color(0xFFF2F2F2))
        val fg = if (on) t.bg else t.text
        Text(
            label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )
        return
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (on) t.chipOn else t.bg)
            .border(1.dp, if (on) t.chipOn else t.line, shape)
            .clickable(onClick = onClick)
            .padding(start = if (avatar != null) 6.dp else 14.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatar != null) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(if (on) t.onAccent else t.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(avatar, color = if (on) t.chipOn else t.onAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = if (on) t.onAccent else t.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/* ---------- মূল পাতা: হেডার + ট্যাব + নিচের মেনু ---------- */

@Composable
fun MainScreen(vm: MainVm) {
    Box(Modifier.fillMaxSize()) {
        MainScreenBody(vm)
        ProfileDrawer(vm, vm.theme)
    }
}

@Composable
private fun MainScreenBody(vm: MainVm) {
    val t = vm.theme
    Column(
        Modifier
            .fillMaxSize()
            .background(t.bg)
    ) {
        YtHeader(t, onTheme = { vm.showThemes = true }, onSettings = { vm.showProfile = true })
        Box(Modifier.weight(1f)) {
            when (vm.tab) {
                Tab.HOME -> HomeTab(vm, t)
                Tab.SHORTS -> ShortsTab(vm, t)
                Tab.MUSIC -> MusicTab(vm, t)
                Tab.LIBRARY -> LibraryTab(vm, t)
                Tab.SETTINGS -> HomeTab(vm, t)
            }
            // ভিডিও চলার সময় ব্যাক চাপলে ছোট আকারে ভেসে থাকে
            if (vm.current != null && vm.minimized) {
                MiniVideo(vm, t, Modifier.align(Alignment.BottomEnd).padding(12.dp))
            }
        }
        MiniPlayer(vm, t)
        BottomBar(vm, t)
    }

    if (vm.showThemes) {
        AlertDialog(
            onDismissRequest = { vm.showThemes = false },
            confirmButton = { TextButton(onClick = { vm.showThemes = false }) { Text("বন্ধ") } },
            title = { Text("🎨 থিম বেছে নিন") },
            text = {
                LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(YT_THEMES) { th ->
                        val sel = th.id == vm.themeId
                        val shape = RoundedCornerShape(12.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(if (sel) th.accent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (sel) th.accent else t.line, shape)
                                .clickable { vm.pickTheme(th.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(th.accent))
                            Box(Modifier.size(22.dp).clip(CircleShape).background(th.swatch2).border(1.dp, t.line, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text(th.name, Modifier.weight(1f), fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                            if (sel) Text("✓", color = th.accent, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun BottomBar(vm: MainVm, t: YtTheme) {
    val tabs = listOf(
        Triple(Tab.HOME, "🏠", "হোম"),
        Triple(Tab.SHORTS, "🎞", "শর্টস"),
        Triple(Tab.MUSIC, "🎵", "মিউজিক"),
        Triple(Tab.LIBRARY, "📚", "লাইব্রেরি"),
        Triple(Tab.SETTINGS, "⚙️", "সেটিংস")
    )
    Column(Modifier.fillMaxWidth().background(t.panel)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
            for ((tab, icon, label) in tabs) {
                val on = if (tab == Tab.SETTINGS) vm.showProfile else vm.tab == tab
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on && t.headerStyle != HeaderStyle.YOUTUBE) t.accent.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { if (tab == Tab.SETTINGS) vm.showProfile = true else vm.tab = tab }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(icon, fontSize = 20.sp)
                    Text(
                        label, fontSize = 11.sp, maxLines = 1,
                        color = if (on) (if (t.headerStyle == HeaderStyle.YOUTUBE) t.text else t.chipOn) else t.muted,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/* ---------- হোম ট্যাব: সার্চ + বিষয় + নিজে নিজে আসা ভিডিও ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(vm: MainVm, t: YtTheme) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(t.panel)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val ytLook = t.headerStyle == HeaderStyle.YOUTUBE
            val fieldBg = if (ytLook) (if (t.dark) Color(0xFF272727) else Color(0xFFF2F2F2)) else t.bg
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                singleLine = true,
                placeholder = { Text("ইসলামিক ভিডিও খুঁজুন…", color = t.muted) },
                shape = RoundedCornerShape(if (ytLook) 28.dp else 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = t.text,
                    unfocusedTextColor = t.text,
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    focusedBorderColor = if (ytLook) Color.Transparent else t.accent,
                    unfocusedBorderColor = if (ytLook) Color.Transparent else t.line,
                    cursorColor = t.accent
                ),
                modifier = Modifier.weight(1f).onPreviewKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) { vm.search(); true } else false
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() })
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(width = 46.dp, height = 52.dp)
                    .clip(if (ytLook) CircleShape else RoundedCornerShape(12.dp))
                    .background(if (ytLook) Color.Transparent else t.accent)
                    .clickable { vm.search() },
                contentAlignment = Alignment.Center
            ) { Text("🔍", fontSize = 19.sp, color = t.onAccent) }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(width = 46.dp, height = 52.dp)
                    .clip(CircleShape)
                    .clickable { vm.pullRefresh() },
                contentAlignment = Alignment.Center
            ) { Text(if (vm.pulling) "⏳" else "🔄", fontSize = 19.sp) }
        }

        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(t.panel),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("") + TOPICS) { name ->
                val on = if (name.isEmpty()) !vm.showingSearch else (vm.showingSearch && vm.chip == name)
                val label = if (name.isEmpty()) "সব" else name
                ThemedChip(label, on, t, if (name.isEmpty()) "★" else name.take(1)) { vm.pickChip(name) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))

        val searching = vm.showingSearch
        val list = if (searching) vm.items else vm.feed
        val loading = if (searching) vm.loading else vm.feedLoading
        val msg = if (searching) vm.message else vm.feedMessage
        Box(Modifier.fillMaxSize()) {
            VideoGrid(
                vm, t, list, loading,
                { if (searching) { if (vm.canLoadMore) vm.loadMore() } else vm.loadFeed() },
                msg ?: "ভিডিও আসছে…", null
            )
        }
    }
}

/* ---------- ভিডিওর গ্রিড (হোম, সংরক্ষিত, ইতিহাস সবখানে একই) ---------- */

@Composable
fun VideoGrid(
    vm: MainVm,
    t: YtTheme,
    list: List<VideoItem>,
    loading: Boolean,
    nearEnd: (() -> Unit)?,
    emptyText: String,
    topAction: Pair<String, () -> Unit>?
) {
    val minCell = when (t.layout) {
        CardLayout.GRID, CardLayout.MAG -> 190.dp
        CardLayout.YT -> 300.dp
        else -> 400.dp
    }
    val ytLayout = t.layout == CardLayout.YT
    val state = rememberLazyGridState()
    if (nearEnd != null) {
        LaunchedEffect(state, list.size) {
            snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                .collect { last ->
                    val total = state.layoutInfo.totalItemsCount
                    if (total > 0 && last >= total - 4) nearEnd()
                }
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minCell),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = if (ytLayout) PaddingValues(top = 4.dp, bottom = 12.dp) else PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(if (ytLayout) 18.dp else 10.dp)
        ) {
            if (topAction != null && list.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = topAction.second) { Text(topAction.first, color = t.chipOn) }
                    }
                }
            }
            items(list) { v -> VideoCard(v, t) { vm.current = v } }
            if (loading && list.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = t.accent)
                    }
                }
            }
        }
        if (list.isEmpty()) {
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = t.accent)
            } else {
                Text(emptyText, Modifier.align(Alignment.Center).padding(24.dp), color = t.muted, textAlign = TextAlign.Center)
            }
        }
    }
}

/* ---------- ডাউনলোড ট্যাব ---------- */

@Composable
fun DownloadsTab(vm: MainVm, t: YtTheme) {
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshDownloads()
            delay(1500)
        }
    }
    if (vm.downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "কিছু ডাউনলোড করা নেই।\nভিডিও চালিয়ে ⬇ চাপলে এখানে আসবে।",
                color = t.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp)
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(vm.downloads) { d ->
            val pct = vm.dlProgress[d.dlId] ?: 0
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(t.panel)
                    .border(1.dp, t.line, shape)
                    .clickable(enabled = pct == 100) { vm.current = d.video }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = d.video.thumb, contentDescription = null,
                    modifier = Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f)) {
                    Text(d.video.title, color = t.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            pct == 100 -> "✅ ডাউনলোড হয়েছে — চাপলে চলবে"
                            pct < 0 -> "⚠ ব্যর্থ হয়েছে"
                            else -> "⏳ নামছে… $pct%"
                        },
                        color = if (pct < 0) Color(0xFFE53935) else t.muted, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                TextButton(onClick = { vm.deleteDownload(d) }) { Text("🗑", fontSize = 20.sp) }
            }
        }
    }
}

/* ---------- প্রোফাইল: আলাদা পূর্ণ পাতা হিসেবে খোলে (ব্যাক বা ← চাপলে ফেরে) ---------- */

private const val MIFTAH_URL = "https://ahmadjakwan25-commits.github.io"

@Composable
fun ProfileDrawer(vm: MainVm, t: YtTheme) {
    AnimatedVisibility(
        visible = vm.showProfile,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFE8F5EF), Color(0xFFF3F7F5), Color(0xFFFBF9F1))))
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "←", color = Color(0xFF16201B), fontSize = 24.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { vm.showProfile = false }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Text("প্রোফাইল", color = Color(0xFF16201B), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.weight(1f)) {
                ProfileContent(vm)
            }
        }
    }
}

@Composable
private fun ProfileContent(vm: MainVm) {
    var showApps by remember { mutableStateOf(false) }
    val ink = Color(0xFF16201B)
    val soft = Color(0xFF5E6B64)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ওপরের সাদা চিপ ডিজাইন: ওপরে আইকন, নিচে নাম
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFE9ECF3))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            ChipLogo(if (maxWidth < 260.dp) maxWidth else 260.dp)
        }

        // অ্যাপ নির্মাতা
        ProfileRow("✍️", Color(0xFFDDEFE7), Color(0xFF7BC8A4)) {
            Column(Modifier.weight(1f)) {
                Text("অ্যাপ নির্মাতা", color = soft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("যাকওয়ান আহমদ", color = ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text("মুদাররিস", color = soft, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                Text("কেন্দ্রীয় দারুল উলুম বাবুস সালাম মাদরাসা ঢাকা।", color = ink, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        // আমার অন্যান্য অ্যাপ (হাইলাইট করা)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFDE68A), Color(0xFFF6C445), Color(0xFFE6AE2C))))
                .drawBehind { drawRect(Color(0xFFB8860B), size = Size(5.dp.toPx(), size.height)) }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showApps = !showApps }
                    .padding(start = 14.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("★", fontSize = 18.sp, color = Color(0xFF3B2A0A))
                Spacer(Modifier.width(8.dp))
                Text("আমার অন্যান্য অ্যাপ", color = Color(0xFF3B2A0A), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text(if (showApps) "▴" else "▾", color = Color(0xFF3B2A0A), fontSize = 14.sp)
            }
            if (showApps) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, bottom = 12.dp)) {
                    // লিংক আইকনের ভেতরে লুকানো; ওপরে আইকন, নিচে নাম
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x66FFFFFF))
                            .clickable {
                                openUrl(MIFTAH_URL)
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = resImage("almiftah_icon.png"),
                            contentDescription = "আল-মিফতাহ",
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp))
                        )
                        Text("আল-মিফতাহ", color = Color(0xFF3B2A0A), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        // বাকি ব্যবস্থাগুলো
        ProfileRow("🎨", Color(0xFFD9EEF3), Color(0xFF6BB5D6), onClick = { vm.showThemes = true }) {
            Column(Modifier.weight(1f)) {
                Text("থিম বদলান", color = ink, fontSize = 15.sp)
                Text("১৬টি থিমের একটি বেছে নিন", color = soft, fontSize = 11.sp)
            }
        }
        ProfileRow("🔄", Color(0xFFF2F1E2), Color(0xFFEAC66B), onClick = {
            vm.showProfile = false
            vm.tab = Tab.HOME
            vm.refreshFeed()
        }) {
            Column(Modifier.weight(1f)) {
                Text("নতুন ভিডিও আনুন", color = ink, fontSize = 15.sp)
                Text("হোমে ওপর থেকে নিচে টানলেও আসে", color = soft, fontSize = 11.sp)
            }
        }
        ProfileRow("🔒", Color(0xFFEDEBEF), Color(0xFFE8A3B3)) {
            Column(Modifier.weight(1f)) {
                Text("কঠোর স্ক্রিনিং — সবসময় চালু", color = ink, fontSize = 15.sp)
                Text("ভিডিও চালানোর আগে প্রতিটা ভিডিও যাচাই হয়। এটা বন্ধ করা যায় না।", color = soft, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        ProfileRow("🗑", Color(0xFFE3EAEE), Color(0xFFA8B2BC), onClick = { vm.clearHistory() }) {
            Column(Modifier.weight(1f)) {
                Text("ইতিহাস মুছুন", color = ink, fontSize = 15.sp)
                Text("দেখার তালিকা পরিষ্কার হবে (সংরক্ষিত থাকবে)", color = soft, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        Text(
            "মুসলিম টিউব — ফিল্টারসহ ইউটিউব ক্লায়েন্ট।\nসংরক্ষিত, ইতিহাস আর ডাউনলোড শুধু এই ফোনে থাকে।",
            color = soft, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(4.dp)
        )
    }
}

/** বাম পাশে রঙিন দাগওয়ালা হালকা রঙের সারি */
@Composable
private fun ProfileRow(
    emoji: String,
    bg: Color,
    bar: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    var m = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(bg)
        .drawBehind { drawRect(bar, size = Size(5.dp.toPx(), size.height)) }
    if (onClick != null) m = m.clickable(onClick = onClick)
    Row(
        m.padding(start = 14.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        content()
    }
}

/** সাদা চিপ-ডিজাইন (নরম ছায়ার ফোলা সাদা বর্গ, চারপাশে গোল প্রান্তের পিন): ওপরে আইকন, নিচে নাম */
@Composable
private fun ChipLogo(size: Dp) {
    val k = size.value / 300f
    fun d(x: Float): Dp = (x * k).dp
    val pinShape = RoundedCornerShape(d(11f))
    val pinColor = Color(0xFFF7F8FC)
    val sh = Color(0x44566B8C)
    Box(Modifier.size(size)) {
        for (i in 0 until 5) {
            val o = d(68f + i * 36f)
            Box(Modifier.offset(x = o, y = d(10f)).size(width = d(20f), height = d(70f)).shadow(d(6f), pinShape, false, sh, sh).background(pinColor, pinShape))
            Box(Modifier.offset(x = o, y = d(220f)).size(width = d(20f), height = d(70f)).shadow(d(6f), pinShape, false, sh, sh).background(pinColor, pinShape))
            Box(Modifier.offset(x = d(10f), y = o).size(width = d(70f), height = d(20f)).shadow(d(6f), pinShape, false, sh, sh).background(pinColor, pinShape))
            Box(Modifier.offset(x = d(220f), y = o).size(width = d(70f), height = d(20f)).shadow(d(6f), pinShape, false, sh, sh).background(pinColor, pinShape))
        }
        val chip = RoundedCornerShape(d(34f))
        Column(
            Modifier
                .offset(x = d(50f), y = d(50f))
                .size(d(200f))
                .shadow(d(14f), chip, false, sh, sh)
                .background(Color(0xFFFCFCFE), chip),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = resImage("app_icon_art.png"),
                contentDescription = "মুসলিম টিউব",
                modifier = Modifier.size(d(92f)).clip(RoundedCornerShape(d(22f)))
            )
            Text(
                "মুসলিম টিউব", color = Color(0xFF0A4A38), fontSize = (22f * k).sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = d(10f))
            )
        }
    }
}

@Composable
private fun SettingsRow(title: String, sub: String, t: YtTheme, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(t.panel).border(1.dp, t.line, shape).clickable(onClick = onClick).padding(14.dp)
    ) {
        Text(title, color = t.text, fontWeight = FontWeight.Bold)
        Text(sub, color = t.muted, fontSize = 12.sp)
    }
}

/* ---------- ভিডিও কার্ড ---------- */

@Composable
fun VideoCard(v: VideoItem, t: YtTheme, onClick: () -> Unit) {
    val hue = (((v.channel.hashCode() % 360) + 360) % 360).toFloat()
    val h1 = Color.hsl(hue, 0.75f, 0.48f)
    when (t.layout) {
        CardLayout.MAG -> MagCard(v, onClick)
        CardLayout.CINEMA -> CinemaCard(v, onClick)
        CardLayout.LIST -> FrameCard(v, t, h1, true, onClick)
        CardLayout.GRID -> FrameCard(v, t, h1, false, onClick)
        CardLayout.YT -> YtCard(v, t, h1, onClick)
    }
}

@Composable
private fun Thumb(v: VideoItem, modifier: Modifier, border: Pair<Dp, Color>?, shape: Shape) {
    var m = modifier.aspectRatio(16f / 9f).clip(shape)
    if (border != null) m = m.border(border.first, border.second, shape)
    AsyncImage(model = v.thumb, contentDescription = null, modifier = m, contentScale = ContentScale.Crop)
}

@Composable
private fun Tag(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text, color = fg, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 8.dp, vertical = 1.dp)
    )
}

@Composable
private fun Tags(v: VideoItem, bg: Color, fg: Color, center: Boolean = false) {
    val second = listOfNotNull(
        fmtDuration(v.durationSec).ifEmpty { null },
        if (v.views > 0) "${v.views} বার" else null
    ).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = if (center) Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally) else Arrangement.spacedBy(4.dp)
    ) {
        if (v.channel.isNotEmpty()) Tag(v.channel, bg, fg, Modifier.weight(1f, fill = false))
        if (second.isNotEmpty()) Tag(second, bg, fg)
    }
}

@Composable
private fun FrameCard(v: VideoItem, t: YtTheme, h1: Color, row: Boolean, onClick: () -> Unit) {
    val d = t.deco
    val shape: Shape = when (d) {
        Deco.ARCH -> RoundedCornerShape(20.dp, 20.dp, 14.dp, 14.dp)
        Deco.PAPER -> RoundedCornerShape(6.dp)
        else -> RoundedCornerShape(14.dp)
    }
    val imgShape: Shape = when (d) {
        Deco.ARCH -> RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
        Deco.PAPER -> RoundedCornerShape(3.dp)
        else -> RoundedCornerShape(10.dp)
    }
    val bar: Color? = when (d) {
        Deco.HUE -> h1
        Deco.GOLD -> Color(0xFFD4A017)
        Deco.NEON -> Color(0xFF22D3EE)
        else -> null
    }
    val borderCol: Color = when (d) {
        Deco.HUE -> h1.copy(alpha = 0.5f)
        Deco.PLAIN -> Color.Transparent
        Deco.GOLD -> Color(0xFFF0E1B8)
        Deco.NEON -> Color(0x8C22D3EE)
        Deco.ARCH -> Color(0xFFD4A017)
        Deco.PAPER -> Color(0xFFB08D57)
    }
    val borderW: Dp = when (d) {
        Deco.ARCH -> 2.dp
        Deco.PAPER -> 1.5.dp
        Deco.PLAIN -> 0.dp
        else -> 1.dp
    }
    val bg: Brush = when (d) {
        Deco.ARCH -> Brush.verticalGradient(listOf(Color(0xFFFFFDF5), Color(0xFFF7EFD2)))
        Deco.PAPER -> SolidColor(Color(0xFFFFFAF0))
        Deco.NEON -> SolidColor(t.panel.copy(alpha = 0.75f))
        Deco.HUE -> Brush.horizontalGradient(listOf(h1.copy(alpha = 0.22f).compositeOver(t.panel), t.panel))
        else -> SolidColor(t.panel)
    }
    val tagBg: Color
    val tagFg: Color
    when (d) {
        Deco.HUE -> { tagBg = h1.copy(alpha = 0.18f); tagFg = t.text }
        Deco.GOLD, Deco.ARCH -> { tagBg = Color(0xFFFDF1CF); tagFg = Color(0xFF6B4E0A) }
        Deco.NEON -> { tagBg = Color(0x2422D3EE); tagFg = Color(0xFFBFF6FF) }
        Deco.PAPER -> { tagBg = Color(0xFFEFE0BF); tagFg = Color(0xFF5E4520) }
        Deco.PLAIN -> { tagBg = Color(0xFFF2F2F2); tagFg = t.muted }
    }
    val imgBorder: Pair<Dp, Color>? = when (d) {
        Deco.HUE -> 2.dp to h1
        Deco.ARCH -> 2.dp to Color(0xFFD4A017)
        Deco.NEON -> 1.dp to Color(0x9922D3EE)
        Deco.PAPER -> 3.dp to Color.White
        else -> null
    }

    var mod = Modifier.fillMaxWidth().clip(shape).background(bg)
    if (borderW > 0.dp) mod = mod.border(borderW, borderCol, shape)
    if (bar != null) mod = mod.drawBehind { drawRect(bar, size = Size(6.dp.toPx(), size.height)) }
    mod = mod.clickable(onClick = onClick)
        .padding(start = if (bar != null) 12.dp else 6.dp, top = 6.dp, end = 6.dp, bottom = 6.dp)

    val center = d == Deco.ARCH
    if (row) {
        Row(mod, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumb(v, Modifier.width(132.dp), imgBorder, imgShape)
            Column(Modifier.weight(1f)) {
                Text(v.title, color = t.text, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Tags(v, tagBg, tagFg)
            }
        }
    } else {
        Column(mod) {
            Thumb(v, Modifier.fillMaxWidth(), imgBorder, imgShape)
            Text(
                v.title, color = t.text, fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = if (center) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            Tags(v, tagBg, tagFg, center)
        }
    }
}

@Composable
private fun CinemaCard(v: VideoItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Color(0xFF141414)).clickable(onClick = onClick)) {
        AsyncImage(
            model = v.thumb, contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(start = 13.dp, end = 13.dp, top = 11.dp, bottom = 14.dp)) {
            Text(v.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Tags(v, Color(0xFF262626), Color(0xFFCCCCCC))
        }
    }
}

@Composable
private fun MagCard(v: VideoItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(shape).background(Color(0xFF111111)).clickable(onClick = onClick)) {
        AsyncImage(model = v.thumb, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE0000000))))
                .padding(start = 10.dp, end = 10.dp, top = 34.dp, bottom = 9.dp)
        ) {
            Text(v.title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Tags(v, Color(0x33FFFFFF), Color.White)
        }
    }
}

/* ---------- ইউটিউবের মতো কার্ড: বড় থাম্বনেইল, নিচে চ্যানেলের গোল ছবি, শিরোনাম, ⋮ ---------- */

@Composable
private fun YtCard(v: VideoItem, t: YtTheme, hue: Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(Modifier.padding(horizontal = 12.dp)) {
            AsyncImage(
                model = v.thumb, contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(t.line),
                contentScale = ContentScale.Crop
            )
            val d = fmtDuration(v.durationSec)
            if (d.isNotEmpty()) {
                Text(
                    d, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(hue),
                contentAlignment = Alignment.Center
            ) {
                Text(v.channel.trimStart('@').take(1).ifEmpty { "?" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    v.title, color = t.text, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                val meta = listOfNotNull(
                    v.channel.ifEmpty { null },
                    if (v.views > 0) "${fmtCount(v.views)} বার দেখা" else null
                ).joinToString(" • ")
                Text(meta, color = t.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            Text("⋮", color = t.muted, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 12.dp))
        }
    }
}
