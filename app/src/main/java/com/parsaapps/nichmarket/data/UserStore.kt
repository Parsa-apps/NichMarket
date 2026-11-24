package com.parsaapps.nichmarket.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.parsaapps.nichmarket.data.model.ExamResult
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
class UserStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val KEY_PURCHASES = stringPreferencesKey("purchased_skus")
    private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks")
    private val KEY_RESULTS = stringPreferencesKey("results")

    val purchasedSkus: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_PURCHASES]?.let { json.decodeFromString(SetSerializer(String.serializer()), it) } ?: emptySet()
    }

    val bookmarks: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_BOOKMARKS]?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }
            ?: emptyList()
    }

    val results: Flow<List<ExamResult>> = context.dataStore.data.map { prefs ->
        prefs[KEY_RESULTS]?.let { json.decodeFromString(ListSerializer(ExamResult.serializer()), it) }
            ?: emptyList()
    }

    suspend fun addPurchase(sku: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_PURCHASES]
                ?.let { json.decodeFromString(SetSerializer(String.serializer()), it) } ?: emptySet()
            prefs[KEY_PURCHASES] = json.encodeToString(SetSerializer(String.serializer()), current + sku)
        }
    }

    suspend fun setPurchases(skus: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PURCHASES] = json.encodeToString(SetSerializer(String.serializer()), skus)
        }
    }

    suspend fun toggleBookmark(packId: String, questionId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKS]
                ?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }
                ?: emptyList()
            val key = "$packId:$questionId"
            val updated = if (key in current) current - key else current + key
            prefs[KEY_BOOKMARKS] = json.encodeToString(ListSerializer(String.serializer()), updated)
        }
    }

    suspend fun addResult(result: ExamResult) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RESULTS]
                ?.let { json.decodeFromString(ListSerializer(ExamResult.serializer()), it) }
                ?: emptyList()
            prefs[KEY_RESULTS] = json.encodeToString(
                ListSerializer(ExamResult.serializer()), listOf(result) + current.take(99)
            )
        }
    }

    suspend fun clearResults() {
        context.dataStore.edit { it.remove(KEY_RESULTS) }
    }
}
