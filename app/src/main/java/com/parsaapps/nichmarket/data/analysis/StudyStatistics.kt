package com.parsaapps.nichmarket.data.analysis

import com.parsaapps.nichmarket.data.model.ExamResult

data class StudyStatistics(
    val exams: Int,
    val questions: Long,
    val correct: Long,
    val durationSeconds: Long
) {
    /** Weighted by question count, not the arithmetic mean of differently sized exams. */
    val accuracyPercent: Int get() = if (questions > 0) (correct * 100 / questions).toInt() else 0
}

/** Statistics are limited to the locally retained exam history, not all-time or national rankings. */
fun studyStatistics(results: List<ExamResult>): StudyStatistics = StudyStatistics(
    exams = results.size,
    questions = results.sumOf { it.total.coerceAtLeast(0).toLong() },
    correct = results.sumOf { it.correct.coerceIn(0, it.total.coerceAtLeast(0)).toLong() },
    durationSeconds = results.sumOf { it.spentSeconds.coerceAtLeast(0).toLong() }
)
