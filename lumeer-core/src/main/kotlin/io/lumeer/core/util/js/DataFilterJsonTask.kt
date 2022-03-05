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

import com.google.gson.*
import io.lumeer.api.model.*
import io.lumeer.api.model.Collection
import io.lumeer.api.model.common.Resource
import io.lumeer.core.js.JsEngineFactory
import io.lumeer.core.util.Tuple
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.logging.Level
import java.util.logging.Logger
import java.lang.Double
import java.lang.Float
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime


data class DataFilterJsonTask(val documents: List<Document>,
                              val collections: List<Collection>,
                              val linkTypes: List<LinkType>,
                              val linkInstances: List<LinkInstance>,
                              val query: Query,
                              val collectionsPermissions: Map<String, AllowedPermissions>,
                              val linkTypesPermissions: Map<String, AllowedPermissions>,
                              val constraintData: ConstraintData,
                              val includeChildren: Boolean,
                              val language: Language = Language.EN) : Callable<Tuple<List<Document>, List<LinkInstance>>> {

    override fun call(): Tuple<List<Document>, List<LinkInstance>> {
        val emptyTuple = Tuple<List<Document>, List<LinkInstance>>(emptyList(), emptyList())
        val context = getContext()

        return try {
            val filterJsValue = getFunction(context)

            val json = convertToJson(DataFilterJson(documents, collections, linkTypes, linkInstances, query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildren, language.toLanguageTag()))

            val result = filterJsValue.execute(json)

            if (result != null) {
                val documentsMap = documents.groupBy { it.id }
                val resultDocumentsList = mutableListOf<Document>()
                val resultDocuments = result.getMember("documentsIds")
                for (i in 0 until resultDocuments.arraySize) resultDocumentsList.addAll(documentsMap[resultDocuments.getArrayElement(i).asString()].orEmpty())

                val linkInstancesMap = linkInstances.groupBy { it.id }
                val resultLinksList = mutableListOf<LinkInstance>()
                val resultLinks = result.getMember("linkInstancesIds")
                for (i in 0 until resultLinks.arraySize) resultLinksList.addAll(linkInstancesMap[resultLinks.getArrayElement(i).asString()].orEmpty())

                Tuple(resultDocumentsList, resultLinksList)
            } else {
                logger.log(Level.SEVERE, "Error filtering data - null result.")
                emptyTuple
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error filtering data: ", e)
            emptyTuple
        } finally {
            context.close()
        }
    }

    companion object {
        private val logger: Logger = Logger.getLogger(DataFilterJsonTask::class.simpleName)
        private const val FILTER_JS = "filterDocumentsAndLinksIdsFromJson"
        private var filterJsCode: String? = null
        private val engine = JsEngineFactory.getEngine()

        fun getContext(): Context {
            val context = Context
                    .newBuilder("js")
                    .engine(engine)
                    .allowAllAccess(true)
                    .build()
            context.initialize("js")

            return context
        }

        fun getFunction(context: Context): Value {
            if (filterJsCode != null) {
                context.eval("js", filterJsCode)
                return context.getBindings("js").getMember(FILTER_JS)
            } else {
                throw IOException("Filters JS code not present.")
            }
        }

        fun convertToJson(dataFilterJson: DataFilterJson): String {
            val strategy: ExclusionStrategy = object : ExclusionStrategy {
                override fun shouldSkipField(field: FieldAttributes): Boolean {
                    if (field.declaringClass == Document::class.java && !listOf("id", "data", "metaData", "collectionId").contains(field.name)) {
                        return true
                    }
                    if (field.declaringClass == Collection::class.java && !listOf("id", "attributes").contains(field.name)) {
                        return true
                    }
                    if (field.declaringClass == Resource::class.java && !listOf("id").contains(field.name)) {
                        return true
                    }
                    if (field.declaringClass == LinkType::class.java && !listOf("id", "attributes", "collectionIds").contains(field.name)) {
                        return true
                    }
                    if (field.declaringClass == LinkInstance::class.java && !listOf("id", "data", "linkTypeId", "documentIds").contains(field.name)) {
                        return true
                    }
                    if (field.declaringClass == User::class.java && !listOf("id", "name", "email").contains(field.name)) {
                        return true
                    }
                    if (field.declaringClass == Attribute::class.java && !listOf("id", "name", "constraint").contains(field.name)) {
                        return true
                    }
                    return false
                }

                override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                    return false
                }
            }

            val conditionTypeSerializer: JsonSerializer<ConditionType> = JsonSerializer<ConditionType> { condition, _, _ ->
                JsonPrimitive(condition.value)
            }

            // we need to use java.lang.Double because we use Java objects as input parameters
            val doubleSerializer: JsonSerializer<Double> = JsonSerializer<Double> { number: Double, _, _ ->
                when {
                    number.isNaN -> JsonPrimitive("NaN")
                    number.isInfinite -> JsonPrimitive("Infinity")
                    else -> JsonPrimitive(number)
                }
            }

            // we need to use java.lang.Float because we use Java objects as input parameters
            val floatSerializer: JsonSerializer<Float> = JsonSerializer<Float> { number, _, _ ->
                when {
                    number.isNaN -> JsonPrimitive("NaN")
                    number.isInfinite -> JsonPrimitive("Infinity")
                    else -> JsonPrimitive(number)
                }
            }

            val localDateTimeSerializer: JsonSerializer<LocalDateTime> = JsonSerializer<LocalDateTime> { dt, srcType, _ ->
                JsonPrimitive(dt.toInstant(ZoneOffset.UTC).epochSecond)
            }

            val zonedDateTimeSerializer2: JsonSerializer<ZonedDateTime> = JsonSerializer<ZonedDateTime> { dt, srcType, _ ->
                JsonPrimitive(dt.toInstant().epochSecond)
            }

            return GsonBuilder()
                    .addSerializationExclusionStrategy(strategy)
                    .registerTypeAdapter(ConditionType::class.java, conditionTypeSerializer)
                    .registerTypeAdapter(Double::class.java, doubleSerializer)
                    .registerTypeAdapter(Float::class.java, floatSerializer)
                    .registerTypeAdapter(LocalDateTime::class.java, localDateTimeSerializer)
                    .registerTypeAdapter(ZonedDateTime::class.java, zonedDateTimeSerializer2)
                    .create()
                    .toJson(dataFilterJson)
        }

        init {
            try {
                DataFilterJsonTask::class.java.getResourceAsStream("/lumeer-data-filters.min.js").use { stream ->
                    filterJsCode = String(stream.readAllBytes(), StandardCharsets.UTF_8).plus("; function ${FILTER_JS}(json) { return Filter.filterDocumentsAndLinksIdsFromJson(json); }")
                }
            } catch (ioe: IOException) {
                filterJsCode = null
            }
        }
    }
}

data class DataFilterJson(val documents: List<Document>,
                          val collections: List<Collection>,
                          val linkTypes: List<LinkType>,
                          val linkInstances: List<LinkInstance>,
                          val query: Query,
                          val collectionsPermissions: Map<String, AllowedPermissions>,
                          val linkTypesPermissions: Map<String, AllowedPermissions>,
                          val constraintData: ConstraintData,
                          val includeChildren: Boolean,
                          val language: String)
