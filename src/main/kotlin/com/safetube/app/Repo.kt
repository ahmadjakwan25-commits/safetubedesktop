package com.safetube.app

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream

data class VideoItem(
    val url: String,
    val title: String,
    val channel: String,
    val thumb: String?,
    val durationSec: Long,
    val views: Long
)

data class CommentItem(val author: String, val text: String, val likes: Int, val date: String)

class CommentPage(val items: List<CommentItem>, val next: Page?, val info: CommentsInfo)

fun fmtCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK".format(n / 1_000.0)
    else -> n.toString()
}

class SearchPage(val handler: SearchQueryHandler, val items: List<VideoItem>, val next: Page?)

object Repo {
    private val yt get() = ServiceList.YouTube

    fun search(q: String): SearchPage {
        val h = yt.searchQHFactory.fromQuery(q, listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), "")
        val info = SearchInfo.getInfo(yt, h)
        return SearchPage(h, map(info.relatedItems), info.nextPage)
    }

    fun more(h: SearchQueryHandler, page: Page): SearchPage {
        val p = SearchInfo.getMoreItems(yt, h, page)
        return SearchPage(h, map(p.items), p.nextPage)
    }

    private fun map(list: List<InfoItem>): List<VideoItem> =
        list.filterIsInstance<StreamInfoItem>().map {
            VideoItem(
                url = it.url,
                title = it.name ?: "",
                channel = it.uploaderName ?: "",
                thumb = it.thumbnails.firstOrNull()?.url,
                durationSec = it.duration,
                views = it.viewCount
            )
        }

    private fun mapComments(list: List<CommentsInfoItem>): List<CommentItem> = list.map {
        val raw: Any? = it.commentText
        val text = (raw as? org.schabi.newpipe.extractor.stream.Description)?.content ?: (raw?.toString() ?: "")
        CommentItem(it.uploaderName ?: "", text, it.likeCount, it.textualUploadDate ?: "")
    }

    fun comments(info: StreamInfo): CommentPage {
        val ci = CommentsInfo.getInfo(info.url)
        return CommentPage(mapComments(ci.relatedItems), ci.nextPage, ci)
    }

    fun moreComments(ci: CommentsInfo, page: Page): CommentPage {
        val p = CommentsInfo.getMoreItems(yt, ci, page)
        return CommentPage(mapComments(p.items), p.nextPage, ci)
    }

    // একই ভিডিওর তথ্য ২০ মিনিট মনে রাখে, তাই দ্বিতীয়বার বা আগে থেকে আনা ভিডিও সাথে সাথে চলে
    private val infoCache = LinkedHashMap<String, Pair<Long, StreamInfo>>()

    fun streamInfo(url: String): StreamInfo {
        synchronized(infoCache) {
            val e = infoCache[url]
            if (e != null && System.currentTimeMillis() - e.first < 20 * 60 * 1000) return e.second
        }
        val info = StreamInfo.getInfo(yt, url)
        synchronized(infoCache) {
            infoCache[url] = System.currentTimeMillis() to info
            while (infoCache.size > 16) infoCache.remove(infoCache.keys.first())
        }
        return info
    }

    fun invalidate(url: String) {
        synchronized(infoCache) { infoCache.remove(url) }
    }

    /** অডিওসহ সোজা ভিডিও স্ট্রিম, ৭২০p বা তার নিচের সেরাটা */
    fun bestMuxed(info: StreamInfo): VideoStream? {
        val muxed = info.videoStreams.filter { !it.isVideoOnly && !it.content.isNullOrEmpty() }
        return muxed.filter { it.height <= 720 }.maxByOrNull { it.height } ?: muxed.firstOrNull()
    }

    fun muxedUrl(info: StreamInfo): String? = bestMuxed(info)?.content

    /** এই ভিডিওর সাথে মিলিয়ে আসা অন্য ভিডিও */
    fun related(info: StreamInfo): List<VideoItem> = try {
        map(info.relatedItems)
    } catch (e: Exception) {
        emptyList()
    }

    private fun fmtLabel(st: VideoStream): String {
        val suffix = try { st.format?.suffix?.uppercase() ?: "" } catch (e: Exception) { "" }
        val hd = if (st.height >= 720) " HD" else ""
        return (st.height.toString() + "p " + suffix + hd).trim()
    }

    /** চালানোর উপায়গুলো সেরা থেকে কম ভালো ক্রমে; একটা ব্যর্থ হলে পরেরটা চেষ্টা হয় */
    fun candidates(info: StreamInfo): List<Candidate> {
        val list = mutableListOf<Candidate>()
        val muxed = info.videoStreams.filter { !it.isVideoOnly && !it.content.isNullOrEmpty() }
        val ordered = muxed.filter { it.height <= 720 }.sortedByDescending { it.height } +
            muxed.filter { it.height > 720 }.sortedBy { it.height }
        for (st in ordered) list.add(Candidate(fmtLabel(st), st.content))
        val hls = info.hlsUrl
        if (!hls.isNullOrEmpty()) list.add(Candidate("অটো (HLS)", hls))
        // উচ্চ মান: ভিডিও আর অডিও আলাদা ফাইল জোড়া লাগিয়ে
        val vOnly = info.videoOnlyStreams.filter { !it.content.isNullOrEmpty() }
        val a = info.audioStreams.filter { !it.content.isNullOrEmpty() }.maxByOrNull { it.averageBitrate }
        if (a != null) {
            for (h in listOf(1080, 720, 480)) {
                val same = vOnly.filter { it.height == h }
                val v = same.firstOrNull { (try { it.format?.suffix } catch (e: Exception) { null }) == "mp4" } ?: same.firstOrNull() ?: continue
                list.add(Candidate(fmtLabel(v) + " (জোড়া)", v.content, a.content))
            }
        }
        return list
    }

    /** শুধু অডিও লিংক (মিউজিক ট্যাবের জন্য), না পেলে সাধারণ ভিডিও স্ট্রিম */
    fun audioUrl(info: StreamInfo): String? {
        val a = info.audioStreams.filter { !it.content.isNullOrEmpty() }.maxByOrNull { it.averageBitrate }
        if (a != null) return a.content
        return muxedUrl(info) ?: info.hlsUrl?.takeIf { it.isNotEmpty() }
    }
}
