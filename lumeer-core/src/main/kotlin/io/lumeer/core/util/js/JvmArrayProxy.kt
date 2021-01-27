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
package io.lumeer.core.util.js

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray

class JvmArrayProxy(var values: Array<Any?>) : ProxyArray {

    fun proxyArray() = values

    override fun get(index: Long): Any? {
        checkIndex(index)
        val idx = index.toInt()
        return if (values[idx] != null) JvmObjectProxy.encodeObject(values[idx]!!)
        else null
    }

    override fun set(index: Long, value: Value) {
        checkIndex(index)
        values[index.toInt()] = if (value.isHostObject) value.asHostObject() else value
    }

    private fun checkIndex(index: Long) {
        if (index > 2147483647L || index < 0L) {
            throw ArrayIndexOutOfBoundsException("invalid index.")
        }
    }

    override fun getSize(): Long = values.size.toLong()
}

