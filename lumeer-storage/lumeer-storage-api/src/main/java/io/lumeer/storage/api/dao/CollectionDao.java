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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SearchSuggestionQuery;

import java.util.List;
import java.util.Set;

public interface CollectionDao extends ProjectScopedDao {

   Collection createCollection(Collection collection);

   Collection updateCollection(String id, Collection collection, Collection originalCollection);

   Collection updateCollection(String id, Collection collection, Collection originalCollection, boolean pushNotification);

   Collection updateCollectionRules(final Collection collection);

   void deleteCollection(String id);

   Collection getCollectionByCode(String code);

   Collection getCollectionById(String id);

   List<Collection> getCollectionsByIds(java.util.Collection<String> ids);

   List<Collection> getAllCollections();

   List<Collection> getCollections(DatabaseQuery query);

   List<Collection> getCollections(SearchSuggestionQuery query, boolean skipPermissions);

   List<Collection> getCollectionsByAttributes(SearchSuggestionQuery query, boolean skipPermissions);

   List<Collection> getCollectionsByPurpose(CollectionPurposeType purposeType);

   long getCollectionsCount();

   Set<String> getAllCollectionCodes();

   Set<String> getAllCollectionNames();

   Set<String> getAllCollectionIds();

   Collection bookAttributesNum(String id, Collection collection, int count);

   void ensureIndexes(final Project project);
}
