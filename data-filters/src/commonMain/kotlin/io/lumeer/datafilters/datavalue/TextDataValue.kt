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

import io.lumeer.datafilters.constraint.CaseStyle
import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.removeAccentFromString
import io.lumeer.datafilters.util.stripTextHtmlTags
import io.lumeer.datafilters.util.transformTextToSentenceCase
import io.lumeer.datafilters.util.transformTextToTitleCase

class TextDataValue(val value: Any?, private val caseStyle: CaseStyle?) : DataValue {

    override fun format(): String {
        if (value !is String) {
            return formatUnknownDataValue(value)
        }
        if (value.isEmpty()) {
            return ""
        }
        return when (caseStyle) {
            CaseStyle.LowerCase -> value.lowercase()
            CaseStyle.UpperCase -> value.uppercase()
            CaseStyle.TitleCase -> transformTextToTitleCase(value)
            CaseStyle.SentenceCase -> transformTextToSentenceCase(value)
            else -> value
        }
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean =
        meetTextCondition(condition, values, { TextDataValue(it, caseStyle) }) { jsTrim(stripTextHtmlTags(it.format(), false).lowercase()) }

    override fun meetFullTexts(fulltexts: List<String>): Boolean {
        val formattedValue = removeAccentFromString(jsTrim(stripTextHtmlTags(format(), false)))
        return fulltexts.all { formattedValue.contains(jsTrim(stripTextHtmlTags(it, false).lowercase())) }
    }
}
