package org.antiphish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val sender = messages.firstOrNull()?.originatingAddress ?: "unknown"
            val body = messages.joinToString(" ") { it.messageBody ?: "" }

            TrustManager.record(context, sender)
            val result = Analyzer.analyzeMessage(sender, body)

            // Log every scan
            History.add(context, "SMS", sender, body, result.verdict, result.score)

            if (!TrustManager.isTrusted(context, sender) &&
                (result.verdict == "phishing" || result.verdict == "suspicious")) {
                val title = if (result.verdict == "phishing") "Phishing detected" else "Suspicious message"
                NotificationHelper.show(
                    context,
                    title,
                    "From $sender - score ${result.score}/100",
                    "Do not click any links. Open AntiPhish for details."
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
