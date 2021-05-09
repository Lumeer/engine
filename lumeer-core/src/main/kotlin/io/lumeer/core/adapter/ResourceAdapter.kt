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
import io.lumeer.api.util.ResourceUtils
import io.lumeer.core.util.DocumentUtils
import io.lumeer.core.util.QueryUtils
import io.lumeer.storage.api.dao.CollectionDao
import io.lumeer.storage.api.dao.LinkTypeDao
import io.lumeer.storage.api.dao.UserDao
import io.lumeer.storage.api.dao.ViewDao
import io.lumeer.storage.api.query.DatabaseQuery

class ResourceAdapter(private val collectionDao: CollectionDao, private val linkTypeDao: LinkTypeDao, private val viewDao: ViewDao, private val userDao: UserDao) {

    fun getViews(userId: String, groups: Set<String>, isWorkspaceManager: Boolean): List<View> {
        if (isWorkspaceManager) {
            return viewDao.allViews
        }
        return viewDao.getViews(createSimpleQuery(userId, groups))
    }

    fun getCollections(userId: String, groups: Set<String>, isWorkspaceManager: Boolean): List<Collection> {
        if (isWorkspaceManager) {
            return collectionDao.allCollections
        }
        return collectionDao.getCollections(createSimpleQuery(userId, groups))
    }

    fun getLinkTypes(userId: String, groups: Set<String>, isWorkspaceManager: Boolean): List<LinkType> {
        if (isWorkspaceManager) {
            return linkTypeDao.allLinkTypes
        }
        val allowedCollectionIds = getCollections(userId, groups, isWorkspaceManager).map { it.id }.toMutableSet()
        allowedCollectionIds.addAll(getViewsCollections(userId, groups, isWorkspaceManager).map { it.id })
        return linkTypeDao.allLinkTypes.filter { allowedCollectionIds.containsAll(it.collectionIds) }
    }

    fun getAllCollections(userId: String, groups: Set<String>, isWorkspaceManager: Boolean): List<Collection> {
        if (isWorkspaceManager) {
            return getCollections(userId, groups, isWorkspaceManager)
        }
        return getCollections(userId, groups, isWorkspaceManager).plus(getViewsCollections(userId, groups, isWorkspaceManager))
    }

    fun getAllLinkTypes(userId: String, groups: Set<String>, isWorkspaceManager: Boolean): List<LinkType> {
        if (isWorkspaceManager) {
            return getLinkTypes(userId, groups, isWorkspaceManager)
        }
        return getLinkTypes(userId, groups, isWorkspaceManager).plus(getViewsLinkTypes(userId, groups, isWorkspaceManager))
    }

    fun getViewsLinkTypes(userId: String, groups: Set<String>, isWorkspaceManager: Boolean): List<LinkType> {
        val views = getViews(userId, groups, isWorkspaceManager)
        return getViewsLinkTypes(views)
    }

    fun getViewsLinkTypes(views: List<View>): List<LinkType> {
        val linkTypesIds = views.flatMap { it.query.linkTypeIds }.toSet()
        return linkTypeDao.getLinkTypesByIds(linkTypesIds)
    }

    fun getViewsCollections(userId: String, groups: Set<String>, isWorkspaceManager: Boolean): List<Collection> {
        val views = getViews(userId, groups, isWorkspaceManager)
        return getViewsCollections(views)
    }

    fun getViewsCollections(views: List<View>): List<Collection> {
        val linkTypesIds = views.flatMap { it.query.linkTypeIds.toSet() }.toSet()
        val linkTypes = linkTypeDao.getLinkTypesByIds(linkTypesIds)

        val collectionIds = QueryUtils.getViewsCollectionIds(views, linkTypes)
        return collectionDao.getCollectionsByIds(collectionIds)
    }

    fun getViewReaders(viewId: String, organization: Organization, project: Project): Set<String> {
        val view = viewDao.getViewById(viewId)
        return getViewReaders(view, organization, project)
    }

    fun getViewReaders(view: View, organization: Organization, project: Project): Set<String> {
        return ResourceUtils.getResourceReaders(organization, project, view)
    }

    fun getCollectionManagers(collectionId: String, organization: Organization, project: Project): Set<String> {
        val collection = collectionDao.getCollectionById(collectionId)
        return getCollectionManagers(collection, organization, project)
    }

    fun getCollectionManagers(collection: Collection, organization: Organization, project: Project): Set<String> {
        val viewManagers = getCollectionManagersInViews(collection.id)
        return ResourceUtils.getCollectionManagers(organization, project, collection, viewManagers)
    }

    fun getCollectionReaders(collectionId: String, organization: Organization, project: Project): Set<String> {
        val collection = collectionDao.getCollectionById(collectionId)
        return getCollectionReaders(collection, organization, project)
    }

    fun getCollectionReaders(collection: Collection, organization: Organization, project: Project): Set<String> {
        val viewReaders = getCollectionReadersInViews(collection.id)
        return ResourceUtils.getCollectionReaders(organization, project, collection, viewReaders)
    }

    fun getLinkTypeReaders(linkType: LinkType, organization: Organization, project: Project): Set<String> {
        val users1 = getCollectionReaders(linkType.collectionIds[0], organization, project)
        val users2 = getCollectionReaders(linkType.collectionIds[1], organization, project)
        return users1.filter { users2.contains(it) }.toSet()
    }

    fun getDocumentReaders(document: Document, collection: Collection, organization: Organization, project: Project): Set<String> {
        val viewReaders = getCollectionReadersInViews(collection.id)

        val assigneesEmails = DocumentUtils.getUsersAssigneeEmails(collection, document)
        val assigneeIds = userDao.getUsersByEmails(assigneesEmails).map { it.id }
        return ResourceUtils.getCollectionReaders(organization, project, collection, viewReaders.plus(assigneeIds))
    }

    fun getViewAuthorRights(view: View, organization: Organization?, project: Project?): Map<String, Set<Role>> {
        val user = userDao.getUserById(view.authorId) ?: User(view.authorId, "", "", emptyMap())
        val collections = getViewsCollections(listOf(view))
        return collections.associate { it.id to getCollectionRoles(it, user, organization, project) }
    }

    private fun getCollectionRoles(collection: Collection, user: User, organization: Organization?, project: Project?): Set<Role> {
        if (ResourceUtils.userIsManagerInWorkspace(user.id, organization, project)) {
            return ResourceUtils.getAllResourceRoles(collection)
        }
        return ResourceUtils.getRolesInResource(organization, collection, user)
    }

    fun getCollectionReadersInViews(collectionId: String): Set<String> {
        return getUsersIdsInViews(collectionId, ResourceUtils::canReadByPermission)
    }

    fun getCollectionManagersInViews(collectionId: String): Set<String> {
        return getUsersIdsInViews(collectionId, ResourceUtils::canManageByPermission)
    }

    private fun getUsersIdsInViews(collectionId: String, check: (Permission) -> Boolean): Set<String> {
        val views = viewDao.allViews
        val linkTypes = linkTypeDao.allLinkTypes

        return views.filter { QueryUtils.getQueryCollectionIds(it.query, linkTypes).contains(collectionId) }
                .map { view -> view.permissions?.userPermissions.orEmpty().filter { permission -> check(permission) } }
                .flatMap { list -> list.map { permission -> permission.id } }
                .toSet()
    }

    private fun createSimpleQuery(userId: String, groups: Set<String>): DatabaseQuery {
        return DatabaseQuery.createBuilder(userId)
                .groups(groups)
                .build()
    }
}
