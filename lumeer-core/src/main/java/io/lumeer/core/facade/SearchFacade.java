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
package io.lumeer.core.facade;

import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.ConstraintData;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.CollectionPurposeUtils;
import io.lumeer.core.util.QueryUtils;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.filter.CollectionSearchAttributeFilter;
import io.lumeer.storage.api.filter.LinkSearchAttributeFilter;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import com.mongodb.lang.Nullable;

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

   private static final Integer FETCH_SIZE = 200;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
   }

   private Map<String, Collection> getCollectionsMap(java.util.Collection<Collection> collections) {
      return collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
   }

   private Map<String, LinkType> getLinkTypeMap(java.util.Collection<LinkType> linkTypes) {
      return linkTypes.stream().collect(Collectors.toMap(LinkType::getId, linkType -> linkType));
   }

   public List<LinkInstance> getLinkInstancesPublic(Query query) {
      return getLinkInstances(query, true);
   }

   public List<LinkInstance> getLinkInstances(Query query, Language language) {
      return getLinkInstances(query, false);
   }

   public List<LinkInstance> getLinkInstances(Query query, boolean isPublic) {
      final Query encodedQuery = encodeQuery(query);
      final List<LinkType> linkTypes = getReadLinkTypes(isPublic);
      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, l -> l));

      final List<LinkInstance> result;

      if (encodedQuery.isEmpty()) {
         result = new ArrayList<>(searchLinkInstancesByEmptyQuery(linkTypes));
      } else if (encodedQuery.containsStems()) {
         result = new ArrayList<>(searchLinkInstancesByStems(encodedQuery, linkTypes));
      } else {
         result = new ArrayList<>(searchLinkInstancesByFulltexts(encodedQuery, linkTypes));
      }

      result.forEach(linkInstance ->
            linkInstance.setData(constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()))
      );

      return result;
   }

   private java.util.Collection<LinkInstance> searchLinkInstancesByEmptyQuery(List<LinkType> linkTypes) {
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstancesByLinkTypes(linkTypes.stream().map(LinkType::getId).collect(Collectors.toSet()));
      return setDataForLinkInstances(linkInstances);
   }

   java.util.Collection<LinkInstance> setDataForLinkInstances(java.util.Collection<LinkInstance> linkInstances) {
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

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, Language language) {
      return searchDocumentsAndLinks(query, language, false);
   }

   public List<LinkInstance> getLinkInstancesPublic(Query query, Language language) {
      return getLinkInstances(query, language, true);
   }

   private List<LinkInstance> getLinkInstances(Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      final List<LinkInstance> result = searchDocumentsAndLinks(query, language, isPublic, collectionsMap, linkTypesMap, document -> true).getSecond();

      result.forEach(linkInstance ->
            linkInstance.setData(constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()))
      );

      return result;
   }

   public List<Document> searchDocumentsPublic(final Query query, Language language) {
      return searchDocuments(query, language, true);
   }

   private List<Document> searchDocuments(final Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      final List<Document> result = searchDocumentsAndLinks(query, language, isPublic, collectionsMap, linkTypesMap, document -> true).getFirst();

      result.forEach(document ->
            document.setData(constraintManager.decodeDataTypes(collectionsMap.get(document.getCollectionId()), document.getData()))
      );

      return result;
   }

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinksPublic(final Query query, Language language) {
      return searchDocumentsAndLinks(query, language, true);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      final Tuple<List<Document>, List<LinkInstance>> result = searchDocumentsAndLinks(query, language, isPublic, collectionsMap, linkTypesMap, document -> true);

      result.getFirst().forEach(document ->
            document.setData(constraintManager.decodeDataTypes(collectionsMap.get(document.getCollectionId()), document.getData()))
      );

      result.getSecond().forEach(linkInstance ->
            linkInstance.setData(constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()))
      );

      return result;
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, @Nullable Language language, boolean isPublic, final Map<String, Collection> collectionsMap, Map<String, LinkType> linkTypesMap, final Function<Document, Boolean> documentFilter) {
      final Query encodedQuery = checkQuery(query, collectionsMap, linkTypesMap, isPublic);

      final Set<Document> allDocuments = new HashSet<>();
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      if (encodedQuery.containsStems()) {
         encodedQuery.getStems().forEach(stem -> {
            var result = stem.containsAnyFilter() || encodedQuery.getFulltexts().size() > 0
                  ? searchDocumentsAndLinksInStem(stem, encodedQuery.getFulltexts(), language, collectionsMap, linkTypesMap, documentFilter, null)
                  : searchDocumentsAndLinksInStemWithoutFilters(stem, collectionsMap, linkTypesMap, documentFilter);
            allDocuments.addAll(result.getFirst());
            allLinkInstances.addAll(result.getSecond());
         });
      } else {
         var result = searchDocumentsAndLinksByFulltexts(encodedQuery.getFulltexts(), language, collectionsMap, linkTypesMap, documentFilter, null);
         allDocuments.addAll(result.getFirst());
         allLinkInstances.addAll(result.getSecond());
      }

      return new Tuple<>(new ArrayList<>(allDocuments), new ArrayList<>(allLinkInstances));
   }

   public Tuple<List<Document>, List<LinkInstance>> searchTasksDocumentsAndLinks(final Query query, Language language) {
      return searchTasksDocumentsAndLinks(query, language, false);
   }

   public Tuple<List<Document>, List<LinkInstance>> searchTasksDocumentsAndLinksPublic(final Query query, Language language) {
      return searchTasksDocumentsAndLinks(query, language, true);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchTasksDocumentsAndLinks(final Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final List<Collection> collections = resources.getFirst().stream().filter(collection -> collection.getPurposeType() == CollectionPurposeType.Tasks).collect(Collectors.toList());
      final List<LinkType> linkTypes = filterLinkTypesByCollections(resources.getSecond(), collections);
      final Map<String, Collection> collectionsMap = getCollectionsMap(collections);
      Map<String, LinkType> linkTypesMap = getLinkTypeMap(linkTypes);
      final Tuple<List<Document>, List<LinkInstance>> result = searchDocumentsAndLinks(query, language, isPublic, collectionsMap, linkTypesMap, document -> !CollectionPurposeUtils.isDoneState(document.getData(), collectionsMap.get(document.getCollectionId())));

      result.getFirst().forEach(document ->
            document.setData(constraintManager.decodeDataTypes(collectionsMap.get(document.getCollectionId()), document.getData()))
      );

      result.getSecond().forEach(linkInstance ->
            linkInstance.setData(constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()))
      );

      return result;
   }

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksInStem(final QueryStem stem, final Set<String> fulltexts, @Nullable Language language, final Map<String, Collection> collectionsMap, Map<String, LinkType> linkTypesMap, final Function<Document, Boolean> documentFilter, final ConstraintData constraintData) {
      final Set<Document> allDocuments = new HashSet<>();
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      var resources = getResourcesFromStem(stem, collectionsMap, linkTypesMap);
      final List<Collection> allCollections = resources.getFirst();
      final List<LinkType> allLinkTypes = resources.getSecond();

      if (allCollections.isEmpty()) {
         return new Tuple<>(allDocuments, allLinkInstances);
      }

      final Map<String, AllowedPermissions> collectionsPermissions = permissionsChecker.getCollectionsPermissions(allCollections);
      final Map<String, AllowedPermissions> linkTypesPermissions = permissionsChecker.getLinkTypesPermissions(allLinkTypes, collectionsPermissions);
      final Query query = new Query(Collections.singletonList(stem), fulltexts, null, null);

      var hasMoreDocuments = true;
      var page = 0;

      while (hasMoreDocuments) {
         var previousCollection = allCollections.get(0);
         var firstCollectionDocuments = getDocumentsByCollection(previousCollection, page * FETCH_SIZE, FETCH_SIZE, document -> true);
         var previousDocuments = firstCollectionDocuments.stream().filter(documentFilter::apply).collect(Collectors.toList());
         final Set<Document> currentDocuments = new HashSet<>(previousDocuments);
         final Set<LinkInstance> currentLinkInstances = new HashSet<>();

         for (String linkTypeId : stem.getLinkTypeIds()) {
            var linkType = linkTypesMap.get(linkTypeId);
            var collection = getOtherCollection(linkType, collectionsMap, Utils.computeIfNotNull(previousCollection, Collection::getId));
            if (linkType != null && previousCollection != null) {
               var links = getLinkInstancesByLinkType(linkType, getDocumentsIds(previousDocuments));
               var documents = getDocumentsByCollection(collection, getLinkDocumentsIds(links), documentFilter);

               currentDocuments.addAll(documents);
               currentLinkInstances.addAll(links);

               previousCollection = collection;
               previousDocuments = documents;
            }
         }

         //var result = DataFilter.filterDocumentsAndLinksByQuery(new ArrayList<>(currentDocuments), allCollections, allLinkTypes, new ArrayList<>(currentLinkInstances), query, collectionsPermissions, linkTypesPermissions, constraintData, true, language != null ? language : Language.EN);
         allDocuments.addAll(currentDocuments /*result.getFirst()*/);
         allLinkInstances.addAll(currentLinkInstances /*result.getSecond()*/);

         page++;
         hasMoreDocuments = !firstCollectionDocuments.isEmpty();
      }

      return new Tuple<>(allDocuments, allLinkInstances);
   }

   private Tuple<List<Collection>, List<LinkType>> getResourcesFromStem(final QueryStem stem, final Map<String, Collection> collectionsMap, Map<String, LinkType> linkTypesMap) {
      var previousCollection = collectionsMap.get(stem.getCollectionId());
      if (previousCollection == null) {
         return new Tuple<>(new ArrayList<>(), new ArrayList<>());
      }

      final List<Collection> allCollections = new ArrayList<>(Collections.singleton(previousCollection));
      final List<LinkType> allLinkTypes = new ArrayList<>();
      for (String linkTypeId : stem.getLinkTypeIds()) {
         var linkType = linkTypesMap.get(linkTypeId);
         var collection = getOtherCollection(linkType, collectionsMap, Utils.computeIfNotNull(previousCollection, Collection::getId));
         if (linkType != null && collection != null) {
            allCollections.add(collection);
            allLinkTypes.add(linkType);

            previousCollection = collection;
         }

      }
      return new Tuple<>(allCollections, allLinkTypes);
   }

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksInStemWithoutFilters(final QueryStem stem, final Map<String, Collection> collectionsMap, Map<String, LinkType> linkTypesMap, final Function<Document, Boolean> documentFilter) {
      var previousCollection = collectionsMap.get(stem.getCollectionId());
      if (previousCollection == null) {
         return new Tuple<>(new HashSet<>(), new HashSet<>());
      }

      var previousDocuments = getDocumentsByCollection(previousCollection, null, documentFilter);
      final Set<Document> allDocuments = new HashSet<>(previousDocuments);
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      for (String linkTypeId : stem.getLinkTypeIds()) {
         var linkType = linkTypesMap.get(linkTypeId);
         var collection = getOtherCollection(linkType, collectionsMap, Utils.computeIfNotNull(previousCollection, Collection::getId));
         if (linkType != null && collection != null) {
            var links = getLinkInstancesByLinkType(linkType, getDocumentsIds(previousDocuments));
            var documents = getDocumentsByCollection(collection, getLinkDocumentsIds(links), documentFilter);

            allDocuments.addAll(documents);
            allLinkInstances.addAll(links);

            previousCollection = collection;
            previousDocuments = documents;
         }
      }

      return new Tuple<>(allDocuments, allLinkInstances);
   }

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksByFulltexts(final Set<String> fulltexts, @Nullable Language language, final Map<String, Collection> collectionsMap, Map<String, LinkType> linkTypesMap, final Function<Document, Boolean> documentFilter, final ConstraintData constraintData) {
      final Set<Document> allDocuments = new HashSet<>();
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      // because we are filtering documents (or links) without linked documents, so it is safe to fetch more
      var fetchSizeMultiplier = 3;
      var fetchSize = FETCH_SIZE * fetchSizeMultiplier;

      collectionsMap.values().forEach(collection -> {
         final List<Collection> collections = Collections.singletonList(collection);
         final Map<String, AllowedPermissions> collectionsPermissions = permissionsChecker.getCollectionsPermissions(collections);
         final Map<String, AllowedPermissions> linkTypesPermissions = Collections.emptyMap();
         final Query query = new Query(Collections.emptyList(), fulltexts, null, null);
         var hasMoreDocuments = true;
         var page = 0;
         while (hasMoreDocuments) {
            final List<Document> documents = getDocumentsByCollection(collection, page * fetchSize, fetchSize, documentFilter);
            //var result = DataFilter.filterDocumentsAndLinksByQuery(new ArrayList<>(documents), collections, Collections.emptyList(), new ArrayList<>(), query, collectionsPermissions, linkTypesPermissions, constraintData, true, language != null ? language : Language.EN);
            allDocuments.addAll(documents /*result.getFirst()*/);

            hasMoreDocuments = !documents.isEmpty();
            page++;
         }
      });

      linkTypesMap.values().forEach(linkType -> {
         final List<LinkType> linkTypes = Collections.singletonList(linkType);
         final List<Collection> collections = linkType.getCollectionIds().stream().map(collectionsMap::get).filter(Objects::nonNull).collect(Collectors.toList());
         final Map<String, AllowedPermissions> collectionsPermissions = permissionsChecker.getCollectionsPermissions(collections);
         final Map<String, AllowedPermissions> linkTypesPermissions = permissionsChecker.getLinkTypesPermissions(linkTypes, collectionsPermissions);
         final Query query = new Query(Collections.emptyList(), fulltexts, null, null);
         var hasMoreLinks = true;
         var page = 0;
         while (hasMoreLinks) {
            final List<LinkInstance> linkInstances = getLinkInstancesByLinkType(linkType, page * fetchSize, fetchSize);
            //var result = DataFilter.filterDocumentsAndLinksByQuery(new ArrayList<>(), collections, linkTypes, linkInstances, query, collectionsPermissions, linkTypesPermissions, constraintData, true, language != null ? language : Language.EN);
            allLinkInstances.addAll(linkInstances /*.getSecond()*/);

            hasMoreLinks = !linkInstances.isEmpty();
            page++;
         }
      });

      return new Tuple<>(allDocuments, allLinkInstances);
   }

   private Collection getOtherCollection(final LinkType linkType, final Map<String, Collection> collectionMap, final String collectionId) {
      var otherCollectionId = QueryUtils.getOtherCollectionId(linkType, collectionId);
      if (otherCollectionId != null) {
         return collectionMap.get(otherCollectionId);
      }
      return null;
   }

   private Set<String> getDocumentsIds(final java.util.Collection<Document> documents) {
      return documents.stream().map(Document::getId).collect(Collectors.toSet());
   }

   private Set<String> getLinkDocumentsIds(final java.util.Collection<LinkInstance> linkInstances) {
      return linkInstances.stream()
                          .map(LinkInstance::getDocumentIds)
                          .flatMap(List::stream)
                          .collect(Collectors.toSet());
   }

   private Query checkQuery(final Query query, final Map<String, Collection> collectionsMap, Map<String, LinkType> linkTypesMap, boolean isPublic) {
      final View view = permissionsChecker.getActiveView();
      if (!isPublic && view != null && !permissionsChecker.hasRole(view, Role.MANAGE)) {
         return view.getQuery();
      }

      return constraintManager.encodeQuery(query, collectionsMap, linkTypesMap);
   }

   private Tuple<List<Collection>, List<LinkType>> getReadResources(boolean isPublic, Query query) {
      if (isPublic && this.permissionsChecker.isPublic()) {
         var collections = collectionDao.getAllCollections();
         var linkTypes = getLinkTypesByCollections(collections);
         return new Tuple<>(collections, linkTypes);
      }

      List<Collection> collections;
      List<LinkType> linkTypes;
      if (query.containsStems()) {
         var linkTypeIds = query.getLinkTypeIds();
         linkTypes = linkTypeDao.getLinkTypesByIds(linkTypeIds);
         var collectionIds = QueryUtils.getQueryCollectionIds(query, linkTypes);
         collections = collectionDao.getCollectionsByIds(collectionIds);
      } else {
         linkTypes = linkTypeDao.getAllLinkTypes();
         collections = collectionDao.getAllCollections();
      }

      var filteredCollections = collections.stream()
                                           .filter(collection -> permissionsChecker.hasRoleWithView(collection, Role.READ, Role.READ))
                                           .collect(Collectors.toList());
      var filteredLinkTypes = filterLinkTypesByCollections(linkTypes, filteredCollections);
      return new Tuple<>(filteredCollections, filteredLinkTypes);
   }

   private List<LinkType> getLinkTypesByCollections(java.util.Collection<Collection> collections) {
      return filterLinkTypesByCollections(linkTypeDao.getAllLinkTypes(), collections);
   }

   private List<LinkType> filterLinkTypesByCollections(java.util.Collection<LinkType> linkTypes, java.util.Collection<Collection> collections) {
      final Set<String> allowedCollectionIds = collections.stream().map(Resource::getId)
                                                          .collect(Collectors.toSet());

      return linkTypes.stream()
                      .filter(lt -> allowedCollectionIds.containsAll(lt.getCollectionIds()))
                      .collect(Collectors.toList());
   }

   private List<Document> getDocumentsByCollection(Collection collection, @Nullable Set<String> documentIds, final Function<Document, Boolean> documentFilter) {
      if (documentIds != null) {
         return convertDataDocumentsToDocuments(dataDao.getData(collection.getId(), documentIds))
               .stream().filter(documentFilter::apply).collect(Collectors.toList());
      }
      return convertDataDocumentsToDocuments(dataDao.getData(collection.getId()))
            .stream().filter(documentFilter::apply).collect(Collectors.toList());
   }

   private List<Document> getDocumentsByCollection(Collection collection, Integer skip, Integer limit, final Function<Document, Boolean> documentFilter) {
      return convertDataDocumentsToDocuments(dataDao.getData(collection.getId(), skip, limit))
            .stream().filter(documentFilter::apply).collect(Collectors.toList());
   }

   private java.util.Collection<LinkInstance> convertDataDocumentsToLinkInstances(java.util.Collection<DataDocument> data) {
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstances(data.stream().map(DataDocument::getId).collect(Collectors.toSet()));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return linkInstances.stream()
                          .peek(linkInstance -> linkInstance.setData(Objects.requireNonNullElse(dataMap.get(linkInstance.getId()), new DataDocument())))
                          .collect(Collectors.toSet());
   }

   private List<LinkType> getReadLinkTypes(boolean isPublic) {
      if (isPublic && this.permissionsChecker.isPublic()) {
         return linkTypeDao.getAllLinkTypes();
      }
      final Set<String> allowedCollectionIds = getReadCollections(isPublic).stream().map(Resource::getId)
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

   public List<Document> searchDocumentsPublic(final Query query) {
      return searchDocuments(query, true);
   }

   public List<Document> searchDocuments(final Query query, final Language language) {
      return searchDocuments(query, false);
   }

   public List<Document> searchDocuments(final Query query, boolean isPublic) {
      final Query encodedQuery = encodeQuery(query);
      final List<Collection> collections = getReadCollections(isPublic);
      final Map<String, Collection> collectionMap = collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
      final List<Document> result;

      if (encodedQuery.isEmpty()) {
         result = new ArrayList<>(getChildDocuments(searchDocumentsByEmptyQuery(encodedQuery, collections)));
      } else if (encodedQuery.containsStems()) {
         result = new ArrayList<>(searchDocumentsByStems(encodedQuery, collections));
      } else {
         result = new ArrayList<>(getChildDocuments(searchDocumentsByFulltexts(encodedQuery, collections)));
      }

      result.forEach(document ->
            document.setData(constraintManager.decodeDataTypes(collectionMap.get(document.getCollectionId()), document.getData()))
      );

      return result;
   }

   private List<Collection> getReadCollections(boolean isPublic) {
      if (isPublic && this.permissionsChecker.isPublic()) {
         return collectionDao.getAllCollections();
      }

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

   private List<LinkInstance> getLinkInstancesByLinkType(LinkType linkType, Integer skip, Integer limit) {
      return convertDataDocumentsToLinkInstances2(linkDataDao.getData(linkType.getId(), skip, limit));
   }

   private List<LinkInstance> convertDataDocumentsToLinkInstances2(java.util.Collection<DataDocument> data) {
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstances(data.stream().map(DataDocument::getId).collect(Collectors.toSet()));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return linkInstances.stream()
                          .peek(linkInstance -> linkInstance.setData(Objects.requireNonNullElse(dataMap.get(linkInstance.getId()), new DataDocument())))
                          .collect(Collectors.toList());
   }

   private List<LinkInstance> assignDataDocumentsLinkInstances(java.util.Collection<LinkInstance> linkInstances, String linkTypeId) {
      List<DataDocument> data = linkDataDao.getData(linkTypeId, linkInstances.stream().map(LinkInstance::getId).collect(Collectors.toSet()));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return linkInstances.stream()
                          .peek(linkInstance -> linkInstance.setData(Objects.requireNonNullElse(dataMap.get(linkInstance.getId()), new DataDocument())))
                          .collect(Collectors.toList());
   }

   private List<LinkInstance> getLinkInstancesByLinkType(LinkType linkType, @Nullable Set<String> documentIds) {
      if (documentIds != null) {
         return assignDataDocumentsLinkInstances(linkInstanceDao.getLinkInstancesByDocumentIds(documentIds, linkType.getId()), linkType.getId());
      }
      return convertDataDocumentsToLinkInstances2(linkDataDao.getData(linkType.getId()));
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

            Collection collection = collectionsMap.get(stem.getCollectionId());
            if (collection != null) {
               List<DataDocument> stemData = dataDao.searchData(cleanedStem, searchQuery.getPagination(), collection);
               Set<Document> documentsByData = convertDataDocumentsToDocuments(stemData).stream()
                                                                                        .filter(document -> document.getCollectionId().equals(stem.getCollectionId()))
                                                                                        .collect(Collectors.toSet());
               data.addAll(getChildDocuments(documentsByData));
            }
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

      Set<String> lastStageIds = documentsByData.stream().map(Document::getId).collect(Collectors.toSet());

      for (int i = 0; i < stemsPipeline.size(); i++) {
         String linkTypeId = stem.getLinkTypeIds().get(i);
         SearchQueryStem currentStageStem = stemsPipeline.get(i);

         List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstancesByDocumentIds(lastStageIds, linkTypeId);

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

         final Set<String> lastStageIdsCopy = Collections.unmodifiableSet(lastStageIds);
         Set<String> otherDocumentIds = linkInstances.stream()
                                                     .filter(linkInstance -> searchedLinkInstanceIds.contains(linkInstance.getId()))
                                                     .map(LinkInstance::getDocumentIds)
                                                     .flatMap(List::stream)
                                                     .filter(id -> !lastStageIdsCopy.contains(id))
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
         lastStageIds = currentStageData.stream().map(DataDocument::getId).collect(Collectors.toSet());
      }

      return documentsByData;
   }

   private SearchQueryStem cleanStemForBaseCollection(SearchQueryStem stem, List<Document> documents) {
      return cleanStemForCollectionAndLink(stem, documents, stem.getCollectionId(), null);
   }

   private SearchQueryStem cleanStemForCollectionAndLink(SearchQueryStem stem, List<Document> documents, String collectionId, String linkTypeId) {
      Set<CollectionSearchAttributeFilter> filters = stem.getFilters().stream()
                                                         .filter(filter -> filter.getCollectionId().equals(collectionId))
                                                         .collect(Collectors.toSet());

      Set<LinkSearchAttributeFilter> linkFilters;
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
      return getChildDocumentsBfs(new HashSet<>(rootDocuments), rootDocuments, false);
   }

   private Set<Document> getChildDocumentsBfs(final Set<Document> result, final Set<Document> rootDocuments, boolean setData) {

      // find all document where parentId == one of root documents
      // add root documents to result when they do not exist there
      // add results to rootDocuments
      // repeat while there were any results

      List<Document> nextLevel = documentDao.getDocumentsByParentIds(
            rootDocuments.stream()
                         .map(Document::getId)
                         .collect(Collectors.toSet()))
                                            .stream()
                                            .filter(d -> !result.contains(d))
                                            .collect(Collectors.toList());

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