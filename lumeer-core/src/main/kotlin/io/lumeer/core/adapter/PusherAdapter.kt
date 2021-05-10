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
import io.lumeer.api.util.ResourceUtils
import io.lumeer.core.facade.PusherFacade
import io.lumeer.core.facade.PusherFacade.ObjectWithParent
import io.lumeer.core.facade.PusherFacade.ResourceId
import io.lumeer.core.util.QueryUtils
import io.lumeer.storage.api.dao.CollectionDao
import io.lumeer.storage.api.dao.LinkTypeDao
import io.lumeer.storage.api.dao.ViewDao
import org.marvec.pusher.data.BackupDataEvent
import org.marvec.pusher.data.Event

class PusherAdapter(
      private val facadeAdapter: FacadeAdapter,
      private val permissionAdapter: PermissionAdapter,
      private val viewDao: ViewDao,
      private val linkTypeDao: LinkTypeDao,
      private val collectionDao: CollectionDao,
) {

   fun checkViewPermissionsChange(organization: Organization?, project: Project?, user: User, originalView: View, updatedView: View): List<Event> {
      val projectManagers = ResourceUtils.getProjectManagers(organization, project)
      val removedUsers = ResourceUtils.getRemovedPermissions(originalView, updatedView)
      removedUsers.removeAll(projectManagers)
      removedUsers.remove(user.id)

      val addedUsers = ResourceUtils.getAddedPermissions(originalView, updatedView)
      addedUsers.removeAll(projectManagers)
      addedUsers.remove(user.id)

      if (removedUsers.isEmpty() && addedUsers.isEmpty()) {
         return listOf()
      }

      val views = viewDao.allViews
      val viewsWithoutUpdated = views.filter {  it.id != updatedView.id }
      val allCollections = collectionDao.allCollections
      val allLinkTypes = linkTypeDao.allLinkTypes
      val linkTypeIds = updatedView.query.linkTypeIds
      val linkTypesInView = allLinkTypes.filter {  linkTypeIds.contains(it.id) }
      val collectionIds = QueryUtils.getQueryCollectionIds(updatedView.query, linkTypesInView)
      val collectionsInView = allCollections.filter { collectionIds.contains(it.id) }
      val notifications: MutableList<Event> = ArrayList()

      if (removedUsers.size > 0) {
         removedUsers.forEach { notifications.add(createEventForResource(organization, project, updatedView, PusherFacade.REMOVE_EVENT_SUFFIX, permissionAdapter.getUser(it))) }
         if (collectionsInView.isNotEmpty()) {
            collectionsInView.forEach { notifications.addAll(createRemoveCollectionNotification(organization, project, it, removedUsers, views, allLinkTypes)) }
         }
         if (linkTypesInView.isNotEmpty()) {
            notifications.addAll(createRemoveCollectionLinkTypesNotification(organization, project, linkTypesInView, allCollections, removedUsers, viewsWithoutUpdated))
         }
      }

      for (userId in addedUsers) {
         val linkTypesByUser = filterNewLinkTypesForUser(organization, project, userId, linkTypesInView, viewsWithoutUpdated, allLinkTypes, allCollections)
         notifications.addAll(linkTypesByUser
               .map { createEventForWorkspaceObject(organization, project, it, it.id, PusherFacade.UPDATE_EVENT_SUFFIX, userId) })

         val collectionsByUser = filterNewCollectionsForUser(organization, project, userId, collectionsInView, viewsWithoutUpdated, allLinkTypes)
         notifications.addAll(collectionsByUser
               .map { createEventForObjectWithParent(ObjectWithParent(it, organization?.id.orEmpty(), project?.id.orEmpty()),
                        getResourceId(organization, project, it),
                        PusherFacade.UPDATE_EVENT_SUFFIX, userId)
               })
      }

      return notifications
   }

   private fun createRemoveCollectionNotification(organization: Organization?, project: Project?, collection: Collection, userIds: Set<String>, views: List<View>, linkTypes: List<LinkType>): List<Event> {
      val notifications = mutableListOf<Event>()
      val usersWithRights = ResourceUtils.getReaders(collection)
      for (userId in userIds) { // checks if user has collection in some view
         if (usersWithRights.contains(userId)) {
            continue
         }
         val viewsByUser = views.filter { permissionAdapter.hasRole(organization, project, it, Role.READ, userId) }
         val collectionIdsInViews = viewsByUser.flatMap { QueryUtils.getQueryCollectionIds(it.query, linkTypes) }
         if (!collectionIdsInViews.contains(collection.id)) {
            notifications.add(createEventForResource(organization, project, collection, PusherFacade.REMOVE_EVENT_SUFFIX, permissionAdapter.getUser(userId)))
         }
      }
      return notifications
   }

   private fun createRemoveCollectionLinkTypesNotification(organization: Organization?, project: Project?, linkTypes: List<LinkType>, collections: List<Collection>, userIds: Set<String>, views: List<View>): List<Event> {
      val notifications = mutableListOf<Event>()
      for (userId in userIds) {
         filterLinkTypesNotInViews(organization, project, linkTypes, collections, views, userId, true).forEach {
            notifications.add(createEventForRemove(it.javaClass.simpleName, ResourceId(it.id, organization?.id.orEmpty(), project?.id.orEmpty()), userId))
         }
      }
      return notifications
   }

   private fun filterLinkTypesNotInViews(organization: Organization?, project: Project?, linkTypes: List<LinkType>, collections: List<Collection>, views: List<View>, userId: String, includeReadableCollections: Boolean): List<LinkType> {
      val viewsByUser = views.filter { permissionAdapter.hasRole(organization, project, it, Role.READ, userId) }
      val linkTypeIdsInViews = viewsByUser.flatMap { it.query.linkTypeIds }
      if (includeReadableCollections) {
         val readableCollectionsIds = filterViewsReadableCollectionsIds(organization, project, viewsByUser, linkTypes, collections, userId)
         return linkTypes.filter { !linkTypeIdsInViews.contains(it.id) && !readableCollectionsIds.containsAll(it.collectionIds)}
      }
      return linkTypes.filter { !linkTypeIdsInViews.contains(it.id) }
   }

   private fun filterNewCollectionsForUser(organization: Organization?, project: Project?, userId: String, possibleCollections: List<Collection>, views: List<View>, linkTypes: List<LinkType>): List<Collection> {
      val collectionIdsInViews = views.filter { permissionAdapter.hasRole(organization, project, it, Role.READ, userId) }
            .flatMap {QueryUtils.getQueryCollectionIds(it.query, linkTypes) }

      return possibleCollections.filter { !permissionAdapter.hasRole(organization, project, it, Role.READ, userId) && !collectionIdsInViews.contains(it.id) }
   }

   private fun filterNewLinkTypesForUser(organization: Organization?, project: Project?, userId: String, possibleLinkTypes: List<LinkType>, views: List<View>, linkTypes: List<LinkType>, collections: List<Collection>): List<LinkType> {
      val linkTypeIdsInViews = views.filter { permissionAdapter.hasRole(organization, project, it, Role.READ, userId) }
            .flatMap { it.query.linkTypeIds }
      val collectionMap = collections.associateBy { it.id }
      val newLinkTypes = possibleLinkTypes
            .filter { !permissionAdapter.hasLinkTypeRole(organization, project, it, collectionMap, Role.READ, userId) && !linkTypeIdsInViews.contains(it.id) }
            .toMutableList()
      newLinkTypes.addAll(linkTypesByViewReadPermission(organization, project, userId, views, collections, linkTypes))
      return newLinkTypes
   }

   private fun linkTypesByViewReadPermission(organization: Organization?, project: Project?, userId: String, views: List<View>, collections: List<Collection>, linkTypes: List<LinkType>): List<LinkType> {
      val viewsByUser = views.filter { permissionAdapter.hasRole(organization, project, it, Role.READ, userId) }
      val collectionsIdsByViews = filterViewsReadableCollectionsIds(organization, project, viewsByUser, linkTypes, collections, userId)
      val linkTypesByViews = linkTypes.filter { collectionsIdsByViews.containsAll(it.collectionIds) }.toMutableList()
      val collectionsIdsByViewsWithoutCurrent = filterViewsReadableCollectionsIds(organization, project, views, linkTypes, collections, userId)
      val linkTypesByViewsWithoutCurrent = linkTypes.filter { collectionsIdsByViewsWithoutCurrent.containsAll(it.collectionIds) }
      linkTypesByViews.removeAll(linkTypesByViewsWithoutCurrent)
      return linkTypesByViews
   }

   private fun filterViewsReadableCollectionsIds(organization: Organization?, project: Project?, views: List<View>, linkTypes: List<LinkType>, collections: List<Collection>, userId: String): Set<String> {
      val readableCollectionsIds = QueryUtils.getViewsCollectionIds(views, linkTypes)
      readableCollectionsIds.addAll(collections.filter { permissionAdapter.hasRole(organization, project, it, Role.READ, userId) }.map {  it.id })
      return readableCollectionsIds
   }

   private fun createEvent(organization: Organization?, project: Project?, any: Any, event: String, user: User): Event {
      return if (any is Document) {
         createEventForWorkspaceObject(organization, project, any, any.id, event, user.id)
      } else if (any is LinkType) {
         createEventForWorkspaceObject(organization, project, any, any.id, event, user.id)
      } else if (any is LinkInstance) {
         createEventForWorkspaceObject(organization, project, any, any.id, event, user.id)
      } else if (any is Resource) {
         createEventForResource(organization, project, any, event, user)
      } else if (any is ResourceComment) {
         createEventForWorkspaceObject(organization, project, any, any.id, event, user.id)
      } else if (any is ObjectWithParent) {
         if (any.`object` is Resource) {
            createEventForNestedResource(organization, project, any, event, user)
         } else {
            createEventForObjectWithParent(any, event, user.id)
         }
      } else {
         createEventForObject(any, event, user.id)
      }
   }

   private fun createEventForWorkspaceObject(organization: Organization?, project: Project?, any: Any, id: String, event: String, userId: String): Event {
      val organizationId = organization?.id.orEmpty()
      val projectId = project?.id.orEmpty()
      if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         return createEventForRemove(any.javaClass.simpleName, ResourceId(id, organizationId, projectId), userId)
      }
      val normalMessage = ObjectWithParent(any, organizationId, projectId)
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

   private fun createEventForRemove(className: String, any: ResourceId, userId: String): Event {
      return Event(PusherFacade.eventChannel(userId), className + PusherFacade.REMOVE_EVENT_SUFFIX, any, null)
   }

   private fun createEventForResource(organization: Organization?, project: Project?, resource: Resource, event: String, user: User): Event {
      return if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         createEventForRemove(resource.javaClass.simpleName, getResourceId(organization, project, resource), user.id)
      } else createEventForObject(filterUserRoles(organization, project, user, resource), getResourceId(organization, project, resource), event, user.id)
   }

   private fun createEventForObject(any: Any, event: String, userId: String): Event {
      return Event(eventChannel(userId), any.javaClass.simpleName + event, any)
   }

   private fun createEventForObject(`object`: Any, backupObject: Any, event: String, userId: String): BackupDataEvent {
      return BackupDataEvent(eventChannel(userId), `object`.javaClass.simpleName + event, `object`, backupObject, null)
   }

   private fun createEventForNestedResource(organization: Organization?, project: Project?, objectWithParent: ObjectWithParent, event: String, user: User): Event {
      val resource = objectWithParent.`object` as Resource
      if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         return createEventForRemove(resource.javaClass.simpleName, getResourceId(organization, project, resource), user.id)
      }
      val filteredResource = filterUserRoles(organization, project, user, resource)
      val newObjectWithParent = ObjectWithParent(filteredResource, objectWithParent.organizationId, objectWithParent.projectId)
      newObjectWithParent.correlationId = objectWithParent.correlationId
      return createEventForObjectWithParent(newObjectWithParent, getResourceId(organization, project, resource), event, user.id)
   }

   private fun createEventForObjectWithParent(objectWithParent: ObjectWithParent, event: String, userId: String): Event {
      return Event(eventChannel(userId), objectWithParent.`object`.javaClass.simpleName + event, objectWithParent)
   }

   private fun createEventForObjectWithParent(objectWithParent: ObjectWithParent, resourceId: ResourceId, event: String, userId: String): BackupDataEvent {
      return BackupDataEvent(eventChannel(userId), objectWithParent.`object`.javaClass.simpleName + event, objectWithParent, resourceId, null)
   }

   private fun eventChannel(userId: String): String? {
      return PusherFacade.PRIVATE_CHANNEL_PREFIX + userId
   }

   private fun getResourceId(organization: Organization?, project: Project?, resource: Resource): ResourceId {
      if (resource is Organization) {
         return ResourceId(resource.getId(), null, null)
      } else if (resource is Project) {
         return ResourceId(resource.getId(), organization?.id.orEmpty(), null)
      }
      return ResourceId(resource.id, organization?.id.orEmpty(), project?.id.orEmpty())
   }

   private fun <T : Resource> filterUserRoles(organization: Organization?, project: Project?, user: User, resource: T): T {
      return facadeAdapter.mapResource(organization, project, resource.copy(), user)
   }

}
