package com.parsaapps.nichmarket.data.analysis

import java.text.Normalizer
import java.util.Locale

/** Normalizes Persian/Arabic forms and digits for forgiving local, offline search. */
fun normalizePersianSearch(text: String): String {
    val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
    return buildString {
        for (character in normalized) {
            when {
                Character.getType(character) == Character.NON_SPACING_MARK.toInt() -> Unit
                character.isWhitespace() || character == '\u200c' || character == '\u200d' || character == '\u0640' -> Unit
                character == 'ي' || character == 'ى' -> append('ی')
                character == 'ك' -> append('ک')
                character in '۰'..'۹' -> append('0' + (character - '۰'))
                character in '٠'..'٩' -> append('0' + (character - '٠'))
                else -> append(character)
            }
        }
    }
}

/** All entered words must occur; word order is not significant. */
fun matchesPersianSearch(query: String, vararg fields: String): Boolean {
    val tokens = query.split(Regex("\\s+"))
        .map(::normalizePersianSearch).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return true
    val searchable = fields.joinToString(" ") { normalizePersianSearch(it) }
    return tokens.all { it in searchable }
}
