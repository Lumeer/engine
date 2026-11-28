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

/**
 * Rewrites automation code that the blocks generated before (calling moment, numbro and he through [LegacyScriptShim])
 * to the code the blocks generate now (calling the lumeer bridge). Only the exact shapes the old blocks generated are
 * rewritten, each to code that gives the same result (the old sticky subtract, which added, stays an add); anything
 * else stays as it is and keeps running through the shim. Strings, comments and regular expressions are skipped.
 */
object LegacyCodeMigration {

    class Replacement(val before: String, val after: String)

    class Result(
        /** the migrated code, the original when nothing changed */
        val js: String,
        val replacements: List<Replacement>,
        /** legacy calls that stayed (not a shape the old blocks generated) */
        val legacyCallsLeft: List<String>,
        /** why the code was not migrated at all, null otherwise */
        val skipReason: String? = null,
    ) {
        val changed: Boolean get() = replacements.isNotEmpty()
    }

    private val LUMEER_VARIABLE = Regex("""var\s+([A-Za-z_$][\w$]*)\s*=\s*Polyglot\.import\(\s*'lumeer'\s*\)\s*;""")

    private val CHANGE_UNITS = setOf("seconds", "minutes", "hours", "days", "weeks", "months", "quarters", "years")
    private val STICKY_UNITS = setOf("months", "quarters", "years")
    private val SETTERS = mapOf(
        "second" to "seconds", "minute" to "minutes", "hour" to "hours", "week" to "weeks",
        "month" to "months", "quarter" to "quarters", "year" to "years",
    )
    private val SHIFT_OPERATIONS = setOf("startOf", "endOf")
    private val SHIFT_UNITS = setOf("minute", "hour", "day", "week", "month", "quarter", "year")
    private val SIMPLE_FUNCTIONS = mapOf(
        "formatMomentJsDate" to "formatDate",
        "parseMomentJsDate" to "parseDate",
        "lumeer_numbro" to "formatCurrency",
    )
    private val HE_FUNCTIONS = mapOf("escape" to "escapeHtml", "unescape" to "unescapeHtml")
    private val CHAIN_METHODS = setOf("add", "subtract", "businessAdd", "date") + SHIFT_OPERATIONS + SETTERS.keys
    private val LITERAL = Regex("^'([A-Za-z_]+)'$")

    private const val MOMENT_COMMENT = "/** MomentJs **/"
    private const val NUMBRO_COMMENT = "/** numbro.js **/"
    private const val HE_COMMENT = "/** he.js **/"

    fun migrate(js: String?): Result {
        if (js.isNullOrEmpty()) return Result(js.orEmpty(), emptyList(), emptyList())
        val legacy = legacyCalls(js)
        if (legacy.isEmpty()) return Result(js, emptyList(), emptyList())
        val variable = LUMEER_VARIABLE.find(js)?.groupValues?.get(1)
            ?: return Result(js, emptyList(), legacy, "the code does not import the lumeer bridge")
        val replacements = mutableListOf<Replacement>()
        val migrated = Rewriter(variable, replacements).rewrite(js)
        return Result(migrated, replacements, if (replacements.isEmpty()) legacy else legacyCalls(migrated))
    }

    /** The legacy calls in the code (outside strings and comments): moment, formatMomentJsDate, he.escape, ... */
    fun legacyCalls(js: String): List<String> {
        val source = Source(js)
        val calls = mutableListOf<String>()
        var i = 0
        while (i < js.length) {
            if (source.identifierStart(i)) {
                val name = source.identifier(i)
                val end = i + name.length
                if (!source.isMember(i)) {
                    val call = when (name) {
                        "he" -> source.member(end)?.takeIf { it.first in HE_FUNCTIONS }?.let { "he." + it.first }
                        "moment", in SIMPLE_FUNCTIONS -> name.takeIf { source.next(end) == '(' }
                        else -> null
                    }
                    if (call != null) calls.add(call)
                }
                i = end
            } else {
                i++
            }
        }
        return calls
    }

    /** The code with a mask of the characters outside strings, template literals, comments and regular expressions. */
    private class Source(val text: String) {
        private val code = BooleanArray(text.length)
        private val comments = HashSet<Int>()

        init {
            var i = 0
            var regexAllowed = true
            while (i < text.length) {
                val c = text[i]
                when {
                    c == '/' && text.startsWith("//", i) -> i = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                    c == '/' && text.startsWith("/*", i) -> {
                        comments.add(i)
                        i = text.indexOf("*/", i + 2).let { if (it < 0) text.length else it + 2 }
                    }
                    c == '\'' || c == '"' || c == BACKTICK -> {
                        i = skipQuoted(i, c)
                        regexAllowed = false
                    }
                    c == '/' && regexAllowed -> {
                        i = skipRegex(i)
                        regexAllowed = false
                    }
                    else -> {
                        code[i] = true
                        if (!c.isWhitespace()) {
                            regexAllowed = when {
                                // only the last character of a word decides (a keyword like return allows a regex)
                                isIdentifierPart(c) -> (i + 1 == text.length || !isIdentifierPart(text[i + 1])) && keywordBefore(i + 1)
                                c == ')' || c == ']' || c == '}' -> false
                                else -> true
                            }
                        }
                        i++
                    }
                }
            }
        }

        private fun skipQuoted(start: Int, quote: Char): Int {
            var i = start + 1
            while (i < text.length && text[i] != quote) i += if (text[i] == '\\') 2 else 1
            return i + 1
        }

        private fun skipRegex(start: Int): Int {
            var i = start + 1
            var inClass = false
            while (i < text.length && text[i] != '\n') {
                val c = text[i]
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == '[') inClass = true
                if (c == ']') inClass = false
                i++
                if (c == '/' && !inClass) break
            }
            while (i < text.length && text[i].isLetter()) i++
            return i
        }

        /** after a word, a "/" starts a regular expression only for keywords like return */
        private fun keywordBefore(end: Int): Boolean {
            var start = end
            while (start > 0 && isIdentifierPart(text[start - 1])) start--
            return text.substring(start, end) in REGEX_KEYWORDS
        }

        fun isCode(i: Int) = i in text.indices && code[i]

        fun isCommentStart(i: Int) = i in comments

        fun identifierStart(i: Int): Boolean {
            if (!isCode(i)) return false
            val c = text[i]
            if (!(c.isLetter() || c == '_' || c == '$')) return false
            return i == 0 || !isIdentifierPart(text[i - 1])
        }

        fun identifier(i: Int): String {
            var end = i
            while (end < text.length && isIdentifierPart(text[end])) end++
            return text.substring(i, end)
        }

        /** a property (x.moment), not a variable */
        fun isMember(i: Int): Boolean {
            var j = i - 1
            while (j >= 0 && text[j].isWhitespace()) j--
            return j >= 0 && text[j] == '.' && isCode(j)
        }

        fun skipSpace(i: Int): Int {
            var j = i
            while (j < text.length && text[j].isWhitespace()) j++
            return j
        }

        fun next(i: Int): Char? = skipSpace(i).let { if (isCode(it)) text[it] else null }

        /** the name after "." at [i] (spaces allowed) and the index after it; null when no member access follows */
        fun member(i: Int): Pair<String, Int>? {
            val dot = skipSpace(i)
            if (!isCode(dot) || text[dot] != '.') return null
            val start = skipSpace(dot + 1)
            if (!identifierStart(start)) return null
            val name = identifier(start)
            return name to start + name.length
        }

        /** the index after the bracket closing the one at [open], -1 when it is not closed */
        fun close(open: Int): Int {
            var depth = 0
            for (i in open until text.length) {
                if (!code[i]) continue
                when (text[i]) {
                    '(', '[', '{' -> depth++
                    ')', ']', '}' -> if (--depth == 0) return i + 1
                }
            }
            return -1
        }

        /** the arguments, trimmed, of the call whose brackets are at [open] and [end] - 1 */
        fun arguments(open: Int, end: Int): List<String> {
            val args = mutableListOf<String>()
            var depth = 0
            var start = open + 1
            for (i in open + 1 until end - 1) {
                if (!code[i]) continue
                when (text[i]) {
                    '(', '[', '{' -> depth++
                    ')', ']', '}' -> depth--
                    ',' -> if (depth == 0) {
                        args.add(text.substring(start, i).trim())
                        start = i + 1
                    }
                }
            }
            val last = text.substring(start, end - 1).trim()
            if (args.isNotEmpty() || last.isNotEmpty()) args.add(last)
            return args
        }

        companion object {
            const val BACKTICK = '`'
            val REGEX_KEYWORDS = setOf("return", "typeof", "case", "do", "else", "in", "instanceof", "new", "delete", "void", "throw", "yield", "await")
            fun isIdentifierPart(c: Char) = c.isLetterOrDigit() || c == '_' || c == '$'
        }
    }

    private class Rewriter(private val variable: String, private val replacements: MutableList<Replacement>) {

        private class Match(val end: Int, val replacement: String)

        private class Call(val end: Int, val args: List<String>)

        /** [text] with every legacy shape rewritten, also inside the arguments; [top] records the replacements */
        fun rewrite(text: String, top: Boolean = true): String {
            val source = Source(text)
            val out = StringBuilder()
            var copied = 0
            var i = 0
            while (i < text.length) {
                val match = if (source.isCode(i) || source.isCommentStart(i)) match(source, i) else null
                if (match != null) {
                    out.append(text, copied, i).append(match.replacement)
                    // a replacement inside the arguments of another one is a part of it
                    if (top) replacements.add(Replacement(text.substring(i, match.end), match.replacement))
                    copied = match.end
                    i = match.end
                } else {
                    i++
                }
            }
            return out.append(text, copied, text.length).toString()
        }

        private fun match(source: Source, i: Int): Match? {
            val text = source.text
            if (source.isCommentStart(i)) {
                val after = { comment: String -> source.skipSpace(i + comment.length) }
                return when {
                    text.startsWith(MOMENT_COMMENT, i) -> after(MOMENT_COMMENT).let { sticky(source, it) ?: chain(source, it) }
                    text.startsWith(NUMBRO_COMMENT, i) -> simple(source, after(NUMBRO_COMMENT))
                    text.startsWith(HE_COMMENT, i) -> he(source, after(HE_COMMENT))
                    else -> null
                }
            }
            return simple(source, i) ?: he(source, i)
        }

        private fun name(source: Source, i: Int): String? =
            if (source.identifierStart(i) && !source.isMember(i)) source.identifier(i) else null

        /** formatMomentJsDate(a, b, c), parseMomentJsDate(a, b, c), lumeer_numbro(a, b, c) */
        private fun simple(source: Source, i: Int): Match? {
            val name = name(source, i) ?: return null
            val function = SIMPLE_FUNCTIONS[name] ?: return null
            val call = call(source, i + name.length, 3) ?: return null
            return Match(call.end, "$variable.$function(${call.args.joinToString(", ")})")
        }

        /** he.escape(a), he.unescape(a) */
        private fun he(source: Source, i: Int): Match? {
            if (name(source, i) != "he") return null
            val (name, nameEnd) = source.member(i + 2) ?: return null
            val function = HE_FUNCTIONS[name] ?: return null
            val call = call(source, nameEnd, 1) ?: return null
            return Match(call.end, "$variable.$function(${call.args[0]})")
        }

        /**
         * moment(d).add((n), 'unit').toDate(), moment(d).businessAdd((n)).toDate(), moment(d).date((n) - 1).toDate(),
         * moment(d).month(n).toDate(), moment(d).startOf('unit').toDate()
         */
        private fun chain(source: Source, i: Int): Match? {
            val date = moment(source, i) ?: return null
            val (method, methodEnd) = source.member(date.end) ?: return null
            if (method !in CHAIN_METHODS) return null
            val call = call(source, methodEnd, null) ?: return null
            val end = toDate(source, call.end) ?: return null
            val d = date.args[0]
            val args = call.args
            val replacement = when (method) {
                "add", "subtract" -> {
                    val unit = args.takeIf { it.size == 2 }?.let { literal(it[1]) }?.takeIf { it in CHANGE_UNITS } ?: return null
                    if (!isParenthesized(args[0])) return null
                    change(d, method, args[0], unit)
                }
                "businessAdd" -> {
                    val count = args.singleOrNull() ?: return null
                    when {
                        isParenthesized(count) -> change(d, "add", count, "businessDays")
                        count.startsWith("-") && isParenthesized(count.substring(1)) -> change(d, "subtract", count.substring(1), "businessDays")
                        else -> return null
                    }
                }
                "date" -> {
                    val count = args.singleOrNull()?.takeIf { it.endsWith(" - 1") }?.removeSuffix(" - 1") ?: return null
                    if (!isParenthesized(count)) return null
                    change(d, "set", count, "days")
                }
                in SHIFT_OPERATIONS -> {
                    val unit = args.singleOrNull()?.let { literal(it) }?.takeIf { it in SHIFT_UNITS } ?: return null
                    "new Date($variable.shiftDate($d, '$method', '$unit'))"
                }
                in SETTERS -> change(d, "set", parenthesize(args.singleOrNull() ?: return null), SETTERS.getValue(method))
                else -> return null
            }
            return Match(end, replacement)
        }

        /**
         * ((t = moment(d)).daysInMonth() === t.date() ? t.add((n), 'months').endOf('months') : t.add((n), 'months')).toDate(),
         * and the same with a setter (t.month(n)) instead of add
         */
        private fun sticky(source: Source, i: Int): Match? {
            val text = source.text
            if (!text.startsWith("((", i) || !source.isCode(i + 1)) return null
            val outerEnd = source.close(i)
            if (outerEnd < 0) return null
            val end = toDate(source, outerEnd) ?: return null
            val tmp = name(source, i + 2) ?: return null
            var p = expect(source, i + 2 + tmp.length, "=") ?: return null
            if (text.startsWith("=", p)) return null
            val date = moment(source, source.skipSpace(p)) ?: return null
            p = expect(source, date.end, ").daysInMonth() === $tmp.date() ? $tmp.") ?: return null
            val first = setOrAdd(source, p) ?: return null
            val (endOf, endOfEnd) = source.member(first.second.end) ?: return null
            if (endOf != "endOf") return null
            val endOfCall = call(source, endOfEnd, 1) ?: return null
            val unit = literal(endOfCall.args[0])?.takeIf { it in STICKY_UNITS } ?: return null
            p = expect(source, endOfCall.end, ": $tmp.") ?: return null
            val second = setOrAdd(source, p) ?: return null
            if (second.first != first.first || second.second.args != first.second.args) return null
            if (source.skipSpace(second.second.end) != outerEnd - 1) return null
            val d = date.args[0]
            if (Regex("(^|[^\\w$])" + Regex.escape(tmp) + "([^\\w$]|$)").containsMatchIn(d)) return null
            val args = first.second.args
            val replacement = when {
                first.first == "add" && args.size == 2 && literal(args[1]) == unit && isParenthesized(args[0]) ->
                    change(d, "add", args[0], "sticky_$unit") // the old block generated add() for subtract too
                SETTERS[first.first] == unit && args.size == 1 -> change(d, "set", parenthesize(args[0]), "sticky_$unit")
                else -> return null
            }
            return Match(end, replacement)
        }

        /** add(...) or a setter call at [i] */
        private fun setOrAdd(source: Source, i: Int): Pair<String, Call>? {
            if (!source.identifierStart(i)) return null
            val method = source.identifier(i)
            if (method != "add" && method !in SETTERS) return null
            return method to (call(source, i + method.length, null) ?: return null)
        }

        /** [expected] at [i] (spaces around tokens as written), the index after it */
        private fun expect(source: Source, i: Int, expected: String): Int? {
            val start = source.skipSpace(i)
            if (!source.text.startsWith(expected, start)) return null
            if ((start until start + expected.length).any { !source.isCode(it) }) return null
            return start + expected.length
        }

        private fun change(date: String, operation: String, count: String, unit: String) =
            "new Date($variable.changeDate($date, '$operation', $count, '$unit'))"

        /** moment(d) at [i] with exactly one argument */
        private fun moment(source: Source, i: Int): Call? {
            if (name(source, i) != "moment") return null
            return call(source, i + "moment".length, 1)
        }

        /** the call whose "(" follows [nameEnd], its arguments rewritten; null when the arity differs */
        private fun call(source: Source, nameEnd: Int, arity: Int?): Call? {
            val open = source.skipSpace(nameEnd)
            if (!source.isCode(open) || source.text[open] != '(') return null
            val end = source.close(open)
            if (end < 0) return null
            val args = source.arguments(open, end)
            if (arity != null && args.size != arity) return null
            if (args.any { it.isEmpty() }) return null
            return Call(end, args.map { rewrite(it, top = false) })
        }

        /** ".toDate()" after [i]: the index after it */
        private fun toDate(source: Source, i: Int): Int? {
            val (name, nameEnd) = source.member(i) ?: return null
            if (name != "toDate") return null
            return expect(source, nameEnd, "()")
        }

        private fun literal(arg: String): String? = LITERAL.find(arg)?.groupValues?.get(1)

        private fun isParenthesized(arg: String): Boolean = arg.startsWith("(") && Source(arg).close(0) == arg.length

        private fun parenthesize(arg: String) = if (isParenthesized(arg)) arg else "($arg)"
    }
}
