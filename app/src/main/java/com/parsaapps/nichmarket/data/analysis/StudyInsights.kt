package com.parsaapps.nichmarket.data.analysis

import com.parsaapps.nichmarket.data.model.ExamResult
import java.util.Calendar
import java.util.TimeZone

/** Calendar days in the device's time zone, not rolling 24-hour periods. Future results are excluded. */
fun recentResults(results: List<ExamResult>, days: Int, now: Long, zone: TimeZone = TimeZone.getDefault()): List<ExamResult> {
    require(days > 0)
    val start = Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -(days - 1))
    }.timeInMillis
    return results.filter { it.date in start..now }
}

/** Count actual answered questions from completed, locally retained exams. */
fun answeredQuestions(results: List<ExamResult>): Long = results.sumOf {
    (it.correct.coerceAtLeast(0).toLong() + it.wrong.coerceAtLeast(0).toLong())
        .coerceAtMost(it.total.coerceAtLeast(0).toLong())
}

/** Only compare an earlier result for exactly the same snapshot; sampled exams may differ. */
fun comparablePrevious(current: ExamResult, results: List<ExamResult>): ExamResult? {
    val questions = current.questionSnapshot ?: return null
    if (questions.isEmpty() || questions.size != current.total || questions.map { it.id }.toSet().size != questions.size) return null
    val snapshot = questions.associateBy { it.id }
    return results.asSequence().filter { other ->
        val previousQuestions = other.questionSnapshot ?: return@filter false
        other.id != current.id && other.packId == current.packId && other.date < current.date &&
            other.total == current.total && previousQuestions.size == questions.size &&
            previousQuestions.associateBy { it.id } == snapshot
    }.maxByOrNull { it.date }
}
