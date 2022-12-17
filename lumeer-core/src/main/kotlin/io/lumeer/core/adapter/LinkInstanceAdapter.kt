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

import io.lumeer.api.model.LinkInstance
import io.lumeer.api.model.ResourceType
import io.lumeer.storage.api.dao.ResourceCommentDao
import io.lumeer.storage.api.dao.context.DaoContextSnapshot

class LinkInstanceAdapter(val resourceCommentDao: ResourceCommentDao) {

   fun getCommentsCount(linkInstanceId: String): Long = resourceCommentDao.getCommentsCount(ResourceType.LINK, linkInstanceId)

   fun getCommentsCounts(linkInstanceIds: Set<String>): Map<String, Int> = resourceCommentDao.getCommentsCounts(ResourceType.LINK, linkInstanceIds)

   fun getCommentsCounts(linkTypeId: String): Map<String, Int> = resourceCommentDao.getCommentsCounts(ResourceType.LINK, linkTypeId)

   fun mapLinkInstanceData(linkInstance: LinkInstance): LinkInstance = linkInstance.apply { commentsCount = getCommentsCount(id) }

   fun mapLinkInstancesData(linkInstances: List<LinkInstance>): List<LinkInstance> {
      val commentCounts = obtainCommentCounts(linkInstances)
      return linkInstances.onEach {
         it.commentsCount = (commentCounts[it.id] ?: 0).toLong()
      }
   }

   fun deleteComments(linkInstanceId: String) {
      resourceCommentDao.deleteComments(ResourceType.LINK, linkInstanceId)
   }

   fun deleteComments(linkInstanceIds: Set<String>) {
      resourceCommentDao.deleteComments(ResourceType.LINK, linkInstanceIds)
   }

   private fun obtainCommentCounts(linkInstances: List<LinkInstance>): Map<String, Int> {
      val linkInstanceIds = linkInstances.map { obj: LinkInstance -> obj.id }.toSet()
      if (linkInstances.size < 100) {
         return getCommentsCounts(linkInstanceIds)
      }

      val commentCounts = mutableMapOf<String, Int>()
      val linksByLinkTypeId = linkInstances.groupBy { it.linkTypeId }
      linksByLinkTypeId.keys.forEach { k ->
         if (linksByLinkTypeId[k].orEmpty().size < 100) {
            commentCounts.putAll(getCommentsCounts(linksByLinkTypeId[k].orEmpty().map { it.id }.toSet()))
         } else {
            commentCounts.putAll(getCommentsCounts(k))
         }
      }

      return commentCounts
   }

   companion object {
      fun createFromDaoSnapshot(dao: DaoContextSnapshot) = LinkInstanceAdapter(dao.resourceCommentDao)
   }
}
