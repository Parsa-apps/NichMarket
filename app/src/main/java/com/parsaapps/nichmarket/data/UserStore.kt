package com.parsaapps.nichmarket.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.parsaapps.nichmarket.data.model.ActiveExam
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.bookmarkKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "nich_market")

/**
 * ذخیره‌سازی وضعیت کاربر: خریدها، بوکمارک‌ها و کارنامه آزمون‌ها
 * (با DataStore؛ بدون نیاز به سرور)
 */
class UserStore(private val context: Context, private val store: DataStore<Preferences> = context.dataStore) {
    fun bootCount(): Int? = android.provider.Settings.Global.getInt(
        context.contentResolver, android.provider.Settings.Global.BOOT_COUNT, -1
    ).takeIf { it >= 0 }

    private val json = Json { ignoreUnknownKeys = true }
    private val KEY_PURCHASES = stringPreferencesKey("purchased_skus")
    private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks")
    private val KEY_RESULTS = stringPreferencesKey("results")
    private val KEY_ACTIVE_EXAM = stringPreferencesKey("active_exam")

    val purchasedSkus: Flow<Set<String>> = store.data.map { prefs ->
        prefs[KEY_PURCHASES]?.let { json.decodeFromString(SetSerializer(String.serializer()), it) } ?: emptySet()
    }

    val bookmarks: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_BOOKMARKS]?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }
            ?: emptyList()
    }

    val results: Flow<List<ExamResult>> = store.data.map { prefs ->
        prefs[KEY_RESULTS]?.let { json.decodeFromString(ListSerializer(ExamResult.serializer()), it) }
            ?: emptyList()
    }

    val activeExam: Flow<ActiveExam?> = store.data.map { prefs ->
        prefs[KEY_ACTIVE_EXAM]?.let { json.decodeFromString(ActiveExam.serializer(), it) }
    }

    /** Start or replace only the session the caller has explicitly seen. */
    suspend fun beginExam(draft: ActiveExam, expectedSessionId: String?): Boolean {
        var written = false
        store.edit { prefs ->
            val current = prefs[KEY_ACTIVE_EXAM]?.let { json.decodeFromString(ActiveExam.serializer(), it) }
            if (current?.sessionId == expectedSessionId) {
                prefs[KEY_ACTIVE_EXAM] = json.encodeToString(ActiveExam.serializer(), draft)
                written = true
            }
        }
        return written
    }

    /** Ignore late writes from old revisions or from a previous exam. */
    suspend fun updateExam(draft: ActiveExam): Boolean {
        var found = false
        store.edit { prefs ->
            val current = prefs[KEY_ACTIVE_EXAM]?.let { json.decodeFromString(ActiveExam.serializer(), it) }
            if (current?.sessionId == draft.sessionId) {
                found = true
                if (draft.revision > current.revision) {
                    prefs[KEY_ACTIVE_EXAM] = json.encodeToString(ActiveExam.serializer(), draft)
                }
            }
        }
        return found
    }

    suspend fun discardExam(sessionId: String) {
        store.edit { prefs ->
            val current = prefs[KEY_ACTIVE_EXAM]?.let { json.decodeFromString(ActiveExam.serializer(), it) }
            if (current?.sessionId == sessionId) prefs.remove(KEY_ACTIVE_EXAM)
        }
    }

    /** Save the result and remove its draft in the same DataStore transaction. */
    suspend fun completeExam(result: ExamResult, sessionId: String) {
        store.edit { prefs ->
            val current = prefs[KEY_RESULTS]
                ?.let { json.decodeFromString(ListSerializer(ExamResult.serializer()), it) } ?: emptyList()
            val active = prefs[KEY_ACTIVE_EXAM]?.let { json.decodeFromString(ActiveExam.serializer(), it) }
            check(active?.sessionId == sessionId || current.any { it.id == result.id }) {
                "Exam session is no longer active"
            }
            val withoutDuplicate = current.filterNot { it.id == result.id }
            prefs[KEY_RESULTS] = json.encodeToString(
                ListSerializer(ExamResult.serializer()), listOf(result) + withoutDuplicate.take(99)
            )
            if (active?.sessionId == sessionId) prefs.remove(KEY_ACTIVE_EXAM)
        }
    }

    suspend fun addPurchase(sku: String) {
        store.edit { prefs ->
            val current = prefs[KEY_PURCHASES]
                ?.let { json.decodeFromString(SetSerializer(String.serializer()), it) } ?: emptySet()
            prefs[KEY_PURCHASES] = json.encodeToString(SetSerializer(String.serializer()), current + sku)
        }
    }

    suspend fun addPurchases(skus: Set<String>) {
        store.edit { prefs ->
            val current = prefs[KEY_PURCHASES]
                ?.let { json.decodeFromString(SetSerializer(String.serializer()), it) } ?: emptySet()
            prefs[KEY_PURCHASES] = json.encodeToString(SetSerializer(String.serializer()), current + skus)
        }
    }

    suspend fun toggleBookmark(packId: String, questionId: Int, legacy: Boolean = false) {
        store.edit { prefs ->
            val current = prefs[KEY_BOOKMARKS]
                ?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }
                ?: emptyList()
            val key = if (legacy) "$packId:$questionId" else bookmarkKey(packId, questionId)
            val updated = if (key in current) current - key else current + key
            prefs[KEY_BOOKMARKS] = json.encodeToString(ListSerializer(String.serializer()), updated)
        }
    }

    suspend fun addResult(result: ExamResult) {
        store.edit { prefs ->
            val current = prefs[KEY_RESULTS]
                ?.let { json.decodeFromString(ListSerializer(ExamResult.serializer()), it) }
                ?: emptyList()
            prefs[KEY_RESULTS] = json.encodeToString(
                ListSerializer(ExamResult.serializer()), listOf(result) + current.take(99)
            )
        }
    }

    suspend fun clearResults() {
        store.edit { it.remove(KEY_RESULTS) }
    }
}
