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

import com.fasterxml.jackson.databind.ObjectMapper
import io.lumeer.datafilters.automation.AutomationDates
import io.lumeer.datafilters.automation.AutomationHtml
import io.lumeer.datafilters.automation.AutomationNumbers
import io.lumeer.datafilters.automation.DateChangeOperation
import io.lumeer.datafilters.automation.DateChangeUnit
import io.lumeer.datafilters.automation.DateShiftOperation
import io.lumeer.datafilters.automation.DateShiftUnit
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Parity vectors of the automation functions: inputs and the results of the original libraries (moment 2.31, numbro 2.5,
 * he 2.0 in GraalVM), recorded by the local oracle tests and replayed by [AutomationParityTest].
 */
object AutomationParityVectors {

    /** A script value: JS code, or a Java object as the bridge passes it to a script */
    data class Arg(var js: String? = null, var javaDate: Long? = null, var zoned: Long? = null, var zonedZone: String? = null, var local: String? = null) {
        fun create(context: Context): Value {
            val code = js
            val date = javaDate
            val zonedTime = zoned
            return when {
                code != null -> context.eval("js", "($code)")
                else -> context.eval("js", "(function (v) { return v; })").execute(
                    when {
                        date != null -> Date(date)
                        zonedTime != null -> Instant.ofEpochMilli(zonedTime).atZone(ZoneId.of(zonedZone))
                        else -> LocalDateTime.parse(local)
                    }
                )
            }
        }

        companion object {
            fun js(code: String) = Arg(js = code)
            fun string(value: String) = Arg(js = ObjectMapper().writeValueAsString(value))
        }
    }

    /**
     * kind: change (op, unit, [date, count]), shift (op, unit, [date]), format ([time, format, locale]), parse ([date, format, locale]),
     * formatUtc, parseUtc (same args), currency (locale = op, decimals = unit, [value]), escape, unescape ([value]).
     */
    data class Case(var kind: String = "", var zone: String? = null, var now: Long? = null, var op: String? = null, var unit: String? = null, var args: List<Arg> = emptyList(), var expected: String = "")

    class Recorder(private val file: File?) {
        private val cases = mutableListOf<Case>()
        fun add(case: Case) {
            if (file != null) cases.add(case)
        }
        fun write() {
            if (file == null) return
            val existing = if (file.exists()) read(file.inputStream()) else emptyList()
            GZIPOutputStream(file.outputStream()).use { ObjectMapper().writeValue(it, existing + cases) }
        }
        companion object {
            /** -Dautomation.record=path appends the cases to that vectors file */
            fun fromProperty() = Recorder(System.getProperty("automation.record")?.let { File(it) })
        }
    }

    fun read(stream: InputStream): List<Case> = GZIPInputStream(stream).use { input ->
        val mapper = ObjectMapper()
        mapper.readValue(input, mapper.typeFactory.constructCollectionType(List::class.java, Case::class.java))
    }

    fun format(time: Double): String = if (time.isNaN()) "NaN" else time.toLong().toString()

    /** The functions of one time zone (and "now") */
    class Environment(val zone: String?, val now: Long) : AutoCloseable {
        val context: Context = Context.newBuilder("js").engine(JsEngineFactory.getEngine()).allowAllAccess(true).let { builder ->
            if (zone != null) builder.allowExperimentalOptions(true).option("js.timezone", zone) else builder
        }.build().apply {
            eval("js", "Date.now = function () { return $now; };")
        }
        private val zoneId: ZoneId = zone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val dates = AutomationDates(zone, { now }, { input: String -> context.getBindings("js").getMember("Date").newInstance(input).invokeMember("getTime").asDouble() })

        fun js(arg: Arg): Any? = AutomationJsValues.toJs(arg.create(context), zoneId)

        override fun close() = context.close()
    }

    /** The result of the Kotlin implementation, "ERROR" when it fails (as the original script did) */
    fun replay(case: Case, env: Environment): String = try {
        val a = case.args.map { env.js(it) }
        when (case.kind) {
            "change" -> format(env.dates.changeDate(a[0], DateChangeOperation.fromValue(case.op!!), a[1], DateChangeUnit.fromValue(case.unit!!)))
            "shift" -> format(env.dates.shiftDate(a[0], DateShiftOperation.fromValue(case.op!!), DateShiftUnit.fromValue(case.unit!!)))
            "format" -> env.dates.formatDate(a[0], a[1], a[2])
            "formatUtc" -> env.dates.formatDate(a[0], a[1], a[2], utc = true)
            "parse" -> format(env.dates.parseDate(a[0], a[1], a[2]))
            "parseUtc" -> format(env.dates.parseDate(a[0], a[1], a[2], utc = true))
            "currency" -> AutomationNumbers.formatCurrency(case.op!!, case.unit!!.toDouble(), a[0])
            "escape" -> AutomationHtml.escape(a[0])
            "unescape" -> AutomationHtml.unescape(a[0])
            else -> error("Unknown kind ${case.kind}")
        }
    } catch (e: Exception) {
        "ERROR"
    }
}
