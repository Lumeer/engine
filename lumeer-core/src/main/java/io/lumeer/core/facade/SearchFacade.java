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
