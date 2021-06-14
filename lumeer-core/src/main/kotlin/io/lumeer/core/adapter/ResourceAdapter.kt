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
      return getCollections(organization, project, userId).toMutableSet().plus(getViewsCollections(organization, project, userId)).toList()
   }

   fun getAllLinkTypes(organization: Organization, project: Project, userId: String): List<LinkType> {
      return getLinkTypes(organization, project, userId).toMutableSet().plus(getViewsLinkTypes(organization, project, userId)).toList()
   }

   fun getViewsLinkTypes(organization: Organization, project: Project, userId: String): List<LinkType> {
      val views = getViews(organization, project, userId)
      return getViewsLinkTypes(views)
   }

   private fun getViewsLinkTypes(views: List<View>): List<LinkType> {
      val linkTypesIds = views.flatMap { it.query?.linkTypeIds.orEmpty() }.toSet()
      return linkTypeDao.getLinkTypesByIds(linkTypesIds)
   }

   fun getViewsCollections(organization: Organization, project: Project, userId: String): List<Collection> {
      val views = getViews(organization, project, userId)
      return getViewsCollections(views)
   }

   private fun getViewsCollections(views: List<View>): List<Collection> {
      val linkTypesIds = views.flatMap { it.query?.linkTypeIds.orEmpty() }.toSet()
      val linkTypes = linkTypeDao.getLinkTypesByIds(linkTypesIds)

      val collectionIds = QueryUtils.getViewsCollectionIds(views, linkTypes)
      return collectionDao.getCollectionsByIds(collectionIds)
   }

   fun getAllLinkTypes(organization: Organization, project: Project?, linkTypes: List<LinkType>, views: List<View>, collections: List<Collection>, userId: String): List<LinkType> {
      val viewsByUser = filterViewsByUser(organization, project, views, userId)
      val linkTypeIdsInViews = viewsByUser.flatMap { it.query?.linkTypeIds.orEmpty() }
      return linkTypes.filter { linkTypeIdsInViews.contains(it.id) || permissionAdapter.hasRoleInLinkType(organization, project, it, collections, RoleType.Read, userId) }
   }

   fun getAllCollections(organization: Organization, project: Project?, linkTypes: List<LinkType>, views: List<View>, collections: List<Collection>, userId: String): List<Collection> {
      val viewsByUser = filterViewsByUser(organization, project, views, userId)
      val collectionIdsInViews = QueryUtils.getViewsCollectionIds(viewsByUser, linkTypes)
      return collections.filter { collectionIdsInViews.contains(it.id) || permissionAdapter.hasRole(organization, project, it, RoleType.Read, userId) }
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
      val viewReaders = getCollectionReadersInViews(organization, project, collection.id)
      return permissionAdapter.getResourceUsersByRole(organization, project, collection, RoleType.Read).plus(viewReaders)
   }

   fun getLinkTypeReaders(organization: Organization, project: Project, linkType: LinkType): Set<String> {
      val viewReaders = getLinkTypeReadersInViews(organization, project, linkType.id)
      return permissionAdapter.getLinkTypeUsersByRole(organization, project, linkType, RoleType.Read).plus(viewReaders)
   }

   fun getDocumentReaders(organization: Organization, project: Project, collection: Collection, document: Document): Set<String> {
      val assigneesEmails = DocumentUtils.getUsersAssigneeEmails(collection, document)
      val assigneeIds = userDao.getUsersByEmails(assigneesEmails).map { it.id }

      return getCollectionReaders(organization, project, collection).plus(assigneeIds)
   }

   fun getViewAuthorRights(organization: Organization?, project: Project?, view: View): Map<String, Set<RoleType>> {
      val user = userDao.getUserById(view.authorId) ?: User(view.authorId, "", "", emptyMap())
      val collections = getViewsCollections(listOf(view))
      return collections.associate { it.id to getCollectionRoles(organization, project, it, user) }
   }

   private fun getCollectionRoles(organization: Organization?, project: Project?, collection: Collection, user: User): Set<RoleType> {
      return permissionAdapter.getUserRolesInResource(organization, project, collection, user)
   }

   fun getCollectionReadersInViews(organization: Organization, project: Project, collectionId: String): Set<String> {
      val linkTypes = linkTypeDao.allLinkTypes
      val views = viewDao.allViews.filter { QueryUtils.getQueryCollectionIds(it.query, linkTypes).contains(collectionId) }

      return views.flatMap { permissionAdapter.getResourceUsersByRole(organization, project, it, RoleType.Read) }.toSet()
   }

   fun getLinkTypeReadersInViews(organization: Organization, project: Project, linkTypeId: String): Set<String> {
      val views = viewDao.allViews.filter { it.query?.linkTypeIds.orEmpty().contains(linkTypeId) }

      return views.flatMap { permissionAdapter.getResourceUsersByRole(organization, project, it, RoleType.Read) }.toSet()
   }

   private fun filterViewsByUser(organization: Organization?, project: Project?, views: List<View>, userId: String): List<View> {
      return views.filter { permissionAdapter.hasRole(organization, project, it, RoleType.Read, userId) }
   }
}
