package io.lumeer.core.adapter

import io.lumeer.api.model.LinkInstance
import io.lumeer.api.model.ResourceType

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
class LinkInstanceAdapter(val resourceCommentAdapter: ResourceCommentAdapter) {

   fun getCommentsCount(linkInstanceId: String): Long = resourceCommentAdapter.getCommentsCount(ResourceType.LINK, linkInstanceId)

   fun getCommentsCounts(linkInstanceIds: Set<String>): Map<String, Int> = resourceCommentAdapter.getCommentsCounts(ResourceType.LINK, linkInstanceIds)

   fun mapLinkInstanceData(linkInstance: LinkInstance): LinkInstance = linkInstance.apply { commentsCount = getCommentsCount(id) }

}