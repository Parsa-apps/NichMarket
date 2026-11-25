package com.parsaapps.nichmarket

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parsaapps.nichmarket.data.analysis.AnswerStatus
import com.parsaapps.nichmarket.data.analysis.answerStatus
import com.parsaapps.nichmarket.data.analysis.subjectPerformance
import com.parsaapps.nichmarket.data.model.Question
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewAnalysisTest {
    private fun question(id: Int, subject: String) = Question(
        id = id, text = "Question", options = listOf("A", "B", "C", "D"),
        answerIndex = 1, subject = subject
    )

    @Test fun classifiesAnswersAndSortsSubjectsIncludingBlanks() {
        val questions = listOf(question(1, "ریاضی"), question(2, "ریاضی"), question(3, "حقوق"))
        val answers = mapOf(1 to 1, 3 to 0)
        assertEquals(AnswerStatus.CORRECT, answerStatus(questions[0], answers))
        assertEquals(AnswerStatus.BLANK, answerStatus(questions[1], answers))
        assertEquals(AnswerStatus.WRONG, answerStatus(questions[2], answers))
        val stats = subjectPerformance(questions, answers)
        assertEquals("حقوق", stats[0].subject)
        assertEquals(0, stats[0].correct)
        assertEquals("ریاضی", stats[1].subject)
        assertEquals(1, stats[1].correct)
        assertEquals(2, stats[1].total)
        assertEquals(0.5f, stats[1].accuracy, 0.001f)
    }

    @Test fun ignoresEmptySubjectsAndHandlesEmptyBank() {
        assertEquals(emptyList<Any>(), subjectPerformance(emptyList(), emptyMap()))
        assertEquals(emptyList<Any>(), subjectPerformance(listOf(question(1, "")), emptyMap()))
    }
}
