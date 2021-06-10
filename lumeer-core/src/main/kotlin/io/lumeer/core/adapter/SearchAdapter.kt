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
import io.lumeer.core.constraint.ConstraintManager
import io.lumeer.engine.api.data.DataDocument
import io.lumeer.storage.api.dao.DataDao
import io.lumeer.storage.api.dao.DocumentDao
import io.lumeer.storage.api.dao.LinkDataDao
import io.lumeer.storage.api.dao.LinkInstanceDao
import io.lumeer.storage.api.filter.CollectionSearchAttributeFilter
import io.lumeer.storage.api.query.SearchQueryStem

class SearchAdapter(private val permissionAdapter: PermissionAdapter,
                    private val constraintManager: ConstraintManager,
                    private val documentDao: DocumentDao,
                    private val dataDao: DataDao,
                    private val linkInstanceDao: LinkInstanceDao,
                    private val linkDataDao: LinkDataDao) {

   private val MAX_IDS_QUERY = 500

   fun getDocuments(organization: Organization?, project: Project?, collection: Collection, documentIds: Set<String>, userId: String): List<Document> {
      val documents = mutableListOf<Document>()
      if (canReadAllDocuments(organization, project, collection, userId)) {
         return getAllDocuments(collection, documentIds)
      }
      if (canReadContributionDocuments(organization, project, collection, userId)) {
         documents.addAll(getContributionDocuments(collection, documentIds, userId))
      }
      if (collection.purposeType == CollectionPurposeType.Tasks) {
         documents.addAll(getAssigneeDocuments(collection, null, null, documentIds, userId))
      }
      return documents
   }

   private fun getAllDocuments(collection: Collection, documentIds: Set<String>): List<Document> {
      val documents = documentDao.getDocumentsByCollection(collection.id, documentIds)
      return mapDocumentsData(collection, documents)
   }

   private fun getContributionDocuments(collection: Collection, documentIds: Set<String>, userId: String): List<Document> {
      val documents = documentDao.getDocumentsByCreator(collection.id, userId, documentIds)
      return mapDocumentsData(collection, documents)
   }

   fun getDocuments(organization: Organization?, project: Project?, collection: Collection, userId: String): List<Document> {
      return getDocuments(organization, project, collection, null, null, userId)
   }

   fun getDocuments(organization: Organization?, project: Project?, collection: Collection, skip: Int?, limit: Int?, userId: String): List<Document> {
      val documents = mutableListOf<Document>()
      if (canReadAllDocuments(organization, project, collection, userId)) {
         return getAllDocuments(collection, skip, limit)
      }
      if (canReadContributionDocuments(organization, project, collection, userId)) {
         documents.addAll(getContributionDocuments(collection, skip, limit, userId))
      }
      if (collection.purposeType == CollectionPurposeType.Tasks) {
         documents.addAll(getAssigneeDocuments(collection, skip, limit, null, userId))
      }
      return documents
   }

   private fun getAllDocuments(collection: Collection, skip: Int?, limit: Int?): List<Document> {
      val documents = documentDao.getDocumentsByCollection(collection.id, Pagination(skip, limit))
      return mapDocumentsData(collection, documents)
   }

   private fun getContributionDocuments(collection: Collection, skip: Int?, limit: Int?, userId: String): List<Document> {
      val documents = documentDao.getDocumentsByCreator(collection.id, userId, Pagination(skip, limit))
      return mapDocumentsData(collection, documents)
   }

   private fun mapDocumentsData(collection: Collection, documents: List<Document>): List<Document> {
      if (documents.isNotEmpty()) {
         val data = if (documents.size < MAX_IDS_QUERY) { // large queries throw error in DB
            dataDao.getData(collection.id, documents.map { it.id }.toSet())
         } else {
            dataDao.getData(collection.id)
         }
         val dataMap = data.associateBy { it.id }
         return documents.onEach { it.data = constraintManager.decodeDataTypes(collection, dataMap.getOrDefault(it.id, DataDocument())) }
      }
      return documents
   }

   private fun getAssigneeDocuments(collection: Collection, skip: Int?, limit: Int?, documentIds: Set<String>?, userId: String): List<Document> {
      val assigneeAttribute = ResourceUtils.findAttribute(collection.attributes, collection.purpose?.assigneeAttributeId)
      if (assigneeAttribute != null) {
         val user = permissionAdapter.getUser(userId)
         val searchQuery = SearchQueryStem.createBuilder(collection.id)
               .filters(setOf(CollectionSearchAttributeFilter(collection.id, ConditionType.HAS_SOME, assigneeAttribute.id, user.email)))
               .build()
         val data = if (documentIds != null) dataDao.searchDataByIds(searchQuery, documentIds, collection) else dataDao.searchData(searchQuery, Pagination(skip, limit), collection)
         if (data.isNotEmpty()) {
            val documentsMap = documentDao.getDocumentsByCollection(collection.id).associateBy { it.id }
            return data.mapNotNull { documentsMap[it.id]?.apply { setData(constraintManager.decodeDataTypes(collection, it)) } }
         }
      }
      return listOf()
   }

   private fun canReadAllDocuments(organization: Organization?, project: Project?, collection: Collection, userId: String): Boolean {
      return permissionAdapter.hasRoleInCollectionWithView(organization, project, collection, RoleType.DataRead, userId)
   }

   private fun canReadContributionDocuments(organization: Organization?, project: Project?, collection: Collection, userId: String): Boolean {
      return permissionAdapter.hasRoleInCollectionWithView(organization, project, collection, RoleType.DataContribute, userId)
   }

   fun getLinkInstances(organization: Organization?, project: Project?, linkType: LinkType, documentIds: Set<String>, userId: String): List<LinkInstance> {
      val linkInstances = mutableListOf<LinkInstance>()
      if (canReadAllLinkInstances(organization, project, linkType, userId)) {
         return getAllLinkInstances(linkType, documentIds)
      }
      if (canReadContributionLinkInstances(organization, project, linkType, userId)) {
         linkInstances.addAll(getContributionLinkInstances(linkType, documentIds, userId))
      }
      return linkInstances
   }

   private fun getAllLinkInstances(linkType: LinkType, documentIds: Set<String>): List<LinkInstance> {
      val linkInstances = linkInstanceDao.getLinkInstancesByDocumentIds(documentIds, linkType.id)
      return mapLinkData(linkType, linkInstances)
   }

   private fun getContributionLinkInstances(linkType: LinkType, documentIds: Set<String>, userId: String): List<LinkInstance> {
      val documents = linkInstanceDao.getLinkInstancesByCreator(linkType.id, userId, documentIds)
      return mapLinkData(linkType, documents)
   }

   fun getLinkInstances(organization: Organization?, project: Project?, linkType: LinkType, userId: String): List<LinkInstance> {
      return getLinkInstances(organization, project, linkType, null, null, userId)
   }

   fun getLinkInstances(organization: Organization?, project: Project?, linkType: LinkType, skip: Int?, limit: Int?, userId: String): List<LinkInstance> {
      val linkInstances = mutableListOf<LinkInstance>()
      if (canReadAllLinkInstances(organization, project, linkType, userId)) {
         return getAllLinkInstances(linkType, skip, limit)
      }
      if (canReadContributionLinkInstances(organization, project, linkType, userId)) {
         linkInstances.addAll(getContributionLinkInstances(linkType, skip, limit, userId))
      }
      return linkInstances
   }

   private fun getAllLinkInstances(linkType: LinkType, skip: Int?, limit: Int?): List<LinkInstance> {
      val linkInstances = linkInstanceDao.getLinkInstancesByLinkType(linkType.id, Pagination(skip, limit))
      return mapLinkData(linkType, linkInstances)
   }

   private fun getContributionLinkInstances(linkType: LinkType, skip: Int?, limit: Int?, userId: String): List<LinkInstance> {
      val documents = linkInstanceDao.getLinkInstancesByCreator(linkType.id, userId, Pagination(skip, limit))
      return mapLinkData(linkType, documents)
   }

   private fun mapLinkData(linkType: LinkType, linkInstances: List<LinkInstance>): List<LinkInstance> {
      if (linkInstances.isNotEmpty()) {
         val data = if (linkInstances.size < MAX_IDS_QUERY) { // large queries throw error in DB
            linkDataDao.getData(linkType.id, linkInstances.map { it.id }.toSet())
         } else {
            linkDataDao.getData(linkType.id)
         }
         val dataMap = data.associateBy { it.id }
         return linkInstances.onEach { it.data = constraintManager.decodeDataTypes(linkType, dataMap.getOrDefault(it.id, DataDocument())) }
      }
      return linkInstances
   }

   private fun canReadAllLinkInstances(organization: Organization?, project: Project?, linkType: LinkType, userId: String): Boolean {
      return permissionAdapter.hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.DataRead, userId)
   }

   private fun canReadContributionLinkInstances(organization: Organization?, project: Project?, linkType: LinkType, userId: String): Boolean {
      return permissionAdapter.hasRoleInLinkTypeWithView(organization, project, linkType, RoleType.DataContribute, userId)
   }

}
