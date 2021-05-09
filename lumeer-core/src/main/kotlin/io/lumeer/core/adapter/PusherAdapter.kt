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
import io.lumeer.api.model.common.Resource
import io.lumeer.api.util.ResourceUtils
import io.lumeer.core.facade.PusherFacade
import io.lumeer.core.facade.PusherFacade.ObjectWithParent
import io.lumeer.core.facade.PusherFacade.ResourceId
import org.marvec.pusher.data.BackupDataEvent
import org.marvec.pusher.data.Event

class PusherAdapter(private val facadeAdapter: FacadeAdapter) {

   fun checkViewPermissionsChange(organization: Organization, project: Project, user: User, originalView: View, updatedView: View) {
      val projectManagers = ResourceUtils.getProjectManagers(organization, project)
      val removedUsers = ResourceUtils.getRemovedPermissions(originalView, updatedView)
      removedUsers.removeAll(projectManagers)
      removedUsers.remove(user.id)

      val addedUsers = ResourceUtils.getAddedPermissions(originalView, updatedView)
      addedUsers.removeAll(projectManagers)
      addedUsers.remove(user.id)

      if (removedUsers.isEmpty() && addedUsers.isEmpty()) {
         return
      }

      TODO("Complete method")

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
      } else createEventForObject(filterUserRoles<Resource>(user, resource), getResourceId(organizationId, projectId, resource), event, user.id)
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