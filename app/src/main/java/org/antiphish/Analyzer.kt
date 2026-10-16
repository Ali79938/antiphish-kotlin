package org.antiphish

object Analyzer {

    private val URGENCY = listOf(
        "urgent", "immediately", "act now", "verify now", "within 24 hours",
        "final warning", "account will be closed", "suspended", "limited time",
        "expires today", "respond immediately", "immediate action required",
        "last warning", "your account has been", "unauthorized login",
        "unusual activity", "we detected", "security alert", "verify immediately",
        "confirm now"
    )

    private val FINANCE = listOf(
        "wire transfer", "bitcoin", "crypto wallet", "gift card", "western union",
        "money gram", "payment failed", "refund", "bank account", "ssn",
        "social security", "credit card", "debit card", "routing number",
        "bill payment", "invoice attached", "purchase confirmation",
        "unauthorized transaction", "tax refund", "irs", "hmrc"
    )

    private val CREDENTIALS = listOf(
        "verify your account", "confirm your password", "click here to login",
        "update your payment", "re-enter your password", "reset your password",
        "unusual sign-in", "suspicious activity", "confirm your identity",
        "login attempt", "sign-in attempt", "password expired",
        "update your information", "complete verification",
        "reactivate your account", "confirm your details"
    )

    private val GREETING_MISMATCH = listOf(
        "dear customer", "dear user", "dear member", "dear sir/madam"
    )

    private val BRANDS = listOf(
        "paypal", "apple", "amazon", "microsoft", "google", "netflix",
        "bank of america", "chase", "wells fargo", "coinbase"
    )

    private val BRAND_REAL_DOMAINS = mapOf(
        "paypal" to "paypal.com", "apple" to "apple.com", "amazon" to "amazon.com",
        "microsoft" to "microsoft.com", "google" to "google.com",
        "netflix" to "netflix.com", "bank of america" to "bankofamerica.com",
        "chase" to "chase.com", "wells fargo" to "wellsfargo.com",
        "coinbase" to "coinbase.com"
    )

    private val SUSPICIOUS_TLDS = listOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".work",
        ".click", ".link", ".zip", ".mov", ".rest", ".bar", ".review"
    )

    private val URL_SHORTENERS = listOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd",
        "buff.ly", "rebrand.ly", "cutt.ly", "shorturl.at"
    )

    private val LOOKALIKE = listOf(
        "paypa[l1]".toRegex(), "app[l1]e".toRegex(), "arnazon".toRegex(),
        "micros0ft".toRegex(), "g00gle".toRegex(), "netf[l1]ix".toRegex()
    )

    private val URL_REGEX = Regex("https?://[^\\s<>\"']+")

    fun analyzeMessage(sender: String, body: String): Verdict {
        val signals = mutableListOf<Signal>()

        // Rule 1: urgency
        val urgHits = URGENCY.filter { body.lowercase().contains(it) }
        if (urgHits.isNotEmpty()) {
            signals.add(Signal("urgency", 15, "Urgency language: ${urgHits.take(3).joinToString(", ")}"))
        }

        // Rule 2: finance
        val finHits = FINANCE.filter { body.lowercase().contains(it) }
        if (finHits.isNotEmpty()) {
            signals.add(Signal("finance", 20, "Financial lures: ${finHits.take(3).joinToString(", ")}"))
        }

        // Rule 3: credentials
        val credHits = CREDENTIALS.filter { body.lowercase().contains(it) }
        if (credHits.isNotEmpty()) {
            signals.add(Signal("credentials", 20, "Credential request: ${credHits.take(3).joinToString(", ")}"))
        }

        // Rule 4: generic greeting
        val greetHits = GREETING_MISMATCH.filter { body.lowercase().contains(it) }
        if (greetHits.isNotEmpty()) {
            signals.add(Signal("generic_greeting", 8, "Generic greeting: ${greetHits.take(2).joinToString(", ")}"))
        }

        // Rule 5: brand impersonation
        val bodyLower = body.lowercase()
        val senderLower = sender.lowercase()
        val suspiciousBrands = mutableListOf<String>()
        for (brand in BRANDS) {
            if (bodyLower.contains(brand) || senderLower.contains(brand)) {
                val realDomain = BRAND_REAL_DOMAINS[brand] ?: ""
                if (senderLower.contains(brand) && realDomain.isNotEmpty() && !senderLower.endsWith(realDomain)) {
                    suspiciousBrands.add("$brand (fake sender domain)")
                } else if (!senderLower.contains(brand)) {
                    suspiciousBrands.add(brand)
                }
            }
        }
        if (suspiciousBrands.isNotEmpty()) {
            signals.add(Signal("brand_impersonation", 20, "Brand impersonation: ${suspiciousBrands.take(3).joinToString(", ")}"))
        }

        // Rule 6: suspicious TLD
        val urls = URL_REGEX.findAll(body).map { it.value }.toList()
        val badTlds = urls.filter { u -> SUSPICIOUS_TLDS.any { u.lowercase().endsWith(it) || u.lowercase().contains("$it/") } }
        if (badTlds.isNotEmpty()) {
            signals.add(Signal("suspicious_tld", 18, "Suspicious TLD: ${badTlds.take(2).joinToString(", ")}"))
        }

        // Rule 7: URL shortener
        val shortened = urls.filter { u -> URL_SHORTENERS.any { u.lowercase().contains(it) } }
        if (shortened.isNotEmpty()) {
            signals.add(Signal("shortener", 15, "URL shortener: ${shortened.take(2).joinToString(", ")}"))
        }

        // Rule 8: lookalike domains
        val lookHits = LOOKALIKE.filter { it.containsMatchIn(bodyLower) }
        if (lookHits.isNotEmpty()) {
            signals.add(Signal("lookalike", 22, "Lookalike domain pattern"))
        }

        // Rule 9: raw IP in URL
        val ipUrls = urls.filter { Regex("https?://\\d{1,3}(\\.\\d{1,3}){3}").containsMatchIn(it) }
        if (ipUrls.isNotEmpty()) {
            signals.add(Signal("ip_link", 22, "Raw IP in link: ${ipUrls.take(2).joinToString(", ")}"))
        }

        // Rule 10: @ in URL
        val atUrls = urls.filter { it.contains("@") }
        if (atUrls.isNotEmpty()) {
            signals.add(Signal("at_in_url", 18, "Credentials-style URL"))
        }

        // Rule 11: excessive caps
        val letters = body.filter { it.isLetter() }
        if (letters.length >= 20) {
            val capsRatio = letters.count { it.isUpperCase() }.toDouble() / letters.length
            if (capsRatio > 0.5) {
                signals.add(Signal("excessive_caps", 10, "Excessive capitalization (${(capsRatio * 100).toInt()}% caps)"))
            }
        }

        // Rule 12: excessive punctuation
        if (body.length >= 20 && body.count { it == '!' } >= 4) {
            signals.add(Signal("excessive_punct", 8, "Excessive exclamation marks"))
        }

        return buildVerdict(signals)
    }

    private fun buildVerdict(signals: List<Signal>): Verdict {
        val total = signals.sumOf { it.weight }.coerceAtMost(100)
        val verdict = when {
            total < 25 -> "safe"
            total < 45 -> "suspicious"
            else -> "phishing"
        }
        val recs = buildRecommendations(verdict, signals.map { it.rule }.toSet())
        return Verdict(total, verdict, signals, recs)
    }

    private fun buildRecommendations(verdict: String, fired: Set<String>): List<String> {
        if (verdict == "safe") return listOf("No action needed. Message looks normal.")

        val recs = mutableListOf<String>()
        if (verdict == "phishing") {
            recs.add("DO NOT click any links in this message.")
            recs.add("DO NOT reply, call back, or provide any personal information.")
            recs.add("Delete or report the message as spam/phishing.")
        } else {
            recs.add("Treat links in this message with caution.")
            recs.add("Do not enter credentials or payment info from any link here.")
        }

        if ("credentials" in fired) recs.add("Log in by typing the official URL directly.")
        if ("finance" in fired) recs.add("Never send money, gift cards, or crypto to someone who contacted you first.")
        if ("brand_impersonation" in fired) recs.add("Verify the sender by contacting the real company directly.")
        if ("shortener" in fired || "suspicious_tld" in fired || "ip_link" in fired) recs.add("Inspect any short links before clicking - or skip them.")
        if ("urgency" in fired) recs.add("Urgency is a pressure tactic. Slow down and verify.")
        if ("excessive_caps" in fired || "excessive_punct" in fired) recs.add("Excessive caps and punctuation are common in spam.")

        return recs.distinct()
    }
}
