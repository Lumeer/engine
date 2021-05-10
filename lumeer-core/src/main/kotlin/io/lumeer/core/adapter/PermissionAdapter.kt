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
import io.lumeer.core.exception.NoDocumentPermissionException
import io.lumeer.core.exception.NoPermissionException
import io.lumeer.core.exception.NoResourcePermissionException
import io.lumeer.core.util.DocumentUtils
import io.lumeer.core.util.QueryUtils
import io.lumeer.storage.api.dao.CollectionDao
import io.lumeer.storage.api.dao.LinkTypeDao
import io.lumeer.storage.api.dao.UserDao
import io.lumeer.storage.api.dao.ViewDao

class PermissionAdapter(private val userDao: UserDao,
                        private val viewDao: ViewDao,
                        private val linkTypeDao: LinkTypeDao,
                        private val collectionDao: CollectionDao,
                        private val currentUser: User?) {

   private val hasRoleCache = mutableMapOf<String, Boolean>()
   private val viewCache = mutableMapOf<String, View>()
   private val userCache = mutableMapOf<String, User>()
   private val linkTypes = lazy { linkTypeDao.allLinkTypes }
   private val currentView: View? = null

   private var currentViewId: String? = null

   init {
      if (currentUser != null) userCache[currentUser.id] = currentUser
   }

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
      for (role in Role.values()) {
         hasRoleCache.remove(resource.id + ":" + role.toString())
      }
   }

   fun isManager(organization: Organization?, project: Project?, userId: String) = ResourceUtils.userIsManagerInWorkspace(userId, organization, project)

   fun isPublic(organization: Organization?, project: Project?) = project?.isPublic ?: false

   fun checkRole(organization: Organization?, project: Project?, resource: Resource, role: Role, userId: String) {
      if (isManager(organization, project, userId)) {
         return
      }
      checkOrganizationAndProject(organization, project, resource, Role.READ, userId)
      if (!hasRoleInResource(organization, project, resource, role, userId)) {
         throw NoResourcePermissionException(resource)
      }
   }

   fun checkRoleWithView(organization: Organization?, project: Project?, collection: Collection, role: Role, viewRole: Role, userId: String, viewId: String? = null) {
      if (isManager(organization, project, userId)) {
         return
      }
      checkOrganizationAndProject(organization, project, collection, Role.READ, userId)
      if (!hasRoleWithView(organization, project, collection, role, viewRole, userId, viewId)) {
         throw NoResourcePermissionException(collection)
      }
   }

   fun checkLinkTypeRoleWithView(organization: Organization?, project: Project?, collectionIds: kotlin.collections.Collection<String>, role: Role, userId: String, strict: Boolean, viewId: String? = null) {
      val collections = collectionDao.getCollectionsByIds(collectionIds)
      if (!strict && role == Role.WRITE) {
         val atLeastOneRead = collections.any { hasRoleWithView(organization, project, it, Role.READ, Role.READ, userId, viewId) }
         val atLeastOneWrite = collections.any { hasRoleWithView(organization, project, it, Role.WRITE, Role.WRITE, userId, viewId) }
         if (!atLeastOneRead || !atLeastOneWrite) {
            throw NoPermissionException("LinkType")
         }
      } else {
         for (collection in collections) {
            checkRoleWithView(organization, project, collection, role, role, userId, viewId)
         }
      }
   }

   fun hasLinkTypeRoleWithView(organization: Organization?, project: Project?, linkType: LinkType, role: Role, userId: String, viewId: String? = null): Boolean {
      val collections = collectionDao.getCollectionsByIds(linkType.collectionIds)
      var hasPermissions = true
      for (collection in collections) {
         hasPermissions = hasPermissions && hasRoleWithView(organization, project, collection, role, role, userId, viewId)
      }
      return hasPermissions
   }

   fun hasLinkTypeRole(organization: Organization?, project: Project?, linkType: LinkType, collectionMap: Map<String, Collection>, role: Role, userId: String): Boolean {
      val collections: List<Collection> = linkType.collectionIds.mapNotNull { collectionMap[it] }
      var hasPermissions = true
      for (collection in collections) {
         hasPermissions = hasPermissions && hasRole(organization, project, collection, role, userId)
      }
      return hasPermissions
   }

   fun hasRole(organization: Organization?, project: Project?, resource: Resource, role: Role, userId: String): Boolean {
      return isManager(organization, project, userId) || hasRoleInResource(organization, project, resource, role, userId)
   }

   fun hasAnyRoleInResource(organization: Organization?, project: Project?, resource: Resource, roles: Set<Role>, userId: String): Boolean {
      return roles.any { hasRoleInResource(organization, project, resource, it, userId) }
   }

   fun hasRoleWithView(organization: Organization?, project: Project?, collection: Collection, role: Role, viewRole: Role, userId: String, viewId: String? = null): Boolean {
      return hasRoleInResource(organization, project, collection, role, userId) || getResourceRoleViaView(organization, project, collection, role, viewRole, userId, viewId ?: activeView()?.id)
   }

   private fun getResourceRoleViaView(organization: Organization?, project: Project?, collection: Collection, role: Role, viewRole: Role, userId: String, viewId: String?): Boolean {
      if (viewId.orEmpty().isNotEmpty()) {
         val view = getView(viewId!!)
         if (hasRoleInResource(organization, project, view, viewRole, userId)) { // do we have access to the view?
            val authorId = view.authorId.orEmpty()
            val collectionIds = QueryUtils.getQueryCollectionIds(view.query, linkTypes.value)
            if (collectionIds.contains(collection.id)) { // does the view contain the resource?
               if (authorId.isNotEmpty()) {
                  if (hasRoleInResource(organization, project, collection, role, authorId)) { // has the view author access to the resource?
                     return true // grant access
                  }
               }
            }
         }
      }
      return false
   }

   fun hasRole(organization: Organization?, project: Project?, document: Document, collection: Collection, role: Role, userId: String): Boolean {
      return hasRole(organization, project, collection, role, userId) || hasRoleInDocument(document, collection, role, userId)
   }

   fun hasRoleWithView(organization: Organization?, project: Project?, document: Document, collection: Collection, role: Role, viewRole: Role, userId: String): Boolean {
      return hasRoleWithView(organization, project, collection, role, viewRole, userId) || hasRoleInDocument(document, collection, role, userId)
   }

   fun checkRole(organization: Organization?, project: Project?, document: Document, collection: Collection, role: Role, userId: String) {
      if (!hasRole(organization, project, document, collection, role, userId)) {
         throw NoDocumentPermissionException(document)
      }
   }

   fun checkRoleWithView(organization: Organization?, project: Project?, document: Document, collection: Collection, role: Role, viewRole: Role, userId: String) {
      if (!hasRoleWithView(organization, project, document, collection, role, viewRole, userId)) {
         throw NoDocumentPermissionException(document)
      }
   }

   fun hasRoleInDocument(document: Document?, collection: Collection, role: Role, userId: String): Boolean {
      return when (role) {
         Role.READ, Role.WRITE -> DocumentUtils.isTaskAssignedByUser(collection, document, getUser(userId).email)
         else -> false
      }
   }

   private fun checkOrganizationAndProject(organization: Organization?, project: Project?, resource: Resource, role: Role, userId: String) {
      if (resource !is Organization && organization != null) {
         if (!hasRoleInResource(organization, project, organization, role, userId)) {
            throw NoResourcePermissionException(resource)
         }
         if (resource !is Project && project != null) {
            if (!hasRoleInResource(organization, project, project, role, userId)) {
               throw NoResourcePermissionException(resource)
            }
         }
      }
   }

   fun hasRoleInResource(organization: Organization?, project: Project?, resource: Resource, role: Role, userId: String): Boolean {
      return hasRoleCache.computeIfAbsent(resource.id + ":" + role.toString()) { getActualRoles(organization, project, resource, userId).contains(role) }
   }

   private fun getActualRoles(organization: Organization?, project: Project?, resource: Resource, userId: String): Set<Role> {
      return if (isManager(organization, project, userId)) {
         ResourceUtils.getAllResourceRoles(resource)
      } else ResourceUtils.getRolesInResource(organization, resource, getUser(userId))
   }

   fun getUser(userId: String): User {
      return userCache.computeIfAbsent(userId) { userDao.getUserById(userId) }
   }

   fun getView(viewId: String): View {
      return viewCache.computeIfAbsent(viewId) { viewDao.getViewById(viewId) }
   }

}
