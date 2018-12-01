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
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.filter.AttributeFilter;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public List<LinkInstance> getLinkInstances(Query query) {
      return linkInstanceDao.searchLinkInstances(buildSearchQuery(query));
   }

   private SearchQuery buildSearchQuery(Query query) {
      return SearchQuery.createBuilder(authenticatedUser.getCurrentUser().getEmail())
                        .groups(authenticatedUserGroups.getCurrentUserGroups())
                        .queryStems(query.getStems(), query.getFulltexts())
                        .page(query.getPage())
                        .pageSize(query.getPageSize())
                        .build();
   }

   public List<Document> searchDocuments(final Query query) {
      final List<Collection> collections = getReadCollections();

      if (query.isEmpty()) {
         return new ArrayList<>(getChildDocuments(searchDocumentsByEmptyQuery(query, collections)));
      } else if (query.containsStems()) {
         return new ArrayList<>(searchDocumentsByStems(query, collections));
      } else {
         return new ArrayList<>(getChildDocuments(searchDocumentsByFulltexts(query, collections)));
      }
   }

   private List<Collection> getReadCollections() {
      Set<Collection> userCollections = getUserCollections();

      final View view = permissionsChecker.getActiveView();
      if (view != null) {
         userCollections.addAll(viewFacade.getViewsCollections(Collections.singletonList(view), true));
      }

      return new ArrayList<>(userCollections);
   }

   private Set<Collection> getUserCollections() {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      DatabaseQuery query = DatabaseQuery.createBuilder(user)
                                         .groups(groups)
                                         .build();

      return collectionDao.getCollections(query).stream()
                          .filter(collection -> permissionsChecker.hasRoleWithView(collection, Role.READ, Role.READ))
                          .collect(Collectors.toSet());
   }

   private Set<Document> searchDocumentsByEmptyQuery(Query query, List<Collection> collections) {
      List<DataDocument> data = new ArrayList<>();
      for (Collection collection : collections) {
         SearchQueryStem stem = SearchQueryStem.createBuilder(collection.getId()).build();
         data.addAll(dataDao.searchData(stem, query.getPagination(), collection));
      }

      return convertDataDocumentsToDocuments(data);
   }

   private Set<Document> convertDataDocumentsToDocuments(java.util.Collection<DataDocument> data) {
      Set<String> documentIds = data.stream().map(DataDocument::getId).collect(Collectors.toSet());
      List<Document> documents = documentDao.getDocumentsByIds(documentIds.toArray(new String[0]));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return documents.stream()
                      .peek(document -> document.setData(dataMap.get(document.getId())))
                      .collect(Collectors.toSet());
   }

   private Set<Document> searchDocumentsByStems(Query query, List<Collection> collections) {
      SearchQuery searchQuery = buildSearchQuery(query);

      Set<String> linkTypeIds = query.getLinkTypeIds();
      List<LinkType> linkTypes = !linkTypeIds.isEmpty() ? linkTypeDao.getLinkTypesByIds(linkTypeIds) : Collections.emptyList();

      Set<String> documentIds = query.getDocumentsIds();
      List<Document> documents = !documentIds.isEmpty() ? documentDao.getDocumentsByIds(documentIds.toArray(new String[0])) : Collections.emptyList();

      Map<String, Collection> collectionsMap = collections.stream().collect(Collectors.toMap(Collection::getId, Function.identity()));

      Set<Document> data = new HashSet<>();
      for (SearchQueryStem stem : searchQuery.getStems()) {
         if (stem.containsLinkTypeIdsQuery()) {
            data.addAll(searchDocumentsByStemWithLinks(stem, searchQuery.getPagination(), collectionsMap, linkTypes, documents));
         } else {
            SearchQueryStem cleanedStem = cleanStemForBaseCollection(stem, documents);

            List<DataDocument> stemData = dataDao.searchData(cleanedStem, searchQuery.getPagination(), collectionsMap.get(stem.getCollectionId()));
            Set<Document> documentsByData = convertDataDocumentsToDocuments(stemData);
            data.addAll(getChildDocuments(documentsByData));
         }
      }

      return data;
   }

   private Set<Document> searchDocumentsByStemWithLinks(SearchQueryStem stem, Pagination pagination, Map<String, Collection> collectionsMap, List<LinkType> linkTypes, List<Document> documents) {
      if (!collectionsMap.containsKey(stem.getCollectionId())) {
         return Collections.emptySet();
      }

      SearchQueryStem baseStem = cleanStemForBaseCollection(stem, documents);
      List<SearchQueryStem> stemsPipeline = createStemsPipeline(stem, collectionsMap, linkTypes, documents);

      List<DataDocument> data = dataDao.searchData(baseStem, pagination, collectionsMap.get(baseStem.getCollectionId()));
      // we need to add child documents only for base collection
      Set<Document> documentsByData = convertDataDocumentsToDocuments(data);
      documentsByData.addAll(getChildDocuments(documentsByData));

      List<DataDocument> lastStageData = data;

      for (SearchQueryStem currentStageStem : stemsPipeline) {
         Set<String> lastStageDocumentIds = lastStageData.stream().map(DataDocument::getId).collect(Collectors.toSet());
         List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstancesByDocumentIds(lastStageDocumentIds);
         Set<String> otherDocumentIds = linkInstances.stream().map(LinkInstance::getDocumentIds)
                                                     .flatMap(List::stream)
                                                     .filter(id -> !lastStageDocumentIds.contains(id))
                                                     .collect(Collectors.toSet());

         Set<String> currentDocumentsIds = new HashSet<>(currentStageStem.getDocumentIds());


         if (currentStageStem.containsDocumentIdsQuery()) {
            currentDocumentsIds.retainAll(otherDocumentIds);
         } else {
            currentDocumentsIds.addAll(otherDocumentIds);
         }

         SearchQueryStem modifiedStem = SearchQueryStem.createBuilder(currentStageStem.getCollectionId())
               .linkTypeIds(currentStageStem.getLinkTypeIds())
               .documentIds(currentDocumentsIds)
               .filters(currentStageStem.getFilters())
               .fulltexts(currentStageStem.getFulltexts())
               .build();

         if (!modifiedStem.containsDocumentIdsQuery()) {
            break; // empty ids after interesction or append represents empty search, so we should break
         }

         List<DataDocument> currentStageData = dataDao.searchData(modifiedStem, pagination, collectionsMap.get(modifiedStem.getCollectionId()));
         if (currentStageData.isEmpty()) {
            break;
         }
         documentsByData.addAll(convertDataDocumentsToDocuments(currentStageData));
         lastStageData = currentStageData;
      }

      return documentsByData;
   }

   private SearchQueryStem cleanStemForBaseCollection(SearchQueryStem stem, List<Document> documents) {
      return cleanStemForCollection(stem, documents, stem.getCollectionId());
   }

   private SearchQueryStem cleanStemForCollection(SearchQueryStem stem, List<Document> documents, String collectionId) {
      Set<AttributeFilter> filters = stem.getFilters().stream()
                                         .filter(filter -> filter.getCollectionId().equals(collectionId))
                                         .collect(Collectors.toSet());

      Set<String> documentIds = documents.stream().filter(d -> d.getCollectionId().equals(collectionId) && stem.getDocumentIds().contains(d.getId()))
                                         .map(Document::getId)
                                         .collect(Collectors.toSet());

      return SearchQueryStem.createBuilder(collectionId)
                            .fulltexts(stem.getFulltexts())
                            .filters(filters)
                            .documentIds(documentIds)
                            .build();
   }

   private List<SearchQueryStem> createStemsPipeline(SearchQueryStem stem, Map<String, Collection> collectionsMap, List<LinkType> allLinkTypes, List<Document> allDocuments) {
      List<SearchQueryStem> stemsPipeline = new LinkedList<>();
      String lastCollectionId = stem.getCollectionId();

      Set<LinkType> stemLinkTypes = allLinkTypes.stream().filter(lt -> stem.getLinkTypeIds().contains(lt.getId())).collect(Collectors.toSet());

      for (int i = 0; i < stem.getLinkTypeIds().size(); i++) {
         LinkType linkType = findLinkTypeForCollection(stemLinkTypes, lastCollectionId);
         if (linkType == null) {
            return stemsPipeline; // maximum valid pipeline
         }

         int collectionIdIndex = linkType.getCollectionIds().get(0).equals(lastCollectionId) ? 1 : 0;
         String currentCollectionId = linkType.getCollectionIds().get(collectionIdIndex);

         if (!collectionsMap.containsKey(currentCollectionId)) {
            return stemsPipeline;
         }

         stemsPipeline.add(cleanStemForCollection(stem, allDocuments, currentCollectionId));
         lastCollectionId = currentCollectionId;
         stemLinkTypes.remove(linkType);
      }

      return stemsPipeline;
   }

   private LinkType findLinkTypeForCollection(Set<LinkType> linkTypes, String collectionId) {
      Optional<LinkType> linkType = linkTypes.stream().filter(lt -> lt.getCollectionIds().contains(collectionId)).findFirst();
      return linkType.orElse(null);
   }

   private Set<Document> searchDocumentsByFulltexts(Query query, List<Collection> collections) {
      List<DataDocument> data = dataDao.searchDataByFulltexts(query.getFulltexts(), query.getPagination(), collections);
      return convertDataDocumentsToDocuments(data);
   }

   private Set<Document> getChildDocuments(final Set<Document> rootDocuments) {
      return getChildDocumentsBfs(new HashSet<>(), rootDocuments, false);
   }

   private Set<Document> getChildDocumentsBfs(final Set<Document> result, final Set<Document> rootDocuments, boolean setData) {

      // find all document where parentId == one of root documents
      // add root documents to result when not exist there
      // add results to rootDocuments
      // repeat while there were any results

      List<Document> nextLevel = documentDao.getDocumentsByParentIds(rootDocuments.stream()
                                                                                  .map(Document::getId)
                                                                                  .collect(Collectors.toSet()));

      Set<Document> filteredDocuments = rootDocuments.stream().filter(d -> !result.contains(d))
                                                     .collect(Collectors.toSet());

      if (setData) {
         Map<String, DataDocument> dataMap = getDataFromDocuments(filteredDocuments);
         filteredDocuments.forEach(document -> document.setData(dataMap.get(document.getId())));
      }

      result.addAll(filteredDocuments);

      if (nextLevel.size() > 0) {
         getChildDocumentsBfs(result, new HashSet<>(nextLevel), true);
      }

      return result;
   }

   private Map<String, DataDocument> getDataFromDocuments(Set<Document> documents) {
      Map<String, Set<String>> collectionsDocumentsMap = documents.stream()
                                                                  .collect(Collectors.groupingBy(Document::getCollectionId,
                                                                        Collectors.mapping(Document::getId, Collectors.toSet())));

      Map<String, DataDocument> map = new HashMap<>();
      collectionsDocumentsMap.forEach((collectionId, docIds) -> {
         Map<String, DataDocument> dataDocuments = dataDao.getData(collectionId, docIds).stream()
                                                          .collect(Collectors.toMap(DataDocument::getId, Function.identity()));
         map.putAll(dataDocuments);
      });

      return map;
   }

}
