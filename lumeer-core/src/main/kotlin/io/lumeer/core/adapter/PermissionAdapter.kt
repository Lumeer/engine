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
import io.lumeer.api.util.PermissionUtils
import io.lumeer.api.util.ResourceUtils
import io.lumeer.core.exception.NoDocumentPermissionException
import io.lumeer.core.exception.NoPermissionException
import io.lumeer.core.exception.NoResourcePermissionException
import io.lumeer.core.util.DocumentUtils
import io.lumeer.core.util.QueryUtils
import io.lumeer.storage.api.dao.*

class PermissionAdapter(private val userDao: UserDao,
                        private val groupDao: GroupDao,
                        private val viewDao: ViewDao,
                        private val linkTypeDao: LinkTypeDao,
                        private val collectionDao: CollectionDao) {

   private val usersCache = mutableMapOf<String, List<User>>()
   private val hasRoleCache = mutableMapOf<String, Boolean>()
   private val viewCache = mutableMapOf<String, View>()
   private val userCache = mutableMapOf<String, User>()
   private val groupsCache = mutableMapOf<String, List<Group>>()
   private val linkTypes = lazy { linkTypeDao.allLinkTypes }

   private var currentViewId: String? = null

   fun setViewId(viewId: String) {
      currentViewId = viewId
   }

   fun getViewId() = currentViewId

   fun activeView(): View? {
      if (currentViewId.orEmpty().isNotEmpty()) {
         return getView(currentViewId!!)
      }

      return null
   }

   fun invalidateCache(resource: Resource) {
      for (role in RoleType.values()) {
         hasRoleCache.remove(resource.id + ":" + role.toString())
      }
   }

   fun isPublic(organization: Organization?, project: Project?) = project?.isPublic ?: false

   fun canReadAllInWorkspace(organization: Organization, project: Project?, userId: String): Boolean {
      val user = getUser(userId)
      val groups = PermissionUtils.getUserGroups(organization, user)
      if (PermissionUtils.getUserRolesInResource(organization, user, groups).any { role -> role.isTransitive && role.roleType === RoleType.Read }) {
         return true
      }
      return project?.let { PermissionUtils.getUserRolesInResource(it, user, groups).any { role -> role.isTransitive && role.roleType === RoleType.Read }  } ?: false
   }

   fun <T : Resource> getResourceUsersByRole(organization: Organization, project: Project?, resource: T, roleType: RoleType): Set<String> {
      return PermissionUtils.getResourceUsersByRole(organization, project, resource, getUsers(organization.id), roleType)
   }

   fun <T : Resource> getUserRolesInResource(organization: Organization?, project: Project?, resource: T, userId: String): Set<RoleType> {
      return PermissionUtils.getUserRolesInResource(organization, project, resource, getUser(userId))
   }

   private fun checkReadWorkspace(organization: Organization?, project: Project?, resource: Resource, userId: String) {
      if (resource !is Organization && organization != null) {
         if (!hasRole(organization, project, organization, RoleType.Read, userId)) {
            throw NoResourcePermissionException(resource)
         }
         if (resource !is Project && project != null) {
            if (!hasRole(organization, project, project, RoleType.Read, userId)) {
               throw NoResourcePermissionException(resource)
            }
         }
      }
   }

   fun checkRole(organization: Organization?, project: Project?, resource: Resource, role: RoleType, userId: String) {
      checkReadWorkspace(organization, project, resource, userId)
      if (!hasRole(organization, project, resource, role, userId)) {
         throw NoResourcePermissionException(resource)
      }
   }

   fun checkRoleWithView(organization: Organization?, project: Project?, collection: Collection, role: RoleType, viewRole: RoleType, userId: String) {
      checkReadWorkspace(organization, project, collection, userId)
      if (!hasRoleWithView(organization, project, collection, role, viewRole, userId)) {
         throw NoResourcePermissionException(collection)
      }
   }

   fun hasAnyRoleInResource(organization: Organization?, project: Project?, resource: Resource, roles: Set<RoleType>, userId: String): Boolean {
      return roles.any { hasRole(organization, project, resource, it, userId) }
   }

   fun hasRoleWithView(organization: Organization?, project: Project?, collection: Collection, role: RoleType, viewRole: RoleType, userId: String): Boolean {
      return hasRole(organization, project, collection, role, userId) || hasResourceRoleViaView(organization, project, collection, role, viewRole, userId, activeView()?.id)
   }

   private fun hasResourceRoleViaView(organization: Organization?, project: Project?, collection: Collection, role: RoleType, viewRole: RoleType, userId: String, viewId: String?): Boolean {
      if (viewId.orEmpty().isNotEmpty()) {
         val view = getView(viewId!!)
         if (hasRole(organization, project, view, viewRole, userId)) { // do we have access to the view?
            val authorId = view.authorId.orEmpty()
            val collectionIds = QueryUtils.getQueryCollectionIds(view.query, linkTypes.value)
            if (collectionIds.contains(collection.id) && authorId.isNotEmpty()) { // does the view contain the resource?
               if (hasRole(organization, project, collection, role, authorId)) { // has the view author access to the resource?
                  return true // grant access
               }
            }
         }
      }
      return false
   }

   fun hasRoleInDocument(organization: Organization?, project: Project?, document: Document, collection: Collection, role: RoleType, userId: String): Boolean {
      return hasRole(organization, project, collection, role, userId) || hasRoleInDocument(document, collection, role, userId)
   }

   private fun hasRoleInDocument(document: Document?, collection: Collection, role: RoleType, userId: String): Boolean {
      return when (role) {
         RoleType.Read, RoleType.Write, RoleType.Contribute -> DocumentUtils.isTaskAssignedByUser(collection, document, getUser(userId).email)
         else -> false
      }
   }

   fun hasRoleInDocumentWithView(organization: Organization?, project: Project?, document: Document, collection: Collection, role: RoleType, viewRole: RoleType, userId: String): Boolean {
      return hasRoleWithView(organization, project, collection, role, viewRole, userId) || hasRoleInDocument(document, collection, role, userId)
   }

   fun checkRoleInDocument(organization: Organization?, project: Project?, document: Document, collection: Collection, role: RoleType, userId: String) {
      if (!hasRoleInDocument(organization, project, document, collection, role, userId)) {
         throw NoDocumentPermissionException(document)
      }
   }

   fun checkRoleInDocumentWithView(organization: Organization?, project: Project?, document: Document, collection: Collection, role: RoleType, viewRole: RoleType, userId: String) {
      if (!hasRoleInDocumentWithView(organization, project, document, collection, role, viewRole, userId)) {
         throw NoDocumentPermissionException(document)
      }
   }

   fun checkRoleInLinkTypeWithView(organization: Organization?, project: Project?, collectionIds: kotlin.collections.Collection<String>, role: RoleType, userId: String, strict: Boolean) {
      val collections = collectionDao.getCollectionsByIds(collectionIds)
      if (!strict && role == RoleType.Write) {
         val atLeastOneRead = collections.any { hasRoleWithView(organization, project, it, RoleType.Read, RoleType.Read, userId) }
         val atLeastOneWrite = collections.any { hasRoleWithView(organization, project, it, RoleType.Write, RoleType.Write, userId) }
         if (!atLeastOneRead || !atLeastOneWrite) {
            throw NoPermissionException("LinkType")
         }
      } else {
         for (collection in collections) {
            checkRoleWithView(organization, project, collection, role, role, userId)
         }
      }
   }

   fun hasRoleInLinkTypeWithView(organization: Organization?, project: Project?, linkType: LinkType, role: RoleType, userId: String): Boolean {
      val collections = collectionDao.getCollectionsByIds(linkType.collectionIds)
      var hasPermissions = true
      for (collection in collections) {
         hasPermissions = hasPermissions && hasRoleWithView(organization, project, collection, role, role, userId)
      }
      return hasPermissions
   }

   fun hasRoleInLinkType(organization: Organization?, project: Project?, linkType: LinkType, collectionMap: Map<String, Collection>, role: RoleType, userId: String): Boolean {
      val collections: List<Collection> = linkType.collectionIds.mapNotNull { collectionMap[it] }
      var hasPermissions = true
      for (collection in collections) {
         hasPermissions = hasPermissions && hasRole(organization, project, collection, role, userId)
      }
      return hasPermissions
   }

   fun hasRole(organization: Organization?, project: Project?, resource: Resource, role: RoleType, userId: String): Boolean {
      return hasRoleCache.computeIfAbsent(resource.id + ":" + role.toString()) { getUserRolesInResource(organization, project, resource, userId).contains(role) }
   }

   fun getUser(userId: String): User {
      return userCache.computeIfAbsent(userId) { userDao.getUserById(userId) }
   }

   fun getUsers(organizationId: String): List<User> {
      return usersCache.computeIfAbsent(organizationId) { userDao.getAllUsers(organizationId) }
   }

   fun getView(viewId: String): View {
      return viewCache.computeIfAbsent(viewId) { viewDao.getViewById(viewId) }
   }

   fun getGroups(organizationId: String): List<Group> {
      return groupsCache.computeIfAbsent(organizationId) { groupDao.allGroups }
   }

}
