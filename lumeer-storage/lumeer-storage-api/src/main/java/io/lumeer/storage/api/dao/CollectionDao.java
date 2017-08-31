/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;

import java.util.List;
import java.util.Set;

public interface CollectionDao {

   void createCollectionsRepository(Project project);

   void deleteCollectionsRepository(Project project);

   void setProject(Project project);

   Collection createCollection(Collection collection);

   Collection updateCollection(String id, Collection collection);

   void deleteCollection(String id);

   Collection getCollectionByCode(String code);

   List<Collection> getCollections(SearchQuery query);

   List<Collection> getCollections(SuggestionQuery query);

   List<Collection> getCollectionsByAttributes(SuggestionQuery query);

   Set<String> getAllCollectionCodes();

}
