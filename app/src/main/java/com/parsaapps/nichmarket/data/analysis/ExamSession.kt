package com.parsaapps.nichmarket.data.analysis

import com.parsaapps.nichmarket.data.model.QuestionPack
import kotlin.random.Random

/** The full exam keeps the original order. A shorter exam samples without replacement. */
fun examSession(pack: QuestionPack, limit: Int, random: Random = Random.Default): QuestionPack {
    val total = pack.questions.size
    if (limit <= 0 || limit >= total || total == 0) return pack
    val minutes = ((pack.durationMinutes.toLong() * limit + total - 1) / total)
        .coerceAtLeast(1).toInt()
    return pack.copy(questions = pack.questions.shuffled(random).take(limit), durationMinutes = minutes)
}
