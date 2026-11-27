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
package io.lumeer.datafilters.parity

/** Minimal JSON reader for the test vectors: objects -> LinkedHashMap, arrays -> List, integers -> Long, other numbers -> Double. */
internal class Json(private val text: String) {
    private var pos = 0

    fun parse(): Any? {
        val value = readValue()
        skipWhitespace()
        return value
    }

    private fun skipWhitespace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }

    private fun readValue(): Any? {
        skipWhitespace()
        return when (val c = text[pos]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> readString()
            't' -> { pos += 4; true }
            'f' -> { pos += 5; false }
            'n' -> { pos += 4; null }
            else -> if (c == '-' || c.isDigit()) readNumber() else error("Unexpected '$c' at $pos")
        }
    }

    private fun readObject(): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        pos++
        skipWhitespace()
        if (text[pos] == '}') { pos++; return map }
        while (true) {
            skipWhitespace()
            val key = readString()
            skipWhitespace()
            pos++ // :
            map[key] = readValue()
            skipWhitespace()
            if (text[pos++] == '}') return map
        }
    }

    private fun readArray(): List<Any?> {
        val list = ArrayList<Any?>()
        pos++
        skipWhitespace()
        if (text[pos] == ']') { pos++; return list }
        while (true) {
            list.add(readValue())
            skipWhitespace()
            if (text[pos++] == ']') return list
        }
    }

    private fun readString(): String {
        val sb = StringBuilder()
        pos++
        while (true) {
            val c = text[pos++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    when (val e = text[pos++]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        'f' -> sb.append(12.toChar())
                        'u' -> { sb.append(text.substring(pos, pos + 4).toInt(16).toChar()); pos += 4 }
                        else -> sb.append(e)
                    }
                }
                else -> sb.append(c)
            }
        }
    }

    private fun readNumber(): Any {
        val start = pos
        while (pos < text.length && (text[pos].isDigit() || text[pos] in "+-.eE")) pos++
        val literal = text.substring(start, pos)
        if (literal.none { it in ".eE" }) {
            literal.toLongOrNull()?.let { return it }
        }
        return literal.toDouble()
    }
}
