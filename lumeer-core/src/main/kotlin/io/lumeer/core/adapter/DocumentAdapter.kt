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

import io.lumeer.api.model.Document
import io.lumeer.api.model.ResourceType
import io.lumeer.storage.api.dao.FavoriteItemDao
import io.lumeer.storage.api.dao.ResourceCommentDao
import io.lumeer.storage.api.dao.context.DaoContextSnapshot

class DocumentAdapter(private val resourceCommentDao: ResourceCommentDao, private val favoriteItemDao: FavoriteItemDao) {

    fun getCommentsCount(documentId: String): Long = resourceCommentDao.getCommentsCount(ResourceType.DOCUMENT, documentId)

    fun getCommentsCounts(documentIds: Set<String>): Map<String, Int> = resourceCommentDao.getCommentsCounts(ResourceType.DOCUMENT, documentIds)

    fun getCommentsCounts(collectionId: String): Map<String, Int> = resourceCommentDao.getCommentsCounts(ResourceType.DOCUMENT, collectionId)

    fun getFavoriteDocumentIds(userId: String, projectId: String): Set<String> = favoriteItemDao.getFavoriteDocumentIds(userId, projectId)

    fun isFavorite(documentId: String, userId: String, projectId: String): Boolean = getFavoriteDocumentIds(userId, projectId).contains(documentId)

    fun mapDocumentData(document: Document, userId: String, projectId: String): Document = document.apply {
        isFavorite = isFavorite(document.id, userId, projectId)
        commentsCount = getCommentsCount(document.id)
    }

    fun mapDocumentsData(documents: List<Document>, userId: String, projectId: String): List<Document> {
        val favoriteDocumentIds = getFavoriteDocumentIds(userId, projectId)
        val commentCounts = obtainCommentCounts(documents)
        return documents.onEach {
            it.isFavorite = favoriteDocumentIds.contains(it.id)
            it.commentsCount = (commentCounts[it.id] ?: 0).toLong()
        }
    }

    fun deleteComments(documentId: String) {
        resourceCommentDao.deleteComments(ResourceType.DOCUMENT, documentId)
    }

    fun deleteComments(documentIds: Set<String>) {
        resourceCommentDao.deleteComments(ResourceType.DOCUMENT, documentIds)
    }

    private fun obtainCommentCounts(documents: List<Document>): Map<String, Int> {
        val documentIds = documents.map { obj: Document -> obj.id }.toSet()
        if (documents.size < 100) {
            return getCommentsCounts(documentIds)
        }

        val commentCounts = mutableMapOf<String, Int>()
        val documentsByCollectionId = documents.groupBy { it.collectionId }
        documentsByCollectionId.keys.forEach { k ->
            if (documentsByCollectionId[k].orEmpty().size < 100) {
                commentCounts.putAll(getCommentsCounts(documentsByCollectionId[k].orEmpty().map { it.id }.toSet()))
            } else {
                commentCounts.putAll(getCommentsCounts(k))
            }
        }

        return commentCounts
    }

    companion object {
        fun createFromDaoSnapshot(dao: DaoContextSnapshot) = DocumentAdapter(dao.resourceCommentDao, dao.favoriteItemDao)
    }
}
