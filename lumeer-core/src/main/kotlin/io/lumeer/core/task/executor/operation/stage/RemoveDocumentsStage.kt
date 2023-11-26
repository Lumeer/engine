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
package io.lumeer.core.task.executor.operation.stage

import io.lumeer.api.model.CollectionPurposeType
import io.lumeer.api.model.Document
import io.lumeer.api.model.LinkInstance
import io.lumeer.api.model.LinkType
import io.lumeer.api.model.ResourceType
import io.lumeer.core.adapter.CollectionAdapter
import io.lumeer.core.adapter.DocumentAdapter
import io.lumeer.core.adapter.LinkInstanceAdapter
import io.lumeer.core.adapter.LinkTypeAdapter
import io.lumeer.core.facade.FunctionFacade
import io.lumeer.core.task.RuleTask
import io.lumeer.core.task.executor.ChangesTracker
import io.lumeer.core.task.executor.operation.DocumentRemovalOperation
import io.lumeer.core.task.executor.operation.OperationExecutor
import io.lumeer.engine.api.event.RemoveDocument
import io.lumeer.engine.api.event.RemoveLinkInstance

class RemoveDocumentsStage(executor: OperationExecutor) : Stage(executor) {

    private val collectionAdapter = CollectionAdapter.createFromDaoSnanpshot(task.daoContextSnapshot)
    private val linkTypeAdapter = LinkTypeAdapter.createFromDaoSnapshot(task.daoContextSnapshot)
    private val documentAdapter = DocumentAdapter.createFromDaoSnapshot(task.daoContextSnapshot)
    private val linkInstanceAdapter = LinkInstanceAdapter.createFromDaoSnapshot(task.daoContextSnapshot)
    private val functionFacade: FunctionFacade = task.functionFacade
    private val taskProcessingFacade = task.getTaskProcessingFacade(taskExecutor, functionFacade)
    private val purposeChangeProcessor = task.purposeChangeProcessor

    override fun call(): ChangesTracker {
        // remove documents
        processRemoveDocuments(operations.filter { operation -> operation is DocumentRemovalOperation }.map { operation -> operation as DocumentRemovalOperation })

        return changesTracker
    }

    private fun processRemoveDocuments(operations: List<DocumentRemovalOperation>) {
        if (operations.isEmpty()) {
            return
        }

        // collect all information that we will need during the process
        val documents = operations.map { it.entity }
        val updatedCollectionIds = documents.map { it.collectionId }
        val updatedCollections = task.daoContextSnapshot.collectionDao.getCollectionsByIds(updatedCollectionIds)

        val removedLinks = task.daoContextSnapshot.linkInstanceDao.getLinkInstancesByDocumentIds(documents.map { it.id }.toSet())
        val updatedLinkTypeIds = removedLinks.map { it.linkTypeId }
        val updatedLinkTypes = task.daoContextSnapshot.linkTypeDao.getLinkTypesByIds(updatedLinkTypeIds.toSet())

        // remove all data - documents, links, favorites, comments
        removeLinks(removedLinks, documents, updatedLinkTypes.associateBy { it.id })
        removeDocuments(documents, updatedCollections.associateBy { it.id })

        // update document and link instance counts
        collectionAdapter.mapCollectionsComputedProperties(updatedCollections, task.initiator.id, task.daoContextSnapshot.projectId)
        linkTypeAdapter.mapLinkTypesComputedProperties(updatedLinkTypes)

        // update collections and link types to get new versions (so that frontend updates store)
        val savedCollections = updatedCollections.map { task.daoContextSnapshot.collectionDao.updateCollection(it.id, it, null, false) }
        val savedLinkTypes = updatedLinkTypes.map { task.daoContextSnapshot.linkTypeDao.updateLinkType(it.id, it, null, false) }

        // update saved changes in changes tracker
        changesTracker.updateCollectionsMap(savedCollections.associateBy { it.id })
        changesTracker.collections.addAll(savedCollections)

        changesTracker.updateLinkTypesMap(savedLinkTypes.associateBy { it.id })
        changesTracker.linkTypes.addAll(savedLinkTypes)
    }

    private fun removeLinks(removedLinks: List<LinkInstance>, documents: List<Document>, linkTypes: Map<String, LinkType>) {
        changesTracker.addRemovedLinkInstances(removedLinks)

        task.daoContextSnapshot.linkInstanceDao.deleteLinkInstancesByDocumentsIds(documents.map { it.id }.toSet())
        linkInstanceAdapter.deleteComments(removedLinks.map { it.id }.toSet())

        removedLinks.groupBy { it.linkTypeId }.forEach { (linkTypeId, linkInstances) ->
            task.daoContextSnapshot.linkDataDao.deleteData(linkTypeId, linkInstances.map { it.id }.toSet())
        }

        removedLinks.forEach { link ->
            val linkType = linkTypes[link.linkTypeId]
            val decodedDeletedData = constraintManager.decodeDataTypes(linkType, link.data)
            auditAdapter.registerDelete(link.linkTypeId, ResourceType.LINK, link.id, task.initiator, automationName, null, decodedDeletedData)
        }

        if (task is RuleTask) {
            if (task.recursionDepth == 0) { // we can still call other rules
                removedLinks.forEach { rl ->
                    taskProcessingFacade.onRemoveLink(RemoveLinkInstance(rl), task.rule.name)
                }
            } else {  // only functions get evaluated
                val removedLinksByType = removedLinks.groupBy { it.linkTypeId }
                removedLinksByType.keys.forEach { removedLinkTypeId ->
                    taskExecutor.submitTask(functionFacade.createTaskForRemovedLinks(linkTypes[removedLinkTypeId], removedLinksByType[removedLinkTypeId]))
                }
            }
        }
    }

    private fun removeDocuments(documents: List<Document>, collections: Map<String, io.lumeer.api.model.Collection>) {
        val documentsByCollectionIds = documents.groupBy { it.collectionId }

        changesTracker.addRemovedDocuments(documents)

        documentAdapter.deleteComments(documents.map { it.id }.toSet())

        documentsByCollectionIds.forEach { (collectionId, docs) ->
            task.daoContextSnapshot.dataDao.deleteData(collectionId, docs.map { it.id }.toSet())

            docs.forEach {
                val collection = collections[collectionId]

                task.daoContextSnapshot.documentDao.deleteDocument(it.id, it.data)
                task.daoContextSnapshot.favoriteItemDao.removeFavoriteDocumentFromUsers(task.daoContextSnapshot.projectId, collectionId, it.id)

                val decodedDeletedData = constraintManager.decodeDataTypes(collection, it.data)
                auditAdapter.registerDelete(it.collectionId, ResourceType.DOCUMENT, it.id, task.initiator, automationName, null, decodedDeletedData)

                if (task is RuleTask) {
                    if (task.recursionDepth == 0) { // we can still call other rules
                        taskProcessingFacade.onRemoveDocument(RemoveDocument(it), task.rule.name)
                    } else { // only functions get evaluated
                        taskExecutor.submitTask(functionFacade.createTaskForRemovedDocument(collection, it))
                    }
                }

                // notify delayed actions about data change
                if (collection!!.purposeType == CollectionPurposeType.Tasks) {
                    purposeChangeProcessor.processChanges(RemoveDocument(it), collection)
                }
            }
        }
    }

}