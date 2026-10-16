package org.antiphish

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "unknown"
                TrustManager.record(context, number)
                if (!TrustManager.isTrusted(context, number)) {
                    NotificationHelper.show(
                        context,
                        "Call from untrusted number",
                        "From $number",
                        "Do not share personal information. Open AntiPhish for details."
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
