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
import org.graalvm.polyglot.proxy.Proxy
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject
import java.lang.Exception
import java.lang.reflect.Field
import java.lang.reflect.Method

class JvmObjectProxy<T>(val proxyObject: T, clazz: Class<T>) : ProxyObject {
    private val fields: List<Field> = listOf(*clazz.fields)
    private val methods: List<Method> = listOf(*clazz.methods)
    private val members: MutableList<String>

    init {
        members = ArrayList()
        members.addAll(fields.filter { !it.name.isUpperCase() }.map { it.name })
        members.addAll(methods.filter { methodAllowed(it) && it.name.startsWith("get") }
                .map { it.name }
                .map { it.substring(3, 4).toLowerCase() + it.substring(4) }
                .filter { it.isNotEmpty() }
        )
    }

    override fun getMember(key: String) = try {
        val field = fields.firstOrNull { f: Field -> f.name == key }
        if (field != null) {
            encodeObject(field[proxyObject])
        }
        val keyMethod = key.substring(0, 1).toUpperCase() + key.substring(1)
        val method = methods.firstOrNull { methodAllowed(it) && it.name == "get$keyMethod" }
        if (method != null) {
            encodeObject(method.invoke(proxyObject))
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    private fun methodAllowed(m: Method): Boolean = m.parameterCount == 0 && m.returnType != Void.TYPE

    override fun getMemberKeys(): Any = members

    override fun hasMember(key: String): Boolean = members.contains(key)

    override fun putMember(key: String, value: Value) = throw UnsupportedOperationException()

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun encodeObject(o: Any): Any {
            if (o is String) {
                return o
            } else if (o is List<*>) {
                return fromList(o)
            } else if (o is Array<*>) {
                return fromArray(o)
            } else if (o is Set<*>) {
                return fromSet(o)
            } else if (o is Map<*, *>) {
                if (o.size > 0 && o.keys.iterator().next() is String) {
                    return fromMap(o as Map<String, Any>)
                }
            } else if (o !is Number && !o.javaClass.isPrimitive && !o.javaClass.isEnum && !o.javaClass.isSynthetic) {
                return JvmObjectProxy(o, o.javaClass)
            }
            return o
        }

        fun fromMap(values: Map<String, Any>): Proxy {
            return ProxyObject.fromMap(values.mapValues { encodeObject(it.value) })
        }

        fun fromList(list: List<*>): Proxy {
            return ProxyArray.fromList(list.map { encodeObject(it!!) })
        }

        fun fromSet(set: Set<*>): Proxy {
            return ProxyArray.fromList(set.map { encodeObject(it!!) })
        }

        fun fromArray(array: Array<*>): Proxy {
            return ProxyArray.fromArray(array.map { encodeObject(it!!) })
        }
    }


    fun String.isUpperCase(): Boolean = !toCharArray().asList().any { it.isLowerCase() }
}

