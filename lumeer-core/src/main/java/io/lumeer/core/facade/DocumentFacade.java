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
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class DocumentFacade extends AbstractFacade {

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
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private FunctionFacade functionFacade;

   @Inject
   private Event<CreateDocument> createDocumentEvent;

   private ConstraintManager constraintManager;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
   }

   public Document createDocument(String collectionId, Document document) {
      Collection collection = checkCollectionWritePermissions(collectionId);
      permissionsChecker.checkDocumentLimits(document);

      DataDocument data = document.getData();
      constraintManager.encodeDataTypes(collection, data);

      Document storedDocument = createDocument(collection, document, new DataDocument(data));


      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), data);
      storedDocument.setData(storedData);

      updateCollectionMetadata(collection, data.keySet(), Collections.emptySet(), 1);

      if (createDocumentEvent != null) {
         createDocumentEvent.fire(new CreateDocument(storedDocument));
      }

      constraintManager.decodeDataTypes(collection, storedData);

      return storedDocument;
   }

   private Document createDocument(Collection collection, Document document, DataDocument data) {
      document.setData(data);
      document.setCollectionId(collection.getId());
      document.setCreatedBy(authenticatedUser.getCurrentUserId());
      document.setCreationDate(ZonedDateTime.now());
      return documentDao.createDocument(document);
   }

   public Document updateDocumentData(String collectionId, String documentId, DataDocument data) {
      Collection collection = checkCollectionWritePermissions(collectionId);

      constraintManager.encodeDataTypes(collection, data);

      DataDocument oldData = dataDao.getData(collectionId, documentId);
      final DataDocument originalData = new DataDocument(oldData);
      Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(oldData.keySet());

      Set<String> attributesIdsToDec = new HashSet<>(oldData.keySet());
      attributesIdsToDec.removeAll(data.keySet());

      updateCollectionMetadata(collection, attributesIdsToAdd, attributesIdsToDec, 0);

      // TODO archive the old document
      DataDocument updatedData = dataDao.updateData(collection.getId(), documentId, data);
      checkAttributesValueChanges(collection, documentId, originalData, updatedData);

      final Document updatedDocument = updateDocument(collection, documentId, updatedData, originalData);
      constraintManager.decodeDataTypes(collection, updatedDocument.getData());

      return updatedDocument;
   }

   private Collection checkCollectionWritePermissions(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);
      return collection;
   }

   private void checkAttributesValueChanges(Collection collection, String documentId, DataDocument oldData, DataDocument newData) {
      Set<Attribute> attributes = collection.getAttributes();
      for (Attribute attribute : attributes) {
         Object oldValue = oldData.get(attribute.getId());
         Object newValue = newData.get(attribute.getId());
         if (!Objects.deepEquals(oldValue, newValue)) {
            functionFacade.onDocumentValueChanged(collection.getId(), attribute.getId(), documentId);
         }
      }
   }

   public Document updateDocumentMetaData(final String collectionId, final String documentId, final DataDocument metaData) {
      Collection collection = checkCollectionWritePermissions(collectionId);

      final Document document = getDocument(collection, documentId);
      final Document originalDocument = copyDocument(document);

      document.setMetaData(metaData);

      return updateDocument(document, originalDocument);
   }

   public Document patchDocumentData(String collectionId, String documentId, DataDocument data) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      constraintManager.encodeDataTypes(collection, data);

      DataDocument oldData = dataDao.getData(collectionId, documentId);
      DataDocument originalData = new DataDocument(oldData);

      Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(oldData.keySet());

      updateCollectionMetadata(collection, attributesIdsToAdd, Collections.emptySet(), 0);

      // TODO archive the old document
      DataDocument patchedData = dataDao.patchData(collection.getId(), documentId, data);
      checkAttributesValueChanges(collection, documentId, originalData, patchedData);

      final Document updatedDocument = updateDocument(collection, documentId, patchedData, originalData);
      constraintManager.decodeDataTypes(collection, updatedDocument.getData());

      return updatedDocument;
   }

   public Document patchDocumentMetaData(final String collectionId, final String documentId, final DataDocument metaData) {
      Collection collection = checkCollectionWritePermissions(collectionId);

      final Document document = getDocument(collection, documentId);
      final Document originalDocument = copyDocument(document);

      if (document.getMetaData() == null) {
         document.setMetaData(new DataDocument());
      }
      metaData.forEach((key, value) -> document.getMetaData().put(key, value));

      return updateDocument(document, originalDocument);
   }

   private Document copyDocument(final Document document) {
      final Document originalDocument = new Document(document);
      originalDocument.setMetaData(new DataDocument(originalDocument.getMetaData())); // deep copy of meta-data

      if (originalDocument.getData() != null) {
         originalDocument.setData(new DataDocument(originalDocument.getData())); // deep copy of data
      }

      return originalDocument;
   }

   private Document updateDocument(final Collection collection, final String documentId, final DataDocument newData, final DataDocument originalData) {
      final Document document = documentDao.getDocumentById(documentId);
      final Document originalDocument = copyDocument(document);
      originalDocument.setData(originalData);

      document.setCollectionId(collection.getId());
      document.setData(newData);

      final var updatedDocument = updateDocument(document, originalDocument);
      updatedDocument.setData(newData);
      return updatedDocument;
   }

   private Document updateDocument(final Document document, final Document originalDocument) {
      document.setUpdatedBy(authenticatedUser.getCurrentUserId());
      document.setUpdateDate(ZonedDateTime.now());

      return documentDao.updateDocument(document.getId(), document, originalDocument);
   }

   public void deleteDocument(String collectionId, String documentId) {
      Collection collection = checkCollectionWritePermissions(collectionId);

      DataDocument data = dataDao.getData(collectionId, documentId);
      updateCollectionMetadata(collection, Collections.emptySet(), data.keySet(), -1);

      documentDao.deleteDocument(documentId);
      dataDao.deleteData(collection.getId(), documentId);

      deleteDocumentBasedData(collectionId, documentId);
   }

   private void deleteDocumentBasedData(String collectionId, String documentId) {
      linkInstanceDao.deleteLinkInstancesByDocumentsIds(Collections.singleton(documentId));
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
      permissionsChecker.checkRole(collection, Role.READ);

      favoriteItemDao.addFavoriteDocument(getCurrentUser().getId(), getCurrentProject().getId(), collectionId, documentId);
   }

   public void removeFavoriteDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.READ);

      String userId = getCurrentUser().getId();
      favoriteItemDao.removeFavoriteDocument(userId, documentId);
   }

   private void updateCollectionMetadata(Collection collection, Set<String> attributesIdsToInc, Set<String> attributesIdsToDec, int documentCountDiff) {
      final Collection originalCollection = collection.copy();
      collection.setAttributes(new HashSet<>(ResourceUtils.incOrDecAttributes(collection.getAttributes(), attributesIdsToInc, attributesIdsToDec)));
      collection.setLastTimeUsed(ZonedDateTime.now());
      collection.setDocumentsCount(Math.max(collection.getDocumentsCount() + documentCountDiff, 0));
      collectionDao.updateCollection(collection.getId(), collection, originalCollection);
   }

   public Document getDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.READ, Role.READ);

      return getDocument(collection, documentId);
   }

   private Document getDocument(Collection collection, String documentId) {
      Document document = documentDao.getDocumentById(documentId);

      DataDocument data = dataDao.getData(collection.getId(), documentId);
      constraintManager.decodeDataTypes(collection, data);
      document.setData(data);

      return document;
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

}
