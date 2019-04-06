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
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.filter.CollectionAttributeFilter;
import io.lumeer.storage.api.filter.LinkAttributeFilter;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
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
   private LinkDataDao linkDataDao;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   private ConstraintManager constraintManager;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
   }

   public List<LinkInstance> getLinkInstances(Query query) {
      final Query encodedQuery = encodeQuery(query);
      final List<LinkType> linkTypes = getReadLinkTypes();
      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, l -> l));

      final List<LinkInstance> result;

      if (encodedQuery.isEmpty()) {
         result = new ArrayList<>(searchLinkInstancesByEmptyQuery(linkTypes));
      } else if (encodedQuery.containsStems()) {
         result = new ArrayList<>(searchLinkInstancesByStems(encodedQuery, linkTypes));
      } else {
         result = new ArrayList<>(searchLinkInstancesByFulltexts(encodedQuery, linkTypes));
      }

      result.forEach(linkInstance -> constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()));

      return result;
   }

   private java.util.Collection<LinkInstance> searchLinkInstancesByEmptyQuery(List<LinkType> linkTypes) {
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstancesByLinkTypes(linkTypes.stream().map(LinkType::getId).collect(Collectors.toSet()));
      return setDataForLinkInstances(linkInstances);
   }

   private java.util.Collection<LinkInstance> setDataForLinkInstances(java.util.Collection<LinkInstance> linkInstances) {
      Map<String, Set<String>> linkInstancesIdsMap = linkInstances.stream().
            collect(Collectors.groupingBy(LinkInstance::getLinkTypeId, Collectors.mapping(LinkInstance::getId, Collectors.toSet())));

      Map<String, DataDocument> allDataMap = new HashMap<>();

      for (Map.Entry<String, Set<String>> entry : linkInstancesIdsMap.entrySet()) {
         Map<String, DataDocument> dataMap = linkDataDao.getData(entry.getKey(), entry.getValue()).stream()
                                                        .collect(Collectors.toMap(DataDocument::getId, dataDocument -> dataDocument));

         allDataMap.putAll(dataMap);
      }

      return new ArrayList<>(linkInstances).stream()
                                           .peek(linkInstance -> linkInstance.setData(Objects.requireNonNullElse(allDataMap.get(linkInstance.getId()), new DataDocument())))
                                           .collect(Collectors.toSet());
   }

   private java.util.Collection<LinkInstance> searchLinkInstancesByStems(Query query, List<LinkType> readLinkTypes) {
      SearchQuery searchQuery = buildSearchQuery(encodeQuery(query));
      Set<String> linkTypeIds = readLinkTypes.stream().map(LinkType::getId).collect(Collectors.toSet());
      List<LinkInstance> linkInstances = linkInstanceDao.searchLinkInstances(searchQuery).stream()
                                                        .filter(linkInstance -> linkTypeIds.contains(linkInstance.getLinkTypeId()))
                                                        .collect(Collectors.toList());
      return setDataForLinkInstances(linkInstances);
   }

   private java.util.Collection<LinkInstance> searchLinkInstancesByFulltexts(Query query, List<LinkType> linkTypes) {
      List<DataDocument> data = linkDataDao.searchDataByFulltexts(query.getFulltexts(), query.getPagination(), linkTypes);
      return convertDataDocumentsToLinkInstances(data);
   }

   private java.util.Collection<LinkInstance> convertDataDocumentsToLinkInstances(java.util.Collection<DataDocument> data) {
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstances(data.stream().map(DataDocument::getId).collect(Collectors.toSet()));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return linkInstances.stream()
                          .peek(linkInstance -> linkInstance.setData(Objects.requireNonNullElse(dataMap.get(linkInstance.getId()), new DataDocument())))
                          .collect(Collectors.toSet());
   }

   private List<LinkType> getReadLinkTypes() {
      final Set<String> allowedCollectionIds = getReadCollections().stream().map(Resource::getId)
                                                                   .collect(Collectors.toSet());
      return linkTypeDao.getAllLinkTypes().stream()
                        .filter(lt -> allowedCollectionIds.containsAll(lt.getCollectionIds()))
                        .collect(Collectors.toList());
   }

   private Query encodeQuery(Query query) {
      Set<String> filterCollectionIds = query.getAttributeFilters().stream().map(io.lumeer.api.model.CollectionAttributeFilter::getCollectionId).collect(Collectors.toSet());
      List<Collection> collections = collectionDao.getCollectionsByIds(filterCollectionIds);
      Set<String> filterLinkTypeIds = query.getLinkAttributeFilters().stream().map(io.lumeer.api.model.LinkAttributeFilter::getLinkTypeId).collect(Collectors.toSet());
      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByIds(filterLinkTypeIds);

      return constraintManager.encodeQuery(query, collections, linkTypes);
   }

   private SearchQuery buildSearchQuery(Query query) {
      return SearchQuery.createBuilder(authenticatedUser.getCurrentUserId())
                        .groups(authenticatedUserGroups.getCurrentUserGroups())
                        .queryStems(query.getStems(), query.getFulltexts())
                        .page(query.getPage())
                        .pageSize(query.getPageSize())
                        .build();
   }

   public List<Document> searchDocuments(final Query query) {
      final Query encodedQuery = encodeQuery(query);
      final List<Collection> collections = getReadCollections();
      final Map<String, Collection> collectionMap = collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
      final List<Document> result;

      if (encodedQuery.isEmpty()) {
         result = new ArrayList<>(getChildDocuments(searchDocumentsByEmptyQuery(encodedQuery, collections)));
      } else if (encodedQuery.containsStems()) {
         result = new ArrayList<>(searchDocumentsByStems(encodedQuery, collections));
      } else {
         result = new ArrayList<>(getChildDocuments(searchDocumentsByFulltexts(encodedQuery, collections)));
      }

      result.forEach(document -> constraintManager.decodeDataTypes(collectionMap.get(document.getCollectionId()), document.getData()));

      return result;
   }

   private List<Collection> getReadCollections() {
      return collectionDao.getAllCollections().stream()
                          .filter(collection -> permissionsChecker.hasRoleWithView(collection, Role.READ, Role.READ))
                          .collect(Collectors.toList());
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
      List<Document> documents = documentDao.getDocumentsByIds(data.stream().map(DataDocument::getId).distinct().toArray(String[]::new));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return documents.stream()
                      .peek(document -> document.setData(Objects.requireNonNullElse(dataMap.get(document.getId()), new DataDocument())))
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

      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, lt -> lt));

      List<DataDocument> lastStageData = data;

      for (int i = 0; i < stemsPipeline.size(); i++) {
         String linkTypeId = stem.getLinkTypeIds().get(i);
         SearchQueryStem currentStageStem = stemsPipeline.get(i);

         Set<String> lastStageDocumentIds = lastStageData.stream().map(DataDocument::getId).collect(Collectors.toSet());
         List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstancesByDocumentIds(lastStageDocumentIds, linkTypeId);

         Set<String> searchedLinkInstanceIds;

         if (currentStageStem.containsLinkFiltersQuery() || currentStageStem.containsFulltextsQuery()) {
            SearchQueryStem linkSearchStem = SearchQueryStem.createBuilder(currentStageStem.getCollectionId())
                                                            .linkInstanceIds(linkInstances.stream().map(LinkInstance::getId).collect(Collectors.toSet()))
                                                            .linkFilters(currentStageStem.getLinkFilters())
                                                            .fulltexts(currentStageStem.getFulltexts())
                                                            .build();

            searchedLinkInstanceIds = linkDataDao.searchData(linkSearchStem, pagination, linkTypesMap.get(linkTypeId))
                                                 .stream().map(DataDocument::getId).collect(Collectors.toSet());
         } else {
            searchedLinkInstanceIds = linkInstances.stream().map(LinkInstance::getId).collect(Collectors.toSet());
         }

         Set<String> otherDocumentIds = linkInstances.stream()
                                                     .filter(linkInstance -> searchedLinkInstanceIds.contains(linkInstance.getId()))
                                                     .map(LinkInstance::getDocumentIds)
                                                     .flatMap(List::stream)
                                                     .filter(id -> !lastStageDocumentIds.contains(id))
                                                     .collect(Collectors.toSet());

         Set<String> currentDocumentsIds = new HashSet<>(currentStageStem.getDocumentIds());

         if (currentStageStem.containsDocumentIdsQuery()) {
            currentDocumentsIds.retainAll(otherDocumentIds);
         } else {
            currentDocumentsIds.addAll(otherDocumentIds);
         }

         if (currentDocumentsIds.isEmpty()) {
            break; // empty ids after intersection or append represents empty search, so we should break
         }

         SearchQueryStem modifiedStem = SearchQueryStem.createBuilder(currentStageStem.getCollectionId())
                                                       .linkTypeIds(currentStageStem.getLinkTypeIds())
                                                       .documentIds(currentDocumentsIds)
                                                       .filters(currentStageStem.getFilters())
                                                       .fulltexts(currentStageStem.getFulltexts())
                                                       .build();

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
      return cleanStemForCollectionAndLink(stem, documents, stem.getCollectionId(), null);
   }

   private SearchQueryStem cleanStemForCollectionAndLink(SearchQueryStem stem, List<Document> documents, String collectionId, String linkTypeId) {
      Set<CollectionAttributeFilter> filters = stem.getFilters().stream()
                                                   .filter(filter -> filter.getCollectionId().equals(collectionId))
                                                   .collect(Collectors.toSet());

      Set<LinkAttributeFilter> linkFilters;
      if (linkTypeId != null) {
         linkFilters = stem.getLinkFilters().stream()
                           .filter(filter -> filter.getLinkTypeId().equals(linkTypeId))
                           .collect(Collectors.toSet());
      } else {
         linkFilters = new HashSet<>();
      }

      Set<String> documentIds = documents.stream().filter(d -> d.getCollectionId().equals(collectionId) && stem.getDocumentIds().contains(d.getId()))
                                         .map(Document::getId)
                                         .collect(Collectors.toSet());

      return SearchQueryStem.createBuilder(collectionId)
                            .fulltexts(stem.getFulltexts())
                            .filters(filters)
                            .linkFilters(linkFilters)
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

         stemsPipeline.add(cleanStemForCollectionAndLink(stem, allDocuments, currentCollectionId, linkType.getId()));
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
