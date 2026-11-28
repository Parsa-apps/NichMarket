package com.parsaapps.nichmarket.data.analysis

import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.Question
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

/** Local spaced-review state. Question text/options are snapshotted so bank updates cannot break reviews. */
@Serializable
data class ReviewItem(
    val packId: String,
    val packTitle: String,
    val question: Question,
    val dueAtEpochMs: Long,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAtEpochMs: Long? = null,
    val markedKnown: Boolean = false
) {
    val key: String get() = "$packId:${question.id}"
}

enum class ReviewGrade { AGAIN, HARD, GOOD, EASY }

/** SM-2-inspired conservative intervals; deterministic and independent of device locale. */
fun scheduleReview(item: ReviewItem, grade: ReviewGrade, now: Long): ReviewItem {
    val next = when (grade) {
        ReviewGrade.AGAIN -> item.copy(
            intervalDays = 0, repetitions = 0, lapses = item.lapses + 1,
            dueAtEpochMs = now + TimeUnit.MINUTES.toMillis(10), lastReviewedAtEpochMs = now, markedKnown = false
        )
        ReviewGrade.HARD -> {
            val days = when {
                item.intervalDays <= 0 -> 1
                else -> (item.intervalDays * 1.2).toInt().coerceAtLeast(item.intervalDays + 1)
            }
            item.copy(intervalDays = days, repetitions = item.repetitions + 1,
                dueAtEpochMs = now + TimeUnit.DAYS.toMillis(days.toLong()), lastReviewedAtEpochMs = now)
        }
        ReviewGrade.GOOD -> {
            val days = when (item.intervalDays) { 0 -> 1; 1 -> 3; else -> (item.intervalDays * 2).coerceAtMost(180) }
            item.copy(intervalDays = days, repetitions = item.repetitions + 1,
                dueAtEpochMs = now + TimeUnit.DAYS.toMillis(days.toLong()), lastReviewedAtEpochMs = now)
        }
        ReviewGrade.EASY -> {
            val days = when (item.intervalDays) { 0 -> 4; else -> (item.intervalDays * 2.5).toInt().coerceAtLeast(item.intervalDays + 1).coerceAtMost(180) }
            item.copy(intervalDays = days, repetitions = item.repetitions + 1,
                dueAtEpochMs = now + TimeUnit.DAYS.toMillis(days.toLong()), lastReviewedAtEpochMs = now, markedKnown = true)
        }
    }
    return next
}

/** Extract wrong and unanswered questions only from immutable exam snapshots. */
fun mergeMissedQuestions(existing: List<ReviewItem>, result: ExamResult): List<ReviewItem> {
    val snapshot = result.questionSnapshot ?: return existing
    if (snapshot.isEmpty()) return existing
    val byKey = existing.associateBy { it.key }.toMutableMap()
    snapshot.forEach { question ->
        if (result.answers[question.id] != question.answerIndex) {
            val key = "${result.packId}:${question.id}"
            val previous = byKey[key]
            byKey[key] = if (previous == null) ReviewItem(
                packId = result.packId, packTitle = result.packTitle, question = question, dueAtEpochMs = result.date
            ) else previous.copy(
                question = question, packTitle = result.packTitle, dueAtEpochMs = minOf(previous.dueAtEpochMs, result.date),
                lapses = previous.lapses + 1, markedKnown = false
            )
        }
    }
    return byKey.values.sortedWith(compareBy<ReviewItem> { it.dueAtEpochMs }.thenByDescending { it.lapses })
}

fun dueReviewItems(items: List<ReviewItem>, now: Long): List<ReviewItem> =
    items.filter { !it.markedKnown && it.dueAtEpochMs <= now }
        .sortedWith(compareBy<ReviewItem> { it.dueAtEpochMs }.thenByDescending { it.lapses })
