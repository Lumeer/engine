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
package io.lumeer.core.adapter

import io.lumeer.api.model.*
import io.lumeer.api.model.Collection
import io.lumeer.api.model.common.Resource
import io.lumeer.core.facade.PusherFacade
import io.lumeer.core.facade.PusherFacade.ObjectWithParent
import io.lumeer.core.facade.PusherFacade.ResourceId
import io.lumeer.storage.api.dao.CollectionDao
import io.lumeer.storage.api.dao.LinkTypeDao
import io.lumeer.storage.api.dao.ViewDao
import org.marvec.pusher.data.BackupDataEvent
import org.marvec.pusher.data.Event

class PusherAdapter(
      private val facadeAdapter: FacadeAdapter,
      private val resourceAdapter: ResourceAdapter,
      private val permissionAdapter: PermissionAdapter,
      private val viewDao: ViewDao,
      private val linkTypeDao: LinkTypeDao,
      private val collectionDao: CollectionDao,
) {

   fun checkLinkTypePermissionsChange(organization: Organization, project: Project?, user: User, originalLinkType: LinkType, updatedLinkType: LinkType): List<Event> {
      val rolesDifference = permissionAdapter.getLinkTypeReadersDifference(organization, project, originalLinkType, updatedLinkType)
      val changedUsers = rolesDifference.removedUsers.toMutableSet()
      changedUsers.addAll(rolesDifference.addedUsers)
      changedUsers.remove(user.id)

      if (changedUsers.isEmpty()) {
         return listOf()
      }

      val allLinkTypes = linkTypeDao.allLinkTypes
      val allViews = viewDao.allViews
      val allCollections = collectionDao.allCollections

      val collectionsBefore = allLinkTypes.toMutableList().plus(originalLinkType)
      val collectionsAfter = allLinkTypes.toMutableList().plus(updatedLinkType)

      return changedUsers.fold(mutableListOf()) { notifications, userId ->
         notifications.addAll(createCollectionsAndLinksChangeNotifications(organization, project, Pair(collectionsBefore, collectionsAfter), Pair(allCollections, allCollections), Pair(allViews, allViews), userId))
         return notifications
      }
   }

   fun checkCollectionsPermissionsChange(organization: Organization, project: Project?, user: User, originalCollection: Collection, updatedCollection: Collection): List<Event> {
      val rolesDifference = permissionAdapter.getResourceReadersDifference(organization, project, originalCollection, updatedCollection)
      val changedUsers = rolesDifference.removedUsers.toMutableSet()
      changedUsers.addAll(rolesDifference.addedUsers)
      changedUsers.remove(user.id)

      if (changedUsers.isEmpty()) {
         return listOf()
      }

      val allLinkTypes = linkTypeDao.allLinkTypes
      val allViews = viewDao.allViews
      val allCollections = collectionDao.allCollections

      val collectionsBefore = allCollections.toMutableList().plus(originalCollection)
      val collectionsAfter = allCollections.toMutableList().plus(updatedCollection)

      return changedUsers.fold(mutableListOf()) { notifications, userId ->
         notifications.addAll(createCollectionsAndLinksChangeNotifications(organization, project, Pair(allLinkTypes, allLinkTypes), Pair(collectionsBefore, collectionsAfter), Pair(allViews, allViews), userId))
         return notifications
      }
   }

   fun checkViewPermissionsChange(organization: Organization, project: Project?, user: User, originalView: View, updatedView: View): List<Event> {
      val rolesDifference = permissionAdapter.getResourceReadersDifference(organization, project, originalView, updatedView)
      val changedUsers = rolesDifference.removedUsers.toMutableSet()
      changedUsers.addAll(rolesDifference.addedUsers)
      changedUsers.remove(user.id)

      if (changedUsers.isEmpty()) {
         return listOf()
      }

      val allViews = viewDao.allViews
      val allCollections = collectionDao.allCollections
      val allLinkTypes = linkTypeDao.allLinkTypes

      val viewsBefore = allViews.toMutableList().plus(originalView)
      val viewsAfter = allViews.toMutableList().plus(updatedView)

      return changedUsers.fold(mutableListOf()) { notifications, userId ->
         notifications.addAll(createCollectionsAndLinksChangeNotifications(organization, project, Pair(allLinkTypes, allLinkTypes), Pair(allCollections, allCollections), Pair(viewsBefore, viewsAfter), userId))
         return notifications
      }
   }

   fun createCollectionsAndLinksChangeNotifications(organization: Organization, project: Project?, linkTypes: Pair<List<LinkType>, List<LinkType>>, collections: Pair<List<Collection>, List<Collection>>, views: Pair<List<View>, List<View>>, userId: String): List<Event> {
      val linkTypesBefore = resourceAdapter.getAllLinkTypes(organization, project, linkTypes.first, views.first, collections.first, userId).toMutableList()
      val linkTypesAfter = resourceAdapter.getAllLinkTypes(organization, project, linkTypes.second, views.second, collections.second, userId).toMutableList()

      val collectionsBefore = resourceAdapter.getAllCollections(organization, project, linkTypes.first, views.first, collections.first, userId).toMutableList()
      val collectionsAfter = resourceAdapter.getAllCollections(organization, project, linkTypes.second, views.second, collections.second, userId).toMutableList()

      val notifications = mutableListOf<Event>()

      val lostLinkTypes = linkTypesBefore.toMutableList().minus(linkTypesAfter)
      val gainedLinkTypes = linkTypesAfter.toMutableList().minus(linkTypesBefore)

      lostLinkTypes.forEach {
         createEventForRemove(it.javaClass.simpleName, ResourceId(it.id, organization.id.orEmpty(), project?.id.orEmpty()), userId)
      }
      gainedLinkTypes.forEach {
         createEventForWorkspaceObject(organization, project, filterUserRoles(organization, project, userId, it), it.id, PusherFacade.UPDATE_EVENT_SUFFIX, userId)
      }

      val lostCollections = collectionsBefore.toMutableList().minus(collectionsAfter)
      val gainedCollections = collectionsAfter.toMutableList().minus(collectionsBefore)

      lostCollections.forEach {
         createEventForRemove(it.javaClass.simpleName, ResourceId(it.id, organization.id.orEmpty(), project?.id.orEmpty()), userId)
      }
      gainedCollections.forEach {
         createEventForWorkspaceObject(organization, project, filterUserRoles(organization, project, userId, it), it.id, PusherFacade.UPDATE_EVENT_SUFFIX, userId)
      }

      return notifications
   }


   fun createEvent(organization: Organization, project: Project?, any: Any, event: String, userId: String): Event {
      return if (any is Document) {
         createEventForWorkspaceObject(organization, project, any, any.id, event, userId)
      } else if (any is LinkType) {
         createEventForWorkspaceObject(organization, project, any, any.id, event, userId)
      } else if (any is LinkInstance) {
         createEventForWorkspaceObject(organization, project, any, any.id, event, userId)
      } else if (any is Resource) {
         createEventForResource(organization, project, any, event, userId)
      } else if (any is ResourceComment) {
         createEventForWorkspaceObject(organization, project, any, any.id, event, userId)
      } else if (any is ObjectWithParent) {
         if (any.`object` is Resource) {
            createEventForNestedResource(organization, project, any, event, userId)
         } else {
            createEventForObjectWithParent(any, event, userId)
         }
      } else {
         createEventForObject(any, event, userId)
      }
   }

   fun createEventForWorkspaceObject(organization: Organization, project: Project?, any: Any, id: String, event: String, userId: String): Event {
      val organizationId = organization.id.orEmpty()
      val projectId = project?.id.orEmpty()
      if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         return createEventForRemove(any.javaClass.simpleName, ResourceId(id, organizationId, projectId), userId)
      }
      val normalMessage = if (any is LinkType) {
         ObjectWithParent(filterUserRoles(organization, project, userId, any), organizationId, projectId)
      } else {
         ObjectWithParent(any, organizationId, projectId)
      }
      val extraId = when (any) {
         is Document -> {
            any.collectionId
         }
         is LinkInstance -> {
            any.linkTypeId
         }
         is ResourceComment -> {
            any.resourceType.toString() + '/' + any.resourceId
         }
         else -> null
      }
      val alternateMessage = ResourceId(id, organizationId, projectId, extraId)
      return createEventForObjectWithParent(normalMessage, alternateMessage, event, userId)
   }

   fun createEventForRemove(className: String, any: ResourceId, userId: String): Event {
      return Event(PusherFacade.eventChannel(userId), className + PusherFacade.REMOVE_EVENT_SUFFIX, any, null)
   }

   fun createEventForResource(organization: Organization, project: Project?, resource: Resource, event: String, userId: String): Event {
      return if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         createEventForRemove(resource.javaClass.simpleName, getResourceId(organization, project, resource), userId)
      } else createEventForObject(filterUserRoles(organization, project, userId, resource), getResourceId(organization, project, resource), event, userId)
   }

   fun createEventForObject(any: Any, event: String, userId: String): Event {
      return Event(eventChannel(userId), any.javaClass.simpleName + event, any)
   }

   private fun createEventForObject(`object`: Any, backupObject: Any, event: String, userId: String): BackupDataEvent {
      return BackupDataEvent(eventChannel(userId), `object`.javaClass.simpleName + event, `object`, backupObject, null)
   }

   private fun createEventForNestedResource(organization: Organization, project: Project?, objectWithParent: ObjectWithParent, event: String, userId: String): Event {
      val resource = objectWithParent.`object` as Resource
      if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         return createEventForRemove(resource.javaClass.simpleName, getResourceId(organization, project, resource), userId)
      }
      val filteredResource = filterUserRoles(organization, project, userId, resource)
      val newObjectWithParent = ObjectWithParent(filteredResource, objectWithParent.organizationId, objectWithParent.projectId)
      newObjectWithParent.correlationId = objectWithParent.correlationId
      return createEventForObjectWithParent(newObjectWithParent, getResourceId(organization, project, resource), event, userId)
   }

   fun createEventForObjectWithParent(objectWithParent: ObjectWithParent, event: String, userId: String): Event {
      return Event(eventChannel(userId), objectWithParent.`object`.javaClass.simpleName + event, objectWithParent)
   }

   private fun createEventForObjectWithParent(objectWithParent: ObjectWithParent, resourceId: ResourceId, event: String, userId: String): BackupDataEvent {
      return BackupDataEvent(eventChannel(userId), objectWithParent.`object`.javaClass.simpleName + event, objectWithParent, resourceId, null)
   }

   private fun getResourceId(organization: Organization?, project: Project?, resource: Resource): ResourceId {
      if (resource is Organization) {
         return ResourceId(resource.getId(), null, null)
      } else if (resource is Project) {
         return ResourceId(resource.getId(), organization?.id.orEmpty(), null)
      }
      return ResourceId(resource.id, organization?.id.orEmpty(), project?.id.orEmpty())
   }

   private fun <T : Resource> filterUserRoles(organization: Organization, project: Project?, userId: String, resource: T): T {
      return facadeAdapter.mapResource(organization, project, resource.copy(), permissionAdapter.getUser(userId))
   }

   private fun filterUserRoles(organization: Organization, project: Project?, userId: String, linkType: LinkType): LinkType {
      return facadeAdapter.mapLinkType(organization, project, LinkType(linkType), permissionAdapter.getUser(userId))
   }

   companion object {

      @JvmStatic
      fun eventChannel(userId: String): String {
         return PusherFacade.PRIVATE_CHANNEL_PREFIX + userId
      }
   }

}
