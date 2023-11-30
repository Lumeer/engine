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

import io.lumeer.api.model.Collection
import io.lumeer.api.model.Document
import io.lumeer.api.model.LinkPermissionsType
import io.lumeer.api.model.LinkType
import io.lumeer.api.model.Organization
import io.lumeer.api.model.Project
import io.lumeer.api.model.RoleType
import io.lumeer.api.model.User
import io.lumeer.api.model.View
import io.lumeer.core.util.DocumentUtils
import io.lumeer.core.util.QueryUtils
import io.lumeer.storage.api.dao.CollectionDao
import io.lumeer.storage.api.dao.LinkTypeDao
import io.lumeer.storage.api.dao.UserDao
import io.lumeer.storage.api.dao.ViewDao

class ResourceAdapter(private val permissionAdapter: PermissionAdapter,
                      private val collectionDao: CollectionDao,
                      private val linkTypeDao: LinkTypeDao,
                      private val viewDao: ViewDao,
                      private val userDao: UserDao) {

   fun getViews(organization: Organization, project: Project, userId: String): List<View> {
      return viewDao.allViews.filter { permissionAdapter.hasRole(organization, project, it, RoleType.Read, userId) }
   }

   fun getCollections(organization: Organization, project: Project, userId: String): List<Collection> {
      return collectionDao.allCollections.filter { permissionAdapter.hasRole(organization, project, it, RoleType.Read, userId) }
   }

   fun getLinkTypes(organization: Organization, project: Project, userId: String): List<LinkType> {
      return linkTypeDao.allLinkTypes.filter { permissionAdapter.hasRoleInLinkType(organization, project, it, RoleType.Read, userId) }
   }

   fun getAllCollections(organization: Organization, project: Project, userId: String): List<Collection> {
      return getAllCollections(organization, project, linkTypeDao.allLinkTypes, viewDao.allViews, collectionDao.allCollections, userId)
   }

   fun getAllLinkTypes(organization: Organization, project: Project, userId: String): List<LinkType> {
      return getAllLinkTypes(organization, project, linkTypeDao.allLinkTypes, viewDao.allViews, collectionDao.allCollections, userId)
   }

   fun getAllLinkTypes(organization: Organization, project: Project?, linkTypes: List<LinkType>, views: List<View>, collections: List<Collection>, userId: String): List<LinkType> {
      val viewsByUser = filterViewsByUser(organization, project, views, userId)
      val linkTypeIdsInViews = viewsByUser.flatMap { it.allLinkTypeIds }
      return linkTypes.filter { linkTypeIdsInViews.contains(it.id) || permissionAdapter.hasRoleInLinkType(organization, project, it, collections, RoleType.Read, userId) }
   }

   fun getAllCollections(organization: Organization, project: Project?, linkTypes: List<LinkType>, views: List<View>, collections: List<Collection>, userId: String): List<Collection> {
      val viewsByUser = filterViewsByUser(organization, project, views, userId)
      val collectionIdsInViews = QueryUtils.getViewsCollectionIds(viewsByUser, linkTypes)

      // when user can read link type by custom permission, then we have to send him both collections
      val linkTypesByCustomRead = filterLinkTypesByCustomPermission(organization, project, linkTypes, userId)
      val collectionIdsInLinkTypes = linkTypesByCustomRead.flatMap { it.collectionIds.orEmpty() }

      return collections.filter { collectionIdsInViews.contains(it.id) || collectionIdsInLinkTypes.contains(it.id) || permissionAdapter.hasRole(organization, project, it, RoleType.Read, userId) }
   }

   private fun filterLinkTypesByCustomPermission(organization: Organization, project: Project?, linkTypes: List<LinkType>, userId: String): List<LinkType> {
      return linkTypes.filter { it.permissionsType == LinkPermissionsType.Custom && permissionAdapter.hasRoleInLinkType(organization, project, it, RoleType.Read, userId) }
   }

   fun getOrganizationReaders(organization: Organization): Set<String> {
      return permissionAdapter.getOrganizationUsersByRole(organization, RoleType.Read)
   }

   fun getProjectReaders(organization: Organization, project: Project): Set<String> {
      return permissionAdapter.getProjectUsersByRole(organization, project, RoleType.Read)
   }

   fun getViewReaders(organization: Organization, project: Project, viewId: String): Set<String> {
      return getViewReaders(organization, project, permissionAdapter.getView(viewId))
   }

   fun getViewReaders(organization: Organization, project: Project, view: View): Set<String> {
      return permissionAdapter.getResourceUsersByRole(organization, project, view, RoleType.Read)
   }

   fun getCollectionReaders(organization: Organization, project: Project, collectionId: String): Set<String> {
      val collection = collectionDao.getCollectionById(collectionId)
      return getCollectionReaders(organization, project, collection)
   }

   fun getCollectionReaders(organization: Organization, project: Project, collection: Collection): Set<String> {
      val viewReaders = getCollectionTransitiveReaders(organization, project, collection.id)
      return permissionAdapter.getResourceUsersByRole(organization, project, collection, RoleType.Read).plus(viewReaders)
   }

   fun getLinkTypeReaders(organization: Organization, project: Project, linkType: LinkType): Set<String> {
      val viewReaders = getLinkTypeTransitiveReaders(organization, project, linkType.id)
      return permissionAdapter.getLinkTypeUsersByRole(organization, project, linkType, RoleType.Read).plus(viewReaders)
   }

   fun getDocumentReaders(organization: Organization, project: Project, collection: Collection, document: Document): Set<String> {
      val assigneesEmails = DocumentUtils.getUsersAssigneeEmails(collection, document, permissionAdapter.getGroups(organization.id), permissionAdapter.getUsers(organization.id))
      val assigneeIds = userDao.getUsersByEmails(assigneesEmails).map { it.id }

      return getCollectionReaders(organization, project, collection).plus(assigneeIds)
   }

   fun getViewAuthorCollectionsRoles(organization: Organization, project: Project?, view: View): Map<String, Set<RoleType>> {
      val user = userDao.getUserById(view.authorId) ?: User(view.authorId, "", "", emptySet())
      val collections = getViewCollections(view)
      return collections.associate { it.id to getCollectionRoles(organization, project, it, user) }
   }

   fun getViewAuthorLinkTypesRoles(organization: Organization, project: Project?, view: View): Map<String, Set<RoleType>> {
      val user = userDao.getUserById(view.authorId) ?: User(view.authorId, "", "", emptySet())
      val linkTypes = getViewLinkTypes(view)
      return linkTypes.associate { it.id to getLinkTypesRoles(organization, project, it, user) }
   }

   private fun getViewCollections(view: View): List<Collection> {
      val linkTypeIds = view.allLinkTypeIds
      val linkTypes = if (linkTypeIds.isEmpty()) listOf() else linkTypeDao.getLinkTypesByIds(linkTypeIds)
      val collectionIds = QueryUtils.getViewAllCollectionIds(view, linkTypes)
      return collectionDao.getCollectionsByIds(collectionIds)
   }

   private fun getViewLinkTypes(view: View): List<LinkType> {
      val linkTypeIds = view.allLinkTypeIds
      return if (linkTypeIds.isEmpty()) listOf() else linkTypeDao.getLinkTypesByIds(linkTypeIds)
   }

   private fun getCollectionRoles(organization: Organization, project: Project?, collection: Collection, user: User): Set<RoleType> {
      return permissionAdapter.getUserRolesInResource(organization, project, collection, user)
   }

   private fun getLinkTypesRoles(organization: Organization, project: Project?, linkType: LinkType, user: User): Set<RoleType> {
      return permissionAdapter.getUserRolesInLinkType(organization, project, linkType, user)
   }

   fun getCollectionTransitiveReaders(organization: Organization, project: Project, collectionId: String): Set<String> {
      val linkTypes = linkTypeDao.allLinkTypes
      val views = viewDao.allViews.filter { QueryUtils.getViewAllCollectionIds(it, linkTypes).contains(collectionId) }
      val linkTypesByCustomPermissions = linkTypes.filter { it.permissionsType == LinkPermissionsType.Custom && it.collectionIds.orEmpty().contains(collectionId) }

      val viewsReaders = views.flatMap { permissionAdapter.getResourceUsersByRole(organization, project, it, RoleType.Read) }.toSet()
      val linkTypeReaders = linkTypesByCustomPermissions.flatMap { permissionAdapter.getLinkTypeUsersByRole(organization, project, it, RoleType.Read)  }.toSet()

      return viewsReaders.plus(linkTypeReaders)
   }

   fun getLinkTypeTransitiveReaders(organization: Organization, project: Project, linkTypeId: String): Set<String> {
      val views = viewDao.allViews.filter { it.allLinkTypeIds.contains(linkTypeId) }

      return views.flatMap { permissionAdapter.getResourceUsersByRole(organization, project, it, RoleType.Read) }.toSet()
   }

   private fun filterViewsByUser(organization: Organization?, project: Project?, views: List<View>, userId: String): List<View> {
      return views.filter { permissionAdapter.hasRole(organization, project, it, RoleType.Read, userId) }
   }
}
