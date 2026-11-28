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

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value

/** `new Date(string).getTime()` of the script engine (no libraries), for parsing dates outside of scripts. */
object ScriptDateParser {

    private val lock = Any()
    private var parse: Value? = null

    @JvmStatic
    fun parse(input: String): Double = synchronized(lock) {
        // the same host access as the automation contexts: GraalVM requires one configuration for all contexts of the shared engine
        val function = parse ?: Context.newBuilder("js").engine(JsEngineFactory.getEngine()).allowAllAccess(true).build()
            .eval("js", "(function (input) { return new Date(input).getTime(); })").also { parse = it }
        function.execute(input).asDouble()
    }
}
