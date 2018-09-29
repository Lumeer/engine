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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.SearchQuery;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class DocumentFacade extends AbstractFacade {

   public static final Integer INITIAL_VERSION = 1;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public Document createDocument(String collectionId, Document document) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);
      permissionsChecker.checkDocumentLimits(document);

      DataDocument data = document.getData();

      Document storedDocument = createDocument(collection, document);

      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), data);
      storedDocument.setData(storedData);

      updateCollectionMetadata(collection, data.keySet(), Collections.emptySet(), 1);

      return storedDocument;
   }

   private Document createDocument(Collection collection, Document document) {
      document.setCollectionId(collection.getId());
      document.setCreatedBy(authenticatedUser.getCurrentUserId());
      document.setCreationDate(ZonedDateTime.now());
      document.setDataVersion(INITIAL_VERSION);
      return documentDao.createDocument(document);
   }

   public Document updateDocumentData(String collectionId, String documentId, DataDocument data) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      DataDocument oldData = dataDao.getData(collectionId, documentId);
      Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(oldData.keySet());

      Set<String> attributesIdsToDec = new HashSet<>(oldData.keySet());
      attributesIdsToDec.removeAll(data.keySet());

      updateCollectionMetadata(collection, attributesIdsToAdd, attributesIdsToDec, 0);

      // TODO archive the old document
      DataDocument updatedData = dataDao.updateData(collection.getId(), documentId, data);

      Document updatedDocument = updateDocument(collection, documentId);
      updatedDocument.setData(updatedData);

      return updatedDocument;
   }

   public Document updateDocumentMetaData(final String collectionId, final String documentId, final DataDocument metaData) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      final Document document = documentDao.getDocumentById(documentId);
      document.setMetaData(metaData);

      return updateDocument(document);
   }

   public Document patchDocumentData(String collectionId, String documentId, DataDocument data) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      DataDocument oldData = dataDao.getData(collectionId, documentId);

      Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(oldData.keySet());

      updateCollectionMetadata(collection, attributesIdsToAdd, Collections.emptySet(), 0);

      // TODO archive the old document
      DataDocument patchedData = dataDao.patchData(collection.getId(), documentId, data);

      Document updatedDocument = updateDocument(collection, documentId);
      updatedDocument.setData(patchedData);

      return updatedDocument;
   }

   public Document patchDocumentMetaData(final String collectionId, final String documentId, final DataDocument metaData) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      final Document document = documentDao.getDocumentById(documentId);
      if (document.getMetaData() == null) {
         document.setMetaData(new DataDocument());
      }
      metaData.forEach((key, value) -> document.getMetaData().put(key, value));

      return updateDocument(document);
   }

   private Document updateDocument(final Collection collection, final String documentId) {
      final Document document = documentDao.getDocumentById(documentId);
      document.setCollectionId(collection.getId());

      return updateDocument(document);
   }

   private Document updateDocument(final Document document) {
      document.setUpdatedBy(authenticatedUser.getCurrentUserId());
      document.setUpdateDate(ZonedDateTime.now());
      document.setDataVersion(document.getDataVersion() + 1);

      return documentDao.updateDocument(document.getId(), document);
   }

   public void deleteDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      DataDocument data = dataDao.getData(collectionId, documentId);
      updateCollectionMetadata(collection, Collections.emptySet(), data.keySet(), -1);

      documentDao.deleteDocument(documentId);
      dataDao.deleteData(collection.getId(), documentId);

      deleteDocumentBasedData(collectionId, documentId);

   }

   private void deleteDocumentBasedData(String collectionId, String documentId) {
      linkInstanceDao.deleteLinkInstances(createQueryForLinkInstances(documentId));
      favoriteItemDao.removeFavoriteDocumentFromUsers(getCurrentProject().getId(), collectionId, documentId);
   }

   public boolean isFavorite(String documentId) {
      return getFavoriteDocumentsIds().contains(documentId);
   }

   public Set<String> getFavoriteDocumentsIds() {
      String projectId = getCurrentProject().getId();
      String userId = getCurrentUser().getId();

      return favoriteItemDao.getFavoriteDocumentIds(userId, projectId);
   }

   public void addFavoriteDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.WRITE);

      String projectId = getCurrentProject().getId();
      String userId = getCurrentUser().getId();
      favoriteItemDao.addFavoriteDocument(userId, projectId, collectionId, documentId);
   }

   public void removeFavoriteDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.WRITE);

      String userId = getCurrentUser().getId();
      favoriteItemDao.removeFavoriteDocument(userId, documentId);
   }

   private void updateCollectionMetadata(Collection collection, Set<String> attributesIdsToInc, Set<String> attributesIdsToDec, int documentCountDiff) {
      Map<String, Attribute> oldAttributes = collection.getAttributes().stream()
                                                       .collect(Collectors.toMap(Attribute::getId, Function.identity()));
      Set<String> incrementedAttributeIds = new HashSet<>(attributesIdsToInc);

      oldAttributes.keySet().forEach(attributeId -> {
         if (incrementedAttributeIds.contains(attributeId)) {
            Attribute attribute = oldAttributes.get(attributeId);
            attribute.setUsageCount(attribute.getUsageCount() + 1);
            incrementedAttributeIds.remove(attributeId);
         } else if (attributesIdsToDec.contains(attributeId)) {
            Attribute attribute = oldAttributes.get(attributeId);
            attribute.setUsageCount(Math.max(attribute.getUsageCount() - 1, 0));
         }

      });

      collection.setAttributes(new HashSet<>(oldAttributes.values()));
      collection.setLastTimeUsed(ZonedDateTime.now());
      collection.setDocumentsCount(collection.getDocumentsCount() + documentCountDiff);
      collectionDao.updateCollection(collection.getId(), collection);
   }

   public Document getDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.READ, Role.READ);

      Document document = documentDao.getDocumentById(documentId);

      DataDocument data = dataDao.getData(collection.getId(), documentId);
      document.setData(data);

      return document;
   }

   public List<Document> getDocuments(String collectionId, Pagination pagination) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.READ, Role.READ);

      Map<String, DataDocument> dataDocuments = getDataDocuments(collection.getId(), pagination);

      return getDocuments(dataDocuments);
   }

   private Project getCurrentProject() {
      if (!workspaceKeeper.getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return workspaceKeeper.getProject().get();
   }

   private User getCurrentUser() {
      return authenticatedUser.getCurrentUser();
   }

   private Map<String, DataDocument> getDataDocuments(String collectionId, Pagination pagination) {
      SearchQuery searchQuery = createPaginationQuery(pagination);
      return dataDao.getData(collectionId, searchQuery).stream()
                    .collect(Collectors.toMap(DataDocument::getId, Function.identity()));
   }

   private List<Document> getDocuments(Map<String, DataDocument> dataDocuments) {
      String[] documentIds = dataDocuments.keySet().toArray(new String[] {});
      List<Document> documents = documentDao.getDocumentsByIds(documentIds);
      documents.forEach(document -> document.setData(dataDocuments.get(document.getId())));
      return documents;
   }

   private SearchQuery createQueryForLinkInstances(String documentId) {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return SearchQuery.createBuilder(user).groups(groups)
                        .documentIds(Collections.singleton(documentId))
                        .build();
   }
}
