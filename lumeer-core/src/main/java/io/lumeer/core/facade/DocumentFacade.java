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

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Role;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.query.SearchQuery;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
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

   public Document createDocument(String collectionCode, Document document) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.WRITE);

      DataDocument data = DocumentUtils.checkDocumentKeysValidity(document.getData());

      Document storedDocument = createDocument(collection, document);

      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), data);
      storedDocument.setData(storedData);

      updateCollectionMetadataOnCreation(collection, data);

      return storedDocument;
   }

   private Document createDocument(Collection collection, Document document) {
      document.setCollectionId(collection.getId());
      document.setCreatedBy(authenticatedUser.getCurrentUsername());
      document.setCreationDate(LocalDateTime.now());
      document.setDataVersion(INITIAL_VERSION);
      return documentDao.createDocument(document);
   }

   private void updateCollectionMetadataOnCreation(Collection collection, DataDocument data) {
      Map<String, Attribute> oldAttributes = collection.getAttributes().stream()
                                                       .collect(Collectors.toMap(Attribute::getFullName, Function.identity()));
      Set<Attribute> newAttributes = new LinkedHashSet<>();

      Set<String> attributeNames = DocumentUtils.getDocumentAttributes(data);
      attributeNames.forEach(attributeName -> {
         if (oldAttributes.containsKey(attributeName)) {
            Attribute attribute = oldAttributes.get(attributeName);
            attribute.setUsageCount(attribute.getUsageCount() + 1);
         } else {
            Attribute attribute = new JsonAttribute(attributeName, attributeName, Collections.emptySet(), 1);
            newAttributes.add(attribute);
         }
      });

      newAttributes.addAll(oldAttributes.values());
      collection.setAttributes(newAttributes);
      collection.setDocumentsCount(collection.getDocumentsCount() + 1);
      collection.setLastTimeUsed(LocalDateTime.now());
      collectionDao.updateCollection(collection.getId(), collection);
   }

   public Document updateDocumentData(String collectionCode, String documentId, DataDocument data) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.WRITE);

      // TODO archive the old document
      DataDocument updatedData = dataDao.updateData(collection.getId(), documentId, data);

      Document updatedDocument = updateDocument(collection, documentId);
      updatedDocument.setData(updatedData);

      return updatedDocument;
   }

   public Document patchDocumentData(String collectionCode, String documentId, DataDocument data) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.WRITE);

      // TODO archive the old document
      DataDocument patchedData = dataDao.patchData(collection.getId(), documentId, data);

      Document updatedDocument = updateDocument(collection, documentId);
      updatedDocument.setData(patchedData);

      return updatedDocument;
   }

   private Document updateDocument(Collection collection, String documentId) {
      Document document = documentDao.getDocumentById(documentId);

      document.setCollectionId(collection.getId());
      document.setUpdatedBy(authenticatedUser.getCurrentUsername());
      document.setUpdateDate(LocalDateTime.now());
      document.setDataVersion(document.getDataVersion() + 1);

      return documentDao.updateDocument(document.getId(), document);
   }

   public void deleteDocument(String collectionCode, String documentId) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.WRITE);

      documentDao.deleteDocument(documentId);

      dataDao.deleteData(collection.getId(), documentId);
   }

   public Document getDocument(String collectionCode, String documentId) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.READ);

      Document document = documentDao.getDocumentById(documentId);
      document.setCollectionCode(collectionCode);

      DataDocument data = dataDao.getData(collection.getId(), documentId);
      document.setData(data);

      return document;
   }

   public List<Document> getDocuments(String collectionCode, Pagination pagination) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.READ);

      Map<String, DataDocument> dataDocuments = getDataDocuments(collection.getId(), pagination);

      return getDocuments(collectionCode, dataDocuments);
   }

   private Map<String, DataDocument> getDataDocuments(String collectionId, Pagination pagination) {
      SearchQuery searchQuery = createPaginationQuery(pagination);
      return dataDao.getData(collectionId, searchQuery).stream()
                    .collect(Collectors.toMap(DataDocument::getId, Function.identity()));
   }

   private List<Document> getDocuments(String collectionCode, Map<String, DataDocument> dataDocuments) {
      String[] documentIds = dataDocuments.keySet().toArray(new String[] {});
      List<Document> documents = documentDao.getDocumentsByIds(documentIds);
      documents.forEach(document -> {
         document.setCollectionCode(collectionCode);
         document.setData(dataDocuments.get(document.getId()));
      });
      return documents;
   }
}
