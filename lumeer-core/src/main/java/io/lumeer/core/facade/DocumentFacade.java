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

import io.lumeer.api.model.*;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateDocumentsAndLinks;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.ImportCollectionContent;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
   private LinkInstanceFacade linkInstanceFacade;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private Event<CreateDocument> createDocumentEvent;

   @Inject
   private Event<UpdateDocument> updateDocumentEvent;

   @Inject
   private Event<ImportCollectionContent> importCollectionContentEvent;

   @Inject
   private Event<CreateDocumentsAndLinks> createChainEvent;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   private ConstraintManager constraintManager;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
   }

   public Document createDocument(String collectionId, Document document) {
      Collection collection = checkCollectionWritePermissions(collectionId);
      permissionsChecker.checkDocumentLimits(document);

      final Tuple<Document, Document> documentTuple = createDocument(collection, document);

      if (createDocumentEvent != null) {
         createDocumentEvent.fire(new CreateDocument(documentTuple.getFirst()));
      }

      return documentTuple.getSecond();
   }

   private Tuple<Document, Document> createDocument(Collection collection, Document document) {
      DataDocument data = document.getData();
      constraintManager.encodeDataTypes(collection, data);

      Document storedDocument = createDocument(collection, document, new DataDocument(data));

      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), data);
      storedDocument.setData(storedData);

      Document storedDocumentCopy = new Document(storedDocument);

      updateCollectionMetadata(collection, data.keySet(), Collections.emptySet(), 1);

      constraintManager.decodeDataTypes(collection, storedData);

      return new Tuple<>(storedDocumentCopy, storedDocument);
   }

   public List<Document> createDocuments(final String collectionId, final List<Document> documents, final boolean sendNotification) {
      final Collection collection = checkCollectionWritePermissions(collectionId);
      final Map<String, Integer> usages = new HashMap<>();
      final Map<String, DataDocument> documentsData = new HashMap<>();
      permissionsChecker.checkDocumentLimits(documents);

      // encode the original data and remember them by their original template id
      documents.forEach(document -> {
         DataDocument data = document.getData();
         documentsData.put((String) document.createIfAbsentMetaData().computeIfAbsent(Document.META_TEMPLATE_ID, key -> UUID.randomUUID().toString()), data);
         constraintManager.encodeDataTypes(collection, data);
      });

      final List<Document> storedDocuments = createDocuments(collection, documents);

      // map the original data to the newly created documents
      storedDocuments.forEach(storedDocument -> {
         final DataDocument data = documentsData.get(storedDocument.getMetaData().getString(Document.META_TEMPLATE_ID));
         data.setId(storedDocument.getId());
         storedDocument.setData(data);
      });

      // store the documents data
      final List<DataDocument> storedData = dataDao.createData(collection.getId(), storedDocuments.stream().map(Document::getData).collect(Collectors.toList()));

      // map the stored data to the document ids
      final Map<String, DataDocument> storedDocumentsData = new HashMap<>();
      storedData.forEach(dd -> storedDocumentsData.put(dd.getId(), dd));

      // put the stored data to the stored documents, decode data types and count attributes usage
      storedDocuments.forEach(storedDocument -> {
         final DataDocument singleStoredData = storedDocumentsData.get(storedDocument.getId());
         storedDocument.setData(singleStoredData);
         singleStoredData.keySet().forEach(key -> usages.put(key, usages.computeIfAbsent(key, k -> 0) + 1));
         constraintManager.decodeDataTypes(collection, storedDocument.getData());
      });

      updateCollectionMetadata(collection, usages, storedDocuments.size());

      if (sendNotification && importCollectionContentEvent != null) {
         importCollectionContentEvent.fire(new ImportCollectionContent(collection));
      }

      return storedDocuments;
   }

   private List<Document> createDocuments(Collection collection, List<Document> documents) {
      documents.forEach(document -> {
         document.setCollectionId(collection.getId());
         document.setCreatedBy(authenticatedUser.getCurrentUserId());
         document.setCreationDate(ZonedDateTime.now());
      });
      return documentDao.createDocuments(documents);
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

      final Document updatedDocument = updateDocument(collection, documentId, updatedData, originalData);
      constraintManager.decodeDataTypes(collection, updatedDocument.getData());

      return updatedDocument;
   }

   public DocumentsChain createDocumentsChain(List<Document> documents, List<LinkInstance> linkInstances) {
      var collectionsMap = documents.stream()
                                    .map(document -> checkCollectionWritePermissions(document.getCollectionId()))
                                    .collect(Collectors.toMap(Resource::getId, collection -> collection));

      permissionsChecker.checkDocumentLimits(documents);

      if (documents.isEmpty()) {
         return new DocumentsChain(Collections.emptyList(), Collections.emptyList());
      }

      List<Document> createdDocuments = new ArrayList<>();
      List<LinkInstance> createdLinks = new ArrayList<>();

      String previousDocumentId = linkInstances.size() == documents.size() ? Utils.firstNotNullElement(linkInstances.get(0).getDocumentIds()) : null;
      var linkInstanceIndex = 0;
      for (Document document : documents) {
         String currentDocumentId;
         if (document.getId() != null) {
            currentDocumentId = document.getId();
         } else {
            var collection = collectionsMap.get(document.getCollectionId());
            var tuple = createDocument(collection, document);
            createdDocuments.add(tuple.getSecond());
            currentDocumentId = tuple.getFirst().getId();
            updateCollectionMetadata(collection, tuple.getSecond().getData().keySet(), Collections.emptySet(), 1);
         }

         var linkInstance = linkInstances.size() > linkInstanceIndex ? linkInstances.get(linkInstanceIndex) : null;
         if (previousDocumentId != null && linkInstance != null) {
            var linkType = linkTypeDao.getLinkType(linkInstance.getLinkTypeId());
            linkInstance.setDocumentIds(Arrays.asList(previousDocumentId, currentDocumentId));
            if (linkInstance.getId() != null) {
               var updatedLinkInstance = linkInstanceDao.updateLinkInstance(linkInstance.getId(), linkInstance);
               updatedLinkInstance.setData(linkInstance.getData());
               createdLinks.add(updatedLinkInstance);
            } else {
               var linkData = linkInstanceFacade.createLinkInstance(linkType, linkInstance);
               createdLinks.add(linkData.getSecond());
            }

            linkInstanceIndex++;
         }

         previousDocumentId = currentDocumentId;
      }
      if (this.createChainEvent != null) {
         this.createChainEvent.fire(new CreateDocumentsAndLinks(createdDocuments, createdLinks));
      }

      return new DocumentsChain(createdDocuments, createdLinks);
   }

   private Collection checkCollectionWritePermissions(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);
      return collection;
   }

   private Collection checkCollectionReadPermissions(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.READ, Role.READ);
      return collection;
   }

   public Document updateDocumentMetaData(final String collectionId, final String documentId, final DataDocument metaData) {
      final Collection collection = checkCollectionWritePermissions(collectionId);

      final Document document = getDocument(collection, documentId);
      final Document originalDocument = new Document(document);

      document.setMetaData(metaData);

      final Document updatedDocument = updateDocument(document, originalDocument);
      updatedDocument.setData(document.getData());
      constraintManager.decodeDataTypes(collection, updatedDocument.getData());

      return updatedDocument;
   }

   public List<Document> updateDocumentsMetaData(final String collectionId, final List<Document> documents) {
      final Collection collection = checkCollectionWritePermissions(collectionId);
      final List<Document> updatedDocuments = new ArrayList<>();

      documents.forEach(document -> {
         final Document originalDocument = new Document(document);
         document.setUpdatedBy(authenticatedUser.getCurrentUserId());
         document.setUpdateDate(ZonedDateTime.now());
         final Document updatedDocument = documentDao.updateDocument(document.getId(), document);
         updatedDocument.setData(document.getData());
         constraintManager.decodeDataTypes(collection, updatedDocument.getData());
         updatedDocuments.add(updatedDocument);
      });

      if (importCollectionContentEvent != null) {
         importCollectionContentEvent.fire(new ImportCollectionContent(collection));
      }

      return updatedDocuments;
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

      final Document updatedDocument = updateDocument(collection, documentId, patchedData, originalData);

      constraintManager.decodeDataTypes(collection, updatedDocument.getData());

      return updatedDocument;
   }

   public Document patchDocumentMetaData(final String collectionId, final String documentId, final DataDocument metaData) {
      Collection collection = checkCollectionWritePermissions(collectionId);

      final Document document = getDocument(collection, documentId);
      final Document originalDocument = new Document(document);

      if (document.getMetaData() == null) {
         document.setMetaData(new DataDocument());
      }
      metaData.forEach((key, value) -> document.getMetaData().put(key, value));

      final Document updatedDocument = updateDocument(document, originalDocument);
      updatedDocument.setData(document.getData());
      constraintManager.decodeDataTypes(collection, updatedDocument.getData());

      return updatedDocument;
   }

   private Document updateDocument(final Collection collection, final String documentId, final DataDocument newData, final DataDocument originalData) {
      final Document document = documentDao.getDocumentById(documentId);
      final Document originalDocument = new Document(document);
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

      final Document updatedDocument = documentDao.updateDocument(document.getId(), document);

      fireDocumentUpdate(document, updatedDocument, originalDocument);

      return updatedDocument;
   }

   private void fireDocumentUpdate(final Document toBeStored, final Document updatedDocument, final Document originalDocument) {
      if (updateDocumentEvent != null) {
         final Document updatedDocumentWithData = new Document(updatedDocument);
         updatedDocumentWithData.setData(toBeStored.getData());
         updateDocumentEvent.fire(new UpdateDocument(updatedDocumentWithData, originalDocument));
      }
   }

   public void deleteDocument(String collectionId, String documentId) {
      Collection collection = checkCollectionWritePermissions(collectionId);

      DataDocument data = dataDao.getData(collectionId, documentId);
      updateCollectionMetadata(collection, Collections.emptySet(), data.keySet(), -1);

      documentDao.deleteDocument(documentId);
      dataDao.deleteData(collection.getId(), documentId);

      deleteDocumentBasedData(collectionId, documentId);

      // remove all file attachments
      collection.getAttributes().forEach(attribute -> {
         if (attribute.getConstraint() != null && attribute.getConstraint().getType().equals(ConstraintType.FileAttachment)) {
            fileAttachmentFacade.removeAllFileAttachments(collectionId, documentId, attribute.getId(), FileAttachment.AttachmentType.DOCUMENT);
         }
      });
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

   public List<Document> duplicateDocuments(final String collectionId, final List<String> documentIds) {
      final Collection collection = checkCollectionWritePermissions(collectionId);
      final Map<String, Integer> usages = new HashMap<>();
      permissionsChecker.checkDocumentLimits(documentIds.size());

      final List<Document> documents = documentDao.duplicateDocuments(documentIds);
      final Map<String, Document> documentsDirectory = new HashMap<>();
      final Map<String, String> keyMap = new HashMap<>();
      documents.forEach(d -> {
         documentsDirectory.put(d.getId(), d);
         keyMap.put(d.getMetaData().getString(Document.META_ORIGINAL_DOCUMENT_ID), d.getId());
      });

      final List<DataDocument> dataList = dataDao.duplicateData(collectionId, keyMap);
      dataList.forEach(data -> {
         constraintManager.decodeDataTypes(collection, data);
         if (documentsDirectory.containsKey(data.getId())) {
            documentsDirectory.get(data.getId()).setData(data);
         }
         data.keySet().forEach(key -> usages.put(key, usages.computeIfAbsent(key, k -> 0) + 1));
      });

      fileAttachmentFacade.duplicateFileAttachments(collection.getId(), keyMap, FileAttachment.AttachmentType.DOCUMENT);

      if (this.createChainEvent != null) {
         this.createChainEvent.fire(new CreateDocumentsAndLinks(documents, Collections.emptyList()));
      }

      updateCollectionMetadata(collection, usages, documents.size());

      return documents;
   }

   private void updateCollectionMetadata(Collection collection, Set<String> attributesIdsToInc, Set<String> attributesIdsToDec, int documentCountDiff) {
      final Collection originalCollection = collection.copy();
      collection.setAttributes(new HashSet<>(ResourceUtils.incOrDecAttributes(collection.getAttributes(), attributesIdsToInc, attributesIdsToDec)));
      collection.setLastTimeUsed(ZonedDateTime.now());
      collection.setDocumentsCount(Math.max(collection.getDocumentsCount() + documentCountDiff, 0));
      collectionDao.updateCollection(collection.getId(), collection, originalCollection);
   }

   private void updateCollectionMetadata(final Collection collection, final Map<String, Integer> attributesToInc, final int documentCountDiff) {
      final Collection originalCollection = collection.copy();
      collection.setAttributes(new HashSet<>(ResourceUtils.incAttributes(collection.getAttributes(), attributesToInc)));
      collection.setLastTimeUsed(ZonedDateTime.now());
      collection.setDocumentsCount(Math.max(collection.getDocumentsCount() + documentCountDiff, 0));
      collectionDao.updateCollection(collection.getId(), collection, originalCollection);
   }

   public Document getDocument(String collectionId, String documentId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.READ, Role.READ);

      final Document result = getDocument(collection, documentId);
      constraintManager.decodeDataTypes(collection, result.getData());

      return result;
   }

   public List<Document> getDocuments(Set<String> ids) {
      var documentsMap = documentDao.getDocumentsByIds(ids.toArray(new String[0]))
                                    .stream()
                                    .collect(Collectors.groupingBy(Document::getCollectionId));

      documentsMap.forEach((collectionId, value) -> {
         var collection = collectionDao.getCollectionById(collectionId);
         if (permissionsChecker.hasRoleWithView(collection, Role.READ, Role.READ)) {

            var dataMap = dataDao.getData(collectionId, value.stream().map(Document::getId).collect(Collectors.toSet()))
                                 .stream()
                                 .collect(Collectors.toMap(DataDocument::getId, d -> d));

            value.forEach(document -> {
               var data = dataMap.get(document.getId());
               if (data != null) {
                  constraintManager.decodeDataTypes(collection, data);
                  document.setData(data);
               }
            });
         }
      });

      return documentsMap.entrySet().stream()
                         .flatMap(entry -> entry.getValue().stream())
                         .collect(Collectors.toList());
   }

   public List<Document> getRecentDocuments(final String collectionId, final boolean byUpdate) {
      final Collection collection = checkCollectionReadPermissions(collectionId);
      final List<Document> documents = documentDao.getRecentDocuments(collectionId, byUpdate);

      documents.forEach(doc -> {
         DataDocument data = dataDao.getData(collection.getId(), doc.getId());
         doc.setData(data);
         constraintManager.decodeDataTypes(collection, doc.getData());
      });

      return documents;
   }

   private Document getDocument(Collection collection, String documentId) {
      Document document = documentDao.getDocumentById(documentId);

      DataDocument data = dataDao.getData(collection.getId(), documentId);
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
