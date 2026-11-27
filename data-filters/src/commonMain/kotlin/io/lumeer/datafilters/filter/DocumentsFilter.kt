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
package io.lumeer.datafilters.filter

import io.lumeer.datafilters.constraint.UnknownConstraint
import io.lumeer.datafilters.datavalue.DataValue
import io.lumeer.datafilters.datavalue.UserDataValue
import io.lumeer.datafilters.model.Attribute
import io.lumeer.datafilters.model.AttributeFilter
import io.lumeer.datafilters.model.AttributeLock
import io.lumeer.datafilters.model.AttributeLockGroupType
import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.model.ConditionValueType
import io.lumeer.datafilters.model.ConstraintType
import io.lumeer.datafilters.model.EquationOperator
import io.lumeer.datafilters.model.AttributesResource
import io.lumeer.datafilters.model.Collection
import io.lumeer.datafilters.model.ConstraintData
import io.lumeer.datafilters.model.DataResource
import io.lumeer.datafilters.model.DocumentModel
import io.lumeer.datafilters.model.LinkInstanceModel
import io.lumeer.datafilters.model.LinkType
import io.lumeer.datafilters.model.Query
import io.lumeer.datafilters.model.QueryStem
import io.lumeer.datafilters.util.escapeHtml
import io.lumeer.datafilters.util.removeAccentFromString

class FilterResult<D : DocumentModel, L : LinkInstanceModel>(
    val documents: List<D>,
    val linkInstances: List<L>,
)

/** One collection or link type of a query stem with its data and filters. */
private class FilterPipeline(
    val resource: AttributesResource,
    val dataResources: List<DataResource>,
    val filters: List<AttributeFilter>,
    val fulltexts: List<String>,
    val documentIds: Set<String>,
) {
    /** attributes needed by the filters (including the ones used by action button locks) */
    val attributes: List<Attribute> = filterAttributesByFilters(resource.attributes, filters)
    val attributesMap: Map<String, Attribute> = attributes.associateBy { it.id }
    val resourceAttributesMap: Map<String, Attribute> = resource.attributes.associateBy { it.id }

    /** filters on attributes that exist */
    val definedFilters: List<AttributeFilter> = filters.filter { attributesMap.containsKey(it.attributeId) }

    val documents: List<DocumentModel> by lazy { dataResources.map { it as DocumentModel } }

    /** link instances by linked document id, in the original order */
    val linksByDocumentId: Map<String, List<LinkInstanceModel>> by lazy {
        val map = LinkedHashMap<String, MutableList<LinkInstanceModel>>()
        for (link in dataResources) {
            link as LinkInstanceModel
            for (documentId in link.documentIds.distinct()) {
                map.getOrPut(documentId) { mutableListOf() }.add(link)
            }
        }
        map
    }
}

private class Filtered {
    val allDocuments = mutableListOf<DocumentModel>()
    val allLinkInstances = mutableListOf<LinkInstanceModel>()
}

/**
 * Port of filterDocumentsAndLinksByQuery from @lumeer/data-filters.
 * The returned objects are the same instances that were passed in.
 */
fun <D : DocumentModel, L : LinkInstanceModel> filterDocumentsAndLinksByQuery(
    documents: List<D>,
    collections: List<Collection>,
    linkTypes: List<LinkType>,
    linkInstances: List<L>,
    query: Query?,
    constraintData: ConstraintData?,
    includeChildren: Boolean = false,
    includeNonLinkedDocuments: Boolean = false,
): FilterResult<D, L> {
    if (query == null || (query.stems.isEmpty() && query.fulltexts.isEmpty())) {
        return FilterResult(paginate(documents, query), linkInstances)
    }
    val context = FilterContext(constraintData)

    var uniqueDocuments: List<DocumentModel> = emptyList()
    var uniqueLinkInstances: List<LinkInstanceModel> = emptyList()

    val stems = query.stems.ifEmpty { collections.map { QueryStem(collectionId = it.id) } }
    val documentsByCollections = documents.groupBy { it.collectionId }
    val linkInstancesByLinkTypes = linkInstances.groupBy { it.linkTypeId }
    val escapedFulltexts = query.fulltexts.map { removeAccentFromString(escapeHtml(it)) }

    for (stem in stems) {
        val filtered = filterDocumentsAndLinksByStem(
            collections, documentsByCollections, linkTypes, linkInstancesByLinkTypes,
            context, stem, escapedFulltexts, includeChildren, includeNonLinkedDocuments,
        )
        uniqueDocuments = mergeById(uniqueDocuments, filtered.allDocuments)
        uniqueLinkInstances = mergeById(uniqueLinkInstances, filtered.allLinkInstances)
    }

    @Suppress("UNCHECKED_CAST")
    return FilterResult(paginate(uniqueDocuments as List<D>, query), uniqueLinkInstances as List<L>)
}

/**
 * Appends the items of the next stem whose id is not in the result yet. Like the original, duplicates within one stem
 * stay (a link instance reached from several documents is listed once per document).
 */
private fun <T : DataResource> mergeById(result: List<T>, next: List<T>): List<T> {
    if (result.isEmpty() || next.isEmpty()) return result.ifEmpty { next }
    val ids = result.mapTo(HashSet()) { it.id }
    return result + next.filter { it.id !in ids }
}

private fun filterDocumentsAndLinksByStem(
    collections: List<Collection>,
    documentsByCollections: Map<String, List<DocumentModel>>,
    linkTypes: List<LinkType>,
    linkInstancesByLinkTypes: Map<String, List<LinkInstanceModel>>,
    context: FilterContext,
    stem: QueryStem,
    fulltexts: List<String>,
    includeChildren: Boolean,
    includeNonLinkedDocuments: Boolean,
): Filtered {
    val filtered = Filtered()
    val pipeline = queryStemAttributesResourcesOrder(stem, collections, linkTypes).map { resource ->
        val filters: List<AttributeFilter>
        val dataResources: List<DataResource>
        when (resource) {
            is Collection -> {
                filters = stem.filters.filter { it.collectionId == resource.id }
                dataResources = documentsByCollections[resource.id].orEmpty()
            }
            is LinkType -> {
                filters = stem.linkFilters.filter { it.linkTypeId == resource.id }
                dataResources = linkInstancesByLinkTypes[resource.id].orEmpty()
            }
        }
        FilterPipeline(resource, dataResources, filters, fulltexts, stem.documentIds)
    }

    val currentPipeline = pipeline.firstOrNull() ?: return filtered
    val pushedIds = pipeline.map { mutableSetOf<String>() }
    val currentPushedIds = pushedIds[0]
    val documentsMap = if (includeChildren) documentChildrenMap(currentPipeline.documents) else emptyMap()

    for (document in currentPipeline.documents) {
        val dataValues = createDataValuesMap(document, currentPipeline.attributes, context)
        if (!dataValuesMeetsFilters(dataValues, currentPipeline.filters, currentPipeline.resourceAttributesMap, context) || currentPushedIds.contains(document.id)) {
            continue
        }
        val fulltextFound = dataMeetsFulltexts(document, currentPipeline.fulltexts, currentPipeline.resource.attributes, context)
        if (checkAndFillDataResources(document, pipeline, filtered, context, pushedIds, 1, fulltextFound) ||
            pipelineContainsDocumentByIds(currentPipeline, document)
        ) {
            filtered.allDocuments.add(document)
            currentPushedIds.add(document.id)

            val children = if (includeChildren) getDocumentChildren(document, documentsMap) else emptyList()
            for (child in children) {
                if (!currentPushedIds.contains(child.id)) {
                    val childFulltextFound = dataMeetsFulltexts(child, currentPipeline.fulltexts, currentPipeline.resource.attributes, context)
                    checkAndFillDataResources(child, pipeline, filtered, context, pushedIds, 1, childFulltextFound)
                    filtered.allDocuments.add(child)
                    currentPushedIds.add(child.id)
                }
            }
        }
    }

    if (includeNonLinkedDocuments) {
        // collections further in the stem (every second resource) may add documents without links
        for (i in 2 until pipeline.size step 2) {
            val pipe = pipeline[i]
            for (document in pipe.documents) {
                val dataValues = createDataValuesMap(document, pipe.attributes, context)
                if (!pushedIds[i].contains(document.id) &&
                    dataValuesMeetsFilters(dataValues, pipe.filters, pipe.resourceAttributesMap, context) &&
                    dataMeetsFulltexts(document, pipe.fulltexts, pipe.attributes, context)
                ) {
                    filtered.allDocuments.add(document)
                    pushedIds[i].add(document.id)
                }
            }
        }
    }

    return filtered
}

private fun checkAndFillDataResources(
    previousDataResource: DataResource,
    pipeline: List<FilterPipeline>,
    filtered: Filtered,
    context: FilterContext,
    pushedIds: List<MutableSet<String>>,
    pipelineIndex: Int,
    fulltextFound: Boolean,
): Boolean {
    if (pipelineIndex >= pipeline.size) {
        return if (pipeline[0].documentIds.isNotEmpty()) false else pipeline[0].fulltexts.isEmpty() || fulltextFound
    }

    val currentPipeline = pipeline[pipelineIndex]
    if (currentPipeline.resource is LinkType) {
        val previousDocument = previousDataResource as DocumentModel
        val linkedLinks = currentPipeline.linksByDocumentId[previousDocument.id].orEmpty().filter { dataMeetsFilters(it, currentPipeline, context) }
        if (linkedLinks.isEmpty() && containsAnyFilterInPipeline(pipeline, pipelineIndex)) {
            return false
        }

        var someLinkPassed = (currentPipeline.fulltexts.isEmpty() || fulltextFound) && linkedLinks.isEmpty()
        for (linkedLink in linkedLinks) {
            val found = fulltextFound || dataMeetsFulltexts(linkedLink, currentPipeline.fulltexts, currentPipeline.resource.attributes, context)
            if (checkAndFillDataResources(linkedLink, pipeline, filtered, context, pushedIds, pipelineIndex + 1, found) ||
                pipelineContainsLinkByIds(currentPipeline, linkedLink)
            ) {
                someLinkPassed = true
                filtered.allLinkInstances.add(linkedLink)
            }
        }
        return someLinkPassed
    } else {
        val previousLink = previousDataResource as LinkInstanceModel
        val linkedIds = previousLink.documentIds.toSet()
        val linkedDocuments = currentPipeline.documents.filter { linkedIds.contains(it.id) && dataMeetsFilters(it, currentPipeline, context) }
        if (linkedDocuments.isEmpty() && containsAnyFilterInPipeline(pipeline, pipelineIndex)) {
            return false
        }

        var someDocumentPassed = (currentPipeline.fulltexts.isEmpty() || fulltextFound) && linkedDocuments.isEmpty()
        for (linkedDocument in linkedDocuments) {
            val found = fulltextFound || dataMeetsFulltexts(linkedDocument, currentPipeline.fulltexts, currentPipeline.resource.attributes, context)
            if (checkAndFillDataResources(linkedDocument, pipeline, filtered, context, pushedIds, pipelineIndex + 1, found) ||
                pipelineContainsDocumentByIds(currentPipeline, linkedDocument)
            ) {
                someDocumentPassed = true
                if (pushedIds[pipelineIndex].add(linkedDocument.id)) {
                    filtered.allDocuments.add(linkedDocument)
                }
            }
        }
        return someDocumentPassed
    }
}

private fun pipelineContainsDocumentByIds(pipeline: FilterPipeline, document: DocumentModel): Boolean =
    pipeline.documentIds.isNotEmpty() && pipeline.documentIds.contains(document.id)

private fun pipelineContainsLinkByIds(pipeline: FilterPipeline, linkInstance: LinkInstanceModel): Boolean {
    // Same operator precedence as the original: (size > 0 && length === 2 && has(ids[0])) || has(ids[1])
    val ids = linkInstance.documentIds
    return (pipeline.documentIds.isNotEmpty() && ids.size == 2 && pipeline.documentIds.contains(ids[0])) ||
        (ids.size > 1 && pipeline.documentIds.contains(ids[1]))
}

private fun containsAnyFilterInPipeline(pipeline: List<FilterPipeline>, fromIndex: Int): Boolean =
    pipeline.subList(fromIndex, pipeline.size).any { it.filters.isNotEmpty() }

private fun getDocumentChildren(document: DocumentModel, documentsMap: Map<String, List<DocumentModel>>): List<DocumentModel> {
    val result = LinkedHashMap<String, DocumentModel>()
    val queue = ArrayDeque<DocumentModel>()
    queue.add(document)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!result.containsKey(current.id)) {
            result[current.id] = current
            queue.addAll(documentsMap[current.id].orEmpty())
        }
    }
    result.remove(document.id)
    return result.values.toList()
}

private fun documentChildrenMap(documents: List<DocumentModel>): Map<String, List<DocumentModel>> =
    documents.filter { !it.parentId.isNullOrEmpty() }.groupBy { it.parentId!! }

private fun <D> paginate(documents: List<D>, query: Query?): List<D> {
    val page = query?.page ?: return documents
    val pageSize = query.pageSize ?: return documents
    if (page == 0 && pageSize == 0) {
        return documents
    }
    val from = (page * pageSize).coerceIn(0, documents.size)
    val to = ((page + 1) * pageSize).coerceIn(from, documents.size)
    return documents.subList(from, to)
}

private fun queryStemAttributesResourcesOrder(stem: QueryStem, collections: List<Collection>, linkTypes: List<LinkType>): List<AttributesResource> {
    val baseCollection = collections.firstOrNull { it.id == stem.collectionId } ?: return emptyList()
    val chain = mutableListOf<AttributesResource>(baseCollection)
    var previousCollection = baseCollection
    for (linkTypeId in stem.linkTypeIds) {
        val linkType = linkTypes.firstOrNull { it.id == linkTypeId }
        val otherCollectionId = linkType?.let {
            if (it.collectionIds.getOrNull(0) == previousCollection.id) it.collectionIds.getOrNull(1) else it.collectionIds.getOrNull(0)
        }
        val otherCollection = otherCollectionId?.let { id -> collections.firstOrNull { it.id == id } }
        if (linkType != null && otherCollection != null) {
            chain.add(linkType)
            chain.add(otherCollection)
            previousCollection = otherCollection
        } else {
            break
        }
    }
    return chain
}

private fun filterAttributesByFilters(attributes: List<Attribute>, filters: List<AttributeFilter>): List<Attribute> {
    val attributesMap = attributes.associateBy { it.id }
    val result = LinkedHashMap<String, Attribute>()
    for (filter in filters) {
        val attribute = attributesMap[filter.attributeId] ?: continue
        result[attribute.id] = attribute
        if (attribute.constraint?.type == ConstraintType.Action) {
            for (lockFilter in collectAttributeLockFilters(attribute.lock)) {
                attributesMap[lockFilter.attributeId]?.let { result[it.id] = it }
            }
        }
    }
    return result.values.toList()
}

private fun collectAttributeLockFilters(lock: AttributeLock?): List<AttributeFilter> =
    lock?.exceptionGroups.orEmpty().flatMap { group -> group.equation?.equations.orEmpty().mapNotNull { it.filter } }

private fun createDataValuesMap(dataResource: DataResource, attributes: List<Attribute>, context: FilterContext): Map<String, DataValue> {
    val map = LinkedHashMap<String, DataValue>()
    for (attribute in attributes) {
        map[attribute.id] = (attribute.constraint ?: UnknownConstraint).createDataValue(dataResource.value(attribute.id), context)
    }
    return map
}

private fun dataValuesMeetsFilters(dataValues: Map<String, DataValue>, filters: List<AttributeFilter>, attributesMap: Map<String, Attribute>, context: FilterContext): Boolean =
    filters.all { filter ->
        val dataValue = dataValues[filter.attributeId] ?: return@all false
        val attribute = attributesMap[filter.attributeId]
        if (attribute?.constraint?.type == ConstraintType.Action) {
            when (filter.condition) {
                ConditionType.Enabled -> isActionButtonEnabled(dataValues, attributesMap, attribute.lock, context)
                ConditionType.Disabled -> !isActionButtonEnabled(dataValues, attributesMap, attribute.lock, context)
                else -> false
            }
        } else {
            dataValue.meetCondition(filter.condition, filter.conditionValues)
        }
    }

/** data-aggregation.utils.ts isActionButtonEnabled */
private fun isActionButtonEnabled(dataValues: Map<String, DataValue>, attributesMap: Map<String, Attribute>, lock: AttributeLock?, context: FilterContext): Boolean {
    var satisfy = false
    for (group in lock?.exceptionGroups.orEmpty()) {
        if (group.type == AttributeLockGroupType.EVERYONE ||
            (group.type == AttributeLockGroupType.USERS_AND_TEAMS && exceptionGroupContainsCurrentUser(group.typeValue, context))
        ) {
            val equations = group.equation?.equations.orEmpty()
            val filters = equations.mapNotNull { it.filter }
            val operator = equations.firstOrNull()?.operator ?: EquationOperator.And
            val definedFilters = filters.filter { attributesMap.containsKey(it.attributeId) }
            val stats = definedFilters.map { dataValuesMeetsFilters(dataValues, listOf(it), attributesMap, context) }
            val groupSatisfy = if (operator == EquationOperator.Or) stats.isEmpty() || stats.any { it } else stats.isEmpty() || stats.all { it }
            satisfy = satisfy || groupSatisfy
        }
    }
    return satisfy
}

private fun exceptionGroupContainsCurrentUser(typeValue: List<String>, context: FilterContext): Boolean {
    val dataValue = UserDataValue(typeValue, context)
    return dataValue.meetCondition(ConditionType.HasSome, listOf(ConditionValue(type = ConditionValueType.CURRENT_USER))) ||
        dataValue.meetCondition(ConditionType.HasSome, listOf(ConditionValue(type = ConditionValueType.CURRENT_TEAMS)))
}

private fun dataMeetsFilters(dataResource: DataResource, pipeline: FilterPipeline, context: FilterContext): Boolean =
    dataValuesMeetsFilters(createDataValuesMap(dataResource, pipeline.attributes, context), pipeline.definedFilters, pipeline.attributesMap, context)

private fun dataMeetsFulltexts(dataResource: DataResource, fulltexts: List<String>, attributes: List<Attribute>, context: FilterContext): Boolean {
    if (fulltexts.isEmpty()) {
        return true
    }
    val dataValues = createDataValuesMap(dataResource, attributes, context).values
    return fulltexts.any { fulltext -> dataValues.any { it.meetFullTexts(listOf(fulltext)) } }
}
