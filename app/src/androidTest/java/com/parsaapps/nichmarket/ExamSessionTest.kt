package com.parsaapps.nichmarket

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parsaapps.nichmarket.data.analysis.examSession
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.data.model.QuestionPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class ExamSessionTest {
    private val pack = QuestionPack(
        id = "test", title = "test", durationMinutes = 50,
        questions = (1..40).map { Question(it, "Q$it", listOf("A", "B", "C", "D"), 0) }
    )

    @Test fun sampleUsesUniqueOriginalQuestionsAndProportionalTime() {
        val sample = examSession(pack, 20, Random(42))
        assertEquals(20, sample.questions.size)
        assertEquals(20, sample.questions.map { it.id }.toSet().size)
        assertTrue(sample.questions.all { it in pack.questions })
        assertEquals(25, sample.durationMinutes)
        assertEquals(pack.id, sample.id)
    }

    @Test fun fullAndInvalidLimitsKeepOriginalPack() {
        assertEquals(pack, examSession(pack, 0))
        assertEquals(pack, examSession(pack, 40))
        assertEquals(pack, examSession(pack, 100))
    }
}
