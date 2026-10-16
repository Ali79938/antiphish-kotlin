package org.antiphish

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryEntry(
    val timestamp: Long,
    val source: String,
    val sender: String,
    val body: String,
    val verdict: String,
    val score: Int
)

object History {
    private const val PREFS = "antiphish_history"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 200

    fun add(context: Context, source: String, sender: String, body: String, verdict: String, score: Int) {
        val entries = getAll(context).toMutableList()
        entries.add(
            0,
            HistoryEntry(
                timestamp = System.currentTimeMillis(),
                source = source,
                sender = sender,
                body = body,
                verdict = verdict,
                score = score
            )
        )
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(entries.size - 1)
        }

        val arr = JSONArray()
        for (e in entries) {
            val o = JSONObject()
            o.put("ts", e.timestamp)
            o.put("source", e.source)
            o.put("sender", e.sender)
            o.put("body", e.body)
            o.put("verdict", e.verdict)
            o.put("score", e.score)
            arr.put(o)
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }

    fun getAll(context: Context): List<HistoryEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<HistoryEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    HistoryEntry(
                        timestamp = o.optLong("ts"),
                        source = o.optString("source"),
                        sender = o.optString("sender"),
                        body = o.optString("body"),
                        verdict = o.optString("verdict"),
                        score = o.optInt("score")
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }

    fun formatTime(ts: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return fmt.format(Date(ts))
    }
}
