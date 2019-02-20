/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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

import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.query.SearchQuery;

import java.util.List;
import java.util.Set;

public interface LinkInstanceDao {

   void createLinkInstanceRepository(Project project);

   void deleteLinkInstanceRepository(Project project);

   void setProject(Project project);

   LinkInstance createLinkInstance(LinkInstance linkInstance);

   List<LinkInstance> createLinkInstances(final List<LinkInstance> linkInstances);

   LinkInstance updateLinkInstance(String id, LinkInstance linkInstance);

   void deleteLinkInstance(String id);

   long deleteLinkInstances(final SearchQuery query);

   void deleteLinkInstancesByLinkTypesIds(Set<String> linkTypeIds);

   void deleteLinkInstancesByDocumentsIds(Set<String> documentsIds);

   LinkInstance getLinkInstance(String id);

   List<LinkInstance> getLinkInstances(Set<String> ids);

   List<LinkInstance> getLinkInstancesByLinkType(String linkTypeId);

   List<LinkInstance> getLinkInstancesByDocumentIds(Set<String> documentIds, String linkTypeId);

   List<LinkInstance> searchLinkInstances(SearchQuery query);

}
