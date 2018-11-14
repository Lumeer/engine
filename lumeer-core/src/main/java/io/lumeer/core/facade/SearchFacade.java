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
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.util.FilterParser;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
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
   private LinkTypeDao linkTypeDao;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public List<Collection> searchCollections(Query query) {
      Set<Collection> collections = getQueryCollections(query);

      final View view = permissionsChecker.getActiveView();
      if (view != null) {
         collections.addAll(getQueryCollections(view.getQuery()));
      }

      return new ArrayList<>(collections);
   }

   public List<Collection> searchCollectionsByView(final View view, final boolean allRights) {
      java.util.Collection<Collection> collections = getQueryCollections(view.getQuery(), view, false);

      if (allRights) {
         return new ArrayList<>(collections);
      } else {
         return collections.stream().map(this::mapResource).collect(Collectors.toList());
      }
   }

   private Set<Collection> getQueryCollections(final Query query) {
      return getQueryCollections(query, permissionsChecker.getActiveView(), true).stream().map(this::mapResource).collect(Collectors.toSet());
   }

   private Set<Collection> getQueryCollections(final Query query, final View view, final boolean filterByDocs) {
      Set<Collection> collections = new HashSet<>();

      if (query.getLinkTypeIds() != null && query.getLinkTypeIds().size() > 0) {
         final List<LinkType> linkTypes = query.getLinkTypeIds().stream().map(linkTypeDao::getLinkType).collect(Collectors.toList());
         linkTypes.forEach(linkType -> collections.addAll(collectionDao.getCollectionsByIds(linkType.getCollectionIds())));
      }

      if ((query.getFulltext() != null && !query.getFulltext().isEmpty()) || (query.getFilters() != null && !query.getFilters().isEmpty())) {
         collections.addAll(getCollectionsByDocumentsSearch(query, view, filterByDocs));
      }

      if (query.getDocumentIds() != null && !query.getDocumentIds().isEmpty()) {
         collections.addAll(getCollectionsByDocumentsIds(query.getDocumentIds()));
      }

      if (collectionQueryIsNotEmpty(query) || isEmptyQuery(query)) {
         collections.addAll(getCollectionsByCollectionSearch(query, view));
      }

      return collections;
   }

   private java.util.Collection<Collection> getCollectionsByDocumentsSearch(final Query query, final View view, final boolean filterByDocs) {
      java.util.Collection<Collection> searchedCollections = getCollections(query, view).values();

      if (filterByDocs) {
         List<Collection> matchedCollections = new ArrayList<>();

         for (Collection collection : searchedCollections) {
            long documentCount = dataDao.getDataCount(collection.getId(), createSearchQuery(query, view));
            if (documentCount > 0) {
               matchedCollections.add(collection);
            }
         }

         return matchedCollections;
      }

      return searchedCollections;
   }

   public List<Document> searchDocuments(final Query query) {
      final Map<String, Collection> collectionCache = new HashMap<>();
      final Set<Document> documents = new HashSet<>();

      if (query.getDocumentIds() != null && !query.getDocumentIds().isEmpty()) {
         documents.addAll(getDocumentsByIds(query.getDocumentIds()));
      }

      if (!isOnlyDocumentsIdsQuery(query) || isEmptyQuery(query)) {
         documents.addAll(searchDocumentsByQuery(query, null));
      }

      final Set<Document> documentsWithChildren = getChildDocuments(documents);

      return documentsWithChildren
            .stream()
            .filter(document ->
                  permissionsChecker.hasRoleWithView(
                        collectionCache.computeIfAbsent(document.getCollectionId(), id -> collectionDao.getCollectionById(id)),
                        Role.READ,
                        Role.READ,
                        query))
            .collect(Collectors.toList());
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

   private List<Document> searchDocumentsByQuery(Query query, final View userView) {
      Set<String> collectionIdsFromFilters = collectionIdsFromFilters(parseAttributeFilters(query.getFilters()));
      SearchQuery collectionQuery = createCollectionQuery(query, collectionIdsFromFilters, userView);
      Map<String, Collection> collections = getCollections(collectionQuery);

      Map<String, DataDocument> dataDocuments = new HashMap<>();

      for (Collection collection : collections.values()) {
         SearchQuery usedSearchQuery = collectionIdsFromFilters.contains(collection.getId()) ? createSearchQuery(query, userView) : createSearchQueryWithoutFilters(query, userView);
         Map<String, DataDocument> foundDocuments = getDataDocuments(collection.getId(), usedSearchQuery);
         dataDocuments.putAll(foundDocuments);
      }

      return getDocuments(dataDocuments);
   }

   private Map<String, DataDocument> getDataDocuments(String collectionId, SearchQuery query) {
      return dataDao.getData(collectionId, query).stream()
                    .collect(Collectors.toMap(DataDocument::getId, Function.identity()));
   }

   public List<View> searchViews(Query query) {
      return getViewsByFulltext(query, null);
   }

   private List<Collection> getCollectionsByCollectionSearch(Query query, final View userView) {
      SearchQuery searchQuery = createSearchQuery(query, userView);

      return collectionDao.getCollections(searchQuery);
   }

   private List<View> getViewsByFulltext(Query query, final View userView) {
      SearchQuery searchQuery = createSearchQuery(query, userView);
      return viewDao.getViews(searchQuery).stream()
                    .map(this::mapResource)
                    .collect(Collectors.toList());
   }

   private Map<String, Collection> getCollections(Query query, final View userView) {
      SearchQuery collectionQuery = createCollectionQuery(query, userView);
      return getCollections(collectionQuery);
   }

   private Map<String, Collection> getCollections(SearchQuery query) {
      return collectionDao.getCollections(query).stream()
                          .collect(Collectors.toMap(Resource::getId, Function.identity()));
   }

   private List<Document> getDocuments(Map<String, DataDocument> dataDocuments) {
      if (dataDocuments.size() > 0) {
         String[] documentIds = dataDocuments.keySet().toArray(new String[] {});
         List<Document> documents = documentDao.getDocumentsByIds(documentIds);
         documents.forEach(document -> {
            document.setData(dataDocuments.get(document.getId()));
         });
         return documents;
      }

      return new ArrayList<>();
   }

   private SearchQuery createCollectionQuery(Query query, final View userView) {
      Set<AttributeFilter> filters = parseAttributeFilters(query.getFilters());
      Set<String> collectionIdsFromFilters = collectionIdsFromFilters(filters);

      return createCollectionQuery(query, collectionIdsFromFilters, userView);
   }

   private SearchQuery createCollectionQuery(Query query, Set<String> additionalCollectionIds, final View userView) {
      return createCollectionSearchQueryBuilder(query, additionalCollectionIds, userView)
            .build();
   }

   private SearchQuery createSearchQuery(Query query, final View userView) {
      Set<AttributeFilter> filters = parseAttributeFilters(query.getFilters());
      Set<String> collectionIdsFromFilters = collectionIdsFromFilters(filters);

      return createSearchQueryBuilder(query, collectionIdsFromFilters, userView)
            .filters(filters)
            .build();
   }

   private SearchQuery createSearchQueryWithoutFilters(Query query, final View userView) {
      return createSearchQueryBuilder(query, new HashSet<>(), userView).build();
   }

   private SearchQuery.Builder createSearchQueryBuilder(Query query, Set<String> additionalCollectionIds, final View userView) {
      return createCollectionSearchQueryBuilder(query, additionalCollectionIds, userView)
            .documentIds(query.getDocumentIds())
            .fulltext(query.getFulltext())
            .page(query.getPage())
            .pageSize(query.getPageSize());
   }

   private SearchQuery.Builder createCollectionSearchQueryBuilder(Query query, Set<String> additionalCollectionIds, final View userView) {
      final View view = permissionsChecker.getActiveView();
      final String currentUserId = authenticatedUser.getCurrentUserId();
      final Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();
      final List<String> users = new ArrayList<>();

      users.add(currentUserId);
      if (view != null) {
         users.add(view.getAuthorId());
      }

      if (userView != null) {
         users.add(userView.getAuthorId());
      }

      Set<String> collectionIds = query.getCollectionIds() != null ? new HashSet<>(query.getCollectionIds()) : new HashSet<>();
      collectionIds.addAll(additionalCollectionIds);

      return SearchQuery.createBuilder(users.toArray(new String[users.size()])).groups(groups)
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
                                      .filter(f -> f != null)
                                      .collect(Collectors.toSet())
            : Collections.emptySet();
   }

   private Set<String> collectionIdsFromFilters(Set<AttributeFilter> filters) {
      return filters != null ? filters.stream()
                                      .filter(filter -> filter.getCollectionId() != null)
                                      .map(AttributeFilter::getCollectionId)
                                      .collect(Collectors.toSet())
            : Collections.emptySet();
   }

   private Set<Document> getChildDocuments(final Set<Document> rootDocuments) {
      return getChildDocumentsBfs(new HashSet<>(), rootDocuments);
   }

   private Set<Document> getChildDocumentsBfs(final Set<Document> result, final Set<Document> rootDocuments) {

      // find all document where parentId == one of root documents
      // add root documents to result when not exist there
      // add results to rootDocuments
      // repeat while there were any results

      List<Document> nextLevel = documentDao.getDocumentsByParentIds(rootDocuments.stream().map(Document::getId).collect(Collectors.toSet()));
      result.addAll(rootDocuments.stream().filter(d -> !result.contains(d)).map(document -> {
         document.setData(dataDao.getData(document.getCollectionId(), document.getId()));
         return document;
      }).collect(Collectors.toSet()));

      if (nextLevel.size() > 0) {
         getChildDocumentsBfs(result, new HashSet<>(nextLevel));
      }

      return result;
   }

}
