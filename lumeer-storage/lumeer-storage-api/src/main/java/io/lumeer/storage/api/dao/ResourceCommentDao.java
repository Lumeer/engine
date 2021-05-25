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
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ResourceCommentDao extends ProjectScopedDao {
   ResourceComment createComment(final ResourceComment comment);
   ResourceComment getComment(final String id);
   long getCommentsCount(final ResourceType resourceType, final String resourceId);
   Map<String, Integer> getCommentsCounts(final ResourceType resourceType, final Set<String> resourceIds);
   Map<String, Integer> getCommentsCounts(final ResourceType resourceType, final String parentId);
   ResourceComment updateComment(final ResourceComment comment);
   ResourceComment pureUpdateComment(final ResourceComment comment);
   boolean deleteComment(final ResourceComment comment);
   boolean deleteComments(final ResourceType resourceType, final String resourceId);
   boolean deleteComments(final ResourceType resourceType, final java.util.Collection<String> resourceIds);

   List<ResourceComment> getResourceComments(final ResourceType resourceType, final String resourceId, final int pageStart, final int pageLenght);
   List<ResourceComment> getResourceComments(final ResourceType resourceType);

   long updateParentId(final ResourceType resourceType, final String resourceId, final String parentId);
   void ensureIndexes(final Project project);
}
