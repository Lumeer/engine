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
package io.lumeer.datafilters.datavalue

import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.util.isEmailValid
import io.lumeer.datafilters.util.jsStringOrEmpty
import io.lumeer.datafilters.util.jsTrim

/** Saved value is formatted as 'Link [Text]' */
class LinkDataValue(val value: Any?) : DataValue {
    val linkValue: String
    val titleValue: String

    init {
        val (rawLink, rawTitle) = parseLinkValue(jsStringOrEmpty(value))
        if (isEmailValid(rawLink)) {
            linkValue = "mailto:$rawLink"
            titleValue = rawTitle.ifEmpty { rawLink }
        } else {
            linkValue = rawLink
            titleValue = rawTitle
        }
    }

    /** Title or link, the text used for fulltexts */
    override fun format(): String = titleValue.ifEmpty { linkValue }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val linkFormatted = jsTrim(linkValue.lowercase())
        val titleFormatted = jsTrim(titleValue.lowercase())
        val other = values.firstOrNull()?.let { jsTrim(jsStringOrEmpty(it.value).lowercase()) } ?: "undefined"
        return when (condition) {
            ConditionType.Contains -> linkFormatted.contains(other) || titleFormatted.contains(other)
            ConditionType.NotContains -> !linkFormatted.contains(other) && !titleFormatted.contains(other)
            ConditionType.IsEmpty -> linkFormatted.isEmpty() && titleFormatted.isEmpty()
            ConditionType.NotEmpty -> linkFormatted.isNotEmpty() || titleFormatted.isNotEmpty()
            else -> false
        }
    }

    private fun parseLinkValue(value: String): Pair<String, String> {
        val trimmed = jsTrim(value)
        if (trimmed.endsWith(']')) {
            val titleStartIndex = trimmed.lastIndexOf('[')
            if (titleStartIndex != -1) {
                val title = trimmed.substring(titleStartIndex + 1, trimmed.length - 1)
                return Pair(jsTrim(trimmed.substring(0, titleStartIndex)).ifEmpty { title }, title)
            }
        }
        return Pair(trimmed, "")
    }
}
