package com.safetube.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler

enum class Tab { HOME, SHORTS, MUSIC, LIBRARY, SETTINGS }

val MUSIC_TOPICS = listOf("ইসলামিক গজল", "নাত", "হামদ", "নাশিদ", "কুরআন তিলাওয়াত", "কাসিদা", "দোয়া")

class MainVm {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val store = Store()

    var tab by mutableStateOf(Tab.HOME)
    var current by mutableStateOf<VideoItem?>(null)
    var showThemes by mutableStateOf(false)
    var showProfile by mutableStateOf(false)

    // থিম
    var themeId by mutableStateOf(initialTheme())
        private set

    // নতুন সংস্করণ প্রথমবার খুললে ইউটিউব থিম বসে যায়; এরপর আপনার বাছাই মনে থাকে
    private fun initialTheme(): String {
        return Prefs.getString("ytTheme", "ytx")
    }
    val theme: YtTheme get() = YT_THEMES.firstOrNull { it.id == themeId } ?: YT_THEMES[0]
    fun pickTheme(id: String) {
        themeId = id
        Prefs.putString("ytTheme", id)
    }

    // ফিল্টার: কঠোর স্ক্রিনিং সবসময় চালু, বন্ধ করার কোনো উপায় নেই
    val islamicOnly: Boolean = true

    fun keep(v: VideoItem): Boolean = ContentFilter.textScore(v.title + " " + v.channel) >= 1

    // ভ্রমণ/প্রকৃতির সার্চে ইসলামিক শব্দ ছাড়াও (বিনোদন ছাড়া) ভিডিও দেখানো হয়
    private var searchIntent = false
    private val travelOk = HashSet<String>()

    private fun keepSearch(v: VideoItem): Boolean {
        if (!searchIntent) return keep(v)
        val ok = ContentFilter.textScore(v.title + " " + v.channel) >= 0
        if (ok) travelOk.add(v.url)
        return ok
    }

    fun screenStrict(v: VideoItem): Boolean = islamicOnly && !travelOk.contains(v.url)

    // টেনে নতুন ভিডিও আনা
    var pulling by mutableStateOf(false)

    fun pullRefresh() {
        pulling = true
        if (showingSearch && query.isNotBlank()) search(query) else refreshFeed()
    }

    // সার্চ
    var query by mutableStateOf("")
    var items by mutableStateOf<List<VideoItem>>(emptyList())
    var loading by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var showingSearch by mutableStateOf(false)
    var chip by mutableStateOf("")
    var canLoadMore by mutableStateOf(false)
        private set

    private var handler: SearchQueryHandler? = null
    private var nextPage: Page? = null
    private var job: Job? = null

    fun pickChip(name: String) {
        if (name.isEmpty()) {
            if (showingSearch) {
                showingSearch = false
                chip = ""
                query = ""
            } else {
                refreshFeed()
            }
        } else {
            chip = name
            query = name
            search(name)
        }
    }

    fun search(q: String = query) {
        val text = q.trim()
        if (text.isEmpty()) return
        query = text
        if (text != chip) chip = ""
        showingSearch = true
        searchIntent = ContentFilter.topicScore(text) >= 1
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            message = null
            items = emptyList()
            canLoadMore = false
            handler = null
            nextPage = null
            try {
                if (ContentFilter.isBad(text)) {
                    message = "এই বিষয়টি এখানে খোঁজা যায় না।"
                    return@launch
                }
                val first = withContext(Dispatchers.IO) { Repo.search(text) }
                handler = first.handler
                nextPage = first.next
                val found = first.items.filter { keepSearch(it) }.toMutableList()
                var tries = 0
                while (found.size < 10 && nextPage != null && tries < 3) {
                    val more = withContext(Dispatchers.IO) { Repo.more(handler!!, nextPage!!) }
                    nextPage = more.next
                    found += more.items.filter { keepSearch(it) }
                    tries++
                }
                items = found
                canLoadMore = nextPage != null
                if (found.isEmpty()) message = "কিছু পাওয়া যায়নি।"
            } catch (e: Exception) {
                message = "সমস্যা হয়েছে: " + (e.message ?: e.javaClass.simpleName)
            } finally {
                loading = false
                pulling = false
            }
        }
    }

    fun loadMore() {
        val h = handler ?: return
        val p = nextPage ?: return
        if (loading) return
        job = viewModelScope.launch {
            loading = true
            try {
                val found = mutableListOf<VideoItem>()
                var page: Page? = p
                var tries = 0
                while (page != null && found.size < 10 && tries < 3) {
                    val more = withContext(Dispatchers.IO) { Repo.more(h, page!!) }
                    page = more.next
                    found += more.items.filter { keepSearch(it) }
                    tries++
                }
                nextPage = page
                items = items + found
                canLoadMore = page != null
            } catch (e: Exception) {
                message = "আরও আনা যায়নি: " + (e.message ?: e.javaClass.simpleName)
            } finally {
                loading = false
            }
        }
    }

    // হোমের নিজে নিজে আসা ভিডিও
    val feed = mutableStateListOf<VideoItem>()
    var feedLoading by mutableStateOf(false)
    var feedMessage by mutableStateOf<String?>(null)
    private val feedQueue = ArrayDeque<String>()
    private val seen = HashSet<String>()
    private var feedJob: Job? = null
    private var feedGen = 0
    private val feedBase = TOPICS + listOf("ইসলামিক গজল", "হজ্জ ও ওমরাহ", "নবীদের কাহিনী", "তাফসীর", "জুমার খুতবা")

    // আগে একবার দেখানো ভিডিও (এই চালুর সময়ে) টেনে নতুন করে আনলে আর আসবে না
    private val shownEver = HashSet<String>()
    private val feedSuffix = listOf("", " নতুন", " ২০২৬", " সর্বশেষ", " বাংলা", " সম্পূর্ণ", " পর্ব")

    private fun refillQueue() {
        feedQueue.clear()
        history.map { it.channel }.filter { it.isNotEmpty() }.distinct().shuffled().take(3).forEach { feedQueue.addLast(it) }
        // একই বিষয়ে একটু আলাদা শব্দ জুড়ে, যাতে প্রতিবার আলাদা ফলাফল আসে
        feedBase.shuffled().forEach { feedQueue.addLast(it + feedSuffix.random()) }
    }

    private suspend fun feedPass(ignoreShown: Boolean): Int {
        var added = 0
        var tries = 0
        while (added < 8 && tries < 5) {
            if (feedQueue.isEmpty()) refillQueue()
            val q = feedQueue.removeFirst()
            tries++
            val page = runCatching { withContext(Dispatchers.IO) { Repo.search(q) } }.getOrNull() ?: continue
            val batches = mutableListOf(page.items)
            val nx = page.next
            if (nx != null && Random.nextBoolean()) {
                val h = page.handler
                val more = runCatching { withContext(Dispatchers.IO) { Repo.more(h, nx) } }.getOrNull()
                if (more != null) batches.add(more.items)
            }
            for (items in batches) {
                for (v in items) {
                    if (keep(v) && seen.add(v.url) && (ignoreShown || v.url !in shownEver)) {
                        shownEver.add(v.url)
                        feed.add(v)
                        added++
                    }
                }
            }
        }
        return added
    }

    /** পরে চাপা হতে পারে এমন ভিডিওর তথ্য আগে থেকে আনে, যাতে চাপলে দ্রুত চলে */
    fun prefetch(list: List<VideoItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (v in list) {
                runCatching { Repo.streamInfo(v.url) }
            }
        }
    }

    var minimized by mutableStateOf(false)

    fun minimizePlayer() {
        minimized = true
        WindowCtl.fullscreen = false
    }

    fun videoStarted(v: VideoItem) { }

    fun closePlayer() {
        current = null
        minimized = false
        Video.stop()
        WindowCtl.fullscreen = false
    }

    fun refreshFeed() {
        feedJob?.cancel()
        feed.clear()
        seen.clear()
        feedQueue.clear()
        feedMessage = null
        feedLoading = false
        loadFeed()
    }

    fun loadFeed() {
        if (feedLoading) return
        val gen = ++feedGen
        feedJob = viewModelScope.launch {
            feedLoading = true
            try {
                val added = feedPass(false)
                // নতুন কিছু না পেলে আগের দেখানোগুলোও আবার ঘুরিয়ে আনে, যাতে পাতা ফাঁকা না থাকে
                if (feed.isEmpty() && added == 0) {
                    shownEver.clear()
                    feedPass(true)
                }
                if (feed.size in 1..12) prefetch(feed.take(3))
                feedMessage = if (feed.isEmpty()) "ভিডিও আনা যায়নি। ইন্টারনেট দেখে ওপর থেকে নিচে টেনে আবার চেষ্টা করুন।" else null
            } finally {
                if (gen == feedGen) {
                    feedLoading = false
                    pulling = false
                }
            }
        }
    }

    // সংরক্ষিত
    val saved = mutableStateListOf<VideoItem>()
    fun isSaved(v: VideoItem): Boolean = saved.any { it.url == v.url }
    fun toggleSave(v: VideoItem) {
        val i = saved.indexOfFirst { it.url == v.url }
        if (i >= 0) saved.removeAt(i) else saved.add(0, v)
        store.saveVideos("saved", saved)
    }

    // পছন্দের (👍) ভিডিও — শুধু এই ফোনে
    val liked = mutableStateListOf<VideoItem>()
    fun isLiked(v: VideoItem): Boolean = liked.any { it.url == v.url }
    fun toggleLike(v: VideoItem) {
        val i = liked.indexOfFirst { it.url == v.url }
        if (i >= 0) liked.removeAt(i) else liked.add(0, v)
        store.saveVideos("liked", liked)
    }

    // ইতিহাস
    val history = mutableStateListOf<VideoItem>()
    fun addHistory(v: VideoItem) {
        history.removeAll { it.url == v.url }
        history.add(0, v)
        while (history.size > 200) history.removeAt(history.size - 1)
        store.saveVideos("history", history)
    }

    fun clearHistory() {
        history.clear()
        store.saveVideos("history", history)
    }

    // ডাউনলোড
    val downloads = mutableStateListOf<DlItem>()
    val dlProgress = mutableStateMapOf<Long, Int>()

    fun startDownload(v: VideoItem, url: String): Boolean {
        if (downloads.any { it.video.url == v.url }) return false
        val name = "v" + (v.url.hashCode().toLong() and 0xffffffffL) + ".mp4"
        val id = System.nanoTime() and 0x7fffffffffffL
        val target = File(Dirs.videos, name)
        downloads.add(0, DlItem(v, id, target.absolutePath))
        store.saveDownloads(downloads)
        dlProgress[id] = 0
        Downloader2.start(id, url, target) { pct -> dlProgress[id] = pct }
        return true
    }

    /** আগের চালুর অসম্পূর্ণ/সম্পূর্ণ ডাউনলোডের অবস্থা ফাইল দেখে ঠিক করে */
    fun refreshDownloads() {
        for (d in downloads.toList()) {
            val cur = dlProgress[d.dlId]
            if (cur == null) {
                dlProgress[d.dlId] = if (File(d.path).exists()) 100 else -1
            }
        }
    }

    fun deleteDownload(d: DlItem) {
        Downloader2.cancel(d.dlId)
        try { File(d.path).delete() } catch (e: Exception) { }
        try { File(d.path + ".part").delete() } catch (e: Exception) { }
        downloads.removeAll { it.dlId == d.dlId }
        dlProgress.remove(d.dlId)
        store.saveDownloads(downloads)
    }

    /** ডাউনলোড সম্পূর্ণ হলে ফাইলের পথ, নইলে null */
    fun localFile(v: VideoItem): String? {
        val d = downloads.firstOrNull { it.video.url == v.url } ?: return null
        return if ((dlProgress[d.dlId] ?: 0) == 100 && File(d.path).exists()) d.path else null
    }


    // ---------- শর্টস ----------
    val shorts = mutableStateListOf<VideoItem>()
    var shortsLoading by mutableStateOf(false)
    private val shortQueue = ArrayDeque<String>()
    private val seenShorts = HashSet<String>()

    fun loadShorts() {
        if (shortsLoading) return
        viewModelScope.launch {
            shortsLoading = true
            try {
                var added = 0
                var tries = 0
                while (added < 6 && tries < 4) {
                    if (shortQueue.isEmpty()) feedBase.shuffled().forEach { shortQueue.addLast("$it shorts") }
                    val q = shortQueue.removeFirst()
                    tries++
                    var page = runCatching { withContext(Dispatchers.IO) { Repo.search(q) } }.getOrNull() ?: continue
                    var pages = 0
                    while (true) {
                        for (v in page.items) {
                            if (v.durationSec in 1..65 && keep(v) && seenShorts.add(v.url)) {
                                shorts.add(v)
                                added++
                            }
                        }
                        val nx = page.next
                        if (nx == null || pages >= 2 || added >= 6) break
                        val h = page.handler
                        page = runCatching { withContext(Dispatchers.IO) { Repo.more(h, nx) } }.getOrNull() ?: break
                        pages++
                    }
                }
            } finally {
                shortsLoading = false
            }
        }
    }

    // ---------- মিউজিক (অডিও) ----------
    val music = mutableStateListOf<VideoItem>()
    var musicLoading by mutableStateOf(false)
    var musicMessage by mutableStateOf<String?>(null)
    var musicChip by mutableStateOf(MUSIC_TOPICS[0])
    private var musicHandler: SearchQueryHandler? = null
    private var musicNext: Page? = null
    private var musicJob: Job? = null

    var audioItem by mutableStateOf<VideoItem?>(null)
    val audioPlaying: Boolean get() = Audio.player.isPlaying
    var audioLoading by mutableStateOf(false)
    private var audioQueue: List<VideoItem> = emptyList()
    private var audioIndex = 0
    private var audioJob: Job? = null
    private var audioGen = 0

    private suspend fun fillMusic(target: Int) {
        var tries = 0
        while (music.size < target && musicNext != null && tries < 3) {
            val h = musicHandler ?: return
            val p = musicNext ?: return
            val more = withContext(Dispatchers.IO) { Repo.more(h, p) }
            musicNext = more.next
            for (v in more.items) {
                if (keep(v) && music.none { it.url == v.url }) music.add(v)
            }
            tries++
        }
    }

    fun pickMusic(topic: String) {
        musicChip = topic
        musicJob?.cancel()
        music.clear()
        musicHandler = null
        musicNext = null
        musicMessage = null
        musicJob = viewModelScope.launch {
            musicLoading = true
            try {
                val first = withContext(Dispatchers.IO) { Repo.search(topic) }
                musicHandler = first.handler
                musicNext = first.next
                for (v in first.items) if (keep(v) && music.none { it.url == v.url }) music.add(v)
                fillMusic(10)
                if (music.isEmpty()) musicMessage = "কিছু পাওয়া যায়নি।"
            } catch (e: Exception) {
                musicMessage = "সমস্যা হয়েছে: " + (e.message ?: e.javaClass.simpleName)
            } finally {
                musicLoading = false
            }
        }
    }

    fun loadMoreMusic() {
        if (musicLoading || musicNext == null) return
        musicJob = viewModelScope.launch {
            musicLoading = true
            try {
                fillMusic(music.size + 8)
            } catch (e: Exception) {
                musicNext = null
            } finally {
                musicLoading = false
            }
        }
    }

    fun playAudio(list: List<VideoItem>, index: Int) {
        val v = list.getOrNull(index) ?: return
        audioQueue = list
        audioIndex = index
        audioItem = v
        audioJob?.cancel()
        val gen = ++audioGen
        audioJob = viewModelScope.launch {
            audioLoading = true
            try {
                val info = withContext(Dispatchers.IO) { Repo.streamInfo(v.url) }
                val d = try { info.description?.content ?: "" } catch (e: Exception) { "" }
                val verdict = Screening.check(info, v, d, screenStrict(v))
                if (!verdict.ok) {
                    musicMessage = "🚫 বাদ দেওয়া হলো: " + verdict.reason
                    if (index + 1 < list.size) playAudio(list, index + 1)
                    return@launch
                }
                val src = withContext(Dispatchers.IO) { Repo.audioUrl(info) }
                if (src == null) {
                    musicMessage = "এই অডিওর লিংক পাওয়া যায়নি"
                    if (index + 1 < list.size) playAudio(list, index + 1)
                    return@launch
                }
                Video.player.pause()
                Audio.player.onError = {
                    musicMessage = "এই অডিও চালানো যায়নি"
                    if (index + 1 < list.size) viewModelScope.launch { playAudio(list, index + 1) }
                }
                Audio.player.play(Candidate("অডিও", src))
            } catch (e: Exception) {
                musicMessage = "সমস্যা হয়েছে: " + (e.message ?: e.javaClass.simpleName)
            } finally {
                if (gen == audioGen) audioLoading = false
            }
        }
    }

    fun audioNext() {
        if (audioIndex + 1 < audioQueue.size) playAudio(audioQueue, audioIndex + 1)
    }

    fun audioPrev() {
        if (audioIndex > 0) playAudio(audioQueue, audioIndex - 1)
    }

    fun toggleAudio() {
        Audio.player.toggle()
    }

    fun stopAudio() {
        audioJob?.cancel()
        audioGen++
        audioLoading = false
        Audio.player.stop()
        audioItem = null
    }

    init {
        Audio.player.onEnded = {
            viewModelScope.launch { if (audioIndex + 1 < audioQueue.size) audioNext() }
        }
    }

    init {
        saved.addAll(store.loadVideos("saved"))
        liked.addAll(store.loadVideos("liked"))
        history.addAll(store.loadVideos("history"))
        downloads.addAll(store.loadDownloads())
        refreshDownloads()
        loadFeed()
    }
}
