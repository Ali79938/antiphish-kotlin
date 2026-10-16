package org.antiphish

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

        val username = EditText(this).apply {
            hint = "Username"
        }

        val password = EditText(this).apply {
            hint = "Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val button = Button(this).apply {
            text = "Sign in"
        }

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

    private fun showDashboard() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 100, 40, 40)
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

        val status = TextView(this).apply {
            text = "\nSMS, calls, and voice messages are being watched.\n\n" +
                    "Signals will pop notifications on suspicious activity."
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text))
        }

        val logout = Button(this).apply {
            text = "Logout"
        }

        logout.setOnClickListener {
            SessionManager.username = ""
            SessionManager.plan = ""
            showLoginScreen()
        }

        layout.addView(welcome)
        layout.addView(plan)
        layout.addView(status)
        layout.addView(logout)

        setContentView(layout)
    }
}
