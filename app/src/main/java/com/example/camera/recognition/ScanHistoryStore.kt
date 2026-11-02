package com.example.camera.recognition

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Small on-device history of scanned codes (JSON file in app storage, newest first, capped). */
class ScanHistoryStore(context: Context) {

    private val file = File(context.filesDir, "scan_history.json")

    @Synchronized
    fun load(): List<RecognitionResult> = runCatching {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        (0 until array.length()).map {
            val o = array.getJSONObject(it)
            RecognitionResult(o.getString("text"), o.getString("format"), o.getLong("time"))
        }
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(items: List<RecognitionResult>) {
        val array = JSONArray()
        items.take(MAX_ITEMS).forEach {
            array.put(JSONObject().put("text", it.text).put("format", it.format).put("time", it.timestamp))
        }
        val tmp = File(file.parentFile, "scan_history.tmp")
        tmp.writeText(array.toString())
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    private companion object { const val MAX_ITEMS = 200 }
}
