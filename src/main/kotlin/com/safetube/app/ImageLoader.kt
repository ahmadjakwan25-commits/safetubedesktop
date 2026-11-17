package com.safetube.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/** Coil-এর বদলে ছোট নিজস্ব ছবি-লোডার (থাম্বনেইলের জন্য), ২৫০টা ছবি মনে রাখে */
private object ImgCache {
    val map: MutableMap<String, ImageBitmap> = Collections.synchronizedMap(
        object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
            override fun removeEldestEntry(e: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 250
        }
    )
}

private suspend fun loadImage(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    ImgCache.map[url]?.let { return@withContext it }
    try {
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        Http.client.newCall(req).execute().use { r ->
            val bytes = r.body?.bytes() ?: return@use null
            val img = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
            ImgCache.map[url] = img
            img
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun AsyncImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bmp by produceState<ImageBitmap?>(ImgCache.map[model ?: ""], model) {
        value = if (model.isNullOrEmpty()) null else loadImage(model)
    }
    val b = bmp
    if (b != null) {
        Image(b, contentDescription, modifier, contentScale = contentScale)
    } else {
        androidx.compose.foundation.layout.Box(modifier.background(Color(0x22888888)))
    }
}

@Composable
fun resImage(name: String): androidx.compose.ui.graphics.painter.Painter {
    val bmp = androidx.compose.runtime.remember(name) {
        androidx.compose.ui.res.useResource(name) { androidx.compose.ui.res.loadImageBitmap(it) }
    }
    return androidx.compose.ui.graphics.painter.BitmapPainter(bmp)
}
