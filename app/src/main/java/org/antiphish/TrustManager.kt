package org.antiphish

import android.content.Context

object TrustManager {
    private const val PREFS = "antiphish_trust"

    fun record(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt("$key:count", 0) + 1
        prefs.edit().putInt("$key:count", count).apply()
    }

    fun isTrusted(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt("$key:count", 0) >= 3
    }
}
