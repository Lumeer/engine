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
package io.lumeer.core.js

import io.lumeer.datafilters.automation.JsDate
import io.lumeer.datafilters.automation.JsObject
import io.lumeer.datafilters.automation.JsUndefined
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.time.ZoneId

/** Converts script values to the JS value model of the data-filters automation functions. */
object AutomationJsValues {

    @JvmStatic
    @JvmOverloads
    fun toJs(value: Value?, zone: ZoneId = ZoneId.systemDefault()): Any? = when {
        value == null -> JsUndefined
        value.isNull -> if (value.toString() == "undefined") JsUndefined else null
        value.isBoolean -> value.asBoolean()
        value.isNumber -> value.asDouble()
        value.isString -> value.asString()
        // JS Date: its time value (asInstant() goes through local time and is ambiguous in a DST overlap)
        isJsDate(value) -> JsDate(value.invokeMember("getTime").asDouble(), value.invokeMember("toString").asString())
        // java.util.Date, ZonedDateTime, Instant (the script engine gives them the Date prototype)
        value.isInstant -> JsDate(value.asInstant().toEpochMilli().toDouble())
        value.hasArrayElements() -> (0 until value.arraySize).map { toJs(value.getArrayElement(it), zone) }
        !value.isHostObject && value.hasMembers() -> value.memberKeys.associateWith { toJs(value.getMember(it), zone) }
        else -> JsObject(value.toString())
    }

    private fun isJsDate(value: Value): Boolean =
        !value.isHostObject && value.metaObject?.metaSimpleName == "Date" && value.canInvokeMember("getTime")

    /** `new Date(string).getTime()` in the current script context */
    @JvmStatic
    fun jsDateParse(input: String): Double =
        Context.getCurrent().getBindings("js").getMember("Date").newInstance(input).invokeMember("getTime").asDouble()
}
