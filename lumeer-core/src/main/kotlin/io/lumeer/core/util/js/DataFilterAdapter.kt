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

import io.lumeer.api.model.Attribute
import io.lumeer.api.model.AttributeFilter
import io.lumeer.api.model.AttributeFilterEquation
import io.lumeer.api.model.AttributeLock
import io.lumeer.api.model.Collection
import io.lumeer.api.model.ConditionValue
import io.lumeer.api.model.ConstraintData
import io.lumeer.api.model.Document
import io.lumeer.api.model.LinkInstance
import io.lumeer.api.model.LinkType
import io.lumeer.api.model.Query
import io.lumeer.api.model.User
import io.lumeer.core.util.Tuple
import io.lumeer.datafilters.constraint.createConstraint
import io.lumeer.datafilters.filter.filterDocumentsAndLinksByQuery
import io.lumeer.datafilters.model.DocumentModel
import io.lumeer.datafilters.model.LinkInstanceModel
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Logger
import io.lumeer.datafilters.model.Attribute as KAttribute
import io.lumeer.datafilters.model.AttributeFilter as KAttributeFilter
import io.lumeer.datafilters.model.AttributeFilterEquation as KAttributeFilterEquation
import io.lumeer.datafilters.model.AttributeLock as KAttributeLock
import io.lumeer.datafilters.model.AttributeLockExceptionGroup as KAttributeLockExceptionGroup
import io.lumeer.datafilters.model.Collection as KCollection
import io.lumeer.datafilters.model.CollectionAttributeFilter as KCollectionAttributeFilter
import io.lumeer.datafilters.model.ConditionType as KConditionType
import io.lumeer.datafilters.model.ConditionValue as KConditionValue
import io.lumeer.datafilters.model.ConstraintData as KConstraintData
import io.lumeer.datafilters.model.CurrencyData as KCurrencyData
import io.lumeer.datafilters.model.EquationOperator as KEquationOperator
import io.lumeer.datafilters.model.LinkAttributeFilter as KLinkAttributeFilter
import io.lumeer.datafilters.model.LinkType as KLinkType
import io.lumeer.datafilters.model.Query as KQuery
import io.lumeer.datafilters.model.QueryStem as KQueryStem
import io.lumeer.datafilters.model.SelectOption as KSelectOption
import io.lumeer.datafilters.model.SelectionList as KSelectionList
import io.lumeer.datafilters.model.Team as KTeam
import io.lumeer.datafilters.model.User as KUser

/** Maps engine models to the data-filters library models (without copying document data) and back. */
object DataFilterAdapter {

    private val logger: Logger = Logger.getLogger(DataFilterAdapter::class.simpleName)

    /** "Now" for relative date conditions, replaceable in tests. */
    internal var clock: () -> Long = { System.currentTimeMillis() }

    private class DocumentAdapter(val document: Document) : DocumentModel {
        override val id: String get() = document.id
        override val collectionId: String get() = document.collectionId
        override val parentId: String? get() = document.metaData?.get(Document.META_PARENT_ID) as? String
        override fun value(attributeId: String): Any? = normalizeValue(document.data?.get(attributeId))
    }

    private class LinkInstanceAdapter(val linkInstance: LinkInstance) : LinkInstanceModel {
        override val id: String get() = linkInstance.id
        override val linkTypeId: String get() = linkInstance.linkTypeId
        override val documentIds: List<String> get() = linkInstance.documentIds.orEmpty()
        override fun value(attributeId: String): Any? = normalizeValue(linkInstance.data?.get(attributeId))
    }

    /** Values as the JavaScript filter received them (serialized with Gson before). */
    private fun normalizeValue(value: Any?): Any? = when (value) {
        is Double -> if (value.isNaN()) "NaN" else if (value.isInfinite()) "Infinity" else value
        is Float -> normalizeValue(value.toDouble())
        is LocalDateTime -> value.toInstant(ZoneOffset.UTC).epochSecond
        is ZonedDateTime -> value.toInstant().epochSecond
        is List<*> -> value.map { normalizeValue(it) }
        else -> value
    }

    fun filter(
        documents: List<Document>,
        collections: List<Collection>,
        linkTypes: List<LinkType>,
        linkInstances: List<LinkInstance>,
        query: Query,
        constraintData: ConstraintData?,
        includeChildren: Boolean,
        includeNonLinkedDocuments: Boolean,
    ): Tuple<List<Document>, List<LinkInstance>> {
        return try {
            val result = filterDocumentsAndLinksByQuery(
                documents = documents.map { DocumentAdapter(it) },
                collections = collections.map { KCollection(it.id, toAttributes(it.attributes)) },
                linkTypes = linkTypes.map { KLinkType(it.id, it.collectionIds.orEmpty(), toAttributes(it.attributes)) },
                linkInstances = linkInstances.map { LinkInstanceAdapter(it) },
                query = toQuery(query),
                constraintData = toConstraintData(constraintData),
                includeChildren = includeChildren,
                includeNonLinkedDocuments = includeNonLinkedDocuments,
            )
            Tuple(
                restoreById(result.documents.map { it.document }, documents) { it.id },
                restoreById(result.linkInstances.map { it.linkInstance }, linkInstances) { it.id },
            )
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error filtering data: ", e)
            Tuple(emptyList(), emptyList())
        }
    }

    /** Like the JavaScript version did, maps the resulting ids back to all original objects with that id. */
    internal fun <T> restoreById(result: List<T>, originals: List<T>, id: (T) -> String): List<T> {
        val originalsById = originals.groupBy(id)
        return result.flatMap { originalsById[id(it)].orEmpty() }
    }

    private fun toUser(user: User?): KUser? = user?.let { KUser(it.id, it.name, it.email) }

    private fun toConstraintData(data: ConstraintData?): KConstraintData = KConstraintData(
        users = data?.users.orEmpty().mapNotNull { toUser(it) },
        teams = data?.teams.orEmpty().map { KTeam(it.id, it.name, it.users.orEmpty()) },
        currentUser = toUser(data?.currentUser),
        durationUnitsMap = data?.durationUnitsMap,
        currencyData = data?.currencyData?.let { KCurrencyData(it.abbreviations.orEmpty(), it.ordinals.orEmpty()) },
        selectionLists = data?.selectionLists.orEmpty().map { list ->
            KSelectionList(list.id, list.displayValues == true, list.options.orEmpty().map { KSelectOption(it.value, it.displayValue) })
        },
        timezone = data?.timezone,
        nowMillis = clock(),
    )

    private fun toAttributes(attributes: kotlin.collections.Collection<Attribute>?): List<KAttribute> =
        attributes.orEmpty().map { attribute ->
            val constraint = attribute.constraint
            KAttribute(
                attribute.id,
                constraint?.let { createConstraint(it.type?.name, toConfig(it.config)) },
                toLock(attribute.lock),
            )
        }

    private fun toConfig(config: Any?): Map<String, Any?>? =
        (config as? Map<*, *>)?.entries?.associate { (key, value) -> key.toString() to value }

    private fun toLock(lock: AttributeLock?): KAttributeLock? = lock?.let {
        KAttributeLock(it.locked, it.exceptionGroups.orEmpty().map { group ->
            KAttributeLockExceptionGroup(group.type, group.typeValue.orEmpty(), toEquation(group.equation))
        })
    }

    private fun toEquation(equation: AttributeFilterEquation?): KAttributeFilterEquation? = equation?.let {
        KAttributeFilterEquation(
            it.equations.orEmpty().mapNotNull { child -> toEquation(child) },
            it.filter?.let { filter -> toFilter(filter) },
            KEquationOperator.fromValue(it.operator),
        )
    }

    private fun toFilter(filter: AttributeFilter): KAttributeFilter =
        KAttributeFilter(filter.attributeId, toCondition(filter), toConditionValues(filter.conditionValues))

    private fun toCondition(filter: AttributeFilter): KConditionType? = KConditionType.fromValue(filter.condition?.value)

    private fun toConditionValues(values: List<ConditionValue>?): List<KConditionValue> =
        values.orEmpty().map { KConditionValue(it.type, normalizeValue(it.value)) }

    private fun toQuery(query: Query): KQuery = KQuery(
        stems = query.stems.orEmpty().map { stem ->
            KQueryStem(
                collectionId = stem.collectionId,
                linkTypeIds = stem.linkTypeIds.orEmpty(),
                documentIds = stem.documentIds.orEmpty(),
                filters = stem.filters.orEmpty().map {
                    KCollectionAttributeFilter(it.collectionId, it.attributeId, toCondition(it), toConditionValues(it.conditionValues))
                },
                linkFilters = stem.linkFilters.orEmpty().map {
                    KLinkAttributeFilter(it.linkTypeId, it.attributeId, toCondition(it), toConditionValues(it.conditionValues))
                },
            )
        },
        fulltexts = query.fulltexts.orEmpty().toList(),
        page = query.page,
        pageSize = query.pageSize,
    )
}
