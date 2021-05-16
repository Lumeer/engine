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
import io.lumeer.core.auth.PermissionsChecker

class FacadeAdapter {

   fun <T : Resource> getResourceManagers(organization: Organization?, project: Project?, resource: T): Set<String> = when (resource) {
      is Organization -> {
         ResourceUtils.getOrganizationManagers(resource)
      }
      is Project -> {
         ResourceUtils.getProjectManagers(organization, resource)
      }
      else -> {
         ResourceUtils.getResourceManagers(organization, project, resource)
      }
   }

   fun <T : Resource> mapResource(organization: Organization?, project: Project?, resource: T, user: User): T {
      return if (getResourceManagers(organization, project, resource).contains(user.id)) {
         resource
      } else keepOnlyActualUserRoles(organization, project, resource, user)
   }

   fun <T : Resource> keepStoredPermissions(resource: T, storedPermissions: Permissions) {
      resource.permissions.updateUserPermissions(storedPermissions.userPermissions)
      resource.permissions.updateGroupPermissions(storedPermissions.groupPermissions)
   }

   fun <T : Resource> keepUnmodifiableFields(destinationResource: T, originalResource: T) {
      destinationResource.isNonRemovable = originalResource.isNonRemovable
   }

   fun <T : Resource> setupPublicPermissions(resource: T, userId: String): T {
      val userPermission = Permission(userId, HashSet())
      val currentUserPermission = resource.permissions.userPermissions.firstOrNull { it.id == userId } ?: userPermission
      resource.permissions.clear()
      currentUserPermission.roles.add(Role.READ)
      resource.permissions.addUserPermissions(setOf(currentUserPermission))
      return resource
   }

   private fun <T : Resource> keepOnlyActualUserRoles(organization: Organization?, project: Project?, resource: T, user: User): T {
      val roles = PermissionsChecker.getActualRoles(organization, project, resource, user)
      val permission = Permission.buildWithRoles(user.id, roles)
      val managers = getResourceManagers(organization, project, resource)
      val keepReadRights = keepReadRights(resource)
      val managersUserPermission = resource.permissions.userPermissions
            .map { perm: Permission ->
               if (keepReadRights) {
                  if (managers.contains(perm.id)) perm
                  else Permission.buildWithRoles(perm.id, setOf(Role.READ))
               } else perm
            }
            .filter { keepReadRights || managers.contains(it.id) }
            .toSet()

      resource.permissions.clear()
      resource.permissions.updateUserPermissions(managersUserPermission)
      resource.permissions.updateUserPermissions(permission)
      return resource
   }

   private fun <T : Resource> keepReadRights(resource: T): Boolean {
      return resource is Organization || resource is Project
   }

}
