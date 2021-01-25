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

import kotlin.Throws
import com.fasterxml.jackson.core.JsonProcessingException
import io.lumeer.api.model.Document
import io.lumeer.engine.api.data.DataDocument
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Value
import org.junit.Test

class JvmObjectProxyTest {
    private lateinit var context: Context
    private lateinit var fce: Value
    private var jsCode: String? = null
    private val engine = Engine
            .newBuilder()
            .allowExperimentalOptions(true)
            .option("js.experimental-foreign-object-prototype", "true")
            .option("js.foreign-object-prototype", "true")
            .build()

    private fun initContext() {
        jsCode = "function fce(documents) { return documents[0].creationDate.minute }"
        context = Context
                .newBuilder("js")
                .engine(engine)
                .allowAllAccess(true)
                .build()
        context.initialize("js")
        val result = context.eval("js", jsCode)
        fce = context.getBindings("js").getMember("fce")
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun test() {
        val d = Document(DataDocument("useři", listOf("user1@lumeerio.com", "user2@lumeerio.com")).append("další", arrayOf("user1", "user2")))
                .apply {
                    id = "abc123"
                    isFavorite = true
                    collectionId = "myCollId"
                    creationDate = ZonedDateTime.now()
                }
        initContext()
        val res = fce.execute(listOf(JvmObjectProxy(d, Document::class.java)))
        Assertions.assertThat(res.asInt()).isEqualTo(d.creationDate.minute)
        context.close()
    }
}
