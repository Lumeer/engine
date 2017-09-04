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
package io.lumeer.core.facade;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.View;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.query.SearchQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class SearchFacade extends AbstractFacade {

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private ViewDao viewDao;

   public List<Collection> searchCollections(Query query) {
      Set<Collection> collections = new HashSet<>();

      if (!isCollectionSelectionOnly(query)) {
         collections.addAll(getCollectionsByDocumentSearch(query));
      }

      collections.addAll(getCollectionsByCollectionSearch(query));

      return new ArrayList<>(collections);
   }

   public List<Document> searchDocuments(Query query) {
      Map<String, Collection> collections = getCollections(query.getCollectionCodes());
      Map<String, DataDocument> dataDocuments = getDataDocuments(collections.values(), query);

      return getDocuments(collections, dataDocuments);
   }

   public List<View> searchViews(Query query) {
      if (isValidViewSearch(query)) {
         return getViewsByFulltext(query);
      }
      return Collections.emptyList();
   }

   private boolean isCollectionSelectionOnly(Query query) {
      return (query.getFulltext() == null || query.getFulltext().isEmpty()) && (query.getFilters() == null || query.getFilters().isEmpty());
   }

   private List<Collection> getCollectionsByCollectionSearch(Query query) {
      SearchQuery searchQuery = createSearchQuery(query);

      return collectionDao.getCollections(searchQuery).stream()
                          .map(this::keepOnlyActualUserRoles)
                          .collect(Collectors.toList());
   }

   private List<Collection> getCollectionsByDocumentSearch(Query query) {
      java.util.Collection<Collection> searchedCollections = getCollections(query.getCollectionCodes()).values();
      List<Collection> matchedCollections = new ArrayList<>();

      for (Collection collection : searchedCollections) {
         long documentCount = dataDao.getDataCount(collection.getId(), createSearchQuery(query));
         if (documentCount > 0) {
            matchedCollections.add(collection);
         }
      }

      return matchedCollections;
   }

   private static boolean isValidViewSearch(Query query) {
      return query.getCollectionCodes().isEmpty() && query.getFilters().isEmpty();
   }

   private List<View> getViewsByFulltext(Query query) {
      SearchQuery searchQuery = createSearchQuery(query);
      return viewDao.getViews(searchQuery).stream()
                    .map(this::keepOnlyActualUserRoles)
                    .collect(Collectors.toList());
   }

   private Map<String, Collection> getCollections(Set<String> collectionCodes) {
      SearchQuery collectionQuery = createCollectionQuery(collectionCodes);
      return collectionDao.getCollections(collectionQuery).stream()
                          .collect(Collectors.toMap(Resource::getId, Function.identity()));
   }

   private SearchQuery createCollectionQuery(Set<String> collectionCodes) {
      String user = authenticatedUser.getCurrentUsername();
      Set<String> groups = userCache.getUser(user).getGroups();

      return SearchQuery.createBuilder(user)
                        .groups(groups)
                        .collectionCodes(collectionCodes)
                        .build();
   }

   private Map<String, DataDocument> getDataDocuments(Iterable<Collection> collections, Query query) {
      Map<String, DataDocument> dataDocuments = new HashMap<>();
      for (Collection collection : collections) {
         dataDocuments.putAll(getDataDocuments(collection.getId(), query));
      }
      return dataDocuments;
   }

   private Map<String, DataDocument> getDataDocuments(String collectionId, Query query) {
      SearchQuery documentQuery = createSearchQuery(query);
      return dataDao.getData(collectionId, documentQuery).stream()
                    .collect(Collectors.toMap(DataDocument::getId, Function.identity()));
   }

   private List<Document> getDocuments(Map<String, Collection> collections, Map<String, DataDocument> dataDocuments) {
      String[] documentIds = dataDocuments.keySet().toArray(new String[] {});
      List<Document> documents = documentDao.getDocumentsByIds(documentIds);
      documents.forEach(document -> {
         document.setCollectionCode(collections.get(document.getCollectionId()).getCode());
         document.setData(dataDocuments.get(document.getId()));
      });
      return documents;
   }

   private SearchQuery createSearchQuery(Query query) {
      String user = authenticatedUser.getCurrentUsername();
      Set<String> groups = userCache.getUser(user).getGroups();

      return SearchQuery.createBuilder(user).groups(groups)
                        .collectionCodes(query.getCollectionCodes())
                        .fulltext(query.getFulltext()) // TODO add filters
                        .page(query.getPage())
                        .pageSize(query.getPageSize())
                        .build();
   }

}
