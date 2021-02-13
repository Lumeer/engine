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
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.*

class JvmMapProxy(val values: MutableMap<String, Any?>, val locale: Locale = Locale.getDefault()) : ProxyObject {

    private val objects: MutableMap<String, Any?> = mutableMapOf()

    fun proxyMap() = values

    override fun putMember(key: String?, value: Value) = throw UnsupportedOperationException()

    override fun hasMember(key: String?): Boolean = values.containsKey(key)

    override fun getMemberKeys(): Any = object : ProxyArray {
        private val keys: Array<Any> = values.keys.toTypedArray()

        override fun set(index: Long, value: Value) {
            throw UnsupportedOperationException()
        }

        override fun getSize(): Long {
            return keys.size.toLong()
        }

        override fun get(index: Long): Any {
            return if (index in 0L..2147483647L) {
                keys[index.toInt()]
            } else {
                throw ArrayIndexOutOfBoundsException()
            }
        }
    }

    override fun getMember(key: String?): Any? {
        if (key != null) {
            val o = objects[key]
            if (o != null) {
                return o
            }

            val v = values[key]
            if (v != null) {
                val enc = JvmObjectProxy.encodeObject(v!!, locale)
                objects[key] = enc
                return enc
            }
        }

        return null
    }

    override fun removeMember(key: String?): Boolean = throw UnsupportedOperationException()
}
