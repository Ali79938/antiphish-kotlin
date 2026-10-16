package org.antiphish

object SessionManager {
    const val PAID_USER = "xitexe"
    const val PAID_PASS = "bossman"
    const val FREE_USER = "guest"
    const val FREE_PASS = "guest"
    const val FREE_SECONDS = 300

    var username: String = ""
    var plan: String = ""
    var startedAt: Long = 0
}
