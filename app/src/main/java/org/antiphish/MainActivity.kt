package org.antiphish

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
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
import android.view.Gravity

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private var timer: CountDownTimer? = null
    private var timerLabel: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLoginScreen()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    // ============================================================
    // LOGIN
    // ============================================================
    private fun showLoginScreen() {
        timer?.cancel()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 200, 60, 60)
        }

        layout.addView(TextView(this).apply {
            text = "AntiPhish"
            textSize = 32f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
        })
        layout.addView(TextView(this).apply {
            text = "Real-time phishing detection"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            setPadding(0, 0, 0, 40)
        })

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
                    SessionManager.startedAt = 0
                    requestPermissionsAndSplash()
                }
                u == SessionManager.FREE_USER && p == SessionManager.FREE_PASS -> {
                    SessionManager.username = u
                    SessionManager.plan = "free"
                    SessionManager.startedAt = System.currentTimeMillis()
                    requestPermissionsAndSplash()
                }
                else -> Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(username)
        layout.addView(password)
        layout.addView(button)

        setContentView(layout)
    }

    // ============================================================
    // ANDROID PERMISSIONS
    // ============================================================
    private fun requestPermissionsAndSplash() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        } else {
            showSplashScreen()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) showSplashScreen()
    }

    // ============================================================
    // WELCOME SPLASH
    // ============================================================
    private fun showSplashScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "WELCOME"
            textSize = 36f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
            gravity = Gravity.CENTER
        }

        val name = TextView(this).apply {
            text = SessionManager.username.uppercase()
            textSize = 28f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.green))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }

        val subtitle = TextView(this).apply {
            text = "AntiPhishing Tool"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val countdown = TextView(this).apply {
            text = "5"
            textSize = 72f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.green))
            gravity = Gravity.CENTER
        }

        val skip = Button(this).apply {
            text = "Skip"
        }
        skip.setOnClickListener {
            goToDashboard()
        }

        layout.addView(title)
        layout.addView(name)
        layout.addView(subtitle)
        layout.addView(countdown)
        layout.addView(skip)

        setContentView(layout)

        // Tick the countdown 5 → 1, then go to dashboard
        val handler = Handler(Looper.getMainLooper())
        var remaining = 5
        val ticker = object : Runnable {
            override fun run() {
                if (remaining <= 0) {
                    goToDashboard()
                    return
                }
                countdown.text = remaining.toString()
                remaining--
                handler.postDelayed(this, 1000)
            }
        }
        // Show 5 immediately, then start ticking after 1s
        countdown.text = "5"
        handler.postDelayed({
            remaining = 4
            handler.post(ticker)
        }, 1000)
    }

    private fun goToDashboard() {
        showDashboard()
    }

    // ============================================================
    // DASHBOARD
    // ============================================================
    private fun showDashboard() {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        // ---- Timer label (only for free mode) ----
        timerLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
            setPadding(0, 0, 0, 20)
        }
        layout.addView(timerLabel)

        layout.addView(TextView(this).apply {
            text = "Welcome, ${SessionManager.username}"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.purple))
        })

        layout.addView(TextView(this).apply {
            text = "Plan: ${SessionManager.plan}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            setPadding(0, 0, 0, 30)
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

        layout.addView(Button(this).apply {
            text = "Paste & Analyze a message"
            setPadding(0, 30, 0, 0)
            setOnClickListener { showPasteScreen() }
        })

        layout.addView(Button(this).apply {
            text = "View scan history"
            setOnClickListener { showHistoryScreen() }
        })

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
                timer?.cancel()
                SessionManager.username = ""
                SessionManager.plan = ""
                showLoginScreen()
            }
        })

        scroll.addView(layout)
        setContentView(scroll)

        // Start the free-mode timer
        if (SessionManager.plan == "free") {
            startFreeTimer()
        } else {
            timerLabel?.text = ""
        }
    }

    // ============================================================
    // FREE-MODE 5-MINUTE TIMER
    // ============================================================
    private fun startFreeTimer() {
        timer?.cancel()
        val totalMs = SessionManager.FREE_SECONDS * 1000L

        timer = object : CountDownTimer(totalMs, 1000L) {
            override fun onTick(msLeft: Long) {
                val s = msLeft / 1000
                val m = s / 60
                val r = s % 60
                timerLabel?.text = "Free session: %d:%02d".format(m, r)
            }

            override fun onFinish() {
                timerLabel?.text = "Free session expired."
                Toast.makeText(
                    this@MainActivity,
                    "Free session expired. Sign in again.",
                    Toast.LENGTH_LONG
                ).show()
                SessionManager.username = ""
                SessionManager.plan = ""
                showLoginScreen()
            }
        }.start()
    }

    // ============================================================
    // NOTIFICATION ACCESS HELPERS
    // ============================================================
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

    // ============================================================
    // PASTE & ANALYZE
    // ============================================================
    private fun showPasteScreen() {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
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

        val sender = EditText(this).apply { hint = "Sender (optional)" }
        val body = EditText(this).apply {
            hint = "Paste the message here"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 6
            gravity = Gravity.TOP
        }

        val resultView = TextView(this).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
            setPadding(0, 20, 0, 0)
        }

        layout.addView(sender)
        layout.addView(body)

        layout.addView(Button(this).apply {
            text = "Analyze"
            setOnClickListener {
                val s = sender.text.toString().trim().ifBlank { "manual" }
                val b = body.text.toString().trim()
                if (b.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Nothing to analyze", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val r = Analyzer.analyzeMessage(s, b)
                History.add(this@MainActivity, "Manual", s, b, r.verdict, r.score)

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
        })

        layout.addView(resultView)

        layout.addView(Button(this).apply {
            text = "Back"
            setOnClickListener { showDashboard() }
        })

        scroll.addView(layout)
        setContentView(scroll)
    }

    // ============================================================
    // HISTORY
    // ============================================================
    private fun showHistoryScreen() {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
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
                History.clear(this@MainActivity)
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

    // ============================================================
    // RESUME
    // ============================================================
    override fun onResume() {
        super.onResume()
        // Do not auto-refresh the dashboard if user is on splash or login
        // Just refresh if they're already on dashboard
        if (SessionManager.username.isNotEmpty() && timerLabel != null) {
            // Only re-render the dashboard if coming back from notification settings
            // This will restart the timer — acceptable trade-off
            // But avoid a full re-render on first resume after login
            // (login flow calls showSplashScreen directly)
        }
    }
}
