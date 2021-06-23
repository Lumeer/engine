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
import io.lumeer.api.util.PermissionUtils

class FacadeAdapter(private val permissionAdapter: PermissionAdapter) {

   fun <T : Resource> mapResource(organization: Organization?, project: Project?, resource: T, user: User): T {
      return if (getUserAdmins(organization, project, resource).contains(user.id)) {
         resource
      } else resource.apply {
         val groups = if (resource is Organization) PermissionUtils.getUserGroups(resource, user, permissionAdapter.getGroups(resource.id)) else PermissionUtils.getUserGroups(organization, user, permissionAdapter.getGroups(organization!!.id))
         permissions = keepOnlyNecessaryPermissions(resource.permissions, resource.type, user, groups)
      }
   }

   fun mapLinkType(organization: Organization, project: Project?, linkType: LinkType, user: User): LinkType {
      if (linkType.permissionsType == LinkPermissionsType.Merge) {
         return linkType.apply { permissions = Permissions() }
      }
      return if (getUserAdmins(organization, project, linkType).contains(user.id)) {
         linkType
      } else linkType.apply {
         val groups = PermissionUtils.getUserGroups(organization, user, permissionAdapter.getGroups(organization.id))
         permissions = keepOnlyNecessaryPermissions(linkType.permissions, ResourceType.LINK_TYPE, user, groups)
      }
   }

   private fun <T : Resource> getUserAdmins(organization: Organization?, project: Project?, resource: T): Set<String> {
      if (resource is Organization) {
         return permissionAdapter.getOrganizationUsersByRole(resource, RoleType.UserConfig)
      } else if (organization != null) {
         return permissionAdapter.getResourceUsersByRole(organization, project, resource, RoleType.UserConfig)
      }
      return setOf()
   }

   private fun getUserAdmins(organization: Organization, project: Project?, linkType: LinkType): Set<String> {
      return permissionAdapter.getLinkTypeUsersByRole(organization, project, linkType, RoleType.UserConfig)
   }

   fun <T : Resource> keepStoredPermissions(resource: T, storedPermissions: Permissions) {
      resource.permissions?.updateUserPermissions(storedPermissions.userPermissions)
      resource.permissions?.updateGroupPermissions(storedPermissions.groupPermissions)
   }

   fun keepStoredPermissions(linkType: LinkType, storedPermissions: Permissions) {
      linkType.permissions?.updateUserPermissions(storedPermissions.userPermissions)
      linkType.permissions?.updateGroupPermissions(storedPermissions.groupPermissions)
   }

   fun <T : Resource> keepUnmodifiableFields(destinationResource: T, originalResource: T) {
      destinationResource.id = originalResource.id
      destinationResource.isNonRemovable = originalResource.isNonRemovable
   }

   fun <T : Resource> setupPublicPermissions(resource: T, userId: String): T {
      val userPermission = Permission(userId, HashSet())
      val currentUserPermission = resource.permissions.userPermissions.firstOrNull { it.id == userId } ?: userPermission
      resource.permissions.clear()
      currentUserPermission.roles.add(Role(RoleType.Read))
      resource.permissions.addUserPermissions(setOf(currentUserPermission))
      return resource
   }

   private fun keepOnlyNecessaryPermissions(permissions: Permissions?, resourceType: ResourceType, user: User, userGroups: Set<String>): Permissions? {
      val userPermission = permissions?.userPermissions?.filter { it.id == user.id }.orEmpty().toSet()
      val userGroupPermissions = permissions?.groupPermissions?.filter { userGroups.contains(it.id) }.orEmpty().toSet()

      // Some roles are necessary to compute roles in other resources (i.e. Content readers/managers on view sharing)
      val keepRoles = keepRoleTypes(resourceType)
      val userPermissions = filterOnlyNecessaryPermissions(permissions?.userPermissions, keepRoles)
      val groupPermissions = filterOnlyNecessaryPermissions(permissions?.groupPermissions, keepRoles)

      permissions?.clear()
      permissions?.updateUserPermissions(userPermissions)
      // Replace current user permissions
      permissions?.updateUserPermissions(userPermission)
      permissions?.updateGroupPermissions(groupPermissions)
      // Replace current user groups permissions
      permissions?.updateGroupPermissions(userGroupPermissions)
      return permissions
   }

   private fun filterOnlyNecessaryPermissions(permissions: Set<Permission>?, keepRoleTypes: Set<RoleType>): Set<Permission> {
      return permissions.orEmpty()
            .map { it.apply { roles.filter { role -> role.isTransitive || keepRoleTypes.contains(role.type) } } }
            .filter { it.roles.isNotEmpty() }
            .toSet()
   }

   private fun keepRoleTypes(resourceType: ResourceType): Set<RoleType> {
      return when (resourceType) {
         ResourceType.ORGANIZATION -> setOf(RoleType.Read)
         ResourceType.PROJECT -> setOf(RoleType.Read)
         else -> setOf()
      }
   }

}
