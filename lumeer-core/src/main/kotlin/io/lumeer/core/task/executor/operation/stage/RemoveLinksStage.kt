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

import io.lumeer.api.model.LinkInstance
import io.lumeer.api.model.LinkType
import io.lumeer.api.model.ResourceType
import io.lumeer.core.adapter.LinkInstanceAdapter
import io.lumeer.core.adapter.LinkTypeAdapter
import io.lumeer.core.facade.FunctionFacade
import io.lumeer.core.task.RuleTask
import io.lumeer.core.task.executor.ChangesTracker
import io.lumeer.core.task.executor.operation.LinkRemovalOperation
import io.lumeer.core.task.executor.operation.OperationExecutor
import io.lumeer.engine.api.event.RemoveLinkInstance

class RemoveLinksStage(executor: OperationExecutor) : Stage(executor) {

   private val linkTypeAdapter = LinkTypeAdapter.createFromDaoSnapshot(task.daoContextSnapshot)
   private val linkInstanceAdapter = LinkInstanceAdapter.createFromDaoSnapshot(task.daoContextSnapshot)
   private val functionFacade: FunctionFacade = task.functionFacade
   private val taskProcessingFacade = task.getTaskProcessingFacade(taskExecutor, functionFacade)
   private val purposeChangeProcessor = task.purposeChangeProcessor

   override fun call(): ChangesTracker {
      // remove links
      processRemoveLinks(operations.filter { operation -> operation is LinkRemovalOperation }.map { operation -> operation as LinkRemovalOperation })

      return changesTracker
   }

   private fun processRemoveLinks(operations: List<LinkRemovalOperation>) {
      if (operations.isEmpty()) {
         return
      }

      // collect all information that we will need during the process
      val links = operations.map { it.entity }
      val updatedLinkTypeIds = links.map { it.linkTypeId }
      val updatedLinkTypes = task.daoContextSnapshot.linkTypeDao.getLinkTypesByIds(updatedLinkTypeIds)

      // remove all data - links, favorites, comments
      removeLinks(links, updatedLinkTypes.associateBy { it.id })

      // update document and link instance counts
      linkTypeAdapter.mapLinkTypesComputedProperties(updatedLinkTypes)

      // update collections and link types to get new versions (so that frontend updates store)
      val savedLinkTypes = updatedLinkTypes.map { task.daoContextSnapshot.linkTypeDao.updateLinkType(it.id, it, null, false) }

      // update saved changes in changes tracker
      changesTracker.updateLinkTypesMap(savedLinkTypes.associateBy { it.id })
      changesTracker.linkTypes.addAll(savedLinkTypes)
   }

   private fun removeLinks(removedLinks: List<LinkInstance>, linkTypes: Map<String, LinkType>) {
      changesTracker.addRemovedLinkInstances(removedLinks)

      task.daoContextSnapshot.linkInstanceDao.deleteLinkInstances(removedLinks.map { it.id })
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

}