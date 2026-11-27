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
package io.lumeer.datafilters.automation

/** JavaScript `undefined` (Kotlin null is JavaScript null). */
object JsUndefined {
    override fun toString(): String = "undefined"
}

/**
 * A JavaScript Date, or a platform date object that the script engine treats as one. [time] is NaN for an Invalid Date.
 * [string] is its JS String(date) when known (it depends on the script engine and the time zone).
 */
class JsDate(val time: Double, val string: String? = null) {
    override fun toString(): String = string ?: "JsDate($time)"
}

/** Any other object, for example a Java object without a JS counterpart. [string] is its JS String(). */
class JsObject(val string: String) {
    override fun toString(): String = string
}

/** The script would have failed in the original library (TypeError, or a loop that never ends). */
class AutomationFunctionException(message: String) : RuntimeException(message)
