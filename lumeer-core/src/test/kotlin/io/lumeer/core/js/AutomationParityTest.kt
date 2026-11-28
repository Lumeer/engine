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

import io.lumeer.core.task.executor.bridge.LumeerBridge
import io.lumeer.core.util.DateFormats
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.PolyglotException
import org.junit.jupiter.api.Test

/**
 * Replays the automation parity vectors (inputs and results of moment 2.31, numbro 2.5 and he 2.0 in GraalVM,
 * see AutomationParityVectors) through the Kotlin functions, and checks the lumeer bridge methods the scripts call.
 */
class AutomationParityTest {

    private val cases: List<AutomationParityVectors.Case> by lazy {
        AutomationParityVectors.read(javaClass.getResourceAsStream("/automation-parity-vectors.json.gz")!!)
    }

    @Test
    fun replayVectors() {
        val failures = mutableListOf<String>()
        for ((key, group) in cases.groupBy { it.zone to (it.now ?: 0L) }) {
            AutomationParityVectors.Environment(key.first, key.second).use { env ->
                for (case in group) {
                    val actual = AutomationParityVectors.replay(case, env)
                    if (actual != case.expected) failures.add("$case actual=[$actual]")
                }
            }
        }
        println("Automation parity vectors: ${cases.size} cases, ${failures.size} failures " + cases.groupingBy { it.kind }.eachCount())
        assertThat(failures).describedAs(failures.take(20).joinToString("\n")).isEmpty()
    }

    private fun bridge(): Any {
        val unsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
        return unsafe.javaClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, LumeerBridge::class.java)
    }

    /** The code the blocks generate, through the real LumeerBridge, gives the same as the Kotlin functions (server zone). */
    @Test
    fun bridgeMatchesFunctions() {
        val kinds = setOf("change", "shift", "format", "currency", "escape", "unescape")
        val sample = cases.filter { it.kind in kinds && it.args.none { a -> a.js == "undefined" || a.js == "[]" || a.js == "({})" } }.filterIndexed { i, _ -> i % 5 == 0 }
        val failures = mutableListOf<String>()
        val now = System.currentTimeMillis()
        AutomationParityVectors.Environment(null, now).use { env ->
            Context.newBuilder("js").engine(JsEngineFactory.getEngine()).allowAllAccess(true).build().use { context ->
                context.getBindings("js").putMember("lumeer", bridge())
                for (case in sample) {
                    val args = case.args.map { it.create(context) }
                    val code = when (case.kind) {
                        "change" -> "new Date(lumeer.changeDate(a[0], '${case.op}', (a[1]), '${case.unit}')).getTime()"
                        "shift" -> "new Date(lumeer.shiftDate(a[0], '${case.op}', '${case.unit}')).getTime()"
                        "format" -> "lumeer.formatDate(a[0], a[1], a[2])"
                        "currency" -> "lumeer.formatCurrency('${case.op}', ${case.unit}, a[0])"
                        "escape" -> "lumeer.escapeHtml(a[0])"
                        else -> "lumeer.unescapeHtml(a[0])"
                    }
                    val viaBridge = try {
                        val result = context.eval("js", "(function (a) { return $code; })").execute(args)
                        if (result.isNumber) AutomationParityVectors.format(result.asDouble()) else result.asString()
                    } catch (e: Exception) {
                        "ERROR"
                    }
                    val direct = AutomationParityVectors.replay(case.copy(zone = null), env)
                    if (viaBridge != direct) failures.add("$case bridge=[$viaBridge] kotlin=[$direct]")
                }
            }
        }
        println("Automation bridge: ${sample.size} cases, ${failures.size} failures")
        assertThat(failures).describedAs(failures.take(20).joinToString("\n")).isEmpty()
    }

    /** A context like JsExecutor builds: the bridge in the polyglot bindings and the shim for code generated before. */
    private fun shimContext(): Context = Context.newBuilder("js").engine(JsEngineFactory.getEngine()).allowAllAccess(true).build().apply {
        polyglotBindings.putMember("lumeer", bridge())
        eval("js", LegacyScriptShim.SCRIPT)
    }

    /** The code the blocks generated before (moment, numbro, he), exactly as rules stored it. Sticky units always added. */
    private fun legacyCode(case: AutomationParityVectors.Case): String? = when (case.kind) {
        "change" -> {
            val op = case.op!!
            val sticky = case.unit!!.startsWith("sticky_")
            val unit = case.unit!!.removePrefix("sticky_")
            val t = "lumeer_date_tmp"
            "/** MomentJs **/ " + if (op == "add" || op == "subtract") {
                when {
                    sticky -> "(($t = moment(a[0])).daysInMonth() === $t.date() ? $t.add((a[1]), '$unit').endOf('$unit') : $t.add((a[1]), '$unit')).toDate()"
                    unit == "businessDays" -> "moment(a[0]).businessAdd(${if (op == "add") "" else "-"}(a[1])).toDate()"
                    else -> "moment(a[0]).$op((a[1]), '$unit').toDate()"
                }
            } else {
                val setter = unit.removeSuffix("s")
                when {
                    unit == "days" || unit == "businessDays" -> "moment(a[0]).date((a[1]) - 1).toDate()"
                    sticky -> "(($t = moment(a[0])).daysInMonth() === $t.date() ? $t.$setter(a[1]).endOf('$unit') : $t.$setter(a[1])).toDate()"
                    else -> "moment(a[0]).$setter(a[1]).toDate()"
                }
            }
        }
        "shift" -> "/** MomentJs **/ moment(a[0]).${case.op}('${case.unit}').toDate()"
        "format" -> "formatMomentJsDate(a[0], a[1], a[2])"
        "parse" -> "parseMomentJsDate(a[0], a[1], a[2])"
        "currency" -> "/** numbro.js **/ lumeer_numbro('${case.op}', ${case.unit}, a[0])"
        "escape" -> "/** he.js **/ he.escape(a[0])"
        "unescape" -> "/** he.js **/ he.unescape(a[0])"
        else -> null
    }

    /**
     * Rules stored before call moment, numbro and he; LegacyScriptShim runs that code on the bridge. It must give what the
     * Kotlin functions give (which the vectors check against the libraries), with the old sticky "subtract" still adding.
     */
    @Test
    fun legacyShimMatchesFunctions() {
        val sample = cases.filter { it.kind != "formatUtc" && it.kind != "parseUtc" && it.args.none { a -> a.js == "undefined" || a.js == "[]" || a.js == "({})" } }
            .filterIndexed { i, _ -> i % 3 == 0 }
        val failures = mutableListOf<String>()
        AutomationParityVectors.Environment(null, System.currentTimeMillis()).use { env ->
            shimContext().use { context ->
                for (case in sample) {
                    val code = legacyCode(case) ?: continue
                    val args = case.args.map { it.create(context) }
                    val viaShim = try {
                        val result = context.eval("js", "(function (a) { var lumeer_date_tmp; var r = $code; return r instanceof Date ? r.getTime() : r; })").execute(args)
                        if (result.isNumber) AutomationParityVectors.format(result.asDouble()) else result.asString()
                    } catch (e: Exception) {
                        "ERROR"
                    }
                    // the old generator wrote add() also for sticky subtract
                    val expectedCase = if (case.kind == "change" && case.op == "subtract" && case.unit!!.startsWith("sticky_")) case.copy(op = "add") else case
                    val direct = AutomationParityVectors.replay(expectedCase.copy(zone = null), env)
                    if (viaShim != direct) failures.add("$case code=[$code] shim=[$viaShim] kotlin=[$direct]")
                }
            }
        }
        println("Automation legacy shim: ${sample.size} cases, ${failures.size} failures")
        assertThat(failures).describedAs(failures.take(20).joinToString("\n")).isEmpty()
    }

    /**
     * LegacyCodeMigration rewrites every shape the old blocks generated, and the rewritten code (run without the shim)
     * gives what the old code gives through the shim.
     */
    @Test
    fun migratedCodeMatchesLegacy() {
        val sample = cases.filter { it.kind != "formatUtc" && it.kind != "parseUtc" && it.args.none { a -> a.js == "undefined" || a.js == "[]" || a.js == "({})" } }
            .filterIndexed { i, _ -> i % 3 == 1 }
        val failures = mutableListOf<String>()
        val wrap = { code: String -> "var lumeer = Polyglot.import('lumeer');\n(function (a) { var lumeer_date_tmp; var r = $code; return r instanceof Date ? r.getTime() : r; })" }
        val run = { context: Context, script: String, args: List<Any?> ->
            try {
                val result = context.eval("js", script).execute(*args.toTypedArray())
                if (result.isNumber) AutomationParityVectors.format(result.asDouble()) else result.asString()
            } catch (e: Exception) {
                "ERROR"
            }
        }
        var tested = 0
        shimContext().use { legacyContext ->
            Context.newBuilder("js").engine(JsEngineFactory.getEngine()).allowAllAccess(true).build().use { context ->
                context.polyglotBindings.putMember("lumeer", bridge())
                for (case in sample) {
                    val legacy = wrap(legacyCode(case) ?: continue)
                    val migration = LegacyCodeMigration.migrate(legacy)
                    if (migration.replacements.size != 1 || migration.legacyCallsLeft.isNotEmpty()) {
                        failures.add("$case not migrated: $legacy")
                        continue
                    }
                    tested++
                    val expected = run(legacyContext, legacy, listOf(case.args.map { it.create(legacyContext) }))
                    val actual = run(context, migration.js, listOf(case.args.map { it.create(context) }))
                    if (actual != expected) failures.add("$case legacy=[$expected] migrated=[$actual] code=[${migration.replacements[0].after}]")
                }
            }
        }
        println("Automation code migration: $tested cases, ${failures.size} failures")
        assertThat(failures).describedAs(failures.take(20).joinToString("\n")).isEmpty()
    }

    /** Stored sticky "subtract" code keeps adding (as it did), the code generated now subtracts. */
    @Test
    fun legacyStickySubtractKeepsAdding() {
        shimContext().use { context ->
            context.polyglotBindings.getMember("lumeer").let { context.getBindings("js").putMember("lumeer", it) }
            val date = "new Date(Date.UTC(2026, 2, 31, 12))"
            val legacy = context.eval("js", "var lumeer_date_tmp; formatMomentJsDate(/** MomentJs **/ ((lumeer_date_tmp = moment($date)).daysInMonth() === lumeer_date_tmp.date() ? lumeer_date_tmp.add((1), 'months').endOf('months') : lumeer_date_tmp.add((1), 'months')).toDate(), 'YYYY-MM-DD', 'en')").asString()
            val current = context.eval("js", "lumeer.formatDate(new Date(lumeer.changeDate($date, 'subtract', (1), 'sticky_months')), 'YYYY-MM-DD', 'en')").asString()
            assertThat(legacy).isEqualTo("2026-04-30")
            assertThat(current).isEqualTo("2026-02-28")
            assertThat(context.eval("js", "moment($date).daysInMonth()").asInt()).isEqualTo(31)
            // a fractional count of business days fails (the plugin looped forever)
            assertThatThrownBy { context.eval("js", "moment($date).businessAdd(1.5).toDate()") }.isInstanceOf(PolyglotException::class.java)
        }
    }

    /** The Java API of the converters and e-mails: null instead of an error or an invalid date. */
    @Test
    fun javaFunctions() {
        val time = 1795689000000L
        assertThat(DateFormats.format(time, "D. MMMM YYYY", "cs", true)).isEqualTo("26. listopadu 2026")
        assertThat(DateFormats.format(time, "YYYY. MMMM D.", "hu-HU", true)).isEqualTo("2026. november 26.")
        assertThat(DateFormats.format(time, "", "en", true)).isEqualTo("2026-11-26T10:30:00Z")
        assertThat(DateFormats.format(time, "LL", "de", true)).isEqualTo("26. November 2026")
        assertThat(DateFormats.parse("26. listopadu 2026 10:10", "D. MMMM YYYY H:mm", "cs", true) as Long?).isEqualTo(1795687800000L)
        assertThat(DateFormats.parse("abc", "DD.MM.YYYY", "cs", true) as Long?).isNull()
        assertThat(DateFormats.parse(null, null, null, true) as Long?).isNull()
    }
}
