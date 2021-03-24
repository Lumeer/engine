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
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionAttributeFilter;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.ConditionType;
import io.lumeer.api.model.ConditionValueType;
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
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.auth.RequestDataKeeper;
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

   @Inject
   private RequestDataKeeper requestDataKeeper;

   private ConstraintManager constraintManager;
   private Language language;
   private String timezone;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      language = Language.fromString(requestDataKeeper.getUserLocale());
      timezone = requestDataKeeper.getTimezone();
   }

   private static final Integer FETCH_SIZE = 200;

   public void setLanguage(final Language language) {
      this.language = language;
   }

   public List<LinkInstance> getLinkInstancesPublic(Query query) {
      return searchLinkInstances(query, true, true);
   }

   public List<LinkInstance> searchLinkInstances(Query query, boolean includeChildDocuments) {
      return searchLinkInstances(query, false, includeChildDocuments);
   }

   private List<LinkInstance> searchLinkInstances(Query query, boolean isPublic, boolean includeChildDocuments) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      return searchDocumentsAndLinks(query, includeChildDocuments, true, collectionsMap, linkTypesMap, null).getSecond();
   }

   public List<Document> searchDocumentsPublic(final Query query) {
      return searchDocuments(query, true, true);
   }

   public List<Document> searchDocuments(final Query query, boolean includeChildDocuments) {
      return searchDocuments(query, false, includeChildDocuments);
   }

   private List<Document> searchDocuments(final Query query, boolean isPublic, boolean includeChildDocuments) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      return searchDocumentsAndLinks(query, includeChildDocuments, true, collectionsMap, linkTypesMap, null).getFirst();
   }

   private Map<String, Collection> getCollectionsMap(java.util.Collection<Collection> collections) {
      return collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
   }

   private Map<String, LinkType> getLinkTypeMap(java.util.Collection<LinkType> linkTypes) {
      return linkTypes.stream().collect(Collectors.toMap(LinkType::getId, linkType -> linkType));
   }

   public Tuple<List<Document>, List<LinkInstance>> searchTasksDocumentsAndLinks(final Query query, boolean includeChildDocuments) {
      return searchTasksDocumentsAndLinks(query, false, includeChildDocuments);
   }

   public Tuple<List<Document>, List<LinkInstance>> searchTasksDocumentsAndLinksPublic(final Query query) {
      return searchTasksDocumentsAndLinks(query, true, true);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchTasksDocumentsAndLinks(final Query query, boolean isPublic, boolean includeChildDocuments) {
      var resources = getReadResources(isPublic, query);
      final List<Collection> collections = resources.getFirst().stream().filter(collection -> collection.getPurposeType() == CollectionPurposeType.Tasks).collect(Collectors.toList());
      final List<LinkType> linkTypes = filterLinkTypesByCollections(resources.getSecond(), collections);
      final Map<String, Collection> collectionsMap = getCollectionsMap(collections);
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(linkTypes);
      final Query tasksQuery = modifyQueryForTasks(isPublic, query, collections);
      if (tasksQuery == null) {
         return new Tuple<>(Collections.emptyList(), Collections.emptyList());
      }
      final Function<Document, Boolean> documentFilter = query.isEmpty() ? document -> !CollectionPurposeUtils.isDoneState(document.getData(), collectionsMap.get(document.getCollectionId())) : null;
      return searchDocumentsAndLinks(tasksQuery, includeChildDocuments, !isPublic && query.isEmpty(), collectionsMap, linkTypesMap, documentFilter);
   }

   private Query modifyQueryForTasks(boolean isPublic, final Query query, final List<Collection> collections) {
      if (isPublic || !query.isEmpty()) {
         return query;
      }

      final List<QueryStem> stems = collections.stream().map(collection -> {
         final String assigneeAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_ASSIGNEE_ATTRIBUTE_ID) : null;
         final Attribute assigneeAttribute = ResourceUtils.findAttribute(collection.getAttributes(), assigneeAttributeId);
         if (assigneeAttribute != null) {
            final CollectionAttributeFilter filter = CollectionAttributeFilter.createFromTypes(collection.getId(), assigneeAttribute.getId(), ConditionType.HAS_SOME, ConditionValueType.CURRENT_USER.getValue());
            return new QueryStem(collection.getId(), Collections.emptyList(), Collections.emptySet(), Collections.singletonList(filter), Collections.emptyList());
         }
         return new QueryStem(collection.getId(), Collections.emptyList(), Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
      }).collect(Collectors.toList());

      return stems.isEmpty() ? null : new Query(stems);
   }

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, boolean includeChildDocuments) {
      return searchDocumentsAndLinks(query, false, includeChildDocuments);
   }

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinksPublic(final Query query) {
      return searchDocumentsAndLinks(query, true, true);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, boolean isPublic, boolean includeChildDocuments) {
      var resources = getReadResources(isPublic, query);
      final Map<String, Collection> collectionsMap = getCollectionsMap(resources.getFirst());
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(resources.getSecond());
      return searchDocumentsAndLinks(query, includeChildDocuments, true, collectionsMap, linkTypesMap, null);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, boolean includeChildDocuments, boolean shouldCheckQuery, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, @Nullable final Function<Document, Boolean> documentFilter) {
      final Query encodedQuery = checkQuery(query, collectionsMap, linkTypesMap, shouldCheckQuery);

      final Set<Document> allDocuments = new HashSet<>();
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      if (encodedQuery.containsStems()) {
         ConstraintData constraintData = createConstraintData();
         encodedQuery.getStems().forEach(stem -> {
            var result = stem.containsAnyFilter() || encodedQuery.getFulltexts().size() > 0
                  ? searchDocumentsAndLinksInStem(stem, encodedQuery.getFulltexts(), collectionsMap, linkTypesMap, documentFilter, constraintData, includeChildDocuments)
                  : searchDocumentsAndLinksInStemWithoutFilters(stem, collectionsMap, linkTypesMap, documentFilter);
            allDocuments.addAll(result.getFirst());
            allLinkInstances.addAll(result.getSecond());
         });
      } else if (encodedQuery.getFulltexts().size() > 0) {
         var result = searchDocumentsAndLinksByFulltexts(encodedQuery.getFulltexts(), collectionsMap, linkTypesMap, documentFilter, createConstraintData(), includeChildDocuments);
         allDocuments.addAll(result.getFirst());
         allLinkInstances.addAll(result.getSecond());
      } else {
         var result = searchDocumentsAndLinksByEmptyQuery(collectionsMap, linkTypesMap, documentFilter);
         allDocuments.addAll(result.getFirst());
         allLinkInstances.addAll(result.getSecond());
      }

      return new Tuple<>(new ArrayList<>(allDocuments), new ArrayList<>(allLinkInstances));
   }

   private ConstraintData createConstraintData() {
      return new ConstraintData(
            userDao.getAllUsers(workspaceKeeper.getOrganizationId()),
            authenticatedUser.getCurrentUser(),
            translationManager.translateDurationUnitsMap(language),
            new CurrencyData(translationManager.translateAbbreviations(language), translationManager.translateOrdinals(language)),
            timezone
      );
   }

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksInStem(final QueryStem stem, final Set<String> fulltexts, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, @Nullable final Function<Document, Boolean> documentFilter, final ConstraintData constraintData,
         boolean includeChildDocuments) {
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
         var firstCollectionDocuments = getDocumentsByCollection(previousCollection, page * FETCH_SIZE, FETCH_SIZE, null);
         var previousDocuments = filterDocumentsByDocumentFilter(firstCollectionDocuments, documentFilter);
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
            var result = DataFilter.filterDocumentsAndLinksByQueryFromJson(new ArrayList<>(currentDocuments), allCollections, allLinkTypes, new ArrayList<>(currentLinkInstances), query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildDocuments, language);
            allDocuments.addAll(result.getFirst());
            allLinkInstances.addAll(result.getSecond());
         }
         page++;
         hasMoreDocuments = !firstCollectionDocuments.isEmpty();
      }

      return new Tuple<>(allDocuments, allLinkInstances);
   }

   private Tuple<List<Collection>, List<LinkType>> getResourcesFromStem(final QueryStem stem, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap) {
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

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksInStemWithoutFilters(final QueryStem stem, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, @Nullable final Function<Document, Boolean> documentFilter) {
      var previousCollection = collectionsMap.get(stem.getCollectionId());
      if (previousCollection == null) {
         return new Tuple<>(new HashSet<>(), new HashSet<>());
      }

      final Set<Document> allDocuments = new HashSet<>(getDocumentsByCollection(previousCollection, null, documentFilter));
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      for (String linkTypeId : stem.getLinkTypeIds()) {
         var linkType = linkTypesMap.get(linkTypeId);
         var collection = getOtherCollection(linkType, collectionsMap, Utils.computeIfNotNull(previousCollection, Collection::getId));
         if (linkType != null && collection != null) {
            var links = getLinkInstancesByLinkType(linkType, null);
            var documents = getDocumentsByCollection(collection, null, documentFilter);

            allDocuments.addAll(documents);
            allLinkInstances.addAll(links);

            previousCollection = collection;
         }
      }

      return new Tuple<>(allDocuments, allLinkInstances);
   }

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksByFulltexts(final Set<String> fulltexts, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, @Nullable final Function<Document, Boolean> documentFilter, final ConstraintData constraintData,
         boolean includeChildDocuments) {
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
               var result = DataFilter.filterDocumentsAndLinksByQueryFromJson(new ArrayList<>(documents), collections, Collections.emptyList(), new ArrayList<>(), query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildDocuments, language);
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
               var result = DataFilter.filterDocumentsAndLinksByQueryFromJson(new ArrayList<>(), collections, linkTypes, linkInstances, query, collectionsPermissions, linkTypesPermissions, constraintData, true, language);
               allLinkInstances.addAll(result.getSecond());
            }
            hasMoreLinks = !linkInstances.isEmpty();
            page++;
         }
      });

      return new Tuple<>(allDocuments, allLinkInstances);
   }

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksByEmptyQuery(final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, @Nullable final Function<Document, Boolean> documentFilter) {
      final Set<Document> allDocuments = new HashSet<>();
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      collectionsMap.values().forEach(collection -> allDocuments.addAll(getDocumentsByCollection(collection, null, documentFilter)));

      linkTypesMap.values().forEach(linkType -> allLinkInstances.addAll(getLinkInstancesByLinkType(linkType, null)));

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

   private Query checkQuery(final Query query, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, boolean shouldCheckQuery) {
      final View view = permissionsChecker.getActiveView();
      final Set<String> viewCollectionIds = view != null ? QueryUtils.getQueryCollectionIds(view.getQuery(), linkTypeDao.getLinkTypesByIds(view.getQuery().getLinkTypeIds())) : Collections.emptySet();
      if (shouldCheckQuery && view != null && !permissionsChecker.hasRole(view, Role.MANAGE) && !canReadQueryResources(query, collectionsMap, linkTypesMap, viewCollectionIds)) {
         return constraintManager.decodeQuery(view.getQuery(), collectionsMap, linkTypesMap);
      }

      return constraintManager.decodeQuery(query, collectionsMap, linkTypesMap);
   }

   private boolean canReadQueryResources(final Query query, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, final Set<String> viewCollectionsIds) {
      return query.getStems().stream().allMatch(stem -> {
         var collection = collectionsMap.get(stem.getCollectionId());
         if (!canReadCollection(collection, viewCollectionsIds)) {
            return false;
         }
         var linkTypes = stem.getLinkTypeIds().stream().map(linkTypesMap::get).collect(Collectors.toList());
         for (LinkType linkType : linkTypes) {
            if (!canReadLinkType(linkType, collectionsMap, viewCollectionsIds)) {
               return false;
            }
         }
         return true;
      });
   }

   private boolean canReadCollection(final Collection collection, final Set<String> viewCollectionsIds) {
      return collection != null && (permissionsChecker.hasRole(collection, Role.READ) || viewCollectionsIds.contains(collection.getId()));
   }

   private boolean canReadLinkType(final LinkType linkType, final Map<String, Collection> collectionsMap, final Set<String> viewCollectionsIds) {
      if (linkType != null) {
         var collections = linkType.getCollectionIds().stream().map(collectionsMap::get).filter(Objects::nonNull).collect(Collectors.toList());
         return collections.size() == 2 && collections.stream().allMatch(collection -> canReadCollection(collection, viewCollectionsIds));
      }
      return false;
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

   private List<Document> getDocumentsByCollection(Collection collection, @Nullable Set<String> documentIds, @Nullable final Function<Document, Boolean> documentFilter) {
      var documents = convertDataDocumentsToDocuments(getDocumentData(collection, documentIds));
      return filterDocumentsByDocumentFilter(documents, documentFilter);
   }

   private List<Document> filterDocumentsByDocumentFilter(final List<Document> documents, @Nullable final Function<Document, Boolean> documentFilter) {
      if (documentFilter != null) {
         return documents.stream().filter(documentFilter::apply).collect(Collectors.toList());
      }
      return documents;
   }

   private List<DataDocument> getDocumentData(Collection collection, @Nullable Set<String> documentIds) {
      List<DataDocument> data = documentIds != null ? dataDao.getData(collection.getId(), documentIds) : dataDao.getData(collection.getId());
      return decodeData(collection, data);
   }

   private List<DataDocument> decodeData(Collection collection, List<DataDocument> data) {
      return data.stream().map(d -> constraintManager.decodeDataTypes(collection, d)).collect(Collectors.toList());
   }

   private List<Document> getDocumentsByCollection(Collection collection, Integer skip, Integer limit, @Nullable final Function<Document, Boolean> documentFilter) {
      List<DataDocument> data = decodeData(collection, dataDao.getData(collection.getId(), skip, limit));
      var documents = convertDataDocumentsToDocuments(data);
      return filterDocumentsByDocumentFilter(documents, documentFilter);
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
      return getLinkInstancesWithData(linkType);
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

   private List<LinkInstance> getLinkInstancesWithData(final LinkType linkType) {
      final List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstancesByLinkType(linkType.getId());
      return assignDataDocumentsLinkInstances(linkInstances, linkType);
   }
}
