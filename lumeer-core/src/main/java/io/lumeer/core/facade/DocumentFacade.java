/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.core.facade;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Role;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.query.SearchQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

      Document storedDocument = createDocument(collection, document);

      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), document.getData());
      storedDocument.setData(storedData);

      return storedDocument;
   }

   private Document createDocument(Collection collection, Document document) {
      document.setCollectionId(collection.getId());
      document.setCreatedBy(authenticatedUser.getCurrentUsername());
      document.setCreationDate(LocalDateTime.now());
      document.setDataVersion(INITIAL_VERSION);
      return documentDao.createDocument(document);
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
