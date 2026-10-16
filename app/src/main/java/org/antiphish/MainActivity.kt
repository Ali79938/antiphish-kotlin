package org.antiphish

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLoginScreen()
    }

    // ---------- Login ----------
    private fun showLoginScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 200, 60, 60)
        }

        val title = TextView(this).apply {
            text = "AntiPhish"
            textSize = 32f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
        }

        val subtitle = TextView(this).apply {
            text = "Real-time phishing detection"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
        }

        val username = EditText(this).apply { hint = "Username" }

        val password = EditText(this).apply {
            hint = "Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val button = Button(this).apply { text = "Sign in" }

        button.setOnClickListener {
            val u = username.text.toString().trim()
            val p = password.text.toString()

            if (u == SessionManager.PAID_USER && p == SessionManager.PAID_PASS) {
                SessionManager.username = u
                SessionManager.plan = "paid"
                requestPermissionsAndShowDashboard()
            } else if (u == SessionManager.FREE_USER && p == SessionManager.FREE_PASS) {
                SessionManager.username = u
                SessionManager.plan = "free"
                SessionManager.startedAt = System.currentTimeMillis()
                requestPermissionsAndShowDashboard()
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(username)
        layout.addView(password)
        layout.addView(button)

        setContentView(layout)
    }

    // ---------- Android permissions ----------
    private fun requestPermissionsAndShowDashboard() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        } else {
            showDashboard()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            showDashboard()
        }
    }

    // ---------- Notification access helpers ----------
    private fun isNotificationAccessEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(packageName)
    }

    private fun openNotificationAccessSettings() {
        try {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Open Settings: Apps -> Special access -> Notification access",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------- Dashboard ----------
    private fun showDashboard() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val welcome = TextView(this).apply {
            text = "Welcome, ${SessionManager.username}"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
        }

        val plan = TextView(this).apply {
            text = "Plan: ${SessionManager.plan}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
        }

        layout.addView(welcome)
        layout.addView(plan)

        // ---- Notification access card ----
        val accessEnabled = isNotificationAccessEnabled()

        val accessTitle = TextView(this).apply {
            text = "Scan WhatsApp, Telegram, and Messenger"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
            setPadding(0, 40, 0, 8)
        }

        val accessDesc = TextView(this).apply {
            text = if (accessEnabled) {
                "✓ Notification access is enabled. Messaging apps are being scanned."
            } else {
                "AntiPhish needs notification access to read messages from " +
                        "WhatsApp, Telegram, Messenger, Facebook, Instagram, and Signal. " +
                        "Nothing leaves your phone."
            }
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            setPadding(0, 0, 0, 12)
        }

        val accessButton = Button(this).apply {
            text = if (accessEnabled) "Disable notification access" else "Enable notification access"
        }

        accessButton.setOnClickListener {
            if (isNotificationAccessEnabled()) {
                // To disable, we open the same settings — user toggles off manually
                openNotificationAccessSettings()
                Toast.makeText(
                    this,
                    "Toggle AntiPhish OFF in the list",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                openNotificationAccessSettings()
            }
        }

        layout.addView(accessTitle)
        layout.addView(accessDesc)
        layout.addView(accessButton)

        // ---- Status ----
        val status = TextView(this).apply {
            text = "\nSMS and calls are being watched.\n" +
                    "Notifications are " +
                    (if (accessEnabled) "being watched." else "not being watched yet.")
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
            setPadding(0, 30, 0, 0)
        }
        layout.addView(status)

        val logout = Button(this).apply { text = "Logout" }
        logout.setOnClickListener {
            SessionManager.username = ""
            SessionManager.plan = ""
            showLoginScreen()
        }

        layout.addView(logout)

        setContentView(layout)
    }

    // Re-check notification access when returning from settings
    override fun onResume() {
        super.onResume()
        if (SessionManager.username.isNotEmpty()) {
            showDashboard()
        }
    }
}
