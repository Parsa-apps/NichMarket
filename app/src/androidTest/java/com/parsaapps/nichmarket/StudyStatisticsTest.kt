package com.parsaapps.nichmarket

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parsaapps.nichmarket.data.analysis.studyStatistics
import com.parsaapps.nichmarket.data.model.ExamResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyStatisticsTest {
    @Test fun weightsAccuracyByQuestionCountAndIncludesUnanswered() {
        val short = ExamResult("1", "p", "t", 0, 10, 0, 0, 10, 60)
        val long = ExamResult("2", "p", "t", 0, 0, 0, 90, 90, 120)
        val stats = studyStatistics(listOf(short, long))
        assertEquals(2, stats.exams)
        assertEquals(100L, stats.questions)
        assertEquals(10L, stats.correct)
        assertEquals(10, stats.accuracyPercent)
        assertEquals(180L, stats.durationSeconds)
    }

    @Test fun emptyHistoryHasZeroAccuracy() {
        assertEquals(0, studyStatistics(emptyList()).accuracyPercent)
    }
}
