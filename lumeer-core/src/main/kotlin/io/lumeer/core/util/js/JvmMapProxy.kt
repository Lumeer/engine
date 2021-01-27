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

class JvmMapProxy(val values: MutableMap<String, Any>) : ProxyObject {

    fun proxyMap() = values

    override fun putMember(key: String?, value: Value) {
        if (key != null) values[key] = (if (value.isHostObject) value.asHostObject<Any>() else value)
    }

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

    override fun getMember(key: String?): Any? = if (values[key] != null) JvmObjectProxy.encodeObject(values[key]!!)
    else null

    override fun removeMember(key: String?): Boolean = if (values.containsKey(key)) {
        values.remove(key)
        true
    } else {
        false
    }
}
