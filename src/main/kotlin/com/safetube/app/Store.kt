package com.safetube.app

import org.json.JSONArray
import org.json.JSONObject

data class DlItem(val video: VideoItem, val dlId: Long, val path: String)

/** সংরক্ষিত, ইতিহাস আর ডাউনলোডের তালিকা কম্পিউটারেই জমা রাখে */
class Store {
    private fun toJson(v: VideoItem): JSONObject = JSONObject()
        .put("url", v.url)
        .put("title", v.title)
        .put("channel", v.channel)
        .put("thumb", v.thumb ?: "")
        .put("dur", v.durationSec)
        .put("views", v.views)

    private fun fromJson(o: JSONObject): VideoItem = VideoItem(
        url = o.optString("url"),
        title = o.optString("title"),
        channel = o.optString("channel"),
        thumb = o.optString("thumb").ifEmpty { null },
        durationSec = o.optLong("dur"),
        views = o.optLong("views")
    )

    fun loadVideos(key: String): List<VideoItem> = try {
        val a = JSONArray(Prefs.getString("list_$key", "[]"))
        (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveVideos(key: String, list: List<VideoItem>) {
        val a = JSONArray()
        list.forEach { a.put(toJson(it)) }
        Prefs.putString("list_$key", a.toString())
    }

    fun loadDownloads(): List<DlItem> = try {
        val a = JSONArray(Prefs.getString("downloads", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            DlItem(fromJson(o.getJSONObject("v")), o.getLong("id"), o.getString("path"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveDownloads(list: List<DlItem>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("v", toJson(it.video)).put("id", it.dlId).put("path", it.path)) }
        Prefs.putString("downloads", a.toString())
    }
}
