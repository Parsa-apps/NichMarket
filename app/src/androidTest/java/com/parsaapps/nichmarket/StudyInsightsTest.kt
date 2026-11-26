package com.parsaapps.nichmarket

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parsaapps.nichmarket.data.analysis.answeredQuestions
import com.parsaapps.nichmarket.data.analysis.comparablePrevious
import com.parsaapps.nichmarket.data.analysis.recentResults
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class StudyInsightsTest {
    private val q1 = Question(1, "الف", listOf("۱", "۲"), 0)
    private val q2 = Question(2, "ب", listOf("۱", "۲"), 1)
    private fun result(id: String, date: Long, questions: List<Question>? = listOf(q1, q2)) =
        ExamResult(id, "pack", "آزمون", date, 1, 0, 1, 2, 120, questionSnapshot = questions)

    @Test fun recentResultsUsesLocalCalendarDaysAndExcludesFuture() {
        val zone = TimeZone.getTimeZone("America/New_York")
        fun epoch(day: Int, hour: Int) = Calendar.getInstance(zone).apply {
            clear(); set(2025, Calendar.MARCH, day, hour, 0)
        }.timeInMillis
        val now = epoch(10, 12)
        val results = listOf(result("early", epoch(9, 0)), result("yesterday", epoch(9, 1)),
            result("today", epoch(10, 1)), result("future", epoch(11, 1)))
        assertEquals(listOf("today"), recentResults(results, 1, now, zone).map { it.id })
        assertEquals(listOf("early", "yesterday", "today"), recentResults(results, 2, now, zone).map { it.id })
        assertEquals(3L, answeredQuestions(recentResults(results, 2, now, zone)))
    }

    @Test fun comparisonRequiresSameSnapshotAndAnEarlierResult() {
        val now = result("now", 100)
        val sameDifferentOrder = result("previous", 90, listOf(q2, q1))
        val changed = result("changed", 95, listOf(q1.copy(text = "جدید"), q2))
        val future = result("future", 110)
        assertEquals(sameDifferentOrder, comparablePrevious(now, listOf(changed, sameDifferentOrder, future)))
        assertNull(comparablePrevious(now.copy(questionSnapshot = null), listOf(sameDifferentOrder)))
        assertNull(comparablePrevious(now, listOf(changed, future)))
    }

    @Test fun answeredCountsAreBoundedByTotal() {
        val bad = result("bad", 1).copy(correct = 100, wrong = 100, total = 2)
        assertEquals(2L, answeredQuestions(listOf(bad)))
        assertEquals(0L, answeredQuestions(listOf(bad.copy(total = -1))))
    }
}
