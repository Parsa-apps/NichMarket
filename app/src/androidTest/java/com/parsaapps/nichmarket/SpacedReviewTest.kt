package com.parsaapps.nichmarket

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parsaapps.nichmarket.data.analysis.*
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.Question
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SpacedReviewTest {
    private val question = Question(7, "پرسش", listOf("الف", "ب", "ج", "د"), 1, "توضیح")
    private val result = ExamResult("r1", "pack", "بسته", 1_000L, 0, 1, 0, 1, 30,
        answers = mapOf(7 to 0), questionSnapshot = listOf(question))

    @Test fun wrongQuestionIsAddedAndCorrectQuestionIsNot() {
        val items = mergeMissedQuestions(emptyList(), result)
        assertEquals(1, items.size)
        assertEquals("pack:7", items.single().key)
        assertTrue(mergeMissedQuestions(emptyList(), result.copy(answers = mapOf(7 to 1))).isEmpty())
    }

    @Test fun repeatedMistakeIncreasesLapsesWithoutDuplicatingItem() {
        val first = mergeMissedQuestions(emptyList(), result).single()
        val second = mergeMissedQuestions(listOf(first), result.copy(id = "r2", date = 2_000L))
        assertEquals(1, second.size)
        assertEquals(1, second.single().lapses)
    }

    @Test fun gradesScheduleProgressivelyAndAgainIsSoon() {
        val first = mergeMissedQuestions(emptyList(), result).single()
        val now = 10_000L
        val good = scheduleReview(first, ReviewGrade.GOOD, now)
        assertEquals(1, good.intervalDays)
        assertEquals(now + TimeUnit.DAYS.toMillis(1), good.dueAtEpochMs)
        val again = scheduleReview(good, ReviewGrade.AGAIN, now)
        assertEquals(0, again.intervalDays)
        assertEquals(now + TimeUnit.MINUTES.toMillis(10), again.dueAtEpochMs)
        assertEquals(1, again.lapses)
    }

    @Test fun dueQueueExcludesFutureAndKnownItems() {
        val first = mergeMissedQuestions(emptyList(), result).single()
        val future = first.copy(dueAtEpochMs = 20_000L)
        val known = first.copy(markedKnown = true)
        assertEquals(listOf(first), dueReviewItems(listOf(first, future, known), 10_000L))
    }
}
