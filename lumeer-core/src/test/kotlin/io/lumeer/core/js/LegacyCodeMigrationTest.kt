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

import org.assertj.core.api.Assertions.assertThat
import org.graalvm.polyglot.Context
import org.junit.jupiter.api.Test

class LegacyCodeMigrationTest {

    private val header = "var lumeer = Polyglot.import('lumeer');\n"

    private fun migrated(code: String): String {
        val result = LegacyCodeMigration.migrate(header + code)
        assertThat(result.legacyCallsLeft).describedAs(result.js).isEmpty()
        return result.js.removePrefix(header)
    }

    @Test
    fun simpleFunctions() {
        assertThat(migrated("x = formatMomentJsDate(lumeer.getDocumentAttribute(i, 'a3'), 'DD.MM.YYYY', 'cz');"))
            .isEqualTo("x = lumeer.formatDate(lumeer.getDocumentAttribute(i, 'a3'), 'DD.MM.YYYY', 'cz');")
        assertThat(migrated("x = parseMomentJsDate(s, 'D.M.YYYY', 'en');")).isEqualTo("x = lumeer.parseDate(s, 'D.M.YYYY', 'en');")
        assertThat(migrated("x = /** numbro.js **/ lumeer_numbro('cs-CZ', 2, y);")).isEqualTo("x = lumeer.formatCurrency('cs-CZ', 2, y);")
        assertThat(migrated("x = /** he.js **/ he.escape(y) + /** he.js **/ he.unescape(z);")).isEqualTo("x = lumeer.escapeHtml(y) + lumeer.unescapeHtml(z);")
    }

    @Test
    fun dateChange() {
        assertThat(migrated("x = (/** MomentJs **/ moment(((new Date()))).add((1), 'months').toDate()).toISOString();"))
            .isEqualTo("x = (new Date(lumeer.changeDate(((new Date())), 'add', (1), 'months'))).toISOString();")
        assertThat(migrated("x = /** MomentJs **/ moment(d).subtract((n * 2), 'days').toDate();"))
            .isEqualTo("x = new Date(lumeer.changeDate(d, 'subtract', (n * 2), 'days'));")
        assertThat(migrated("x = /** MomentJs **/ moment(d).businessAdd((3)).toDate();"))
            .isEqualTo("x = new Date(lumeer.changeDate(d, 'add', (3), 'businessDays'));")
        assertThat(migrated("x = /** MomentJs **/ moment(d).businessAdd(-(3)).toDate();"))
            .isEqualTo("x = new Date(lumeer.changeDate(d, 'subtract', (3), 'businessDays'));")
        assertThat(migrated("x = /** MomentJs **/ moment(d).date((5) - 1).toDate();"))
            .isEqualTo("x = new Date(lumeer.changeDate(d, 'set', (5), 'days'));")
        assertThat(migrated("x = /** MomentJs **/ moment(d).month(5).toDate();"))
            .isEqualTo("x = new Date(lumeer.changeDate(d, 'set', (5), 'months'));")
        assertThat(migrated("x = /** MomentJs **/ moment(d).startOf('week').toDate();"))
            .isEqualTo("x = new Date(lumeer.shiftDate(d, 'startOf', 'week'));")
    }

    @Test
    fun stickyChange() {
        // the old block generated add() also for subtract, the migrated code keeps adding
        assertThat(migrated("x = /** MomentJs **/ ((lumeer_date_tmp = moment(d)).daysInMonth() === lumeer_date_tmp.date() ? lumeer_date_tmp.add((1), 'months').endOf('months') : lumeer_date_tmp.add((1), 'months')).toDate();"))
            .isEqualTo("x = new Date(lumeer.changeDate(d, 'add', (1), 'sticky_months'));")
        assertThat(migrated("x = /** MomentJs **/ ((lumeer_date_tmp2 = moment(d)).daysInMonth() === lumeer_date_tmp2.date() ? lumeer_date_tmp2.year(2030).endOf('years') : lumeer_date_tmp2.year(2030)).toDate();"))
            .isEqualTo("x = new Date(lumeer.changeDate(d, 'set', (2030), 'sticky_years'));")
    }

    @Test
    fun nestedCalls() {
        assertThat(migrated("x = formatMomentJsDate(/** MomentJs **/ moment(lumeer.getDocumentAttribute(thisDocument, 'a1')).endOf('month').toDate(), 'YYYY-MM-DD', 'en');"))
            .isEqualTo("x = lumeer.formatDate(new Date(lumeer.shiftDate(lumeer.getDocumentAttribute(thisDocument, 'a1'), 'endOf', 'month')), 'YYYY-MM-DD', 'en');")
        // one replacement, the inner call is a part of it
        assertThat(LegacyCodeMigration.migrate(header + "x = formatMomentJsDate(/** he.js **/ he.escape(y), 'D', 'en');").replacements.map { it.after })
            .containsExactly("lumeer.formatDate(lumeer.escapeHtml(y), 'D', 'en')")
    }

    @Test
    fun usesTheLumeerVariableOfTheCode() {
        val result = LegacyCodeMigration.migrate("var lumeer2 = Polyglot.import('lumeer');\nx = /** he.js **/ he.escape(y);")
        assertThat(result.js).isEqualTo("var lumeer2 = Polyglot.import('lumeer');\nx = lumeer2.escapeHtml(y);")
    }

    @Test
    fun leavesOtherCode() {
        val code = header + listOf(
            "s = 'formatMomentJsDate(a, b, c)';",
            "// he.escape(x)",
            "/* moment(d).add((1), 'days').toDate() */",
            "r = /moment\\(['(]/g;",
            "t = \"/** MomentJs **/ moment(d).add((1), 'days').toDate()\";",
            "o.moment(d); o.he.escape(x);",
            "function f(s) { return /formatMomentJsDate\\(a, (b), 'c'\\)/.test(s) ? returned / 2 : 0; }",
        ).joinToString("\n")
        val result = LegacyCodeMigration.migrate(code)
        assertThat(result.changed).isFalse
        assertThat(result.js).isEqualTo(code)
        assertThat(result.legacyCallsLeft).isEmpty()
    }

    @Test
    fun reportsShapesItDoesNotKnow() {
        val code = header + "x = moment(d).add(1, 'days').format('YYYY'); y = formatMomentJsDate(d, 'YYYY'); z = /** he.js **/ he.escape(w);"
        val result = LegacyCodeMigration.migrate(code)
        assertThat(result.js).isEqualTo(header + "x = moment(d).add(1, 'days').format('YYYY'); y = formatMomentJsDate(d, 'YYYY'); z = lumeer.escapeHtml(w);")
        assertThat(result.legacyCallsLeft).containsExactly("moment", "formatMomentJsDate")
    }

    @Test
    fun skipsCodeWithoutTheLumeerVariable() {
        val result = LegacyCodeMigration.migrate("x = formatMomentJsDate(d, 'YYYY', 'en');")
        assertThat(result.changed).isFalse
        assertThat(result.skipReason).isNotNull
        assertThat(result.legacyCallsLeft).containsExactly("formatMomentJsDate")
    }

    @Test
    fun isIdempotentAndParses() {
        val code = header + "var lumeer_date_tmp;\nif (formatMomentJsDate((new Date()), 'e', 'en') < 2 || formatMomentJsDate((new Date()), 'E', 'en') == lumeer.getDocumentAttribute(i, 'a12')) {\n  lumeer.setDocumentAttribute(u, 'a5', (/** MomentJs **/ moment(((new Date()))).add((1), 'months').toDate()).toISOString());\n}"
        val once = LegacyCodeMigration.migrate(code)
        assertThat(once.replacements).hasSize(3)
        val twice = LegacyCodeMigration.migrate(once.js)
        assertThat(twice.changed).isFalse
        assertThat(twice.js).isEqualTo(once.js)
        Context.newBuilder("js").engine(JsEngineFactory.getEngine()).allowAllAccess(true).build().use { it.parse("js", once.js) }
    }
}
