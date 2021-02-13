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

import io.lumeer.core.util.DataUtils
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.Proxy
import org.graalvm.polyglot.proxy.ProxyObject
import java.lang.Exception
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class JvmObjectProxy<T>(val proxyObject: T, clazz: Class<T>, val locale: Locale = Locale.getDefault()) : ProxyObject {
    private val fields: List<Field> = listOf(*clazz.fields)
    private val objects: MutableMap<String, Any> = mutableMapOf()
    private val methodObjects: MutableMap<String, Any> = mutableMapOf()
    private val methods: List<Method> = listOf(*clazz.methods)
    private val members: MutableList<String>
    private val membersCheck: MutableSet<String> = mutableSetOf()

    init {
        members = ArrayList()
        members.addAll(fields.filter { !it.name.isUpperCase() }.map { it.name })
        members.addAll(methods.filter { methodAllowed(it) && it.name.startsWith("get") }
                .map { it.name }
                .map { it.substring(3, 4).toLowerCase() + it.substring(4) }
                .filter { it.isNotEmpty() }
        )
        membersCheck.addAll(members)
    }

    override fun getMember(key: String): Any? {
        try {
            if (objects[key] != null) {
                return objects[key]
            } else {
                if (methodObjects[key] != null) {
                    return methodObjects[key]
                } else {
                    val field = fields.firstOrNull { f: Field -> f.name == key }
                    if (field != null) {
                        val enc = encodeObject(field[proxyObject], locale)
                        objects[key] = enc
                        return enc
                    }

                    val keyMethod = key.substring(0, 1).toUpperCase() + key.substring(1)
                    val method = methods.firstOrNull { methodAllowed(it) && it.name == "get$keyMethod" }
                    if (method != null) {
                        val obj = encodeObject(method.invoke(proxyObject), locale)
                        methodObjects[key] = obj
                        return obj
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }

        return null
    }

    private fun methodAllowed(m: Method): Boolean = m.parameterCount == 0 && m.returnType != Void.TYPE

    override fun getMemberKeys(): Any = members

    override fun hasMember(key: String): Boolean = membersCheck.contains(key)

    override fun putMember(key: String, value: Value) = throw UnsupportedOperationException()

    @Suppress("UNCHECKED_CAST")
    companion object {
        private val utcZone = ZoneId.ofOffset("UTC", ZoneOffset.UTC)

        fun decodeValue(value: Value): Any? {
            return if (value.isNumber) {
                if (value.fitsInLong()) value.asLong() else value.asDouble()
            } else if (value.isBoolean) {
                value.asBoolean()
            } else if (value.isHostObject && value.asHostObject<Any>() is Date) {
                value.asHostObject<Any>()
            } else if (value.isNull) {
                null
            } else if (value.hasArrayElements()) {
                val list = mutableListOf<Any?>()
                for (i in 0 until value.arraySize) {
                    list.add(decodeValue(value.getArrayElement(i)))
                }
                list
            } else {
                value.asString()
            }
        }

        fun encodeObject(o: Any, locale: Locale = Locale.getDefault()): Any {
            when {
                o is String -> {
                    return o
                }
                o is List<*> -> {
                    return fromList(o, locale)
                }
                o is Array<*> -> {
                    return fromArray(o, locale)
                }
                o is Set<*> -> {
                    return fromSet(o, locale)
                }
                o.javaClass.isEnum -> {
                    return o.toString()
                }
                o is BigDecimal -> {
                    return o.toString()
                }
                o is Date -> {
                    val dt = ZonedDateTime.from(o.toInstant().atZone(utcZone))
                    return try {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", locale).format(dt)
                    } catch (dte: DateTimeException) {
                        o
                    }
                }
                else -> {
                    val pointString = DataUtils.convertPointToString(o)
                    if (pointString != null) {
                        return pointString
                    }

                    // needs to be checked after point, because point is instance of map
                    if (o is Map<*, *> && o.size > 0 && o.keys.iterator().next() is String) {
                        return fromMap(o as Map<String, Any>, locale)
                    }

                    if (o !is Number && !o.javaClass.isPrimitive && !o.javaClass.isEnum && !o.javaClass.isSynthetic) {
                        return JvmObjectProxy(o, o.javaClass, locale)
                    }
                    return o
                }
            }

        }

        fun fromMap(values: Map<String, Any>, locale: Locale = Locale.getDefault()): Proxy {
            return JvmMapProxy(values.toMutableMap(), locale)
        }

        fun fromList(list: List<*>, locale: Locale = Locale.getDefault()): Proxy {
            return JvmListProxy(list.toMutableList(), locale)
        }

        fun fromSet(set: Set<*>, locale: Locale = Locale.getDefault()): Proxy {
            return JvmSetProxy((set as Set<Any?>).toMutableSet(), locale)
        }

        fun fromArray(array: Array<*>, locale: Locale = Locale.getDefault()): Proxy {
            return JvmArrayProxy(array as Array<Any?>, locale)
        }
    }

    fun String.isUpperCase(): Boolean = !toCharArray().asList().any { it.isLowerCase() }
}

