package com.safetube.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.awt.Desktop
import java.io.File
import java.net.URI
import org.json.JSONObject

/** ডেটা রাখার ফোল্ডার: Windows-এ %APPDATA%\MuslimTube, অন্যত্র ~/.muslim-tube */
object Dirs {
    val data: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        val base = if (os.contains("win")) {
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "MuslimTube")
        } else {
            File(System.getProperty("user.home"), ".muslim-tube")
        }
        base.apply { mkdirs() }
    }
    val videos: File by lazy { File(data, "downloads").apply { mkdirs() } }
}

/** ফোনের SharedPreferences-এর বদলে একটা JSON ফাইল */
object Prefs {
    private val file get() = File(Dirs.data, "prefs.json")
    private val obj: JSONObject by lazy {
        try { JSONObject(file.readText()) } catch (e: Exception) { JSONObject() }
    }

    @Synchronized
    private fun flush() {
        try { file.writeText(obj.toString()) } catch (e: Exception) { }
    }

    @Synchronized fun getString(k: String, def: String): String = obj.optString(k, def)
    @Synchronized fun putString(k: String, v: String) { obj.put(k, v); flush() }
    @Synchronized fun getBoolean(k: String, def: Boolean): Boolean = obj.optBoolean(k, def)
    @Synchronized fun putBoolean(k: String, v: Boolean) { obj.put(k, v); flush() }
    @Synchronized fun getInt(k: String, def: Int): Int = obj.optInt(k, def)
    @Synchronized fun putInt(k: String, v: Int) { obj.put(k, v); flush() }
}

fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (e: Exception) { }
}

fun hx(s: String): Color {
    val h = s.removePrefix("#")
    val v = h.toLong(16)
    return if (h.length == 8) Color(
        red = ((v shr 16) and 0xff).toInt(), green = ((v shr 8) and 0xff).toInt(),
        blue = (v and 0xff).toInt(), alpha = ((v shr 24) and 0xff).toInt()
    ) else Color(((v shr 16) and 0xff).toInt(), ((v shr 8) and 0xff).toInt(), (v and 0xff).toInt())
}

/** ছোট বার্তা (Android-এর Toast-এর বদলে), নিচে ২ সেকেন্ড দেখায় */
object Toast {
    var text by mutableStateOf<String?>(null)
    private var gen = 0
    fun show(msg: String) {
        text = msg
        val g = ++gen
        Thread {
            Thread.sleep(2200)
            if (g == gen) text = null
        }.apply { isDaemon = true }.start()
    }
}
