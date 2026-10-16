package org.antiphish

data class Signal(
    val rule: String,
    val weight: Int,
    val why: String
)

data class Verdict(
    val score: Int,
    val verdict: String,
    val signals: List<Signal>,
    val recommendations: List<String>
)
