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
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.util.FilterParser;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.filter.AttributeFilter;
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

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public List<Collection> searchCollections(Query query) {
      Set<Collection> collections = new HashSet<>();

      if ((query.getFulltext() != null && !query.getFulltext().isEmpty()) || (query.getFilters() != null && !query.getFilters().isEmpty())) {
         collections.addAll(getCollectionsByDocumentsSearch(query));
      }

      if (query.getDocumentIds() != null && !query.getDocumentIds().isEmpty()) {
         collections.addAll(getCollectionsByDocumentsIds(query.getDocumentIds()));
      }

      if (collectionQueryIsNotEmpty(query) || isEmptyQuery(query)) {
         collections.addAll(getCollectionsByCollectionSearch(query));
      }

      return new ArrayList<>(collections);
   }

   public List<Document> searchDocuments(Query query) {
      Set<Document> documents = new HashSet<>();

      if (query.getDocumentIds() != null && !query.getDocumentIds().isEmpty()) {
         documents.addAll(getDocumentsByIds(query.getDocumentIds()));
      }

      if (!isOnlyDocumentsIdsQuery(query) || isEmptyQuery(query)) {
         documents.addAll(searchDocumentsByQuery(query));
      }

      return new ArrayList<>(documents);
   }

   private boolean isEmptyQuery(final Query query) {
      return isEmptyQueryExceptDocumentIds(query) && (query.getDocumentIds() == null || query.getDocumentIds().isEmpty());
   }

   private boolean isEmptyQueryExceptDocumentIds(final Query query) {
      return (query.getFilters() == null || query.getFilters().isEmpty()) && (query.getFulltext() == null || query.getFulltext().isEmpty()) && (query.getCollectionIds() == null || query.getCollectionIds().isEmpty());
   }

   private boolean isOnlyDocumentsIdsQuery(final Query query) {
      return isEmptyQueryExceptDocumentIds(query) && (query.getDocumentIds() != null && !query.getDocumentIds().isEmpty());
   }

   private java.util.Collection<Collection> getCollectionsByDocumentsIds(final Set<String> documentIds) {
      List<Document> documents = documentDao.getDocumentsByIds(documentIds.toArray(new String[documentIds.size()]));
      Set<String> collectionIds = documents.stream().map(Document::getCollectionId).collect(Collectors.toSet());

      return collectionDao.getCollectionsByIds(collectionIds);
   }

   private boolean collectionQueryIsNotEmpty(Query query) {
      return query.getCollectionIds() != null && !query.getCollectionIds().isEmpty();
   }

   private List<Document> getDocumentsByIds(Set<String> documentIds) {
      List<Document> documents = documentDao.getDocumentsByIds(documentIds.toArray(new String[documentIds.size()]));
      Map<String, Document> documentsMap = documents.stream().collect(Collectors.toMap(Document::getId, Function.identity()));
      Map<String, Set<String>> collectionsDocumentsMap = documents.stream()
                                                                  .collect(Collectors.groupingBy(Document::getCollectionId, Collectors.mapping(Document::getId, Collectors.toSet())));

      collectionsDocumentsMap.forEach((collectionId, docIds) -> {
         List<DataDocument> dataDocuments = dataDao.getData(collectionId, createDocumentIdsQuery(docIds));
         dataDocuments.forEach(dataDocument -> documentsMap.get(dataDocument.getId()).setData(dataDocument));
      });

      return new ArrayList<>(documentsMap.values());
   }

   private List<Document> searchDocumentsByQuery(Query query) {
      Set<String> collectionIdsFromFilters = collectionIdsFromFilters(parseAttributeFilters(query.getFilters()));
      SearchQuery collectionQuery = createCollectionQuery(query, collectionIdsFromFilters);
      Map<String, Collection> collections = getCollections(collectionQuery);

      Map<String, DataDocument> dataDocuments = new HashMap<>();

      for (Collection collection : collections.values()) {
         SearchQuery usedSearchQuery = collectionIdsFromFilters.contains(collection.getId()) ? createSearchQuery(query) : createSearchQueryWithoutFilters(query);
         dataDocuments.putAll(getDataDocuments(collection.getId(), usedSearchQuery));
      }

      return getDocuments(dataDocuments);
   }

   private Map<String, DataDocument> getDataDocuments(String collectionId, SearchQuery query) {
      return dataDao.getData(collectionId, query).stream()
                    .collect(Collectors.toMap(DataDocument::getId, Function.identity()));
   }

   public List<View> searchViews(Query query) {
      return getViewsByFulltext(query);
   }

   private List<Collection> getCollectionsByCollectionSearch(Query query) {
      SearchQuery searchQuery = createSearchQuery(query);

      return collectionDao.getCollections(searchQuery).stream()
                          .map(this::mapResource)
                          .collect(Collectors.toList());
   }

   private List<Collection> getCollectionsByDocumentsSearch(Query query) {
      java.util.Collection<Collection> searchedCollections = getCollections(query).values();
      List<Collection> matchedCollections = new ArrayList<>();

      for (Collection collection : searchedCollections) {
         long documentCount = dataDao.getDataCount(collection.getId(), createSearchQuery(query));
         if (documentCount > 0) {
            matchedCollections.add(collection);
         }
      }

      return matchedCollections;
   }

   private List<View> getViewsByFulltext(Query query) {
      SearchQuery searchQuery = createSearchQuery(query);
      return viewDao.getViews(searchQuery).stream()
                    .map(this::mapResource)
                    .collect(Collectors.toList());
   }

   private Map<String, Collection> getCollections(Query query) {
      SearchQuery collectionQuery = createCollectionQuery(query);
      return getCollections(collectionQuery);
   }

   private Map<String, Collection> getCollections(SearchQuery query) {
      return collectionDao.getCollections(query).stream()
                          .collect(Collectors.toMap(Resource::getId, Function.identity()));
   }

   private List<Document> getDocuments(Map<String, DataDocument> dataDocuments) {
      String[] documentIds = dataDocuments.keySet().toArray(new String[] {});
      List<Document> documents = documentDao.getDocumentsByIds(documentIds);
      documents.forEach(document -> {
         document.setData(dataDocuments.get(document.getId()));
      });
      return documents;
   }

   private SearchQuery createCollectionQuery(Query query) {
      Set<AttributeFilter> filters = parseAttributeFilters(query.getFilters());
      Set<String> collectionIdsFromFilters = collectionIdsFromFilters(filters);

      return createCollectionQuery(query, collectionIdsFromFilters);
   }

   private SearchQuery createCollectionQuery(Query query, Set<String> additionalCollectionIds) {
      return createCollectionSearchQueryBuilder(query, additionalCollectionIds)
            .build();
   }

   private SearchQuery createSearchQuery(Query query) {
      Set<AttributeFilter> filters = parseAttributeFilters(query.getFilters());
      Set<String> collectionIdsFromFilters = collectionIdsFromFilters(filters);

      return createSearchQueryBuilder(query, collectionIdsFromFilters)
            .filters(filters)
            .build();
   }

   private SearchQuery createSearchQueryWithoutFilters(Query query) {
      return createSearchQueryBuilder(query, new HashSet<>()).build();
   }

   private SearchQuery.Builder createSearchQueryBuilder(Query query, Set<String> additionalCollectionIds) {
      return createCollectionSearchQueryBuilder(query, additionalCollectionIds)
            .documentIds(query.getDocumentIds())
            .fulltext(query.getFulltext())
            .page(query.getPage())
            .pageSize(query.getPageSize());
   }

   private SearchQuery.Builder createCollectionSearchQueryBuilder(Query query, Set<String> additionalCollectionIds) {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      Set<String> collectionIds = query.getCollectionIds() != null ? new HashSet<>(query.getCollectionIds()) : new HashSet<>();
      collectionIds.addAll(additionalCollectionIds);

      return SearchQuery.createBuilder(user).groups(groups)
                        .collectionIds(collectionIds);
   }

   private SearchQuery createDocumentIdsQuery(Set<String> documentIds) {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return SearchQuery.createBuilder(user).groups(groups)
                        .documentIds(documentIds)
                        .build();
   }

   private Set<AttributeFilter> parseAttributeFilters(Set<String> filters) {
      return filters != null ? filters.stream()
                                      .map(FilterParser::parse)
                                      .collect(Collectors.toSet())
            : Collections.emptySet();
   }

   private Set<String> collectionIdsFromFilters(Set<AttributeFilter> filters) {
      return filters != null ? filters.stream()
                                      .map(AttributeFilter::getCollectionId)
                                      .collect(Collectors.toSet())
            : Collections.emptySet();
   }

}
