package com.parsaapps.nichmarket.data.model

import kotlinx.serialization.Serializable

/** یک دسته‌بندی آزمون (مثلاً استخدامی آموزش و پرورش) */
@Serializable
data class Category(
    val id: String,
    val title: String,
    val emoji: String = "📘",
    val description: String = "",
    val packages: List<QuestionPack> = emptyList()
) {
    val totalQuestions: Int get() = packages.sumOf { it.questions.size }
}

/** بسته تست — رایگان (sku = null) یا پولی (sku = شناسه محصول در مایکت) */
@Serializable
data class QuestionPack(
    val id: String,
    val title: String,
    val description: String = "",
    val sku: String? = null,
    val priceToman: Int = 0,
    val durationMinutes: Int = 20,
    val questions: List<Question> = emptyList()
) {
    val isFree: Boolean get() = sku == null
}

/** یک سوال چهارگزینه‌ای */
@Serializable
data class Question(
    val id: Int,
    val text: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String? = null
)

/** نتیجه یک آزمون انجام‌شده */
@Serializable
data class ExamResult(
    val id: String,
    val packId: String,
    val packTitle: String,
    val date: Long,
    val correct: Int,
    val wrong: Int,
    val blank: Int,
    val total: Int,
    val spentSeconds: Int,
    /** پاسخ‌های کاربر: شناسه سوال → گزینه انتخاب‌شده */
    val answers: Map<Int, Int> = emptyMap()
) {
    val percent: Int get() = if (total == 0) 0 else (correct * 100) / total
}

/** کلید یکتا برای بوکمارک کردن سوال */
fun bookmarkKey(packId: String, questionId: Int): String = "$packId:$questionId"
