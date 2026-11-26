package com.parsaapps.nichmarket

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parsaapps.nichmarket.data.analysis.AnswerStatus
import com.parsaapps.nichmarket.data.analysis.matchesReviewQuestion
import com.parsaapps.nichmarket.data.model.Question
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewFilterTest {
    @Test fun filtersAreCombinedAndPersianSearchIsNormalized() {
        val question = Question(1, "كيبورد چیست؟", listOf("ورودی", "خروجی", "ذخیره", "پردازش"),
            0, explanation = "دستگاه ورودی", subject = "رایانه")
        val answers = mapOf(1 to 1)
        assertTrue(matchesReviewQuestion(question, answers, AnswerStatus.WRONG, "رایانه", "کیبورد ورودی"))
        assertFalse(matchesReviewQuestion(question, answers, AnswerStatus.CORRECT, "رایانه", "کیبورد"))
        assertFalse(matchesReviewQuestion(question, answers, AnswerStatus.WRONG, "ریاضی", "کیبورد"))
        assertFalse(matchesReviewQuestion(question, answers, AnswerStatus.WRONG, "رایانه", "ناموجود"))
        assertTrue(matchesReviewQuestion(question, emptyMap(), AnswerStatus.BLANK, "", ""))
    }
}
