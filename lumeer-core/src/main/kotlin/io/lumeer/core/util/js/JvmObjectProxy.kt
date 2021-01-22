package io.lumeer.core.util.js

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject
import java.lang.Exception
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

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

class JvmObjectProxy<T>(val d: T, clazz: Class<T>) : ProxyObject {
    val fields: List<Field>
    val methods: List<Method>
    val members: MutableList<Any>

    @Suppress("UNCHECKED_CAST")
    private fun encodeObject(o: Any): Any {
        if (o is Map<*, *>) {
            if (o.size > 0 && o.keys.iterator().next() is String) {
                return ProxyObject.fromMap(o as Map<String, Any>)
            }
        } else if (o is List<*>) {
            return ProxyArray.fromList(o)
        } else if (o.javaClass.isArray) {
            return ProxyArray.fromArray(o)
        } else if (o !is Number && !o.javaClass.isPrimitive && !o.javaClass.isEnum && !o.javaClass.isSynthetic) {
            return JvmObjectProxy(o, o.javaClass)
        }
        return o
    }

    override fun getMember(key: String): Any? {
        try {
            val field = fields.stream().filter { f: Field -> f.name == key }.findFirst()
            if (field.isPresent) {
                return encodeObject(field.get()[d])
            }
            val method = methods.stream().filter(methodFilter { m: Method -> m.name == "get" + key.substring(0, 1).toUpperCase() + key.substring(1) }).findFirst()
            if (method.isPresent) {
                return encodeObject(method.get().invoke(d))
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    private fun methodFilter(nameFilter: Predicate<Method>): Predicate<Method> {
        return Predicate { m: Method -> nameFilter.test(m) && m.parameterCount == 0 && m.returnType != Void.TYPE }
    }

    override fun getMemberKeys(): Any {
        return ProxyArray.fromList(members)
    }

    override fun hasMember(key: String): Boolean {
        return members.contains(key)
    }

    override fun putMember(key: String, value: Value) {
        throw UnsupportedOperationException()
    }

    init {
        fields = Arrays.asList(*clazz.fields)
        methods = Arrays.asList(*clazz.methods)
        members = ArrayList()
        members.addAll(fields.stream().map { obj: Field -> obj.name }.collect(Collectors.toList()))
        members.addAll(methods.stream().filter(methodFilter { m: Method -> m.name.startsWith("get") }).map { obj: Method -> obj.name }.map { s: String -> s.substring(3, 4).toLowerCase() + s.substring(4) }.collect(Collectors.toList()))
    }
}
