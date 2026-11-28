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
    val explanation: String? = null,
    /** درس/سرفصل سوال (مثلاً «ریاضی و آمار») — برای نمایش در آزمون و مرور */
    val subject: String = "",
    /** سطح دشواری: «آسان» | «متوسط» | «دشوار» */
    val difficulty: String = "",
    /** منبع قابل بررسی برای راستی‌آزمایی سؤال؛ در داده‌های قدیمی null می‌ماند. */
    val sourceTitle: String? = null,
    val sourceUrl: String? = null,
    /** تاریخ بررسی منبع به قالب ISO-8601 (مثلاً 2026-11-28). */
    val sourceCheckedAt: String? = null
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
    val answers: Map<Int, Int> = emptyMap(),
    /** Snapshot protects reviews from subsequent edits to the question bank. */
    val questionSnapshot: List<Question>? = null
) {
    val percent: Int get() = if (total == 0) 0 else (correct * 100) / total
}

/** کلید یکتا برای بوکمارک کردن سوال */
fun bookmarkKey(packId: String, questionId: Int): String = "v2:$packId:$questionId"

/** An unfinished exam is stored locally as a snapshot, including the sampled questions. */
@Serializable
data class ActiveExam(
    val sessionId: String,
    val pack: QuestionPack,
    val startedAtEpochMs: Long,
    val startedAtElapsedMs: Long,
    val bootCount: Int? = null,
    val answers: Map<Int, Int> = emptyMap(),
    val index: Int = 0,
    val revision: Long = 0,
    /** Question IDs flagged for a second look within this exam only. */
    val markedQuestionIds: Set<Int> = emptySet()
)
