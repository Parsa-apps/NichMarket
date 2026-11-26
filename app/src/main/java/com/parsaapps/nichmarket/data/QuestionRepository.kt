package com.parsaapps.nichmarket.data

import android.content.Context
import android.util.Log
import com.parsaapps.nichmarket.data.model.Category
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.data.model.QuestionPack
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** بارگذاری بانک سوالات از پوشه assets/questions */
class QuestionRepository(private val context: Context) {

    @Serializable
    private data class Index(val files: List<String> = emptyList())

    private val json = Json { ignoreUnknownKeys = true }

    val categories: List<Category> by lazy { loadCategories() }

    /** تمام SKUهای پولی موجود در اپ (برای query از مایکت) */
    val allSkus: List<String> by lazy {
        categories.flatMap { c -> c.packages.mapNotNull { it.sku } }
    }

    fun findCategory(id: String): Category? = categories.firstOrNull { it.id == id }

    fun findPack(packId: String): QuestionPack? =
        categories.asSequence().flatMap { it.packages }.firstOrNull { it.id == packId }

    val legacyCategories: List<Category> by lazy { loadCategories("questions_legacy") }

    fun findLegacyPack(packId: String): QuestionPack? =
        legacyCategories.asSequence().flatMap { it.packages }.firstOrNull { it.id == packId }

    private fun loadCategories(directory: String = "questions"): List<Category> = try {
        val indexText = context.assets.open("$directory/index.json")
            .bufferedReader().use { it.readText() }
        val index = json.decodeFromString<Index>(indexText)
        index.files.mapNotNull { file ->
            runCatching {
                val text = context.assets.open("$directory/$file")
                    .bufferedReader().use { it.readText() }
                sanitize(json.decodeFromString<Category>(text), file)
            }.onFailure { Log.w(TAG, "خطا در خواندن فایل سوالات $file", it) }.getOrNull()
        }
    } catch (t: Throwable) {
        Log.e(TAG, "خطا در بارگذاری بانک سوالات", t)
        emptyList()
    }

    /**
     * اعتبارسنجی و پاک‌سازی بانک سوالات: سوال‌های خراب (گزینه ناقص،
     * شاخص پاسخ خارج از بازه، متن خالی یا شناسه تکراری) حذف می‌شوند تا
     * یک خطای تایپی در JSON کل دسته را از کار نیندازد.
     */
    private fun sanitize(category: Category, file: String): Category {
        val cleanPackages = category.packages.map { pack ->
            val seenIds = mutableSetOf<Int>()
            val cleanQuestions = pack.questions.filter { q ->
                val ok = isValid(q) && seenIds.add(q.id)
                if (!ok) Log.w(TAG, "سوال نامعتبر در $file/${pack.id}: id=${q.id}")
                ok
            }
            pack.copy(questions = cleanQuestions)
        }
        return category.copy(packages = cleanPackages)
    }

    private fun isValid(q: Question): Boolean =
        q.text.isNotBlank() &&
            q.options.size == 4 &&
            q.options.all { it.isNotBlank() } &&
            q.answerIndex in q.options.indices &&
            !q.explanation.isNullOrBlank()

    private companion object {
        const val TAG = "QuestionRepository"
    }
}
