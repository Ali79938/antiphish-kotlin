package org.antiphish

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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

    // ============ LOGIN ============
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
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val button = Button(this).apply { text = "Sign in" }

        button.setOnClickListener {
            val u = username.text.toString().trim()
            val p = password.text.toString()
            when {
                u == SessionManager.PAID_USER && p == SessionManager.PAID_PASS -> {
                    SessionManager.username = u
                    SessionManager.plan = "paid"
                    requestPermissionsAndShowDashboard()
                }
                u == SessionManager.FREE_USER && p == SessionManager.FREE_PASS -> {
                    SessionManager.username = u
                    SessionManager.plan = "free"
                    SessionManager.startedAt = System.currentTimeMillis()
                    requestPermissionsAndShowDashboard()
                }
                else -> Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(username)
        layout.addView(password)
        layout.addView(button)

        setContentView(layout)
    }

    // ============ ANDROID PERMISSIONS ============
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
        if (requestCode == 100) showDashboard()
    }

    // ============ NOTIFICATION ACCESS ============
    private fun isNotificationAccessEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
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

    // ============ DASHBOARD ============
    private fun showDashboard() {
        val scroll = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        layout.addView(TextView(this).apply {
            text = "Welcome, ${SessionManager.username}"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
        })

        layout.addView(TextView(this).apply {
            text = "Plan: ${SessionManager.plan}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            setPadding(0, 0, 0, 40)
        })

        // ---- Notification access card ----
        val accessEnabled = isNotificationAccessEnabled()

        layout.addView(TextView(this).apply {
            text = "Scan WhatsApp, Telegram, and Messenger"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = if (accessEnabled) {
                "✓ Notification access is enabled."
            } else {
                "AntiPhish needs notification access to read messages from " +
                        "WhatsApp, Telegram, Messenger, Facebook, Instagram, and Signal."
            }
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            setPadding(0, 0, 0, 12)
        })

        layout.addView(Button(this).apply {
            text = if (accessEnabled) "Disable notification access" else "Enable notification access"
            setOnClickListener {
                openNotificationAccessSettings()
                if (accessEnabled) {
                    Toast.makeText(
                        this@MainActivity,
                        "Toggle AntiPhish OFF in the list",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })

        // ---- Paste & Analyze button ----
        layout.addView(Button(this).apply {
            text = "Paste & Analyze a message"
            setPadding(0, 30, 0, 0)
            setOnClickListener { showPasteScreen() }
        })

        // ---- History button ----
        layout.addView(Button(this).apply {
            text = "View scan history"
            setOnClickListener { showHistoryScreen() }
        })

        // ---- Status ----
        layout.addView(TextView(this).apply {
            text = "\nSMS and calls are being watched.\n" +
                    "Notifications are " +
                    (if (accessEnabled) "being watched." else "not being watched yet.")
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
            setPadding(0, 30, 0, 0)
        })

        layout.addView(Button(this).apply {
            text = "Logout"
            setOnClickListener {
                SessionManager.username = ""
                SessionManager.plan = ""
                showLoginScreen()
            }
        })

        scroll.addView(layout)
        setContentView(scroll)
    }

    // ============ PASTE & ANALYZE ============
    private fun showPasteScreen() {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        layout.addView(TextView(this).apply {
            text = "Paste & Analyze"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = "Paste any suspicious message from WhatsApp, Telegram, or any app."
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            setPadding(0, 0, 0, 16)
        })

        val sender = EditText(this).apply {
            hint = "Sender (optional)"
        }
        val body = EditText(this).apply {
            hint = "Paste the message here"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 6
            gravity = android.view.Gravity.TOP
        }

        val resultView = TextView(this).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
            setPadding(0, 20, 0, 0)
        }

        val analyze = Button(this).apply { text = "Analyze" }
        analyze.setOnClickListener {
            val s = sender.text.toString().trim().ifBlank { "manual" }
            val b = body.text.toString().trim()
            if (b.isEmpty()) {
                Toast.makeText(this, "Nothing to analyze", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val r = Analyzer.analyzeMessage(s, b)
            History.add(this, "Manual", s, b, r.verdict, r.score)

            val sb = StringBuilder()
            sb.append("Verdict: ${r.verdict.uppercase()}\n")
            sb.append("Score: ${r.score}/100\n\n")
            sb.append("What to do:\n")
            for (rec in r.recommendations) sb.append("• $rec\n")
            if (r.signals.isNotEmpty()) {
                sb.append("\nSignals:\n")
                for (sig in r.signals) sb.append("+${sig.weight}  ${sig.why}\n")
            }
            resultView.text = sb.toString()
        }

        val back = Button(this).apply {
            text = "Back"
            setOnClickListener { showDashboard() }
        }

        layout.addView(sender)
        layout.addView(body)
        layout.addView(analyze)
        layout.addView(resultView)
        layout.addView(back)

        scroll.addView(layout)
        setContentView(scroll)
    }

    // ============ HISTORY ============
    private fun showHistoryScreen() {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        layout.addView(TextView(this).apply {
            text = "Scan History"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
            setPadding(0, 0, 0, 16)
        })

        val entries = History.getAll(this)
        if (entries.isEmpty()) {
            layout.addView(TextView(this).apply {
                text = "No scans yet."
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            })
        } else {
            for (e in entries) {
                val v = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 20, 0, 20)
                }

                val verdictColor = when (e.verdict) {
                    "phishing" -> R.color.red
                    "suspicious" -> R.color.amber
                    else -> R.color.green
                }

                v.addView(TextView(this).apply {
                    text = "${e.verdict.uppercase()}  ·  score ${e.score}  ·  ${e.source}"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@MainActivity, verdictColor))
                })
                v.addView(TextView(this).apply {
                    text = "${History.formatTime(e.timestamp)}  ·  from ${e.sender}"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
                })
                v.addView(TextView(this).apply {
                    text = e.body
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
                })

                layout.addView(v)
            }
        }

        layout.addView(Button(this).apply {
            text = "Clear history"
            setOnClickListener {
                History.clear(this)
                showHistoryScreen()
            }
        })

        layout.addView(Button(this).apply {
            text = "Back"
            setOnClickListener { showDashboard() }
        })

        scroll.addView(layout)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.username.isNotEmpty()) showDashboard()
    }
}
