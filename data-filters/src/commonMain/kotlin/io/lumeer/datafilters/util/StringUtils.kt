/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.datafilters.util

private val FORMATTING_TAGS = listOf("strong", "em", "i", "sup", "sub", "u", "strike", "s", "del", "cite", "code", "b", "span", "p", "br", "a")
private val TAGS = FORMATTING_TAGS.joinToString("|")

// JS "." without the s flag (Java "." additionally excludes U+0085, so it is spelled out)
private const val JS_DOT = "[^\\n\\r\\u2028\\u2029]"

private val FORMATTING_TAGS_REGEX = Regex("<(/?(${TAGS})${JS_S}*/?)[^>]*>")
private val NOT_FORMATTING_TAGS_REGEX = Regex("<(?!/?(${TAGS})${JS_S}*/?)[^>]*>")
private val ANY_TAG_REGEX = Regex("<(?:${JS_DOT}|${JS_S})*?>")
private val CLASS_REGEX = Regex("class=\"${JS_DOT}*?\"")
private val COMBINING_MARKS_REGEX = Regex("[\\u0300-\\u036f]")
private val TITLE_WORD_REGEX = Regex("[A-Za-z0-9_][^${JS_WHITESPACE}]*")

/** @lumeer/utils stripTextHtmlTags */
internal fun stripTextHtmlTags(text: String?, keepFormattingTags: Boolean = true): String {
    val withoutTags = if (keepFormattingTags) {
        jsTrim((text ?: "").replace(NOT_FORMATTING_TAGS_REGEX, " "))
    } else {
        jsTrim(jsTrim((text ?: "").replace(FORMATTING_TAGS_REGEX, "")).replace(ANY_TAG_REGEX, " "))
    }
    return withoutTags.replace(CLASS_REGEX, "")
}

/** @lumeer/utils removeAccentFromString */
internal fun removeAccentFromString(value: String?, lowerCase: Boolean = true): String {
    val text = value ?: ""
    return normalizeNfd(if (lowerCase) text.lowercase() else text).replace(COMBINING_MARKS_REGEX, "")
}

private val EMAIL_REGEX = Regex("^(([^<>()\\[\\]\\\\.,;:${JS_WHITESPACE}@\"]+(\\.[^<>()\\[\\]\\\\.,;:${JS_WHITESPACE}@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$")

/** @lumeer/utils isEmailValid */
internal fun isEmailValid(email: String?): Boolean = !email.isNullOrEmpty() && EMAIL_REGEX.containsMatchIn(email)

/** lodash escape */
internal fun escapeHtmlString(value: String): String = buildString(value.length) {
    for (c in value) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }
}

private val HTML_ENTITY_REGEX = Regex("&(?:amp|lt|gt|quot|#39);")

/** lodash unescape */
internal fun unescapeHtmlString(value: String): String = value.replace(HTML_ENTITY_REGEX) {
    when (it.value) {
        "&amp;" -> "&"
        "&lt;" -> "<"
        "&gt;" -> ">"
        "&quot;" -> "\""
        else -> "'"
    }
}

/** @lumeer/utils escapeHtml for string input */
internal fun escapeHtml(value: String): String = escapeHtmlString(unescapeHtmlString(value))

internal fun transformTextToTitleCase(text: String): String =
    text.replace(TITLE_WORD_REGEX) { it.value.substring(0, 1).uppercase() + it.value.substring(1) }

internal fun transformTextToSentenceCase(text: String): String {
    // JS text.split(/([.!?])/g) keeps the separators as separate items
    val parts = mutableListOf<String>()
    var start = 0
    for (i in text.indices) {
        if (text[i] == '.' || text[i] == '!' || text[i] == '?') {
            parts.add(text.substring(start, i))
            parts.add(text[i].toString())
            start = i + 1
        }
    }
    parts.add(text.substring(start))

    val result = StringBuilder()
    for (sentence in parts) {
        if (sentence == "." || sentence == "!" || sentence == "?") {
            result.append(sentence).append(' ')
        } else {
            val words = jsTrim(sentence).split(" ")
            result.append(words.mapIndexed { index, word ->
                if (index == 0) (if (word.isNotEmpty()) word.substring(0, 1).uppercase() + word.substring(1) else "") else word
            }.joinToString(" "))
        }
    }
    return jsTrim(result.toString())
}

/** @lumeer/utils formatUnknownDataValue (the decimal separator is always "."). */
internal fun formatUnknownDataValue(value: Any?): String = if (hasJsDisplayValue(value)) jsString(value) else ""

/** data-filters valueMeetFulltexts */
internal fun valueMeetFulltexts(value: String?, fulltexts: List<String>): Boolean {
    val formattedValue = jsTrim(removeAccentFromString(value ?: ""))
    return fulltexts.all { formattedValue.contains(jsTrim(it.lowercase())) }
}
