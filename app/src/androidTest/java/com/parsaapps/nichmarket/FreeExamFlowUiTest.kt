package com.parsaapps.nichmarket

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreeExamFlowUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun freeExamCanBeSavedResumedAndReviewed() {
        compose.onNodeWithTag("home-list").performScrollToNode(hasText("آزمون‌های عمومی استخدامی"))
        compose.onNodeWithText("آزمون‌های عمومی استخدامی").performClick()
        compose.onNodeWithTag("category-list").performScrollToNode(hasTestTag("start-omoomi_free_demo"))
        compose.onNodeWithTag("start-omoomi_free_demo").performClick()
        compose.onNodeWithText("همه ۱۰ سوال • ۱۲ دقیقه").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("answer-1")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("answer-1").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("خروج").performClick()
        compose.onNodeWithText("ذخیره و خروج").performClick()
        compose.waitUntil(10_000) { runBlocking { Graph.userStore.activeExam.first()?.answers?.get(1) == 1 } }
        assertNotNull(runBlocking { Graph.userStore.activeExam.first() })
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("category-list")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("بازگشت").performClick()
        compose.onNodeWithTag("home-list").performScrollToNode(hasText("آزمون ناتمام"))
        compose.onNodeWithText("آزمون ناتمام").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("answer-1")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("answer-1").assertIsSelected()
        repeat(9) { compose.onNodeWithText("بعدی").performClick() }
        compose.onNodeWithText("اتمام آزمون").performClick()
        compose.onNodeWithText("پایان و مشاهده نتیجه").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("کارنامه آزمون")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("کارنامه آزمون").assertIsDisplayed()
        val result = runBlocking { Graph.userStore.results.first().single() }
        assertEquals("omoomi_free_demo", result.packId)
        assertEquals(1, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(9, result.blank)
        assertEquals(mapOf(1 to 1), result.answers)
        assertNull(runBlocking { Graph.userStore.activeExam.first() })
    }
}
