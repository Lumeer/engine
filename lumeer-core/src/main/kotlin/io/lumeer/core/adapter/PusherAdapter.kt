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

   fun checkViewPermissionsChange(organization: Organization, project: Project, user: User, originalView: View, updatedView: View): List<Event> {
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
         removedUsers.forEach { notifications.add(createEventForResource(organization.id, project.id, updatedView, PusherFacade.REMOVE_EVENT_SUFFIX, permissionAdapter.getUser(it))) }
         if (collectionsInView.isNotEmpty()) {
            collectionsInView.forEach { notifications.addAll(createRemoveCollectionNotification(organization, project, it, removedUsers, views, linkTypesInView)) }
         }
         if (linkTypesInView.isNotEmpty()) {
            notifications.addAll(createRemoveCollectionLinkTypesNotification(organization, project, linkTypesInView, allCollections, removedUsers, viewsWithoutUpdated))
         }
      }

      for (userId in addedUsers) {
         val linkTypesByUser = filterNewLinkTypesForUser(userId, linkTypesInView, viewsWithoutUpdated, allLinkTypes, allCollections)
         notifications.addAll(linkTypesByUser
               .map { createEventForWorkspaceObject(organization.id, project.id, it, it.id, PusherFacade.UPDATE_EVENT_SUFFIX, userId) })

         val collectionsByUser = filterNewCollectionsForUser(userId, collectionsInView, viewsWithoutUpdated, allLinkTypes)
         notifications.addAll(collectionsByUser
               .map { createEventForObjectWithParent(ObjectWithParent(it, organization.id, project.id),
                        getResourceId(organization.id, project.id, it),
                        PusherFacade.UPDATE_EVENT_SUFFIX, userId)
               })
      }

      return notifications
   }

   private fun createRemoveCollectionNotification(organization: Organization, project: Project, collection: Collection, userIds: Set<String>, views: List<View>, linkTypes: List<LinkType>): List<Event> {
      val notifications = mutableListOf<Event>()
      val usersWithRights = ResourceUtils.getReaders(collection)
      for (userId in userIds) { // checks if user has collection in some view
         if (usersWithRights.contains(userId)) {
            continue
         }
         val viewsByUser = views.filter { permissionAdapter.hasRole(it, Role.READ, userId) }
         val collectionIdsInViews = viewsByUser.flatMap { QueryUtils.getQueryCollectionIds(it.query, linkTypes) }
         if (!collectionIdsInViews.contains(collection.id)) {
            notifications.add(createEventForResource(organization.id, project.id, collection, PusherFacade.REMOVE_EVENT_SUFFIX, permissionAdapter.getUser(userId)))
         }
      }
      return notifications
   }

   private fun createRemoveCollectionLinkTypesNotification(organization: Organization, project: Project, linkTypes: List<LinkType>, collections: List<Collection>, userIds: Set<String>, views: List<View>): List<Event> {
      val notifications = mutableListOf<Event>()
      for (userId in userIds) {
         filterLinkTypesNotInViews(linkTypes, collections, views, userId, true).forEach {
            notifications.add(createEventForRemove(it.javaClass.simpleName, ResourceId(it.id, organization.id, project.id), userId))
         }
      }
      return notifications
   }

   private fun filterLinkTypesNotInViews(linkTypes: List<LinkType>, collections: List<Collection>, views: List<View>, userId: String, includeReadableCollections: Boolean): List<LinkType> {
      val viewsByUser = views.filter { permissionAdapter.hasRole(it, Role.READ, userId) }
      val linkTypeIdsInViews = viewsByUser.flatMap { it.query.linkTypeIds }
      if (includeReadableCollections) {
         val readableCollectionsIds = filterViewsReadableCollectionsIds(viewsByUser, linkTypes, collections, userId)
         return linkTypes.filter { !linkTypeIdsInViews.contains(it.id) && !readableCollectionsIds.containsAll(it.collectionIds)}
      }
      return linkTypes.filter { !linkTypeIdsInViews.contains(it.id) }
   }

   private fun filterNewCollectionsForUser(userId: String, possibleCollections: List<Collection>, views: List<View>, linkTypes: List<LinkType>): List<Collection> {
      val collectionIdsInViews = views.filter { permissionAdapter.hasRole(it, Role.READ, userId) }
            .flatMap {QueryUtils.getQueryCollectionIds(it.query, linkTypes) }

      return possibleCollections.filter { !permissionAdapter.hasRole(it, Role.READ, userId) && !collectionIdsInViews.contains(it.id) }
   }

   private fun filterNewLinkTypesForUser(userId: String, possibleLinkTypes: List<LinkType>, views: List<View>, linkTypes: List<LinkType>, collections: List<Collection>): List<LinkType> {
      val linkTypeIdsInViews = views.filter { permissionAdapter.hasRole(it, Role.READ, userId) }
            .flatMap { it.query.linkTypeIds }
      val collectionMap = collections.associateBy { it.id }
      val newLinkTypes = possibleLinkTypes
            .filter { !permissionAdapter.hasLinkTypeRole(it, collectionMap, Role.READ, userId) && !linkTypeIdsInViews.contains(it.id) }
            .toMutableList()
      newLinkTypes.addAll(linkTypesByViewReadPermission(userId, views, collections, linkTypes))
      return newLinkTypes
   }

   private fun linkTypesByViewReadPermission(userId: String, views: List<View>, collections: List<Collection>, linkTypes: List<LinkType>): List<LinkType> {
      val viewsByUser = views.filter { permissionAdapter.hasRole(it, Role.READ, userId) }
      val collectionsIdsByViews = filterViewsReadableCollectionsIds(viewsByUser, linkTypes, collections, userId)
      val linkTypesByViews = linkTypes.filter { collectionsIdsByViews.containsAll(it.collectionIds) }.toMutableList()
      val collectionsIdsByViewsWithoutCurrent = filterViewsReadableCollectionsIds(views, linkTypes, collections, userId)
      val linkTypesByViewsWithoutCurrent = linkTypes.filter { collectionsIdsByViewsWithoutCurrent.containsAll(it.collectionIds) }
      linkTypesByViews.removeAll(linkTypesByViewsWithoutCurrent)
      return linkTypesByViews
   }

   private fun filterViewsReadableCollectionsIds(views: List<View>, linkTypes: List<LinkType>, collections: List<Collection>, userId: String): Set<String> {
      val readableCollectionsIds = QueryUtils.getViewsCollectionIds(views, linkTypes)
      readableCollectionsIds.addAll(collections.filter { permissionAdapter.hasRole(it, Role.READ, userId) }.map {  it.id })
      return readableCollectionsIds
   }

   private fun createEvent(organizationId: String, projectId: String, any: Any, event: String, user: User): Event {
      return if (any is Document) {
         createEventForWorkspaceObject(organizationId, projectId, any, any.id, event, user.id)
      } else if (any is LinkType) {
         createEventForWorkspaceObject(organizationId, projectId, any, any.id, event, user.id)
      } else if (any is LinkInstance) {
         createEventForWorkspaceObject(organizationId, projectId, any, any.id, event, user.id)
      } else if (any is Resource) {
         createEventForResource(organizationId, projectId, any, event, user)
      } else if (any is ResourceComment) {
         createEventForWorkspaceObject(organizationId, projectId, any, any.id, event, user.id)
      } else if (any is ObjectWithParent) {
         if (any.`object` is Resource) {
            createEventForNestedResource(organizationId, projectId, any, event, user)
         } else {
            createEventForObjectWithParent(any, event, user.id)
         }
      } else {
         createEventForObject(any, event, user.id)
      }
   }

   private fun createEventForWorkspaceObject(organizationId: String, projectId: String, any: Any, id: String, event: String, userId: String): Event {
      if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         return createEventForRemove(any.javaClass.simpleName, ResourceId(id, organizationId, projectId), userId)
      }
      val normalMessage = ObjectWithParent(any, organizationId, projectId)
      var extraId: String? = null
      if (any is Document) {
         extraId = any.collectionId
      } else if (any is LinkInstance) {
         extraId = any.linkTypeId
      } else if (any is ResourceComment) {
         val comment = any
         extraId = comment.resourceType.toString() + '/' + comment.resourceId
      }
      val alternateMessage = ResourceId(id, organizationId, projectId, extraId)
      return createEventForObjectWithParent(normalMessage, alternateMessage, event, userId)
   }

   private fun createEventForRemove(className: String, any: ResourceId, userId: String): Event {
      return Event(PusherFacade.eventChannel(userId), className + PusherFacade.REMOVE_EVENT_SUFFIX, any, null)
   }

   private fun createEventForResource(organizationId: String, projectId: String, resource: Resource, event: String, user: User): Event {
      return if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         createEventForRemove(resource.javaClass.simpleName, getResourceId(organizationId, projectId, resource), user.id)
      } else createEventForObject(filterUserRoles(user, resource), getResourceId(organizationId, projectId, resource), event, user.id)
   }

   private fun createEventForObject(any: Any, event: String, userId: String): Event {
      return Event(eventChannel(userId), any.javaClass.simpleName + event, any)
   }

   private fun createEventForObject(`object`: Any, backupObject: Any, event: String, userId: String): BackupDataEvent {
      return BackupDataEvent(eventChannel(userId), `object`.javaClass.simpleName + event, `object`, backupObject, null)
   }

   private fun createEventForNestedResource(organizationId: String, projectId: String, objectWithParent: ObjectWithParent, event: String, user: User): Event {
      val resource = objectWithParent.`object` as Resource
      if (PusherFacade.REMOVE_EVENT_SUFFIX == event) {
         return createEventForRemove(resource.javaClass.simpleName, getResourceId(organizationId, projectId, resource), user.id)
      }
      val filteredResource: Resource = filterUserRoles<Resource>(user, resource)
      val newObjectWithParent = ObjectWithParent(filteredResource, objectWithParent.organizationId, objectWithParent.projectId)
      newObjectWithParent.correlationId = objectWithParent.correlationId
      return createEventForObjectWithParent(newObjectWithParent, getResourceId(organizationId, projectId, resource), event, user.id)
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

   private fun getResourceId(organizationId: String, projectId: String, resource: Resource): ResourceId {
      if (resource is Organization) {
         return ResourceId(resource.getId(), null, null)
      } else if (resource is Project) {
         return ResourceId(resource.getId(), organizationId, null)
      }
      return ResourceId(resource.id, organizationId, projectId)
   }

   private fun <T : Resource> filterUserRoles(user: User, resource: T): T {
      return facadeAdapter.mapResource(resource.copy(), user)
   }

}
