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
import io.lumeer.core.exception.NoDocumentPermissionException
import io.lumeer.core.exception.NoLinkInstancePermissionException
import io.lumeer.core.exception.NoPermissionException
import io.lumeer.core.exception.NoResourcePermissionException
import io.lumeer.core.util.DocumentUtils
import io.lumeer.core.util.FunctionRuleJsParser
import io.lumeer.core.util.LinkInstanceUtils
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
   private val collectionCache = mutableMapOf<String, Collection>()
   private val userCache = mutableMapOf<String, User>()
   private val groupsCache = mutableMapOf<String, List<Group>>()
   private val linkTypes = lazy { linkTypeDao.allLinkTypes }
   private val collections = lazy { collectionDao.allCollections }

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

   fun invalidateUserCache() {
      usersCache.clear()
      userCache.clear()
   }

   fun invalidateCollectionCache() {
      collectionCache.clear()
   }

   fun invalidateCache(resource: Resource) {
      for (role in RoleType.values()) {
         hasRoleCache.remove("${resource.type}:${resource.id}:$role")
      }
   }

   fun isPublic(organization: Organization?, project: Project?) = project?.isPublic ?: false

   fun canReadAllInWorkspace(organization: Organization, project: Project?, userId: String): Boolean {
      val user = getUser(userId)
      val groups = PermissionUtils.getUserGroups(organization, user)
      if (PermissionUtils.getUserRolesInResource(organization, user, groups).any { role -> role.isTransitive && role.roleType === RoleType.Read }) {
         return true
      }
      return project?.let { PermissionUtils.getUserRolesInResource(it, user, groups).any { role -> role.isTransitive && role.roleType === RoleType.Read } } ?: false
   }

   fun getOrganizationUsersByRole(organization: Organization, roleType: RoleType): Set<String> {
      return PermissionUtils.getOrganizationUsersByRole(organization, getUsers(organization.id), roleType)
   }

   fun getOrganizationReadersDifference(organization1: Organization, organization2: Organization): RolesDifference {
      return PermissionUtils.getOrganizationUsersDifferenceByRole(organization1, organization2, getUsers(organization1.id), RoleType.Read)
   }

   fun getProjectUsersByRole(organization: Organization, project: Project?, roleType: RoleType): Set<String> {
      return PermissionUtils.getProjectUsersByRole(organization, project, getUsers(organization.id), roleType)
   }

   fun getProjectReadersDifference(organization: Organization, project1: Project, project2: Project): RolesDifference {
      return PermissionUtils.getProjectUsersDifferenceByRole(organization, project1, project2, getUsers(organization.id), RoleType.Read)
   }

   fun <T : Resource> getResourceUsersByRole(organization: Organization, project: Project?, resource: T, roleType: RoleType): Set<String> {
      return PermissionUtils.getResourceUsersByRole(organization, project, resource, getUsers(organization.id), roleType)
   }

   fun getLinkTypeUsersByRole(organization: Organization, project: Project?, linkType: LinkType, roleType: RoleType): Set<String> {
      return PermissionUtils.getLinkTypeUsersByRole(organization, project, linkType, getLinkTypeCollections(linkType), getUsers(organization.id), roleType)
   }

   fun <T : Resource> getResourceReadersDifference(organization: Organization?, project: Project?, resource1: T, resource2: T): RolesDifference {
      if (resource1.type == ResourceType.ORGANIZATION) {
         return getOrganizationReadersDifference(resource1 as Organization, resource2 as Organization)
      }
      if (organization != null) {
         if (resource1.type == ResourceType.PROJECT) {
            return getProjectReadersDifference(organization, resource1 as Project, resource2 as Project)
         }
         return PermissionUtils.getResourceUsersDifferenceByRole(organization, project, resource1, resource2, getUsers(organization.id), RoleType.Read)
      }
      return RolesDifference(setOf(), setOf())
   }

   fun getLinkTypeReadersDifference(organization: Organization, project: Project?, linkType1: LinkType, linkType2: LinkType): RolesDifference {
      return PermissionUtils.getLinkTypeUsersDifferenceByRole(organization, project, linkType1, linkType2, getLinkTypeCollections(linkType1), getUsers(organization.id), RoleType.Read)
   }

   fun <T : Resource> getUserRolesInResource(organization: Organization?, project: Project?, resource: T, userId: String): Set<RoleType> {
      return getUserRolesInResource(organization, project, resource, getUser(userId))
   }

   fun <T : Resource> getUserRolesInResource(organization: Organization?, project: Project?, resource: T, user: User): Set<RoleType> {
      return PermissionUtils.getUserRolesInResource(organization, project, resource, user)
   }

   fun getUserRolesInCollectionWithView(organization: Organization?, project: Project?, collection: Collection, user: User): Set<RoleType> {
      val view = activeView()
      if (view != null) {
         val viewRoles = getUserRolesInResource(organization, project, view, user)
         val authorId = view.authorId.orEmpty()
         val collectionIds = QueryUtils.getQueryCollectionIds(view.query, linkTypes.value)
         if (collectionIds.contains(collection.id) && authorId.isNotEmpty()) { // does the view contain the collection?
            val authorRoles = getUserRolesInResource(organization, project, collection, authorId)
            return viewRoles.intersect(authorRoles)
         }
      }
      return emptySet()
   }

   fun getUserRolesInLinkTypeWithView(organization: Organization?, project: Project?, linkType: LinkType, user: User): Set<RoleType> {
      val view = activeView()
      if (view != null) {
         val viewRoles = getUserRolesInResource(organization, project, view, user)
         val authorId = view.authorId.orEmpty()
         val linkTypeIds = view.query?.linkTypeIds.orEmpty()
         if (linkTypeIds.contains(linkType.id) && authorId.isNotEmpty()) { // does the view contain the linkType?
            val authorRoles = getUserRolesInLinkType(organization, project, linkType, getUser(authorId))
            return viewRoles.intersect(authorRoles)
         }
      }
      return emptySet()
   }

   fun getUserRolesInLinkType(organization: Organization?, project: Project?, linkType: LinkType, user: User): Set<RoleType> {
      return getUserRolesInLinkType(organization, project, linkType, getLinkTypeCollections(linkType), user)
   }

   fun getUserRolesInLinkType(organization: Organization?, project: Project?, linkType: LinkType, collections: List<Collection>, userId: String): Set<RoleType> {
      return getUserRolesInLinkType(organization, project, linkType, collections, getUser(userId))
   }

   fun getUserRolesInLinkType(organization: Organization?, project: Project?, linkType: LinkType, collections: List<Collection>, user: User): Set<RoleType> {
      return PermissionUtils.getUserRolesInLinkType(organization, project, linkType, collections, user)
   }

   fun checkRole(organization: Organization?, project: Project?, resource: Resource, role: RoleType, userId: String) {
      if (!hasRole(organization, project, resource, role, userId)) {
         throw NoResourcePermissionException(resource)
      }
   }

   fun checkAllRoles(organization: Organization?, project: Project?, resource: Resource, roles: Set<RoleType>, userId: String) {
      if (!hasAllRoles(organization, project, resource, roles, userId)) {
         throw NoResourcePermissionException(resource)
      }
   }

   fun hasAllRoles(organization: Organization?, project: Project?, resource: Resource, roles: Set<RoleType>, userId: String): Boolean {
      return roles.all { hasRole(organization, project, resource, it, userId) }
   }

   fun hasAnyRole(organization: Organization?, project: Project?, resource: Resource, roles: Set<RoleType>, userId: String): Boolean {
      return roles.any { hasRole(organization, project, resource, it, userId) }
   }

   fun checkRoleInCollectionWithView(organization: Organization?, project: Project?, collection: Collection, role: RoleType, userId: String) {
      if (!hasRoleInCollectionWithView(organization, project, collection, role, userId)) {
         throw NoResourcePermissionException(collection)
      }
   }

   fun hasRoleInCollectionWithView(organization: Organization?, project: Project?, collection: Collection, role: RoleType, userId: String): Boolean {
      return hasRole(organization, project, collection, role, userId) || hasRoleInCollectionViaView(organization, project, collection, role, role, userId, activeView())
   }

   fun checkCanDelete(organization: Organization?, project: Project?, resource: Resource, userId: String) {
      if (!hasRole(organization, project, resource, RoleType.Manage, userId) || resource.isNonRemovable) {
         throw NoResourcePermissionException(resource)
      }
   }

   fun checkCanDelete(organization: Organization?, project: Project?, linkType: LinkType, userId: String) {
      if (!hasRole(organization, project, linkType, getLinkTypeCollections(linkType), RoleType.Manage, userId)) {
         throw NoPermissionException(ResourceType.LINK_TYPE.toString())
      }
   }

   private fun hasRoleInCollectionViaView(organization: Organization?, project: Project?, collection: Collection, role: RoleType, viewRole: RoleType, userId: String, view: View?): Boolean {
      if (view != null && hasRole(organization, project, view, viewRole, userId)) { // does user have access to the view?
         val authorId = view.authorId.orEmpty()
         val collectionIds = QueryUtils.getQueryCollectionIds(view.query, linkTypes.value)
         if (collectionIds.contains(collection.id) && authorId.isNotEmpty()) { // does the view contain the collection?
            if (hasRole(organization, project, collection, role, authorId)) { // has the view author access to the collection?
               return true // grant access
            }
         }
      }
      return false
   }

   fun checkCanReadDocument(organization: Organization?, project: Project?, document: Document, collection: Collection, userId: String) {
      if (!canReadDocument(organization, project, document, collection, userId)) {
         throw NoDocumentPermissionException(document)
      }
   }

   fun canReadDocument(organization: Organization?, project: Project?, document: Document, collection: Collection, userId: String): Boolean {
      return (hasRoleInCollectionWithView(organization, project, collection, RoleType.Read, userId) && hasRoleInCollectionWithView(organization, project, collection, RoleType.DataRead, userId))
            || (canReadWorkspace(organization, project, userId) && isDocumentOwner(organization, project, document, collection, userId))
   }

   fun checkCanReadLinkInstance(organization: Organization?, project: Project?, linkInstance: LinkInstance, linkType: LinkType, userId: String) {
      if (!canReadLinkInstance(organization, project, linkInstance, linkType, userId)) {
         throw NoLinkInstancePermissionException(linkInstance)
      }
   }

   fun canReadLinkInstance(organization: Organization?, project: Project?, linkInstance: LinkInstance, linkType: LinkType, userId: String): Boolean {
      return (hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.Read, userId) && hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.DataRead, userId))
            || (canReadWorkspace(organization, project, userId) && isLinkOwner(organization, project, linkInstance, linkType, userId))
   }

   fun checkCanCreateDocuments(organization: Organization?, project: Project?, collection: Collection, userId: String) {
      if (!canCreateDocuments(organization, project, collection, userId)) {
         throw NoResourcePermissionException(collection)
      }
   }

   fun checkCanCreateLinkInstances(organization: Organization?, project: Project?, linkType: LinkType, userId: String) {
      if (!canCreateLinkInstances(organization, project, linkType, userId)) {
         throw NoPermissionException(ResourceType.LINK_TYPE.toString())
      }
   }

   fun canCreateDocuments(organization: Organization?, project: Project?, collection: Collection, userId: String): Boolean {
      return canReadWorkspace(organization, project, userId) && hasRoleInCollectionWithView(organization, project, collection, RoleType.DataContribute, userId)
   }

   private fun canReadWorkspace(organization: Organization?, project: Project?, userId: String): Boolean {
      return project != null && hasRole(organization, project, project, RoleType.Read, userId)
   }

   fun canCreateLinkInstances(organization: Organization?, project: Project?, linkType: LinkType, userId: String): Boolean {
      return canReadWorkspace(organization, project, userId) && hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.DataContribute, userId)
   }

   fun checkCanEditDocument(organization: Organization?, project: Project?, document: Document, collection: Collection, userId: String) {
      if (!canEditDocument(organization, project, document, collection, userId)) {
         throw NoDocumentPermissionException(document)
      }
   }

   fun checkCanEditLinkInstance(organization: Organization?, project: Project?, linkInstance: LinkInstance, linkType: LinkType, userId: String) {
      if (!canEditLinkInstance(organization, project, linkInstance, linkType, userId)) {
         throw NoLinkInstancePermissionException(linkInstance)
      }
   }

   fun canEditDocument(organization: Organization?, project: Project?, document: Document, collection: Collection, userId: String): Boolean {
      return (hasRoleInCollectionWithView(organization, project, collection, RoleType.Read, userId) && hasRoleInCollectionWithView(organization, project, collection, RoleType.DataWrite, userId))
            || (canReadWorkspace(organization, project, userId) && isDocumentOwner(organization, project, document, collection, userId))
   }

   fun canEditLinkInstance(organization: Organization?, project: Project?, linkInstance: LinkInstance, linkType: LinkType, userId: String): Boolean {
      return (hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.Read, userId) && hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.DataWrite, userId))
            || (canReadWorkspace(organization, project, userId) && isLinkOwner(organization, project, linkInstance, linkType, userId))
   }

   fun checkCanDeleteDocument(organization: Organization?, project: Project?, document: Document, collection: Collection, userId: String) {
      if (!canDeleteDocument(organization, project, document, collection, userId)) {
         throw NoDocumentPermissionException(document)
      }
   }

   fun checkCanDeleteLinkInstance(organization: Organization?, project: Project?, linkInstance: LinkInstance, linkType: LinkType, userId: String) {
      if (!canDeleteLinkInstance(organization, project, linkInstance, linkType, userId)) {
         throw NoLinkInstancePermissionException(linkInstance)
      }
   }

   fun canDeleteDocument(organization: Organization?, project: Project?, document: Document, collection: Collection, userId: String): Boolean {
      return (hasRoleInCollectionWithView(organization, project, collection, RoleType.Read, userId) && hasRoleInCollectionWithView(organization, project, collection, RoleType.DataDelete, userId))
            || (canReadWorkspace(organization, project, userId) && isDocumentOwner(organization, project, document, collection, userId))
   }

   fun canDeleteLinkInstance(organization: Organization?, project: Project?, linkInstance: LinkInstance, linkType: LinkType, userId: String): Boolean {
      return (hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.Read, userId) && hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.DataDelete, userId))
            || (canReadWorkspace(organization, project, userId) && isLinkOwner(organization, project, linkInstance, linkType, userId))
   }

   private fun isDocumentOwner(organization: Organization?, project: Project?, document: Document, collection: Collection, userId: String): Boolean {
      return isDocumentContributor(organization, project, document, collection, userId) || DocumentUtils.isDocumentOwnerByPurpose(collection, document, getUser(userId))
   }

   private fun isDocumentContributor(organization: Organization?, project: Project?, document: Document, collection: Collection, userId: String): Boolean {
      return hasRoleInCollectionWithView(organization, project, collection, RoleType.DataContribute, userId) && DocumentUtils.isDocumentOwner(collection, document, userId)
   }

   private fun isLinkOwner(organization: Organization?, project: Project?, linkInstance: LinkInstance, linkType: LinkType, userId: String): Boolean {
      return hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.DataContribute, userId) && LinkInstanceUtils.isLinkInstanceOwner(linkType, linkInstance, userId)
   }

   fun checkRoleInLinkTypeWithView(organization: Organization?, project: Project?, linkType: LinkType, role: RoleType, userId: String) {
      if (!hasRoleInLinkTypeWithView(organization, project, linkType, role, userId)) {
         throw NoPermissionException(ResourceType.LINK_TYPE.toString())
      }
   }

   fun hasRoleInLinkTypeWithView(organization: Organization?, project: Project?, linkType: LinkType, role: RoleType, userId: String): Boolean {
      val collections = getLinkTypeCollections(linkType)
      return hasRole(organization, project, linkType, collections, role, userId) || hasRoleInLinkTypeViaView(organization, project, linkType, collections, role, role, userId, activeView())
   }

   private fun getLinkTypeCollections(linkType: LinkType) = linkType.collectionIds.orEmpty().subList(0, 2).map { getCollection(it) }

   private fun hasRoleInLinkTypeViaView(organization: Organization?, project: Project?, linkType: LinkType, collections: List<Collection>, role: RoleType, viewRole: RoleType, userId: String, view: View?): Boolean {
      if (view != null && hasRole(organization, project, view, viewRole, userId)) { // does user have access to the view?
         val authorId = view.authorId.orEmpty()
         val linkTypeIds = view.query?.linkTypeIds.orEmpty()
         if (linkTypeIds.contains(linkType.id) && authorId.isNotEmpty()) { // does the view contain the linkType?
            if (hasRole(organization, project, linkType, collections, role, authorId)) { // has the view author access to the linkType?
               return true // grant access
            }
         }
      }
      return false
   }

   fun checkRoleInLinkType(organization: Organization?, project: Project?, linkType: LinkType, role: RoleType, userId: String) {
      if (!hasRoleInLinkType(organization, project, linkType, role, userId)) {
         throw NoPermissionException(ResourceType.LINK_TYPE.toString())
      }
   }

   fun hasRoleInLinkType(organization: Organization?, project: Project?, linkType: LinkType, role: RoleType, userId: String): Boolean {
      return hasRoleInLinkType(organization, project, linkType, getLinkTypeCollections(linkType), role, userId)
   }

   fun hasRoleInLinkType(organization: Organization?, project: Project?, linkType: LinkType, collections: List<Collection>, role: RoleType, userId: String): Boolean {
      return hasRole(organization, project, linkType, collections, role, userId)
   }

   fun hasRole(organization: Organization?, project: Project?, resource: Resource, role: RoleType, userId: String): Boolean {
      return getUserRolesInResource(organization, project, resource, userId).contains(role)
      // TODO return hasRoleCache.computeIfAbsent("${resource.type}:${resource.id}:$role") { getUserRolesInResource(organization, project, resource, userId).contains(role) }
   }

   fun hasRole(organization: Organization?, project: Project?, linkType: LinkType, collection: List<Collection>, role: RoleType, userId: String): Boolean {
      return getUserRolesInLinkType(organization, project, linkType, collection, userId).contains(role)
      // TODO return hasRoleCache.computeIfAbsent("${ResourceType.LINK_TYPE}:${linkType.id}:$role") { getUserRolesInLinkType(organization, project, linkType, collection, userId).contains(role) }
   }

   fun checkFunctionRuleAccess(organization: Organization, project: Project?, js: String, role: RoleType, userId: String) {
      val collections = collections.value.associateBy { it.id }
      val collectionIds = collections.keys
      val linkTypes = linkTypes.value.associateBy { it.id }
      val linkTypeIds = linkTypes.keys

      val references = FunctionRuleJsParser.parseRuleFunctionJs(js, collectionIds, linkTypeIds)

      references.forEach { reference ->
         when (reference.resourceType) {
            ResourceType.COLLECTION -> {
               checkRole(organization, project, collections[reference.id]!!, role, userId)
            }
            ResourceType.LINK -> {
               checkRoleInLinkType(organization, project, linkTypes[reference.id]!!, role, userId)
            }
            else -> {
               throw NoPermissionException("Rule")
            }
         }
      }
   }

   fun getUser(userId: String): User {
      if (userCache.containsKey(userId)) {
         return userCache.get(userId)!!
      }
      val user = userDao.getUserById(userId)
      if (user != null) userCache[userId] = user
      return user ?: User(userId, userId, userId, mapOf())
   }

   fun getUsers(organizationId: String): List<User> {
      return userDao.getAllUsers(organizationId)
      // TODO return usersCache.computeIfAbsent(organizationId) { userDao.getAllUsers(organizationId) }
   }

   fun getView(viewId: String): View {
      return viewCache.computeIfAbsent(viewId) { viewDao.getViewById(viewId) }
   }

   fun getCollection(collectionId: String): Collection {
      return collectionCache.computeIfAbsent(collectionId) { collectionDao.getCollectionById(collectionId) }
   }

   fun getGroups(organizationId: String): List<Group> {
      return groupsCache.computeIfAbsent(organizationId) { groupDao.allGroups }
   }

}
