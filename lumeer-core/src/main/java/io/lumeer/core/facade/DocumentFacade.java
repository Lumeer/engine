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
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.query.SearchQuery;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
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

   @Inject
   private LinkInstanceDao linkInstanceDao;

   public Document createDocument(String collectionId, Document document) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.WRITE);

      DataDocument data = DocumentUtils.checkDocumentKeysValidity(document.getData());

      Document storedDocument = createDocument(collection, document);

      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), data);
      storedDocument.setData(storedData);

      updateCollectionMetadata(collection, data.keySet(), Collections.emptySet(), 1);

      return storedDocument;
   }

   private Document createDocument(Collection collection, Document document) {
      document.setCollectionId(collection.getId());
      document.setCreatedBy(authenticatedUser.getCurrentUsername());
      document.setCreationDate(LocalDateTime.now());
      document.setDataVersion(INITIAL_VERSION);
      return documentDao.createDocument(document);
   }

   public Document updateDocumentData(String collectionId, String documentId, DataDocument data) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.WRITE);

      DataDocument oldData = dataDao.getData(collectionId, documentId);
      Set<String> attributesToAdd = new HashSet<>(data.keySet());
      attributesToAdd.removeAll(oldData.keySet());

      Set<String> attributesToDec = new HashSet<>(oldData.keySet());
      attributesToDec.removeAll(data.keySet());

      updateCollectionMetadata(collection, attributesToAdd, attributesToDec, 0);

      // TODO archive the old document
      DataDocument updatedData = dataDao.updateData(collection.getId(), documentId, data);

      Document updatedDocument = updateDocument(collection, documentId);
      updatedDocument.setData(updatedData);

      return updatedDocument;
   }

   public Document patchDocumentData(String collectionId, String documentId, DataDocument data) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.WRITE);

      DataDocument oldData = dataDao.getData(collectionId, documentId);

      Set<String> attributesToAdd = new HashSet<>(data.keySet());
      attributesToAdd.removeAll(oldData.keySet());

      updateCollectionMetadata(collection, attributesToAdd, Collections.emptySet(), 0);

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

   public void deleteDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.WRITE);

      DataDocument data = dataDao.getData(collectionId, documentId);
      updateCollectionMetadata(collection, Collections.emptySet(), data.keySet(), -1);

      documentDao.deleteDocument(documentId);
      dataDao.deleteData(collection.getId(), documentId);
      linkInstanceDao.deleteLinkInstances(createQueryForLinkInstances(documentId));
   }

   private void updateCollectionMetadata(Collection collection, Set<String> attributesToInc, Set<String> attributesToDec, int documentCountDiff) {
      Map<String, Attribute> oldAttributes = collection.getAttributes().stream()
                                                       .collect(Collectors.toMap(Attribute::getFullName, Function.identity()));

      oldAttributes.keySet().forEach(attributeName -> {
         if (attributesToInc.contains(attributeName)) {
            Attribute attribute = oldAttributes.get(attributeName);
            attribute.setUsageCount(attribute.getUsageCount() + 1);
            attributesToInc.remove(attributeName);
         } else if (attributesToDec.contains(attributeName)) {
            Attribute attribute = oldAttributes.get(attributeName);
            attribute.setUsageCount(Math.max(attribute.getUsageCount() - 1, 0));
         }

      });

      Set<Attribute> newAttributes = attributesToInc.stream()
                                                    .map(attributeName -> new JsonAttribute(extractAttributeName(attributeName), attributeName, Collections.emptySet(), 1))
                                                    .collect(Collectors.toSet());

      newAttributes.addAll(oldAttributes.values());

      collection.setAttributes(newAttributes);
      collection.setLastTimeUsed(LocalDateTime.now());
      collection.setDocumentsCount(collection.getDocumentsCount() + documentCountDiff);
      collectionDao.updateCollection(collection.getId(), collection);
   }

   private String extractAttributeName(String attributeName) {
      String[] parts = attributeName.split(".");
      return parts[parts.length - 1];
   }

   public Document getDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.READ);

      Document document = documentDao.getDocumentById(documentId);

      DataDocument data = dataDao.getData(collection.getId(), documentId);
      document.setData(data);

      return document;
   }

   public List<Document> getDocuments(String collectionId, Pagination pagination) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.READ);

      Map<String, DataDocument> dataDocuments = getDataDocuments(collection.getId(), pagination);

      return getDocuments(dataDocuments);
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
      String user = authenticatedUser.getCurrentUsername();
      Set<String> groups = authenticatedUser.getCurrentUserGroups();

      return SearchQuery.createBuilder(user).groups(groups)
                        .documentIds(Collections.singleton(documentId))
                        .build();
   }
}
