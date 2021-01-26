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

import io.lumeer.api.model.*
import io.lumeer.api.model.Collection
import io.lumeer.core.util.Tuple
import java.lang.AutoCloseable
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Value
import java.io.IOException
import java.lang.Exception
import java.nio.charset.StandardCharsets

class DataFiltersJsParser : AutoCloseable {

    companion object {
        private const val FILTER_JS = "filterDocumentsAndLinksByQuery"
        private lateinit var context: Context
        private lateinit var filterJsValue: Value
        private var filterJsCode: String? = null
        private val engine = Engine
                .newBuilder()
                .allowExperimentalOptions(true)
                .option("js.experimental-foreign-object-prototype", "true")
                .option("js.foreign-object-prototype", "true")
                .build()

        fun filterDocumentsAndLinksByQuery(documents: List<Document>,
                                           collections: List<Collection>, linkTypes: List<LinkType>, linkInstances: List<LinkInstance>,
                                           query: Query, collectionsPermissions: Map<String, AllowedPermissions>, linkTypesPermissions: Map<String, AllowedPermissions>,
                                           constraintData: ConstraintData, includeChildren: Boolean): Tuple<List<Document>, List<LinkInstance>> {
            if (!this::context.isInitialized) {
                initContext()
            }
            val emptyTuple = Tuple<List<Document>, List<LinkInstance>>(emptyList(), emptyList())
            return try {
                val result = filterJsValue.execute(JvmObjectProxy(documents, List::class.java),
                        JvmObjectProxy(collections, List::class.java),
                        JvmObjectProxy(linkTypes, List::class.java),
                        JvmObjectProxy(linkInstances, List::class.java),
                        JvmObjectProxy(query, Query::class.java),
                        JvmObjectProxy(collectionsPermissions, Map::class.java),
                        JvmObjectProxy(linkTypesPermissions, Map::class.java),
                        JvmObjectProxy(constraintData, ConstraintData::class.java),
                        includeChildren)

                val resultDocuments = result.getMember("documents").`as`(List::class.java)
                        .map { (it as JvmObjectProxy<*>).proxyObject as Document }
                val resultLinkInstances = result.getMember("linkInstances").`as`(List::class.java)
                        .map { (it as JvmObjectProxy<*>).proxyObject as LinkInstance }

                Tuple(resultDocuments, resultLinkInstances)
            } catch (e: Exception) {
                emptyTuple
            }
        }

        @Synchronized
        private fun initContext() {
            if (filterJsCode != null) {
                context = Context
                        .newBuilder("js")
                        .engine(engine)
                        .allowAllAccess(true)
                        .build()
                context.initialize("js")
                val result = context.eval("js", filterJsCode)
                filterJsValue = context.getBindings("js").getMember(FILTER_JS)
            }
        }

        init {
            try {
                DataFiltersJsParser::class.java.getResourceAsStream("/lumeer-data-filters.min.js").use { stream ->
                    filterJsCode = String(stream.readAllBytes(), StandardCharsets.UTF_8).plus("; function $FILTER_JS(documents,collections,linkTypes,linkInstances,query,constraintData,children) { return Filter.filterDocumentsAndLinksByQuery(documents,collections,linkTypes,linkInstances,query,constraintData,children); }")
                }
            } catch (ioe: IOException) {
                filterJsCode = null
            }
        }
    }

    override fun close() {
        context.close()
    }
}
