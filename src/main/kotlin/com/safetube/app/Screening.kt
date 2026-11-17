package com.safetube.app

import org.schabi.newpipe.extractor.stream.StreamInfo

data class Verdict(val ok: Boolean, val reason: String)

/**
 * ভিডিও চালানোর আগে স্ক্রিনিং: শিরোনাম, চ্যানেল, ট্যাগ, বিবরণ, ক্যাটাগরি আর বয়সসীমা দেখে ঠিক করে।
 * ইসলামিক শব্দ বেশি হলে চলে, বিনোদনের শব্দ বেশি হলে আটকায়। সবচেয়ে খারাপ শব্দ পেলে সরাসরি আটকায়।
 */
object Screening {
    // এই ধরনের ক্যাটাগরির ভিডিওতে ইসলামিক প্রমাণ আরেকটু বেশি লাগে
    private val RISKY = listOf(
        "film", "entertainment", "comedy", "shows", "movies", "trailers", "gaming", "sports"
    )

    fun check(info: StreamInfo, v: VideoItem, desc: String, strict: Boolean): Verdict {
        if (info.ageLimit > 0) return Verdict(false, "ভিডিওটিতে বয়সসীমা দেওয়া আছে")

        val tags = try { info.tags.joinToString(" ") } catch (e: Exception) { "" }
        val uploader = info.uploaderName ?: ""
        val head = v.title + " " + uploader + " " + tags
        val body = desc.take(1500)

        if (ContentFilter.isStrongBad(head) || ContentFilter.isStrongBad(body)) {
            return Verdict(false, "শিরোনাম, ট্যাগ বা বিবরণে অনুচিত শব্দ আছে")
        }
        if (strict) {
            val base = ContentFilter.textScore(v.title + " " + uploader)
            // ভ্রমণ/প্রকৃতির ভিডিও: শিরোনামে সমস্যা নেই আর ট্যাগ বা বিবরণে ভ্রমণ-প্রকৃতির শব্দ আছে
            val travelOk = base >= 0 && ContentFilter.topicScore(head + " " + body.take(600)) >= 2
            if (base < 1 && !travelOk) {
                return Verdict(false, "ইসলামিক বা ভ্রমণ-প্রকৃতির বিষয়ের প্রমাণ কম, বা বিনোদনের শব্দ বেশি")
            }
        }
        val cat = (info.category ?: "").lowercase()
        val total = ContentFilter.islamicScore(head + " " + body)
        if (RISKY.any { cat.contains(it) } && total < 2) {
            return Verdict(false, "বিনোদনের ধরনের ভিডিও, ইসলামিক বিষয়ের পর্যাপ্ত প্রমাণ নেই")
        }
        return Verdict(true, "")
    }
}
