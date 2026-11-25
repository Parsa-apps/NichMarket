package com.parsaapps.nichmarket

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parsaapps.nichmarket.data.analysis.matchesPersianSearch
import com.parsaapps.nichmarket.data.analysis.normalizePersianSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersianSearchTest {
    @Test fun handlesPersianArabicFormsAndDigits() {
        assertEquals(normalizePersianSearch("كيبورد ۱۲۳"), normalizePersianSearch("کیبورد ١٢٣"))
        assertEquals(normalizePersianSearch("می‌خواند"), normalizePersianSearch("می خواند"))
        assertTrue(matchesPersianSearch("كيبورد 123", "کیبورد", "۱۲۳"))
    }

    @Test fun supportsMultipleTermsAndEmptyQueries() {
        assertTrue(matchesPersianSearch("ریاضی درصد", "درس ریاضی", "درصد افزایش"))
        assertTrue(matchesPersianSearch("  ", "هر چیزی"))
        assertFalse(matchesPersianSearch("ریاضی بانک", "درس ریاضی", "درصد افزایش"))
    }
}
