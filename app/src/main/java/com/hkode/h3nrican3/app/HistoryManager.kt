package com.hkode.h3nrican3.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "ENCRYPT" or "DECRYPT"
    val mode: String, // "SYSTEM" or "PASSWORD"
    val content: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("hkode_history_prefs", Context.MODE_PRIVATE)

    fun getHistory(): List<HistoryEntry> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        val list = mutableListOf<HistoryEntry>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HistoryEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        type = obj.optString("type", "ENCRYPT"),
                        mode = obj.optString("mode", "SYSTEM"),
                        content = obj.optString("content", ""),
                        summary = obj.optString("summary", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun addEntry(type: String, mode: String, content: String, summary: String) {
        val list = getHistory().toMutableList()
        val newEntry = HistoryEntry(
            type = type,
            mode = mode,
            content = content,
            summary = summary
        )
        // Keep up to 30 recent items
        list.add(0, newEntry)
        val capped = if (list.size > 30) list.subList(0, 30) else list
        saveList(capped)
    }

    fun deleteEntry(id: String) {
        val list = getHistory().filterNot { it.id == id }
        saveList(list)
    }

    fun clearAll() {
        prefs.edit().remove("items").apply()
    }

    private fun saveList(items: List<HistoryEntry>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("mode", item.mode)
                put("content", item.content)
                put("summary", item.summary)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
