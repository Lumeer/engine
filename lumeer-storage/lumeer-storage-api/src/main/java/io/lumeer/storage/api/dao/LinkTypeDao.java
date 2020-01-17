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

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.query.SearchSuggestionQuery;

import java.util.List;
import java.util.Set;

public interface LinkTypeDao extends ProjectScopedDao {

   LinkType createLinkType(LinkType linkType);

   LinkType updateLinkType(String id, LinkType linkType, LinkType originalLinkType);

   void deleteLinkType(String id);

   void deleteLinkTypesByCollectionId(String collectionId);

   LinkType getLinkType(String id);

   List<LinkType> getAllLinkTypes();

   List<LinkType> getLinkTypesByCollectionId(String collectionId);

   List<LinkType> getLinkTypesByIds(Set<String> ids);

   List<LinkType> getLinkTypes(SearchSuggestionQuery query);

   List<LinkType> getLinkTypesByAttributes(SearchSuggestionQuery query);

}
