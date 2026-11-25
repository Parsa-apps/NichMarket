package com.parsaapps.nichmarket.data.analysis

import com.parsaapps.nichmarket.data.model.Question

enum class AnswerStatus { CORRECT, WRONG, BLANK }

data class SubjectPerformance(val subject: String, val correct: Int, val total: Int) {
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total
}

fun answerStatus(question: Question, answers: Map<Int, Int>): AnswerStatus {
    val answer = answers[question.id] ?: return AnswerStatus.BLANK
    return if (answer == question.answerIndex) AnswerStatus.CORRECT else AnswerStatus.WRONG
}

/** Unanswered questions count toward the total, just as they do in the exam score. */
fun subjectPerformance(questions: List<Question>, answers: Map<Int, Int>): List<SubjectPerformance> =
    questions.filter { it.subject.isNotBlank() }.groupBy { it.subject }
        .map { (subject, items) ->
            SubjectPerformance(subject, items.count { answerStatus(it, answers) == AnswerStatus.CORRECT }, items.size)
        }
        .sortedWith(compareBy<SubjectPerformance> { it.accuracy }.thenBy { it.subject })

/** Apply the review filters together; a null status and empty subject mean no restriction. */
fun matchesReviewQuestion(
    question: Question,
    answers: Map<Int, Int>,
    status: AnswerStatus?,
    subject: String,
    query: String
): Boolean = (status == null || answerStatus(question, answers) == status) &&
    (subject.isEmpty() || question.subject == subject) &&
    matchesPersianSearch(query, question.text, question.subject,
        question.options.joinToString(" "), question.explanation.orEmpty())
