package com.safetube.app

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .build()
}

object DownloaderImpl : Downloader() {
    private val client get() = Http.client

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val url = request.url()
        var body: RequestBody? = request.dataToSend()?.toRequestBody()
        if (body == null && (method == "POST" || method == "PUT")) body = ByteArray(0).toRequestBody()

        val builder = okhttp3.Request.Builder().method(method, body).url(url)
            .addHeader("User-Agent", USER_AGENT)
        for ((name, values) in request.headers()) {
            builder.removeHeader(name)
            for (v in values) builder.addHeader(name, v)
        }

        return client.newCall(builder.build()).execute().use { resp ->
            if (resp.code == 429) throw ReCaptchaException("reCaptcha Challenge requested", url)
            Response(
                resp.code,
                resp.message,
                resp.headers.toMultimap(),
                resp.body?.string(),
                resp.request.url.toString()
            )
        }
    }
}
