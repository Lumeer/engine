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
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Value
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.logging.Level
import java.util.logging.Logger

data class DataFilterTask(val documents: List<Document>,
                          val collections: List<Collection>,
                          val linkTypes: List<LinkType>,
                          val linkInstances: List<LinkInstance>,
                          val query: Query,
                          val collectionsPermissions: Map<String, AllowedPermissions>,
                          val linkTypesPermissions: Map<String, AllowedPermissions>,
                          val constraintData: ConstraintData,
                          val includeChildren: Boolean,
                          val language: Language = Language.EN) : Callable<Value?> {

    override fun call(): Value? {
        val locale = language.toLocale()
        return try {
            val filterJsValue: Value = if (filterJsFunction.get() == null) { filterJsFunction.set(initContext()); filterJsFunction.get() } else filterJsFunction.get()

            filterJsValue.execute(JvmObjectProxy.fromList(documents, locale),
                JvmObjectProxy.fromList(collections, locale),
                JvmObjectProxy.fromList(linkTypes, locale),
                JvmObjectProxy.fromList(linkInstances, locale),
                JvmObjectProxy(query, Query::class.java, locale),
                JvmObjectProxy.fromMap(collectionsPermissions, locale),
                JvmObjectProxy.fromMap(linkTypesPermissions, locale),
                JvmObjectProxy(constraintData, ConstraintData::class.java),
                includeChildren,
                language.toLanguageTag())
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error filtering data: ", e)
            null
        }
    }

    companion object {
        private val logger: Logger = Logger.getLogger(DataFilterTask::class.simpleName)
        private const val FILTER_JS = "filterDocumentsAndLinksByQuery"
        private val filterJsFunction: ThreadLocal<Value> = ThreadLocal()
        private val contexts: MutableList<Context> = mutableListOf()
        private var filterJsCode: String? = null
        private var counter = 1
        private val engine = Engine
            .newBuilder()
            .allowExperimentalOptions(true)
            .option("js.experimental-foreign-object-prototype", "true")
            .option("js.foreign-object-prototype", "true")
            .build()

        @Synchronized
        private fun initContext(): Value {
            if (filterJsCode != null) {
                logger.log(Level.INFO, "Creating filter context no. ${counter++}")
                val context = Context
                    .newBuilder("js")
                    .engine(engine)
                    .allowAllAccess(true)
                    .build()
                context.initialize("js")
                contexts.add(context)
                val result = context.eval("js", filterJsCode)
                return context.getBindings("js").getMember(FILTER_JS)
            }

            throw IOException("Filters JS code not present.")
        }

        init {
            try {
                DataFilterTask::class.java.getResourceAsStream("/lumeer-data-filters.min.js").use { stream ->
                    filterJsCode = String(stream.readAllBytes(), StandardCharsets.UTF_8).plus("; function ${FILTER_JS}(documents, collections, linkTypes, linkInstances, query, collectionPermissions, linkTypePermissions, constraintData, includeChildren, language) { return Filter.filterDocumentsAndLinksByQuery(documents, Filter.createConstraintsInCollections(collections, language), Filter.createConstraintsInLinkTypes(linkTypes, language), linkInstances, query, collectionPermissions, linkTypePermissions, constraintData, includeChildren); }")
                }
            } catch (ioe: IOException) {
                filterJsCode = null
            }
        }

        fun close() {
            contexts.forEach { it.close() }
        }
    }
}