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
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.adapter.DocumentAdapter;
import io.lumeer.core.adapter.LinkInstanceAdapter;
import io.lumeer.core.adapter.SearchAdapter;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.util.CollectionPurposeUtils;
import io.lumeer.core.util.QueryUtils;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.Utils;
import io.lumeer.core.util.js.DataFilter;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.dao.SelectionListDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jetbrains.annotations.NotNull;
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
   private GroupDao groupDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private ResourceCommentDao resourceCommentDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private SelectionListDao selectionListDao;

   @Inject
   private TranslationManager translationManager;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   private ConstraintManager constraintManager;
   private Language language;
   private String timezone;

   private DocumentAdapter documentAdapter;
   private LinkInstanceAdapter linkInstanceAdapter;
   private SearchAdapter searchAdapter;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      language = requestDataKeeper.getUserLanguage();
      timezone = requestDataKeeper.getTimezone();

      documentAdapter = new DocumentAdapter(resourceCommentDao, favoriteItemDao);
      linkInstanceAdapter = new LinkInstanceAdapter(resourceCommentDao);
      searchAdapter = new SearchAdapter(permissionsChecker.getPermissionAdapter(), constraintManager, documentDao, dataDao, linkInstanceDao, linkDataDao);
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
      return searchDocumentsAndLinks(query, includeChildDocuments, true, collectionsMap, linkTypesMap, null, isPublic).getSecond();
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
      return searchDocumentsAndLinks(query, includeChildDocuments, true, collectionsMap, linkTypesMap, null, isPublic).getFirst();
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
      final List<Collection> collections = collectionDao.getCollectionsByPurpose(CollectionPurposeType.Tasks);
      final List<LinkType> linkTypes = Collections.emptyList(); // we don't fetch links for tasks
      final Query tasksQuery = modifyQueryForTasks(isPublic, query, collections);
      if (tasksQuery == null) {
         return new Tuple<>(Collections.emptyList(), Collections.emptyList());
      }

      final Map<String, Collection> collectionsMap = getCollectionsMap(collections);
      final Map<String, LinkType> linkTypesMap = getLinkTypeMap(linkTypes);
      final Function<Document, Boolean> documentFilter = query.isEmpty() ? document -> !CollectionPurposeUtils.isDoneState(document.getData(), collectionsMap.get(document.getCollectionId())) : null;
      return searchDocumentsAndLinks(tasksQuery, includeChildDocuments, !isPublic && query.isEmpty(), collectionsMap, linkTypesMap, documentFilter, isPublic);
   }

   private Query modifyQueryForTasks(boolean isPublic, final Query query, final List<Collection> collections) {
      if (isPublic || !query.isEmpty()) {
         return query;
      }

      final List<QueryStem> stems = collections.stream().map(collection -> {
         final String assigneeAttributeId = collection.getPurpose().getAssigneeAttributeId();
         final Attribute assigneeAttribute = ResourceUtils.findAttribute(collection.getAttributes(), assigneeAttributeId);
         if (assigneeAttribute != null) {
            final CollectionAttributeFilter filter = CollectionAttributeFilter.createFromTypes(collection.getId(), assigneeAttribute.getId(), ConditionType.HAS_SOME, ConditionValueType.CURRENT_USER.getValue());
            return new QueryStem(null, collection.getId(), Collections.emptyList(), Collections.emptySet(), Collections.singletonList(filter), Collections.emptyList());
         }
         if (permissionsChecker.hasAnyRole(collection, Set.of(RoleType.DataRead, RoleType.DataContribute))) {
            return new QueryStem(null, collection.getId(), Collections.emptyList(), Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
         }
         return null;
      }).filter(Objects::nonNull).collect(Collectors.toList());

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
      return searchDocumentsAndLinks(query, includeChildDocuments, true, collectionsMap, linkTypesMap, null, isPublic);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, boolean includeChildDocuments, boolean shouldCheckQuery, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, @Nullable final Function<Document, Boolean> documentFilter, boolean isPublic) {
      final Query encodedQuery = checkQuery(query, collectionsMap, linkTypesMap, shouldCheckQuery);

      final Set<Document> allDocuments = new HashSet<>();
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      if (encodedQuery.containsStems()) {
         ConstraintData constraintData = createConstraintData();
         encodedQuery.getStems().forEach(stem -> {
            var result = stem.containsAnyFilter() || encodedQuery.getFulltexts().size() > 0
                  ? searchDocumentsAndLinksInStem(stem, encodedQuery.getFulltexts(), collectionsMap, linkTypesMap, documentFilter, constraintData, includeChildDocuments)
                  : searchDocumentsAndLinksInStemWithoutFilters(stem, collectionsMap, linkTypesMap, documentFilter, isPublic);
            allDocuments.addAll(result.getFirst());
            allLinkInstances.addAll(result.getSecond());
         });
      } else if (encodedQuery.getFulltexts().size() > 0) {
         var result = searchDocumentsAndLinksByFulltexts(encodedQuery.getFulltexts(), collectionsMap, linkTypesMap, documentFilter, createConstraintData(), includeChildDocuments);
         allDocuments.addAll(result.getFirst());
         allLinkInstances.addAll(result.getSecond());
      } else {
         var result = searchDocumentsAndLinksByEmptyQuery(collectionsMap, linkTypesMap, documentFilter, isPublic);
         allDocuments.addAll(result.getFirst());
         allLinkInstances.addAll(result.getSecond());
      }

      var mappedDocuments = documentAdapter.mapDocumentsData(new ArrayList<>(allDocuments), getCurrentUserId(), selectedWorkspace.getProjectId());
      var mappedLinkInstances = linkInstanceAdapter.mapLinkInstancesData(new ArrayList<>(allLinkInstances));

      return new Tuple<>(mappedDocuments, mappedLinkInstances);
   }

   private ConstraintData createConstraintData() {
      return new ConstraintData(
            userDao.getAllUsers(selectedWorkspace.getOrganizationId()),
            authenticatedUser.getCurrentUser(),
            translationManager.translateDurationUnitsMap(language),
            new CurrencyData(translationManager.translateAbbreviations(language), translationManager.translateOrdinals(language)),
            timezone,
            groupDao.getAllGroups(selectedWorkspace.getOrganizationId()),
            selectionListDao.getAllLists(Collections.singletonList(getProject().getId()))
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
      final Map<String, AllowedPermissions> linkTypesPermissions = permissionsChecker.getLinkTypesPermissions(allLinkTypes);
      final Query query = new Query(Collections.singletonList(stem), fulltexts, null, null);

      var hasMoreDocuments = true;
      var page = 0;

      while (hasMoreDocuments) {
         var previousCollection = allCollections.get(0);
         var firstCollectionDocuments = getDocumentsByCollection(previousCollection, page, FETCH_SIZE);
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

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksInStemWithoutFilters(final QueryStem stem, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, @Nullable final Function<Document, Boolean> documentFilter, boolean isPublic) {
      var previousCollection = collectionsMap.get(stem.getCollectionId());
      if (previousCollection == null) {
         return new Tuple<>(new HashSet<>(), new HashSet<>());
      }

      final Set<Document> allDocuments = new HashSet<>(getDocumentsByCollection(previousCollection, documentFilter, isPublic));
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      for (String linkTypeId : stem.getLinkTypeIds()) {
         var linkType = linkTypesMap.get(linkTypeId);
         var collection = getOtherCollection(linkType, collectionsMap, Utils.computeIfNotNull(previousCollection, Collection::getId));
         if (linkType != null && collection != null) {
            var links = getLinkInstancesByLinkType(linkType, isPublic);
            var documents = getDocumentsByCollection(collection, documentFilter, isPublic);

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
            final List<Document> pagedDocuments = getDocumentsByCollection(collection, page, fetchSize);
            final List<Document> filteredDocuments = filterDocumentsByDocumentFilter(pagedDocuments, documentFilter);
            if (!filteredDocuments.isEmpty()) {
               var result = DataFilter.filterDocumentsAndLinksByQueryFromJson(new ArrayList<>(filteredDocuments), collections, Collections.emptyList(), new ArrayList<>(), query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildDocuments, language);
               allDocuments.addAll(result.getFirst());
            }
            hasMoreDocuments = !pagedDocuments.isEmpty();
            page++;
         }
      });

      linkTypesMap.values().forEach(linkType -> {
         final List<LinkType> linkTypes = Collections.singletonList(linkType);
         final List<Collection> collections = linkType.getCollectionIds().stream().map(collectionsMap::get).filter(Objects::nonNull).collect(Collectors.toList());
         final Map<String, AllowedPermissions> collectionsPermissions = permissionsChecker.getCollectionsPermissions(collections);
         final Map<String, AllowedPermissions> linkTypesPermissions = permissionsChecker.getLinkTypesPermissions(linkTypes);
         final Query query = new Query(Collections.emptyList(), fulltexts, null, null);
         var hasMoreLinks = true;
         var page = 0;
         while (hasMoreLinks) {
            final List<LinkInstance> linkInstances = getLinkInstancesByLinkType(linkType, page, fetchSize);
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

   private Tuple<? extends java.util.Collection<Document>, ? extends java.util.Collection<LinkInstance>> searchDocumentsAndLinksByEmptyQuery(final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap, @Nullable final Function<Document, Boolean> documentFilter, boolean isPublic) {
      final Set<Document> allDocuments = new HashSet<>();
      final Set<LinkInstance> allLinkInstances = new HashSet<>();

      collectionsMap.values().forEach(collection -> allDocuments.addAll(getDocumentsByCollection(collection, documentFilter, isPublic)));

      linkTypesMap.values().forEach(linkType -> allLinkInstances.addAll(getLinkInstancesByLinkType(linkType, isPublic)));

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
      if (shouldCheckQuery && view != null && !permissionsChecker.hasRole(view, RoleType.QueryConfig) && !canReadQueryResources(query, collectionsMap, linkTypesMap) && !view.getAdditionalQueries().contains(query)) {
         return constraintManager.decodeQuery(view.getQuery(), collectionsMap, linkTypesMap);
      }

      return constraintManager.decodeQuery(query, collectionsMap, linkTypesMap);
   }

   private boolean canReadQueryResources(final Query query, final Map<String, Collection> collectionsMap, final Map<String, LinkType> linkTypesMap) {
      return query.getStems().stream().allMatch(stem -> {
         var collection = collectionsMap.get(stem.getCollectionId());
         if (!permissionsChecker.hasRoleInCollectionWithView(collection, RoleType.Read)) {
            return false;
         }
         var linkTypes = stem.getLinkTypeIds().stream().map(linkTypesMap::get).collect(Collectors.toList());
         for (LinkType linkType : linkTypes) {
            if (!permissionsChecker.hasRoleInLinkTypeWithView(linkType, RoleType.Read)) {
               return false;
            }
         }
         return true;
      });
   }

   private Tuple<List<Collection>, List<LinkType>> getReadResources(boolean isPublic, Query query) {
      if (isPublic && this.permissionsChecker.isPublic()) {
         var collections = collectionDao.getAllCollections();
         var linkTypes = linkTypeDao.getAllLinkTypes();
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
                                           .filter(collection -> permissionsChecker.hasRoleInCollectionWithView(collection, RoleType.Read))
                                           .collect(Collectors.toList());
      var filteredLinkTypes = linkTypes.stream()
                                       .filter(collection -> permissionsChecker.hasRoleInLinkTypeWithView(collection, RoleType.Read))
                                       .collect(Collectors.toList());
      return new Tuple<>(filteredCollections, filteredLinkTypes);
   }

   private List<Document> getDocumentsByCollection(Collection collection, @Nullable final Function<Document, Boolean> documentFilter, boolean isPublic) {
      var documents = isPublic ? searchAdapter.getAllDocuments(collection, null, null) : searchAdapter.getDocuments(getOrganization(), getProject(), collection, authenticatedUser.getCurrentUserId());
      return filterDocumentsByDocumentFilter(documents, documentFilter);
   }

   private List<Document> getDocumentsByCollection(Collection collection, @NotNull Set<String> documentIds, @Nullable final Function<Document, Boolean> documentFilter) {
      var documents = searchAdapter.getDocuments(getOrganization(), getProject(), collection, documentIds, authenticatedUser.getCurrentUserId());
      return filterDocumentsByDocumentFilter(documents, documentFilter);
   }

   private List<Document> getDocumentsByCollection(Collection collection, Integer page, Integer limit) {
      return searchAdapter.getDocuments(getOrganization(), getProject(), collection, page, limit, authenticatedUser.getCurrentUserId());
   }

   private List<Document> filterDocumentsByDocumentFilter(final List<Document> documents, @Nullable final Function<Document, Boolean> documentFilter) {
      if (documentFilter != null) {
         return documents.stream().filter(documentFilter::apply).collect(Collectors.toList());
      }
      return documents;
   }

   private List<LinkInstance> getLinkInstancesByLinkType(LinkType linkType, boolean isPublic) {
      if (isPublic) {
         return searchAdapter.getAllLinkInstances(linkType, null, null);
      }
      return searchAdapter.getLinkInstances(getOrganization(), getProject(), linkType, authenticatedUser.getCurrentUserId());
   }

   private List<LinkInstance> getLinkInstancesByLinkType(LinkType linkType, @NotNull Set<String> documentIds) {
      return searchAdapter.getLinkInstances(getOrganization(), getProject(), linkType, documentIds, authenticatedUser.getCurrentUserId());
   }

   private List<LinkInstance> getLinkInstancesByLinkType(LinkType linkType, Integer page, Integer limit) {
      return searchAdapter.getLinkInstances(getOrganization(), getProject(), linkType, page, limit, authenticatedUser.getCurrentUserId());
   }
}
