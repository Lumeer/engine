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
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.util.CollectionPurposeUtils;
import io.lumeer.core.util.QueryUtils;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.js.DataFiltersJsParser;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jetbrains.annotations.Nullable;

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

   public List<LinkInstance> getLinkInstancesPublic(Query query, Language language) {
      return getLinkInstances(query, language, true);
   }

   public List<LinkInstance> getLinkInstances(Query query, Language language) {
      return getLinkInstances(query, language, false);
   }

   private List<LinkInstance> getLinkInstances(Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final List<Collection> collections = resources.getFirst();
      final List<LinkType> linkTypes = resources.getSecond();
      final List<LinkInstance> result = searchDocumentsAndLinks(query, language, isPublic, collections, linkTypes, document -> true).getSecond();

      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, l -> l));
      result.forEach(linkInstance ->
            linkInstance.setData(constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()))
      );

      return result;
   }

   public List<Document> searchDocumentsPublic(final Query query, Language language) {
      return searchDocuments(query, language, true);
   }

   public List<Document> searchDocuments(final Query query, Language language) {
      return searchDocuments(query, language, false);
   }

   private List<Document> searchDocuments(final Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final List<Collection> collections = resources.getFirst();
      final List<LinkType> linkTypes = resources.getSecond();
      final List<Document> result = searchDocumentsAndLinks(query, language, isPublic, collections, linkTypes, document -> true).getFirst();

      final Map<String, Collection> collectionMap = collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
      result.forEach(document ->
            document.setData(constraintManager.decodeDataTypes(collectionMap.get(document.getCollectionId()), document.getData()))
      );

      return result;
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
      ;
      final List<LinkType> linkTypes = filterLinkTypesByCollections(resources.getSecond(), collections);
      final Map<String, Collection> collectionMap = collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
      final Tuple<List<Document>, List<LinkInstance>> result = searchDocumentsAndLinks(query, language, isPublic, collections, linkTypes, document -> !CollectionPurposeUtils.isDoneState(document, collectionMap.get(document.getCollectionId())));

      result.getFirst().forEach(document ->
            document.setData(constraintManager.decodeDataTypes(collectionMap.get(document.getCollectionId()), document.getData()))
      );

      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, l -> l));
      result.getSecond().forEach(linkInstance ->
            linkInstance.setData(constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()))
      );

      return result;
   }

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, Language language) {
      return searchDocumentsAndLinks(query, language, false);
   }

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinksPublic(final Query query, Language language) {
      return searchDocumentsAndLinks(query, language, true);
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, Language language, boolean isPublic) {
      var resources = getReadResources(isPublic, query);
      final List<Collection> collections = resources.getFirst();
      final List<LinkType> linkTypes = resources.getSecond();
      final Tuple<List<Document>, List<LinkInstance>> result = searchDocumentsAndLinks(query, language, isPublic, collections, linkTypes, document -> true);

      final Map<String, Collection> collectionMap = collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
      result.getFirst().forEach(document ->
            document.setData(constraintManager.decodeDataTypes(collectionMap.get(document.getCollectionId()), document.getData()))
      );

      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, l -> l));
      result.getSecond().forEach(linkInstance ->
            linkInstance.setData(constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()))
      );

      return result;
   }

   private Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, @Nullable Language language, boolean isPublic, final List<Collection> collections, final List<LinkType> linkTypes, final Function<Document, Boolean> documentFilter) {
      final Query encodedQuery = checkQuery(query, collections, linkTypes, isPublic);
      final List<Document> documents = getDocumentsByCollections(collections).stream().filter(documentFilter::apply).collect(Collectors.toList());
      final List<LinkInstance> linkInstances = getLinkInstancesByLinkTypes(linkTypes);

      if (encodedQuery.containsAnyFilter()) {
         final Map<String, AllowedPermissions> collectionsPermissions = permissionsChecker.getCollectionsPermissions(collections);
         final Map<String, AllowedPermissions> linkTypesPermissions = permissionsChecker.getLinkTypesPermissions(linkTypes, collectionsPermissions);
         final ConstraintData constraintData = new ConstraintData(
               userDao.getAllUsers(workspaceKeeper.getOrganizationId()),
               authenticatedUser.getCurrentUser(),
               translationManager.translateDurationUnitsMap(language),
               new CurrencyData(translationManager.translateAbbreviations(language), translationManager.translateOrdinals(language))
         );
         return DataFiltersJsParser.filterDocumentsAndLinksByQuery(documents, collections, linkTypes, linkInstances, encodedQuery, collectionsPermissions, linkTypesPermissions, constraintData, true, language != null ? language : Language.EN);
      }

      return new Tuple<>(documents, linkInstances);
   }

   private Query checkQuery(final Query query, final List<Collection> collections, final List<LinkType> linkTypes, boolean isPublic) {
      final View view = permissionsChecker.getActiveView();
      if (!isPublic && view != null && !permissionsChecker.hasRole(view, Role.MANAGE)) {
         return view.getQuery();
      }

      return constraintManager.encodeQuery(query, collections, linkTypes);
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

   private List<Document> getDocumentsByCollections(java.util.Collection<Collection> collections) {
      return convertDataDocumentsToDocuments(collections.stream().flatMap(collection -> dataDao.getData(collection.getId()).stream()).collect(Collectors.toList()));
   }

   private List<Document> convertDataDocumentsToDocuments(java.util.Collection<DataDocument> data) {
      List<Document> documents = documentDao.getDocumentsByIds(data.stream().map(DataDocument::getId).distinct().toArray(String[]::new));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return documents.stream()
                      .peek(document -> document.setData(Objects.requireNonNullElse(dataMap.get(document.getId()), new DataDocument())))
                      .collect(Collectors.toList());
   }

   private List<LinkInstance> getLinkInstancesByLinkTypes(java.util.Collection<LinkType> linkTypes) {
      return convertDataDocumentsToLinkInstances(linkTypes.stream().flatMap(linkType -> linkDataDao.getData(linkType.getId()).stream()).collect(Collectors.toList()));
   }

   private List<LinkInstance> convertDataDocumentsToLinkInstances(java.util.Collection<DataDocument> data) {
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstances(data.stream().map(DataDocument::getId).collect(Collectors.toSet()));
      Map<String, DataDocument> dataMap = data.stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      return linkInstances.stream()
                          .peek(linkInstance -> linkInstance.setData(Objects.requireNonNullElse(dataMap.get(linkInstance.getId()), new DataDocument())))
                          .collect(Collectors.toList());
   }

}
