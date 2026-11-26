package com.parsaapps.nichmarket.data.analysis

import com.parsaapps.nichmarket.data.model.Question

/** Find the next matching question, wrapping around once, without returning the current question. */
fun nextQuestionIndex(questions: List<Question>, current: Int, matches: (Question) -> Boolean): Int? {
    if (questions.isEmpty()) return null
    val start = current.coerceIn(0, questions.lastIndex)
    for (offset in 1 until questions.size) {
        val position = (start + offset) % questions.size
        if (matches(questions[position])) return position
    }
    return null
}
