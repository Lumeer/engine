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
import io.lumeer.api.model.ConstraintData;
import io.lumeer.api.model.CurrencyData;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.js.DataFiltersJsParser;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.util.Collections;
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

   public List<LinkInstance> getLinkInstancesPublic(Query query) {
      return getLinkInstances(query, true);
   }

   public List<LinkInstance> getLinkInstances(Query query) {
      return getLinkInstances(query, false);
   }

   public List<LinkInstance> getLinkInstances(Query query, boolean isPublic) {
      // TODO improve performance
      final List<LinkType> linkTypes = getReadLinkTypes(isPublic);
      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, l -> l));

      final List<LinkInstance> result = searchDocumentsAndLinks(query, isPublic).getSecond();

      result.forEach(linkInstance ->
            linkInstance.setData(constraintManager.decodeDataTypes(linkTypesMap.get(linkInstance.getLinkTypeId()), linkInstance.getData()))
      );

      return result;
   }

   public List<Document> searchDocumentsPublic(final Query query) {
      return searchDocuments(query, true);
   }

   public List<Document> searchDocuments(final Query query) {
      return searchDocuments(query, false);
   }

   public List<Document> searchDocuments(final Query query, boolean isPublic) {
      // TODO improve performance
      final List<Collection> collections = getReadCollections(isPublic);
      final Map<String, Collection> collectionMap = collections.stream().collect(Collectors.toMap(Resource::getId, collection -> collection));
      final List<Document> result = searchDocumentsAndLinks(query, isPublic).getFirst();

      result.forEach(document ->
            document.setData(constraintManager.decodeDataTypes(collectionMap.get(document.getCollectionId()), document.getData()))
      );

      return result;
   }

   public Tuple<List<Document>, List<LinkInstance>> searchDocumentsAndLinks(final Query query, boolean isPublic) {
      final Query encodedQuery = encodeQuery(query);
      final List<Collection> collections = getReadCollections(isPublic);
      final List<Document> documents = getDocumentsByCollections(collections);
      final List<LinkType> linkTypes = getLinkTypesByCollections(collections);
      final List<LinkInstance> linkInstances = getLinkInstancesByLinkTypes(linkTypes);
      final Map<String, AllowedPermissions> collectionsPermissions = collections.stream().collect(Collectors.toMap(Resource::getId, c -> new AllowedPermissions(true, true, true)));
      final Map<String, AllowedPermissions> linkTypesPermissions = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, c -> new AllowedPermissions(true, true, true)));
      final ConstraintData constraintData = new ConstraintData(Collections.emptyList(), null, Collections.emptyMap(), new CurrencyData(Collections.emptyList(), Collections.emptyList()));
      return DataFiltersJsParser.filterDocumentsAndLinksByQuery(documents, collections, linkTypes, linkInstances, encodedQuery, collectionsPermissions, linkTypesPermissions, constraintData, true);
   }

   private Query encodeQuery(Query query) {
      Set<String> filterCollectionIds = query.getAttributeFilters().stream().map(io.lumeer.api.model.CollectionAttributeFilter::getCollectionId).collect(Collectors.toSet());
      List<Collection> collections = collectionDao.getCollectionsByIds(filterCollectionIds);
      Set<String> filterLinkTypeIds = query.getLinkAttributeFilters().stream().map(io.lumeer.api.model.LinkAttributeFilter::getLinkTypeId).collect(Collectors.toSet());
      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByIds(filterLinkTypeIds);

      return constraintManager.encodeQuery(query, collections, linkTypes);
   }

   private List<Collection> getReadCollections(boolean isPublic) {
      if (isPublic && this.permissionsChecker.isPublic()) {
         return collectionDao.getAllCollections();
      }

      return collectionDao.getAllCollections().stream()
                          .filter(collection -> permissionsChecker.hasRoleWithView(collection, Role.READ, Role.READ))
                          .collect(Collectors.toList());
   }

   private List<LinkType> getReadLinkTypes(boolean isPublic) {
      if (isPublic && this.permissionsChecker.isPublic()) {
         return linkTypeDao.getAllLinkTypes();
      }// TODO improve performance
      return getLinkTypesByCollections(getReadCollections(isPublic));
   }

   private List<LinkType> getLinkTypesByCollections(java.util.Collection<Collection> collections) {
      final Set<String> allowedCollectionIds = collections.stream().map(Resource::getId)
                                                                           .collect(Collectors.toSet());
      return linkTypeDao.getAllLinkTypes().stream()
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
