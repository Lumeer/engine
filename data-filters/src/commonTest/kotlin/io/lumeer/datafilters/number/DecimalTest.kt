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
package io.lumeer.datafilters.number

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecimalTest {

    private fun d(value: String) = Decimal.parse(value)!!

    @Test
    fun parsesLikeBigJs() {
        assertEquals("1234.5", d("1234.50").toPlainString())
        assertEquals("1000", d("1e3").toPlainString())
        assertEquals("0.0001", d("1E-4").toPlainString())
        assertEquals("0.5", d(".5").toPlainString())
        assertEquals("5", d("5.").toPlainString())
        assertEquals("1000000000000000000000", d("1e+21").toPlainString())
        assertNull(Decimal.parse("+5"))
        assertNull(Decimal.parse("abc"))
        assertNull(Decimal.parse("Infinity"))
        assertNull(Decimal.parse(""))
    }

    @Test
    fun compares() {
        assertTrue(d("10") > d("9.99"))
        assertTrue(d("-10") < d("-9.99"))
        assertTrue(d("0.1") > d("0.0999999999999999999999"))
        assertEquals(0, d("-0").compareTo(d("0")))
        assertEquals(0, d("1.50").compareTo(d("1.5")))
        assertTrue(d("-1") < d("0"))
    }

    @Test
    fun roundsHalfUp() {
        assertEquals("1.01", d("1.005").roundHalfUp(2).toPlainString())
        assertEquals("-1.01", d("-1.005").roundHalfUp(2).toPlainString())
        assertEquals("10", d("9.95").roundHalfUp(0).toPlainString())
        assertEquals("1000", d("999.5").roundHalfUp(0).toPlainString())
        assertEquals("0", d("0.004").roundHalfUp(2).toPlainString())
        assertEquals("1.2", d("1.2").roundHalfUp(3).toPlainString())
    }

    @Test
    fun roundsHalfCeilingLikeNumbro() {
        assertEquals("-2", d("-2.5").roundHalfCeiling(0).toFixedString(0))
        assertEquals("3", d("2.5").roundHalfCeiling(0).toFixedString(0))
        assertEquals("0.00", d("-0.000001").roundHalfCeiling(2).toFixedString(2))
        assertEquals("1234.57", d("1234.5678").roundHalfCeiling(2).toFixedString(2))
        assertEquals("100.00", d("100").roundHalfCeiling(2).toFixedString(2))
    }
}
