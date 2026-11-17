package com.safetube.app

import java.io.File
import okhttp3.Request

/** Android-এর DownloadManager-এর বদলে নিজস্ব ডাউনলোডার (OkHttp) */
object Downloader2 {
    private val jobs = HashMap<Long, Thread>()
    @Volatile private var cancelled = HashSet<Long>()

    /** অগ্রগতি: ০..১০০, ব্যর্থ হলে -১ */
    fun start(id: Long, url: String, target: File, onProgress: (Int) -> Unit) {
        val t = Thread {
            val part = File(target.path + ".part")
            try {
                target.parentFile?.mkdirs()
                val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                Http.client.newBuilder().readTimeout(java.time.Duration.ofSeconds(60)).build().newCall(req).execute().use { r ->
                    if (!r.isSuccessful) { onProgress(-1); return@use }
                    val body = r.body ?: run { onProgress(-1); return@use }
                    val total = body.contentLength()
                    var got = 0L
                    var last = -1
                    body.byteStream().use { inp ->
                        part.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                if (id in cancelled) { part.delete(); return@Thread }
                                val n = inp.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                got += n
                                if (total > 0) {
                                    val pct = ((got * 100) / total).toInt().coerceIn(0, 99)
                                    if (pct != last) { last = pct; onProgress(pct) }
                                }
                            }
                        }
                    }
                    if (part.renameTo(target)) onProgress(100) else onProgress(-1)
                }
            } catch (e: Exception) {
                try { part.delete() } catch (x: Exception) { }
                onProgress(-1)
            }
        }
        t.isDaemon = true
        synchronized(jobs) { jobs[id] = t }
        t.start()
    }

    fun cancel(id: Long) {
        cancelled = HashSet(cancelled).apply { add(id) }
    }
}
