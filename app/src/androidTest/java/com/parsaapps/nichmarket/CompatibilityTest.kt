package com.parsaapps.nichmarket

import android.icu.util.TimeZone
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.parsaapps.nichmarket.data.QuestionRepository
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.bookmarkKey
import com.parsaapps.nichmarket.util.formatShamsi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.GregorianCalendar

@RunWith(AndroidJUnit4::class)
class CompatibilityTest {
    @Test fun jalaliDatesAndNewYear() {
        val oldZone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            fun date(y: Int, m: Int, d: Int) = GregorianCalendar(y, m - 1, d, 12, 0).timeInMillis
            assertEquals("۱۴۰۳/۱۲/۳۰", formatShamsi(date(2025, 3, 20)))
            assertEquals("۱۴۰۴/۰۱/۰۱", formatShamsi(date(2025, 3, 21)))
            assertEquals("۱۴۰۴/۰۹/۰۴", formatShamsi(date(2025, 11, 25)))
        } finally {
            java.util.TimeZone.setDefault(oldZone)
            TimeZone.setDefault(TimeZone.getTimeZone(oldZone.id))
        }
    }

    @Test fun oldResultsUseOriginalBankAndNewResultsKeepSnapshot() {
        val repo = QuestionRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val old = repo.findLegacyPack("amoozesh_free_demo")!!
        val current = repo.findPack("amoozesh_free_demo")!!
        assertNotEquals(old.questions.first().text, current.questions.first().text)
        assertNotEquals("${current.id}:${current.questions.first().id}", bookmarkKey(current.id, current.questions.first().id))
        val legacyJson = """{"id":"r","packId":"${old.id}","packTitle":"t","date":0,"correct":1,"wrong":0,"blank":0,"total":1,"spentSeconds":0}"""
        assertEquals(null, Json.decodeFromString<ExamResult>(legacyJson).questionSnapshot)
        val result = ExamResult("r", current.id, "t", 0, 1, 0, 0, 1, 0, questionSnapshot = current.questions)
        assertEquals(current.questions, Json.decodeFromString<ExamResult>(Json.encodeToString(result)).questionSnapshot)
    }
}
