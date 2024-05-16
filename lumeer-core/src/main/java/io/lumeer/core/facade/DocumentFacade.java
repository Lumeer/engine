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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.DocumentsChain;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.adapter.CollectionAdapter;
import io.lumeer.core.adapter.DocumentAdapter;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.Tuple;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateDocumentsAndLinks;
import io.lumeer.engine.api.event.ImportCollectionContent;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

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
   private ResourceCommentDao resourceCommentDao;

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

   @Inject
   private TaskProcessingFacade taskProcessingFacade;

   private ConstraintManager constraintManager;

   private DocumentAdapter adapter;
   private CollectionAdapter collectionAdapter;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      adapter = new DocumentAdapter(resourceCommentDao, favoriteItemDao);
      collectionAdapter = new CollectionAdapter(collectionDao, favoriteItemDao, documentDao);
   }

   public DocumentAdapter getAdapter() {
      return adapter;
   }

   public Document createDocument(String collectionId, Document document) {
      Collection collection = checkCreateDocuments(collectionId);
      permissionsChecker.checkDocumentLimits(document);

      final Tuple<Document, Document> documentTuple = createDocument(collection, document);

      if (createDocumentEvent != null) {
         createDocumentEvent.fire(new CreateDocument(documentTuple.getFirst()));
      }

      return documentTuple.getSecond();
   }

   private Tuple<Document, Document> createDocument(Collection collection, Document document) {
      DataDocument data = document.getData();
      data = constraintManager.encodeDataTypes(collection, data);

      Document storedDocument = createDocument(collection, document, new DataDocument(data));

      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), data);
      storedDocument.setData(storedData);

      Document storedDocumentCopy = new Document(storedDocument);

      collectionAdapter.updateCollectionMetadata(collection, data.keySet(), Collections.emptySet());

      storedDocument.setData(constraintManager.decodeDataTypes(collection, storedData));

      return new Tuple<>(storedDocumentCopy, storedDocument);
   }

   public List<Document> createDocuments(final String collectionId, final List<Document> documents, final boolean sendNotification) {
      final Collection collection = checkCreateDocuments(collectionId);
      final Map<String, Integer> usages = new HashMap<>();
      final Map<String, DataDocument> documentsData = new HashMap<>();
      permissionsChecker.checkDocumentLimits(documents);

      // encode the original data and remember them by their original template id
      documents.forEach(document -> {
         DataDocument data = constraintManager.encodeDataTypes(collection, document.getData());
         documentsData.put((String) document.createIfAbsentMetaData().computeIfAbsent(Document.META_TEMPLATE_ID, key -> UUID.randomUUID().toString()), data);
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
         storedDocument.setData(constraintManager.decodeDataTypes(collection, storedDocument.getData()));
      });

      collectionAdapter.updateCollectionMetadata(collection, usages);

      if (sendNotification && importCollectionContentEvent != null) {
         importCollectionContentEvent.fire(new ImportCollectionContent(collection));
      }

      return storedDocuments;
   }

   protected List<Document> updateDocumentsData(final Collection collection, final List<Document> documents, final boolean sendNotification) {
      permissionsChecker.checkRole(collection, RoleType.DataWrite);

      Set<String> ids = documents.stream().map(Document::getId).collect(Collectors.toSet());

      List<DataDocument> newData = documents.stream().map(document -> {
         DataDocument data = constraintManager.encodeDataTypes(collection, document.getData());
         data.setId(document.getId());
         return data;
      }).collect(Collectors.toList());

      dataDao.deleteData(collection.getId(), ids);
      dataDao.createData(collection.getId(), newData);

      if (sendNotification && importCollectionContentEvent != null) {
         importCollectionContentEvent.fire(new ImportCollectionContent(collection));
      }

      return documents;
   }

   private List<Document> createDocuments(Collection collection, List<Document> documents) {
      documents.forEach(document -> {
         document.setCollectionId(collection.getId());
         document.setCreatedBy(getCurrentUserId());
         document.setCreationDate(ZonedDateTime.now());
      });
      return documentDao.createDocuments(documents);
   }

   private Document createDocument(Collection collection, Document document, DataDocument data) {
      document.setData(data);
      document.setCollectionId(collection.getId());
      document.setCreatedBy(getCurrentUserId());
      document.setCreationDate(ZonedDateTime.now());
      return documentDao.createDocument(document);
   }

   public Document updateDocumentData(String collectionId, String documentId, DataDocument data) {
      Tuple<Collection, Document> tuple = checkEditDocument(documentId);
      final Collection collection = tuple.getFirst();
      final Document document = tuple.getSecond();

      data = constraintManager.encodeDataTypes(collection, data);

      final DataDocument originalData = new DataDocument(document.getData());
      Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(originalData.keySet());

      Set<String> attributesIdsToDec = new HashSet<>(originalData.keySet());
      attributesIdsToDec.removeAll(data.keySet());

      if (!isDataDifferent(originalData, data)) { // if there is no difference, just return the document
         return document;
      }

      collectionAdapter.updateCollectionMetadata(collection, attributesIdsToAdd, attributesIdsToDec);

      DataDocument updatedData = dataDao.updateData(collection.getId(), documentId, data);

      final Document updatedDocument = updateDocument(collection, document, updatedData);
      updatedDocument.setData(constraintManager.decodeDataTypes(collection, updatedDocument.getData()));

      return mapDocumentData(updatedDocument);
   }

   private boolean isDataDifferent(final DataDocument oldDoc, final DataDocument newDoc) {
      if (oldDoc == null) {
         return true;
      }

      // we need to use Map's equals
      final Map<String, Object> oldData = new HashMap<>(oldDoc);
      final Map<String, Object> newDataWithId = new HashMap<>(newDoc);
      newDataWithId.put(DataDocument.ID, oldDoc.getId());

      return !oldData.equals(newDataWithId);
   }

   public DocumentsChain createDocumentsChain(List<Document> documents, List<LinkInstance> linkInstances) {
      var collectionsMap = documents.stream()
                                    .filter(document -> document.getId() == null)
                                    .map(document -> checkCreateDocuments(document.getCollectionId()))
                                    .collect(Collectors.toMap(Resource::getId, Function.identity()));
      var linkTypesMap = linkInstances.stream()
                                      .filter(linkInstance -> linkInstance.getId() == null)
                                      .map(linkInstanceId -> checkCreateLinks(linkInstanceId.getLinkTypeId()))
                                      .collect(Collectors.toMap(LinkType::getId, Function.identity()));

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
            collectionAdapter.updateCollectionMetadata(collection, tuple.getSecond().getData().keySet(), Collections.emptySet());
         }

         var linkInstance = linkInstances.size() > linkInstanceIndex ? linkInstances.get(linkInstanceIndex) : null;
         if (previousDocumentId != null && linkInstance != null) {
            linkInstance.setDocumentIds(Arrays.asList(previousDocumentId, currentDocumentId));
            if (linkInstance.getId() != null) {
               var updatedLinkInstance = linkInstanceDao.updateLinkInstance(linkInstance.getId(), linkInstance);
               updatedLinkInstance.setData(linkInstance.getData());
               createdLinks.add(updatedLinkInstance);
            } else {
               var linkType = linkTypesMap.get(linkInstance.getLinkTypeId());
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

   public Document updateDocumentMetaData(final String collectionId, final String documentId, final DataDocument metaData) {
      Tuple<Collection, Document> tuple = checkEditDocument(documentId);
      final Collection collection = tuple.getFirst();
      final Document document = tuple.getSecond();
      final Document originalDocument = new Document(document);

      document.setMetaData(metaData);

      final Document updatedDocument = updateDocument(document, originalDocument);
      updatedDocument.setData(constraintManager.decodeDataTypes(collection, document.getData()));

      return mapDocumentData(updatedDocument);
   }

   protected List<Document> updateDocumentsMetaData(final Collection collection, final List<Document> documents, boolean sendPushNotification) {
      final List<Document> updatedDocuments = new ArrayList<>();

      for (Document document : documents) {
         document.setUpdatedBy(getCurrentUserId());
         document.setUpdateDate(ZonedDateTime.now());
         final Document updatedDocument = documentDao.updateDocument(document.getId(), document);
         updatedDocument.setData(constraintManager.decodeDataTypes(collection, document.getData()));
         updatedDocuments.add(updatedDocument);
      }

      if (sendPushNotification && importCollectionContentEvent != null && collection != null) {
         importCollectionContentEvent.fire(new ImportCollectionContent(collection));
      }

      return updatedDocuments;
   }

   public List<Document> updateDocumentsMetaData(final String collectionId, final List<Document> documents) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      return updateDocumentsMetaData(collection, documents, true);
   }

   public Document patchDocumentData(String collectionId, String documentId, DataDocument data) {
      Tuple<Collection, Document> tuple = checkEditDocument(documentId);
      final Collection collection = tuple.getFirst();
      final Document document = tuple.getSecond();

      data = constraintManager.encodeDataTypes(collection, data);

      DataDocument originalData = new DataDocument(document.getData());

      Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(originalData.keySet());

      if (!isPatchDifferent(originalData, data)) { // if there is no difference, just return the document
         return document;
      }

      collectionAdapter.updateCollectionMetadata(collection, attributesIdsToAdd, Collections.emptySet());

      DataDocument patchedData = dataDao.patchData(collection.getId(), documentId, data);

      final Document updatedDocument = updateDocument(collection, document, patchedData);

      updatedDocument.setData(constraintManager.decodeDataTypes(collection, updatedDocument.getData()));

      return mapDocumentData(updatedDocument);
   }

   private boolean isPatchDifferent(final DataDocument oldDoc, final DataDocument patch) {
      if (oldDoc == null) {
         return true;
      }

      return patch.entrySet().stream().anyMatch(entry -> {
               if (!oldDoc.containsKey(entry.getKey())) {
                  return true;
               }
               return !Objects.equals(oldDoc.get(entry.getKey()), entry.getKey());
            }
      );
   }

   public Document patchDocumentMetaData(final String collectionId, final String documentId, final DataDocument metaData) {
      Tuple<Collection, Document> tuple = checkEditDocument(documentId);
      final Collection collection = tuple.getFirst();
      final Document document = tuple.getSecond();
      final Document originalDocument = new Document(document);

      if (document.getMetaData() == null) {
         document.setMetaData(new DataDocument());
      }
      metaData.forEach((key, value) -> document.getMetaData().put(key, value));

      final Document updatedDocument = updateDocument(document, originalDocument);
      updatedDocument.setData(constraintManager.decodeDataTypes(collection, document.getData()));

      return mapDocumentData(updatedDocument);
   }

   private Document updateDocument(final Collection collection, final Document document, final DataDocument newData) {
      final Document originalDocument = new Document(document);
      originalDocument.setData(document.getData());

      document.setCollectionId(collection.getId());
      document.setData(newData);

      final var updatedDocument = updateDocument(document, originalDocument);
      updatedDocument.setData(newData);
      return updatedDocument;
   }

   private Document updateDocument(final Document document, final Document originalDocument) {
      document.setUpdatedBy(getCurrentUserId());
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
      Tuple<Collection, Document> tuple = checkDeleteDocument(documentId);
      final Collection collection = tuple.getFirst();
      final Document document = tuple.getSecond();

      collectionAdapter.updateCollectionMetadata(collection, Collections.emptySet(), document.getData().keySet());

      documentDao.deleteDocument(documentId, document.getData());
      dataDao.deleteData(collection.getId(), documentId);

      deleteDocumentBasedData(collectionId, documentId);
   }

   public void deleteDocuments(final Set<String> documentIds) {
      if (documentIds != null && !documentIds.isEmpty()) {

         // check rights on multiple documents in multiple collections efficiently
         documentIds.forEach(id -> checkDeleteDocument(id));
         var documents = getDocuments(documentIds);
         final Set<String> collectionIds = documents.stream().map(Document::getCollectionId).collect(Collectors.toSet());
         var collections = collectionDao.getCollectionsByIds(collectionIds);
         final Map<String, Collection> collectionsMap = collections.stream().collect(Collectors.toMap(Collection::getId, Function.identity()));

         documents.forEach(doc -> permissionsChecker.checkDeleteDocument(collectionsMap.get(doc.getCollectionId()), doc));

         final Map<String, List<Document>> documentsByCollection = Utils.categorize(documents.stream(), Document::getCollectionId);
         final Map<String, Document> documentsById = documents.stream().collect(Collectors.toMap(Document::getId, Function.identity()));
         final Map<String, Integer> attributesToDec = new HashMap<>();
         documentsByCollection.forEach((collectionId, docs) -> {
            dataDao.getData(collectionId, docs.stream().map(Document::getId).collect(Collectors.toSet())).forEach(data -> {
               data.keySet().forEach(key -> {
                  if (attributesToDec.containsKey(key)) {
                     attributesToDec.put(key, attributesToDec.get(key) - 1);
                  } else {
                     attributesToDec.put(key, -1);
                  }
               });

               if (documentsById.containsKey(data.getId())) {
                  documentsById.get(data.getId()).setData(data);
               }
            });

            collectionAdapter.updateCollectionMetadata(collectionsMap.get(collectionId), attributesToDec);

            dataDao.deleteData(collectionId, docs.stream().map(Document::getId).collect(Collectors.toSet()));
         });

         documentDao.deleteDocuments(documentIds);

         resourceCommentDao.deleteComments(ResourceType.DOCUMENT, documentIds);
         linkInstanceDao.deleteLinkInstancesByDocumentsIds(documentIds);
         favoriteItemDao.removeFavoriteDocumentsByIdsFromUsers(documentIds);
         fileAttachmentFacade.removeFileAttachments(documentIds, FileAttachment.AttachmentType.DOCUMENT);
      }
   }

   protected void deleteAllDocuments(Collection collection) {
      permissionsChecker.checkRoleInCollectionWithView(collection, RoleType.DataDelete);

      documentDao.deleteDocuments(collection.getId());
      dataDao.deleteData(collection.getId());

      var documentIds = documentDao.getDocumentsIdsByCollection(collection.getId());
      resourceCommentDao.deleteComments(ResourceType.DOCUMENT, documentIds);
      linkInstanceDao.deleteLinkInstancesByDocumentsIds(documentIds);
      favoriteItemDao.removeFavoriteDocumentsByCollectionFromUsers(getCurrentProject().getId(), collection.getId());
      fileAttachmentFacade.removeAllFileAttachments(collection.getId(), FileAttachment.AttachmentType.DOCUMENT);
   }

   private void deleteDocumentBasedData(String collectionId, String documentId) {
      linkInstanceDao.deleteLinkInstancesByDocumentsIds(Collections.singleton(documentId));
      favoriteItemDao.removeFavoriteDocumentFromUsers(getCurrentProject().getId(), collectionId, documentId);
   }

   public boolean isFavorite(String documentId) {
      return isFavorite(documentId, getCurrentUserId());
   }

   public boolean isFavorite(String documentId, String userId) {
      return getFavoriteDocumentsIds(userId).contains(documentId);
   }

   public Set<String> getFavoriteDocumentsIds() {
      return getFavoriteDocumentsIds(getCurrentUserId());
   }

   public Set<String> getFavoriteDocumentsIds(String userId) {
      String projectId = getCurrentProject().getId();

      return adapter.getFavoriteDocumentIds(userId, projectId);
   }

   public void addFavoriteDocument(String collectionId, String documentId) {
      checkReadDocument(documentId);

      favoriteItemDao.addFavoriteDocument(getCurrentUserId(), getCurrentProject().getId(), collectionId, documentId);
   }

   public void removeFavoriteDocument(String collectionId, String documentId) {
      checkReadDocument(documentId);

      favoriteItemDao.removeFavoriteDocument(getCurrentUserId(), documentId);
   }

   public List<Document> duplicateDocuments(final String collectionId, final List<String> documentIds) {
      final Collection collection = checkCreateDocuments(collectionId);
      permissionsChecker.checkDocumentLimits(documentIds.size());

      var dataMap = dataDao.getData(collectionId, new HashSet<>(documentIds)).stream()
                           .collect(Collectors.toMap(DataDocument::getId, d -> d));
      final List<Document> originalDocuments = documentDao.getDocumentsByIds(new HashSet<>(documentIds))
                                                          .stream().peek(document -> document.setData(dataMap.getOrDefault(document.getId(), new DataDocument())))
                                                          .filter(document -> permissionsChecker.canEditDocument(collection, document))
                                                          .collect(Collectors.toList());

      if (originalDocuments.isEmpty()) {
         return originalDocuments;
      }

      final List<Document> documents = documentDao.duplicateDocuments(originalDocuments);
      final Map<String, Document> documentsDirectory = new HashMap<>(); // new document id -> inserted document
      final Map<String, String> keyMap = new HashMap<>(); // original document id -> new document id
      documents.forEach(d -> {
         documentsDirectory.put(d.getId(), d);
         keyMap.put(d.getMetaData().getString(Document.META_ORIGINAL_DOCUMENT_ID), d.getId());
      });

      final Map<String, Integer> usages = new HashMap<>();
      final List<DataDocument> dataList = dataDao.duplicateData(collectionId, dataMap.values(), keyMap);
      dataList.forEach(encodedData -> {
         final DataDocument data = constraintManager.decodeDataTypes(collection, encodedData);
         if (documentsDirectory.containsKey(data.getId())) {
            documentsDirectory.get(data.getId()).setData(data);
         }
         data.keySet().forEach(key -> usages.put(key, usages.computeIfAbsent(key, k -> 0) + 1));
      });

      fileAttachmentFacade.duplicateFileAttachments(collection.getId(), keyMap, FileAttachment.AttachmentType.DOCUMENT);

      // need to take the snapshot of encoded data now because later, the data are encoded again (in CollectionAdapter)
      final List<Document> documentsSnapshot = documentsDirectory.values().stream().map(d -> new Document(d)).collect(Collectors.toList());

      if (this.createChainEvent != null) {
         this.createChainEvent.fire(new CreateDocumentsAndLinks(documents, Collections.emptyList()));
      }

      collectionAdapter.updateCollectionMetadata(collection, usages);

      return documentsSnapshot;
   }

   public Document getDocument(String collectionId, String documentId) {
      Tuple<Collection, Document> tuple = checkReadDocument(documentId);
      final Collection collection = tuple.getFirst();
      final Document document = tuple.getSecond();

      document.setData(constraintManager.decodeDataTypes(collection, document.getData()));

      return mapDocumentData(document);
   }

   public List<Document> getDocuments(Set<String> ids) {
      var documentsMap = documentDao.getDocumentsByIds(ids.toArray(new String[0]))
                                    .stream()
                                    .collect(Collectors.groupingBy(Document::getCollectionId));
      var collectionsMap = new HashMap<String, Collection>();
      var resultDocuments = new ArrayList<Document>();
      documentsMap.forEach((collectionId, value) -> {
         var collection = collectionsMap.computeIfAbsent(collectionId, id -> collectionDao.getCollectionById(id));
         var dataMap = dataDao.getData(collectionId, value.stream().map(Document::getId).collect(Collectors.toSet()))
                              .stream()
                              .collect(Collectors.toMap(DataDocument::getId, d -> d));

         value.forEach(document -> {
            var data = dataMap.getOrDefault(document.getId(), new DataDocument());
            document.setData(constraintManager.decodeDataTypes(collection, data));

            if (permissionsChecker.canReadDocument(collection, document)) {
               resultDocuments.add(document);
            }
         });
      });

      return adapter.mapDocumentsData(resultDocuments, authenticatedUser.getCurrentUserId(), getProject().getId());
   }

   @SuppressWarnings("unchecked")
   public void runRule(final String collectionId, String documentId, String attributeId, final String actionName) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      Constraint constraint = ResourceUtils.findConstraint(collection.getAttributes(), attributeId);
      if (constraint != null) {
         var config = (Map<String, Object>) constraint.getConfig();
         var rule = config.get("rule").toString();
         if (!collection.getRules().containsKey(rule)) {
            throw new IllegalStateException("Rule not found");
         }

         var document = checkReadDocument(collection, documentId);
         taskProcessingFacade.runRule(collection, rule, document, actionName);
      }
   }

   private Document mapDocumentData(final Document document) {
      return adapter.mapDocumentData(document, getCurrentUserId(), workspaceKeeper.getProjectId());
   }

   public List<Document> getRecentDocuments(final String collectionId, final boolean byUpdate) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final List<Document> documents = documentDao.getRecentDocuments(collectionId, byUpdate);

      var dataMap = dataDao.getData(collectionId, documents.stream().map(Document::getId).collect(Collectors.toSet()))
                           .stream()
                           .collect(Collectors.toMap(DataDocument::getId, d -> d));

      documents.forEach(doc -> {
         DataDocument data = dataMap.getOrDefault(doc.getId(), new DataDocument());
         doc.setData(constraintManager.decodeDataTypes(collection, data));
      });

      return documents.stream()
                      .filter(document -> permissionsChecker.canReadDocument(collection, document))
                      .collect(Collectors.toList());
   }

   private Project getCurrentProject() {
      if (workspaceKeeper.getProject().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return workspaceKeeper.getProject().get();
   }

   private Collection checkCreateDocuments(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkCreateDocuments(collection);
      return collection;
   }

   private LinkType checkCreateLinks(String linkTypeId) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      permissionsChecker.checkCreateLinkInstances(linkType);
      return linkType;
   }

   private Tuple<Collection, Document> checkEditDocument(String documentId) {
      var tuple = readCollectionAndDocument(documentId);
      permissionsChecker.checkEditDocument(tuple.getFirst(), tuple.getSecond());
      return tuple;
   }

   private Document checkEditDocument(Collection collection, String documentId) {
      final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, documentId);
      permissionsChecker.checkEditDocument(collection, document);
      return document;
   }

   private Tuple<Collection, Document> checkDeleteDocument(String documentId) {
      var tuple = readCollectionAndDocument(documentId);
      permissionsChecker.checkDeleteDocument(tuple.getFirst(), tuple.getSecond());
      return tuple;
   }

   private Tuple<Collection, Document> checkReadDocument(String documentId) {
      var tuple = readCollectionAndDocument(documentId);
      permissionsChecker.checkReadDocument(tuple.getFirst(), tuple.getSecond());
      return tuple;
   }

   private Document checkReadDocument(Collection collection, String documentId) {
      final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, documentId);
      permissionsChecker.checkReadDocument(collection, document);
      return document;
   }

   private Tuple<Collection, Document> readCollectionAndDocument(String documentId) {
      final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, documentId);
      Collection collection = collectionDao.getCollectionById(document.getCollectionId());
      return new Tuple<>(collection, document);
   }

}
