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
@file:OptIn(ExperimentalJsExport::class)

package io.lumeer.datafilters.js

import io.lumeer.datafilters.constraint.createConstraint
import io.lumeer.datafilters.filter.filterDocumentsAndLinksByQuery as filterByQuery
import io.lumeer.datafilters.model.Attribute
import io.lumeer.datafilters.model.Collection
import io.lumeer.datafilters.model.CollectionAttributeFilter
import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.model.AttributeFilterEquation
import io.lumeer.datafilters.model.AttributeFilter
import io.lumeer.datafilters.model.AttributeLock
import io.lumeer.datafilters.model.AttributeLockExceptionGroup
import io.lumeer.datafilters.model.ConstraintData
import io.lumeer.datafilters.model.CurrencyData
import io.lumeer.datafilters.model.EquationOperator
import io.lumeer.datafilters.model.SelectOption
import io.lumeer.datafilters.model.SelectionList
import io.lumeer.datafilters.model.Team
import io.lumeer.datafilters.model.User
import io.lumeer.datafilters.model.DocumentModel
import io.lumeer.datafilters.model.LinkAttributeFilter
import io.lumeer.datafilters.model.LinkInstanceModel
import io.lumeer.datafilters.model.LinkType
import io.lumeer.datafilters.model.Query
import io.lumeer.datafilters.model.QueryStem

/*
 * JS entry point. Accepts the same plain objects as @lumeer/data-filters (store models,
 * collections with raw or already created constraints) and returns the same object instances.
 */

private class JsDocument(val source: dynamic) : DocumentModel {
    override val id: String = source.id as String
    override val collectionId: String = source.collectionId as String
    override val parentId: String? = source.metaData?.parentId as? String
    private val data: dynamic = source.data

    override fun value(attributeId: String): Any? = if (data == null) null else data[attributeId]
}

private class JsLinkInstance(val source: dynamic) : LinkInstanceModel {
    override val id: String = source.id as String
    override val linkTypeId: String = source.linkTypeId as String
    override val documentIds: List<String> = toList(source.documentIds).map { it as String }
    private val data: dynamic = source.data

    override fun value(attributeId: String): Any? = if (data == null) null else data[attributeId]
}

private fun toList(array: dynamic): List<Any?> =
    if (array == null) emptyList() else (array.unsafeCast<Array<Any?>>()).toList()

private fun toMap(obj: dynamic): Map<String, Any?> {
    if (obj == null) return emptyMap()
    val keys = js("Object.keys(obj)").unsafeCast<Array<String>>()
    return keys.associateWith { obj[it] as Any? }
}

private fun toMapDeep(obj: dynamic): Map<String, Any?> {
    if (obj == null) return emptyMap()
    val keys = js("Object.keys(obj)").unsafeCast<Array<String>>()
    return keys.associateWith { key ->
        val value = obj[key]
        when {
            value == null -> null
            js("Array.isArray(value)") as Boolean -> toList(value).map { item -> if (jsTypeOf(item) == "object" && item != null) toMapDeep(item) else item }
            jsTypeOf(value) == "object" -> toMapDeep(value)
            else -> value as Any?
        }
    }
}

private fun toUser(user: dynamic): User? = if (user == null) null else User(user.id as? String, user.name as? String, user.email as? String)

private fun toConstraintData(data: dynamic): ConstraintData? {
    if (data == null) return null
    val unitsMap = data.durationUnitsMap
    return ConstraintData(
        users = toList(data.users).mapNotNull { toUser(it) },
        teams = toList(data.teams).map { val t = it.asDynamic(); Team(t.id as? String, t.name as? String, toList(t.users).map { u -> u as String }) },
        currentUser = toUser(data.currentUser),
        durationUnitsMap = if (unitsMap == null) null else toMap(unitsMap).mapValues { it.value as String },
        currencyData = if (data.currencyData == null) null else CurrencyData(toList(data.currencyData.abbreviations).map { it as String }, toList(data.currencyData.ordinals).map { it as String }),
        selectionLists = toList(data.selectionLists).map {
            val l = it.asDynamic()
            SelectionList(l.id as? String, l.displayValues == true, toList(l.options).map { o -> SelectOption(o.asDynamic().value as Any?, o.asDynamic().displayValue as? String) })
        },
        timezone = data.timezone as? String,
        locale = data.locale as? String,
    )
}

private fun toEquation(equation: dynamic): AttributeFilterEquation? {
    if (equation == null) return null
    val filter = equation.filter
    return AttributeFilterEquation(
        equations = toList(equation.equations).mapNotNull { toEquation(it) },
        filter = if (filter == null) null else AttributeFilter(filter.attributeId as String, ConditionType.fromValue(filter.condition as? String), toConditionValues(filter.conditionValues)),
        operator = EquationOperator.fromValue(equation.operator as? String),
    )
}

private fun toLock(lock: dynamic): AttributeLock? = if (lock == null) null else AttributeLock(
    locked = lock.locked == true,
    exceptionGroups = toList(lock.exceptionGroups).map {
        val g = it.asDynamic()
        AttributeLockExceptionGroup(g.type as? String, toList(g.typeValue).map { v -> v as String }, toEquation(g.equation))
    },
)

private fun toAttributes(attributes: dynamic): List<Attribute> = toList(attributes).map {
    val attribute = it.asDynamic()
    val constraint = attribute.constraint
    Attribute(
        attribute.id as String,
        if (constraint == null) null else createConstraint(constraint.type as? String, toMapDeep(constraint.config)),
        toLock(attribute.lock),
    )
}

private fun toCollection(collection: dynamic) = Collection(collection.id as String, toAttributes(collection.attributes))

private fun toLinkType(linkType: dynamic) =
    LinkType(linkType.id as String, toList(linkType.collectionIds).map { it as String }, toAttributes(linkType.attributes))

private fun toConditionValues(values: dynamic): List<ConditionValue> =
    toList(values).map { ConditionValue(it.asDynamic().type as? String, it.asDynamic().value as Any?) }

private fun toQuery(query: dynamic): Query? {
    if (query == null) return null
    val stems = toList(query.stems).map {
        val stem = it.asDynamic()
        QueryStem(
            collectionId = stem.collectionId as String,
            linkTypeIds = toList(stem.linkTypeIds).map { id -> id as String },
            documentIds = toList(stem.documentIds).map { id -> id as String }.toSet(),
            filters = toList(stem.filters).map { f ->
                val filter = f.asDynamic()
                CollectionAttributeFilter(filter.collectionId as String, filter.attributeId as String, ConditionType.fromValue(filter.condition as? String), toConditionValues(filter.conditionValues))
            },
            linkFilters = toList(stem.linkFilters).map { f ->
                val filter = f.asDynamic()
                LinkAttributeFilter(filter.linkTypeId as String, filter.attributeId as String, ConditionType.fromValue(filter.condition as? String), toConditionValues(filter.conditionValues))
            },
        )
    }
    return Query(
        stems = stems,
        fulltexts = toList(query.fulltexts).map { it as String },
        page = (query.page as? Number)?.toInt(),
        pageSize = (query.pageSize as? Number)?.toInt(),
    )
}

/**
 * Same signature as filterDocumentsAndLinksByQuery in @lumeer/data-filters.
 * Permissions are accepted for compatibility; the original filter does not use them either.
 */
@JsExport
fun filterDocumentsAndLinksByQuery(
    documents: Array<dynamic>?,
    collections: Array<dynamic>?,
    linkTypes: Array<dynamic>?,
    linkInstances: Array<dynamic>?,
    query: dynamic,
    collectionsPermissions: dynamic,
    linkTypePermissions: dynamic,
    constraintData: dynamic,
    includeChildren: Boolean?,
    includeNonLinkedDocuments: Boolean?,
): dynamic {
    val result = filterByQuery(
        documents = (documents ?: emptyArray()).map { JsDocument(it) },
        collections = (collections ?: emptyArray()).map { toCollection(it) },
        linkTypes = (linkTypes ?: emptyArray()).map { toLinkType(it) },
        linkInstances = (linkInstances ?: emptyArray()).map { JsLinkInstance(it) },
        query = toQuery(query),
        constraintData = toConstraintData(constraintData),
        includeChildren = includeChildren == true,
        includeNonLinkedDocuments = includeNonLinkedDocuments == true,
    )
    val output: dynamic = js("({})")
    output.documents = result.documents.map { it.source }.toTypedArray()
    output.linkInstances = result.linkInstances.map { it.source }.toTypedArray()
    return output
}
