package com.parsaapps.nichmarket

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.parsaapps.nichmarket.data.UserStore
import com.parsaapps.nichmarket.data.analysis.elapsedExamSeconds
import com.parsaapps.nichmarket.data.analysis.nextQuestionIndex
import com.parsaapps.nichmarket.data.model.ActiveExam
import com.parsaapps.nichmarket.data.model.AppSettings
import com.parsaapps.nichmarket.data.model.AppTheme
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.data.model.QuestionPack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ExamRecoveryTest {
    private val pack = QuestionPack(
        id = "pack", title = "Test", durationMinutes = 2,
        questions = listOf(Question(2, "Q2", listOf("A", "B"), 0), Question(1, "Q1", listOf("A", "B"), 1))
    )
    private val draft = ActiveExam("session", pack, 1_000_000, 100_000, bootCount = 4)

    @Test fun clockHandlesSameBootRebootAndBounds() {
        assertEquals(30, elapsedExamSeconds(draft, 1_000, 130_000, 4))
        assertEquals(40, elapsedExamSeconds(draft, 1_040_000, 10_000, 5))
        assertEquals(0, elapsedExamSeconds(draft, 900_000, 10_000, 5))
        assertEquals(120, elapsedExamSeconds(draft, 9_000_000, 10_000, 5))
        assertEquals(20, elapsedExamSeconds(draft, 1_020_000, 130_000, null))
    }

    @Test fun navigationWrapsAndSkipsCurrentQuestion() {
        assertEquals(1, nextQuestionIndex(pack.questions, 0) { it.id == 1 })
        assertEquals(0, nextQuestionIndex(pack.questions, 1) { it.id == 2 })
        assertNull(nextQuestionIndex(pack.questions, 0) { it.id == 2 })
        assertNull(nextQuestionIndex(emptyList(), 0) { true })
    }

    @Test fun oldDraftWithoutMarksStillDecodes() {
        val oldJson = """{"sessionId":"old","pack":{"id":"p","title":"P"},"startedAtEpochMs":10,"startedAtElapsedMs":5}"""
        assertTrue(Json.decodeFromString<ActiveExam>(oldJson).markedQuestionIds.isEmpty())
    }

    @Test fun settingsResetDoesNotDeletePurchasesOrBookmarks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "settings-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = UserStore(context, PreferenceDataStoreFactory.create(scope = scope) { file })
        try {
            assertEquals(AppSettings(), store.settings.first())
            store.addPurchase("purchased")
            store.toggleBookmark("pack", 2)
            store.setTheme(AppTheme.DARK)
            store.setLargeText(true)
            store.setAutoAdvance(true)
            store.setHideTimer(true)
            store.setDailyGoal(20)
            store.setReduceMotion(true)
            assertEquals(AppSettings(AppTheme.DARK, true, true, true, 20, true), store.settings.first())
            store.resetSettings()
            assertEquals(AppSettings(), store.settings.first())
            assertEquals(setOf("purchased"), store.purchasedSkus.first())
            assertEquals(listOf("v2:pack:2"), store.bookmarks.first())
        } finally { scope.cancel(); file.delete() }
    }

    @Test fun savedDraftCanBeReadAfterStoreIsRecreated() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "reopen-${UUID.randomUUID()}.preferences_pb")
        val job1 = SupervisorJob()
        val store1 = UserStore(context, PreferenceDataStoreFactory.create(scope = CoroutineScope(job1 + Dispatchers.IO)) { file })
        val latest = draft.copy(answers = mapOf(2 to 1), index = 1, revision = 1, markedQuestionIds = setOf(1))
        val job2 = SupervisorJob()
        try {
            assertTrue(store1.beginExam(draft, null))
            assertTrue(store1.updateExam(latest))
            job1.cancelAndJoin() // Wait for the first DataStore to release its file.
            val store2 = UserStore(context, PreferenceDataStoreFactory.create(scope = CoroutineScope(job2 + Dispatchers.IO)) { file })
            assertEquals(latest, store2.activeExam.first())
        } finally {
            job1.cancelAndJoin()
            job2.cancelAndJoin()
            file.delete()
        }
    }

    @Test fun draftTransactionsProtectNewerWritesAndCompleteAtomically() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "test-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        val store = UserStore(context, dataStore)
        try {
            assertTrue(store.beginExam(draft, null))
            assertFalse(store.beginExam(draft.copy(sessionId = "other"), null))
            val latest = draft.copy(answers = mapOf(2 to 1), index = 1, revision = 2, markedQuestionIds = setOf(1))
            assertTrue(store.updateExam(latest))
            assertTrue(store.updateExam(draft.copy(revision = 1)))
            assertEquals(latest, store.activeExam.first())
            val other = draft.copy(sessionId = "other")
            assertTrue(store.beginExam(other, draft.sessionId))
            assertFalse(store.updateExam(latest))
            store.discardExam(draft.sessionId)
            assertEquals(other, store.activeExam.first())
            val result = ExamResult("other", pack.id, pack.title, 100, 0, 0, 2, 2, 60, questionSnapshot = pack.questions)
            store.completeExam(result, other.sessionId)
            assertNull(store.activeExam.first())
            assertEquals(listOf(result), store.results.first())
            store.completeExam(result, other.sessionId)
            assertEquals(1, store.results.first().size)
            store.deleteResult("other")
            assertTrue(store.results.first().isEmpty())
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
