package com.parsaapps.nichmarket.data

import android.content.Context
import com.parsaapps.nichmarket.data.model.Category
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

    private fun loadCategories(): List<Category> = try {
        val indexText = context.assets.open("questions/index.json")
            .bufferedReader().use { it.readText() }
        val index = json.decodeFromString<Index>(indexText)
        index.files.mapNotNull { file ->
            runCatching {
                val text = context.assets.open("questions/$file")
                    .bufferedReader().use { it.readText() }
                json.decodeFromString<Category>(text)
            }.getOrNull()
        }
    } catch (t: Throwable) {
        emptyList()
    }
}
