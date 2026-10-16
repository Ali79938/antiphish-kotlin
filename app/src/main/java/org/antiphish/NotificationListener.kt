package org.antiphish

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {

    companion object {
        private val MONITORED_PACKAGES = mapOf(
            "com.whatsapp" to "WhatsApp",
            "org.telegram.messenger" to "Telegram",
            "com.facebook.orca" to "Messenger",
            "com.facebook.katana" to "Facebook",
            "com.instagram.android" to "Instagram",
            "com.google.android.apps.messaging" to "Messages",
            "com.viber.voip" to "Viber",
            "org.thoughtcrime.securesms" to "Signal",
            "com.discord" to "Discord",
            "com.snapchat.android" to "Snapchat"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val packageName = sbn.packageName ?: return
            val appName = MONITORED_PACKAGES[packageName] ?: return

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            val body = when {
                bigText.isNotEmpty() -> bigText
                text.isNotEmpty() -> text
                else -> return
            }

            if (body.isBlank()) return

            val sender = title.ifBlank { appName }
            TrustManager.record(this, sender)
            val result = Analyzer.analyzeMessage(sender, body)

            // Log every scan with app source
            History.add(this, appName, sender, body, result.verdict, result.score)

            if (!TrustManager.isTrusted(this, sender) &&
                (result.verdict == "phishing" || result.verdict == "suspicious")
            ) {
                val heading = if (result.verdict == "phishing") {
                    "Phishing detected on $appName"
                } else {
                    "Suspicious message on $appName"
                }
                NotificationHelper.show(
                    this,
                    heading,
                    "From $sender - score ${result.score}/100",
                    "Do not click any links. Open AntiPhish for details."
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
