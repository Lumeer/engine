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
      private val appId: AppId?,
      private val facadeAdapter: FacadeAdapter,
      private val resourceAdapter: ResourceAdapter,
      private val permissionAdapter: PermissionAdapter,
      private val viewDao: ViewDao,
      private val linkTypeDao: LinkTypeDao,
      private val collectionDao: CollectionDao,
) {

   fun checkLinkTypePermissionsChange(organization: Organization, project: Project?, user: User, originalLinkType: LinkType, updatedLinkType: LinkType): List<Event> {
      val rolesDifference = permissionAdapter.getLinkTypeReadersDifference(organization, project, originalLinkType, updatedLinkType)
      val changedUsers = rolesDifference.changedUsers().toMutableSet().minus(user.id)

      val removedReadersNotifications = rolesDifference.removedUsers
            .map { userId -> createEventForRemove(originalLinkType.javaClass.simpleName, ResourceId(appId, originalLinkType.id, organization.id.orEmpty(), project?.id.orEmpty()), userId) }
            .toMutableList()

      if (changedUsers.isEmpty()) {
         return removedReadersNotifications
      }

      val allLinkTypes = linkTypeDao.allLinkTypes.filter { it.id != originalLinkType.id }
      val allViews = viewDao.allViews
      val allCollections = collectionDao.allCollections

      val linkTypesBefore = allLinkTypes.plus(originalLinkType)
      val linkTypesAfter = allLinkTypes.plus(updatedLinkType)

      return changedUsers.fold(removedReadersNotifications) { notifications, userId ->
         notifications.addAll(createCollectionsChangeNotifications(organization, project, Pair(linkTypesBefore, linkTypesAfter), Pair(allCollections, allCollections), Pair(allViews, allViews), userId))
         return notifications
      }
   }

   fun checkCollectionsPermissionsChange(organization: Organization, project: Project?, user: User, originalCollection: Collection, updatedCollection: Collection): List<Event> {
      val rolesDifference = permissionAdapter.getResourceReadersDifference(organization, project, originalCollection, updatedCollection)
      val changedUsers = rolesDifference.changedUsers().toMutableSet().minus(user.id)

      val removedReadersNotifications = rolesDifference.removedUsers
            .map { userId -> createEventForRemove(originalCollection.javaClass.simpleName, ResourceId(appId, originalCollection.id, organization.id.orEmpty(), project?.id.orEmpty()), userId) }
            .toMutableList()

      if (changedUsers.isEmpty()) {
         return removedReadersNotifications
      }

      val allLinkTypes = linkTypeDao.allLinkTypes
      val allViews = viewDao.allViews
      val allCollections = collectionDao.allCollections.filter { it.id != originalCollection.id}

      val collectionsBefore = allCollections.plus(originalCollection)
      val collectionsAfter = allCollections.plus(updatedCollection)

      return changedUsers.fold(removedReadersNotifications) { notifications, userId ->
         notifications.addAll(createLinksChangeNotifications(organization, project, Pair(allLinkTypes, allLinkTypes), Pair(collectionsBefore, collectionsAfter), Pair(allViews, allViews), userId))
         return notifications
      }
   }

   fun checkViewPermissionsChange(organization: Organization, project: Project?, user: User, originalView: View?, updatedView: View): List<Event> {
      if (originalView == null) {
         return listOf()
      }

      val rolesDifference = permissionAdapter.getResourceReadersDifference(organization, project, originalView, updatedView)
      val changedUsers = rolesDifference.changedUsers().toMutableSet().minus(user.id)

      val removedReadersNotifications = rolesDifference.removedUsers
            .map { userId -> createEventForRemove(originalView.javaClass.simpleName, ResourceId(appId, originalView.id, organization.id.orEmpty(), project?.id.orEmpty()), userId) }
            .toMutableList()

      if (changedUsers.isEmpty()) {
         return removedReadersNotifications
      }

      val allViews = viewDao.allViews.filter { it.id != originalView.id }
      val allCollections = collectionDao.allCollections
      val allLinkTypes = linkTypeDao.allLinkTypes

      val viewsBefore = allViews.plus(originalView)
      val viewsAfter = allViews.plus(updatedView)

      return changedUsers.fold(removedReadersNotifications) { notifications, userId ->
         notifications.addAll(createCollectionsAndLinksChangeNotifications(organization, project, Pair(allLinkTypes, allLinkTypes), Pair(allCollections, allCollections), Pair(viewsBefore, viewsAfter), userId))
         return notifications
      }
   }

   private fun createCollectionsChangeNotifications(organization: Organization, project: Project?, linkTypes: Pair<List<LinkType>, List<LinkType>>, collections: Pair<List<Collection>, List<Collection>>, views: Pair<List<View>, List<View>>, userId: String): List<Event> {
      val collectionsBefore = resourceAdapter.getAllCollections(organization, project, linkTypes.first, views.first, collections.first, userId).toMutableList()
      val collectionsAfter = resourceAdapter.getAllCollections(organization, project, linkTypes.second, views.second, collections.second, userId).toMutableList()

      val notifications = mutableListOf<Event>()

      val lostCollections = collectionsBefore.toMutableSet().minus(collectionsAfter)
      val gainedCollections = collectionsAfter.toMutableSet().minus(collectionsBefore)

      lostCollections.forEach {
         notifications.add(createEventForRemove(it.javaClass.simpleName, ResourceId(appId, it.id, organization.id.orEmpty(), project?.id.orEmpty()), userId))
      }
      gainedCollections.forEach {
         notifications.add(createEventForWorkspaceObject(organization, project, filterUserRoles(organization, project, userId, it), it.id, PusherFacade.UPDATE_EVENT_SUFFIX, userId))
      }

      return notifications
   }

   private fun createLinksChangeNotifications(organization: Organization, project: Project?, linkTypes: Pair<List<LinkType>, List<LinkType>>, collections: Pair<List<Collection>, List<Collection>>, views: Pair<List<View>, List<View>>, userId: String): List<Event> {
      val linkTypesBefore = resourceAdapter.getAllLinkTypes(organization, project, linkTypes.first, views.first, collections.first, userId).toMutableList()
      val linkTypesAfter = resourceAdapter.getAllLinkTypes(organization, project, linkTypes.second, views.second, collections.second, userId).toMutableList()

      val notifications = mutableListOf<Event>()

      val lostLinkTypes = linkTypesBefore.toMutableSet().minus(linkTypesAfter)
      val gainedLinkTypes = linkTypesAfter.toMutableSet().minus(linkTypesBefore)

      lostLinkTypes.forEach {
         notifications.add(createEventForRemove(it.javaClass.simpleName, ResourceId(appId, it.id, organization.id.orEmpty(), project?.id.orEmpty()), userId))
      }
      gainedLinkTypes.forEach {
         notifications.add(createEventForWorkspaceObject(organization, project, filterUserRoles(organization, project, userId, it), it.id, PusherFacade.UPDATE_EVENT_SUFFIX, userId))
      }

      return notifications
   }

   private fun createCollectionsAndLinksChangeNotifications(organization: Organization, project: Project?, linkTypes: Pair<List<LinkType>, List<LinkType>>, collections: Pair<List<Collection>, List<Collection>>, views: Pair<List<View>, List<View>>, userId: String): List<Event> {
      return createCollectionsChangeNotifications(organization, project, linkTypes, collections, views, userId).plus(createLinksChangeNotifications(organization, project, linkTypes, collections, views, userId))
   }

   fun createEvent(organization: Organization?, project: Project?, any: Any, event: String, userId: String): Event {
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

   fun createEventForWorkspaceObject(organization: Organization?, project: Project?, any: Any, id: String, event: String, userId: String): Event {
      val organizationId = organization?.id.orEmpty()
      val projectId = project?.id.orEmpty()
      if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         return createEventForRemove(any.javaClass.simpleName, ResourceId(appId, id, organizationId, projectId), userId)
      }
      val normalMessage = if (any is LinkType) {
         ObjectWithParent(appId, filterUserRoles(organization, project, userId, any), organizationId, projectId)
      } else {
         ObjectWithParent(appId, any, organizationId, projectId)
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
      val alternateMessage = ResourceId(appId, id, organizationId, projectId, extraId)
      return createEventForObjectWithParent(normalMessage, alternateMessage, event, userId)
   }

   fun createEventForRemove(className: String, any: ResourceId, userId: String): Event {
      return Event(PusherFacade.eventChannel(userId), className + PusherFacade.REMOVE_EVENT_SUFFIX, any, null)
   }

   fun createEventForResource(organization: Organization?, project: Project?, resource: Resource, event: String, userId: String): Event {
      return if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         createEventForRemove(resource.javaClass.simpleName, getResourceId(organization, project, resource), userId)
      } else createEventForObject(filterUserRoles(organization, project, userId, resource), getResourceId(organization, project, resource), event, userId)
   }

   fun createEventForObject(any: Any, event: String, userId: String): Event {
      return Event(eventChannel(userId), any.javaClass.simpleName + event, any)
   }

   fun createEventForObject(`object`: Any, backupObject: Any, event: String, userId: String): BackupDataEvent {
      return BackupDataEvent(eventChannel(userId), `object`.javaClass.simpleName + event, `object`, backupObject, null)
   }

   private fun createEventForNestedResource(organization: Organization?, project: Project?, objectWithParent: ObjectWithParent, event: String, userId: String): Event {
      val resource = objectWithParent.`object` as Resource
      if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         return createEventForRemove(resource.javaClass.simpleName, getResourceId(organization, project, resource), userId)
      }
      val filteredResource = filterUserRoles(organization, project, userId, resource)
      val newObjectWithParent = ObjectWithParent(appId, filteredResource, objectWithParent.organizationId, objectWithParent.projectId)
      newObjectWithParent.correlationId = objectWithParent.correlationId
      return createEventForObjectWithParent(newObjectWithParent, getResourceId(organization, project, resource), event, userId)
   }

   fun createEventForObjectWithParent(objectWithParent: ObjectWithParent, event: String, userId: String): Event {
      return Event(eventChannel(userId), objectWithParent.`object`.javaClass.simpleName + event, objectWithParent)
   }

   fun createEventForObjectWithParent(objectWithParent: ObjectWithParent, backupObject: Any, event: String, userId: String): BackupDataEvent {
      return BackupDataEvent(eventChannel(userId), objectWithParent.`object`.javaClass.simpleName + event, objectWithParent, backupObject, null)
   }

   private fun getResourceId(organization: Organization?, project: Project?, resource: Resource): ResourceId {
      if (resource is Organization) {
         return ResourceId(appId, resource.getId(), null, null)
      } else if (resource is Project) {
         return ResourceId(appId, resource.getId(), organization?.id.orEmpty(), null)
      }
      return ResourceId(appId, resource.id, organization?.id.orEmpty(), project?.id.orEmpty())
   }

   private fun <T : Resource> filterUserRoles(organization: Organization?, project: Project?, userId: String, resource: T): T {
      return facadeAdapter.mapResource(organization, project, resource.copy(), permissionAdapter.getUser(userId))
   }

   private fun filterUserRoles(organization: Organization?, project: Project?, userId: String, linkType: LinkType): LinkType {
      return facadeAdapter.mapLinkType(organization, project, LinkType(linkType), permissionAdapter.getUser(userId))
   }

   companion object {

      @JvmStatic
      fun eventChannel(userId: String): String {
         return PusherFacade.PRIVATE_CHANNEL_PREFIX + userId
      }
   }

}
