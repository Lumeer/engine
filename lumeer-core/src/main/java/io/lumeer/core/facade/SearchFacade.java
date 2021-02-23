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
import io.lumeer.api.model.CurrencyData;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.util.CollectionPurposeUtils;
import io.lumeer.core.util.QueryUtils;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.Utils;
import io.lumeer.core.util.js.DataFilter;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
   private UserDao userDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private TranslationManager translationManager;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   private ConstraintManager constraintManager;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
   }

   private static final Integer FETCH_SIZE = 200;

   public List<LinkInstance> getLinkInstancesPublic(Query query, Language language) {
      return getLinkInstances(query, language, true);
   }

   public List<LinkInstance> getLinkInstances(Query query, Language language) {
      return getLinkInstances(query, language, false);
   }

   private List<LinkInstance> getLinkInstances(Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      return searchDocumentsAndLinks(query, language, isPublic, collectionsMap, linkTypesMap, document -> true).getSecond();
   }

   public List<Document> searchDocumentsPublic(final Query query, Language language) {
      return searchDocuments(query, language, true);
   }

   public List<Document> searchDocuments(final Query query, Language language) {
      return searchDocuments(query, language, false);
   }

   private List<Document> searchDocuments(final Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      return searchDocumentsAndLinks(query, language, isPublic, collectionsMap, linkTypesMap, document -> true).getFirst();
   }

   private Map<String, Collection> getCollectionsMap(java.util.Collection<Collection> collections) {
      return collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
   }

   private Map<String, LinkType> getLinkTypeMap(java.util.Collection<LinkType> linkTypes) {
      return linkTypes.stream().collect(Collectors.toMap(LinkType::getId, linkType -> linkType));
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
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(linkTypes);
      return searchDocumentsAndLinks(query, language, isPublic, collectionsMap, linkTypesMap, document -> !CollectionPurposeUtils.isDoneState(document.getData(), collectionsMap.get(document.getCollectionId())));
   }

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, Language language) {
      return searchDocumentsAndLinks(query, language, false);
   }

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinksPublic(final Query query, Language language) {
      return searchDocumentsAndLinks(query, language, true);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      return searchDocumentsAndLinks(query, language, isPublic, collectionsMap, linkTypesMap, document -> true);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, @Nullable Language language, boolean isPublic, final Map<String, Collection> collectionsMap, Map<String, LinkType> linkTypesMap, final Function<Document, Boolean> documentFilter) {
      final Query encodedQuery = checkQuery(query, collectionsMap, linkTypesMap, isPublic);

      final Set<Document> allDocuments = new HashSet<>();
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      final ConstraintData constraintData = new ConstraintData(
            userDao.getAllUsers(workspaceKeeper.getOrganizationId()),
            authenticatedUser.getCurrentUser(),
            translationManager.translateDurationUnitsMap(language),
            new CurrencyData(translationManager.translateAbbreviations(language), translationManager.translateOrdinals(language))
      );

      if (encodedQuery.containsStems()) {
         encodedQuery.getStems().forEach(stem -> {
            var result = stem.containsAnyFilter() || encodedQuery.getFulltexts().size() > 0
                  ? searchDocumentsAndLinksInStem(stem, encodedQuery.getFulltexts(), language, collectionsMap, linkTypesMap, documentFilter, constraintData)
                  : searchDocumentsAndLinksInStemWithoutFilters(stem, collectionsMap, linkTypesMap, documentFilter);
            allDocuments.addAll(result.getFirst());
            allLinkInstances.addAll(result.getSecond());
         });
      } else {
         var result = searchDocumentsAndLinksByFulltexts(encodedQuery.getFulltexts(), language, collectionsMap, linkTypesMap, documentFilter, constraintData);
         allDocuments.addAll(result.getFirst());
         allLinkInstances.addAll(result.getSecond());
      }

      return new Tuple<>(new ArrayList<>(allDocuments), new ArrayList<>(allLinkInstances));
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

         if (!currentDocuments.isEmpty()) {
            var result = DataFilter.filterDocumentsAndLinksByQueryFromJson(new ArrayList<>(currentDocuments), allCollections, allLinkTypes, new ArrayList<>(currentLinkInstances), query, collectionsPermissions, linkTypesPermissions, constraintData, true, language != null ? language : Language.EN);
            allDocuments.addAll(result.getFirst());
            allLinkInstances.addAll(result.getSecond());
         }
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
            if (!documents.isEmpty()) {
               var result = DataFilter.filterDocumentsAndLinksByQueryFromJson(new ArrayList<>(documents), collections, Collections.emptyList(), new ArrayList<>(), query, collectionsPermissions, linkTypesPermissions, constraintData, true, language != null ? language : Language.EN);
               allDocuments.addAll(result.getFirst());
            }
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
            if (!linkInstances.isEmpty()) {
               var result = DataFilter.filterDocumentsAndLinksByQueryFromJson(new ArrayList<>(), collections, linkTypes, linkInstances, query, collectionsPermissions, linkTypesPermissions, constraintData, true, language != null ? language : Language.EN);
               allLinkInstances.addAll(result.getSecond());
            }
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

      return constraintManager.decodeQuery(query, collectionsMap, linkTypesMap);
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
      return convertDataDocumentsToDocuments(getDocumentData(collection, documentIds))
            .stream().filter(documentFilter::apply).collect(Collectors.toList());
   }

   private List<DataDocument> getDocumentData(Collection collection, @Nullable Set<String> documentIds) {
      List<DataDocument> data = documentIds != null ? dataDao.getData(collection.getId(), documentIds) : dataDao.getData(collection.getId());
      return decodeData(collection, data);
   }

   private List<DataDocument> decodeData(Collection collection, List<DataDocument> data) {
      return data.stream().map(d -> constraintManager.decodeDataTypes(collection, d)).collect(Collectors.toList());
   }

   private List<Document> getDocumentsByCollection(Collection collection, Integer skip, Integer limit, final Function<Document, Boolean> documentFilter) {
      List<DataDocument> data = decodeData(collection, dataDao.getData(collection.getId(), skip, limit));
      return convertDataDocumentsToDocuments(data)
            .stream().filter(documentFilter::apply).collect(Collectors.toList());
   }

   private List<Document> convertDataDocumentsToDocuments(java.util.Collection<DataDocument> data) {
      List<Document> documents = documentDao.getDocumentsByIds(data.stream().map(DataDocument::getId).distinct().toArray(String[]::new));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return documents.stream()
                      .peek(document -> document.setData(Objects.requireNonNullElse(dataMap.get(document.getId()), new DataDocument())))
                      .collect(Collectors.toList());
   }

   private List<LinkInstance> getLinkInstancesByLinkType(LinkType linkType, @Nullable Set<String> documentIds) {
      if (documentIds != null) {
         return assignDataDocumentsLinkInstances(linkInstanceDao.getLinkInstancesByDocumentIds(documentIds, linkType.getId()), linkType);
      }
      return convertDataDocumentsToLinkInstances(getLinkInstanceData(linkType, null));
   }

   private List<LinkInstance> getLinkInstancesByLinkType(LinkType linkType, Integer skip, Integer limit) {
      List<DataDocument> data = decodeData(linkType, linkDataDao.getData(linkType.getId(), skip, limit));
      return convertDataDocumentsToLinkInstances(data);
   }

   private List<DataDocument> decodeData(LinkType linkType, List<DataDocument> data) {
      return data.stream().map(d -> constraintManager.decodeDataTypes(linkType, d)).collect(Collectors.toList());
   }

   private List<LinkInstance> convertDataDocumentsToLinkInstances(java.util.Collection<DataDocument> data) {
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstances(data.stream().map(DataDocument::getId).collect(Collectors.toSet()));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return linkInstances.stream()
                          .peek(linkInstance -> linkInstance.setData(Objects.requireNonNullElse(dataMap.get(linkInstance.getId()), new DataDocument())))
                          .collect(Collectors.toList());
   }

   private List<LinkInstance> assignDataDocumentsLinkInstances(java.util.Collection<LinkInstance> linkInstances, LinkType linkType) {
      List<DataDocument> data = getLinkInstanceData(linkType, linkInstances.stream().map(LinkInstance::getId).collect(Collectors.toSet()));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return linkInstances.stream()
                          .peek(linkInstance -> linkInstance.setData(Objects.requireNonNullElse(dataMap.get(linkInstance.getId()), new DataDocument())))
                          .collect(Collectors.toList());
   }

   private List<DataDocument> getLinkInstanceData(LinkType linkType, @Nullable Set<String> linkInstanceIds) {
      List<DataDocument> data = linkInstanceIds != null ? linkDataDao.getData(linkType.getId(), linkInstanceIds) : linkDataDao.getData(linkType.getId());
      return decodeData(linkType, data);
   }

}
