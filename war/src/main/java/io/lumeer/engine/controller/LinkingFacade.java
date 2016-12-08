/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.controller;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.event.DropDocument;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.LinkAlreadyExistsException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.mongodb.MongoUtils;

import com.mongodb.client.model.Filters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
@SessionScoped
public class LinkingFacade implements Serializable {

   /*
      Main linking table name is "_system_linking"
      attributes - "collection1" name of first collection
                 - "collection2" name of second collection
                 - "collection_name" name of collection, where links are located
                 - "count" number of links in collection

      Collection linking table
      attributes - "id_doc1" id of first document
                 - "id_doc2" id of second document

    */

   @Inject
   private DataStorage dataStorage;

   /**
    * Creates main linking table if not exists
    */
   @PostConstruct
   public void init() {
      if (!dataStorage.hasCollection(LumeerConst.Linking.MainTable.NAME)) {
         dataStorage.createCollection(LumeerConst.Linking.MainTable.NAME);
      }
   }

   public void onDropDocument(@Observes(notifyObserver = Reception.IF_EXISTS) final DropDocument dropDocument) throws CollectionNotFoundException {
      dropAllDocumentLinks(dropDocument.getCollectionName(), dropDocument.getDocument().getId());
   }

   /**
    * Read all linking documents for specified document
    *
    * @param collectionName
    *       the name of the document's collection
    * @param documentId
    *       the id of the document to search for links
    * @return map of collections and its documents
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    */
   public Map<String, List<DataDocument>> readAllDocumentLinks(String collectionName, String documentId) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      // retrieve all documents, where collectionName is first or second attribute
      List<DataDocument> linkingTables = readLinkingTables(collectionName);
      Map<String, List<DataDocument>> docLinks = new HashMap<>();
      for (DataDocument lt : linkingTables) { // run in each linking table
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         List<DataDocument> linkingDocuments = readLinkingDocuments(colName, documentId);

         // find right name of collection where the linking document is located
         String firstColName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL1);
         String linkingCollectionName = !firstColName.equals(collectionName) ? firstColName : lt.getString(LumeerConst.Linking.MainTable.ATTR_COL2);

         // add all linking documents from storage
         List<DataDocument> links = readDocumentsFromLinkingDocuments(linkingDocuments, documentId, linkingCollectionName);
         if (!links.isEmpty()) {
            docLinks.put(linkingCollectionName, links);
         }
      }
      return docLinks;
   }

   /**
    * Check if link exist between two documents
    *
    * @param firstCollectionName
    *       the name of the first document's collection
    * @param firstDocumentId
    *       the id of the first document
    * @param secondCollectionName
    *       the name of the second document's collection
    * @param secondDocumentId
    *       the id of the second document
    * @throws CollectionNotFoundException
    *       if first or second collection is not found in database
    */
   public boolean linkExistsBetweenDocuments(String firstCollectionName, String firstDocumentId, String secondCollectionName, String secondDocumentId) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(firstCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(firstCollectionName));
      }
      if (!dataStorage.hasCollection(secondCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(secondCollectionName));
      }
      DataDocument linkingTable = readLinkingTable(firstCollectionName, secondCollectionName);
      if (linkingTable == null) {
         return false;
      }
      String colName = linkingTable.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
      DataDocument linkingDocuments = readLinkingDocument(colName, firstDocumentId, secondDocumentId);
      return linkingDocuments != null;
   }

   /**
    * Read all linking documents for specified document and collection
    *
    * @param firstCollectionName
    *       the name of the document's collection
    * @param firstDocumentId
    *       the id of the document to search for links
    * @param secondCollectionName
    *       the name of the collection to search for linking documents
    * @return list of linking documents
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    */
   public List<DataDocument> readDocWithCollectionLinks(String firstCollectionName, String firstDocumentId, String secondCollectionName) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(firstCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(firstCollectionName));
      }
      if (!dataStorage.hasCollection(secondCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(secondCollectionName));
      }
      DataDocument linkingTable = readLinkingTable(firstCollectionName, secondCollectionName);
      if (linkingTable == null) {
         return new ArrayList<>();
      }

      String colName = linkingTable.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
      List<DataDocument> linkingDocuments = readLinkingDocuments(colName, firstDocumentId);

      return readDocumentsFromLinkingDocuments(linkingDocuments, firstDocumentId, secondCollectionName);
   }

   /**
    * Drop all links for specified document
    *
    * @param collectionName
    *       the name of the document's collection
    * @param documentId
    *       the id of the document to drop links
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    */
   public void dropAllDocumentLinks(String collectionName, String documentId) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      List<DataDocument> linkingTables = readLinkingTables(collectionName);
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         removeAllDocuments(colName, documentId);
         checkEmptinessAndRemoveEventually(colName);
      }
   }

   /**
    * Drop link between two documents
    *
    * @param firstCollectionName
    *       the name of the first document's collection
    * @param firstDocumentId
    *       the id of the first document
    * @param secondCollectionName
    *       the name of the second document's collection
    * @param secondDocumentId
    *       the id of the second document
    * @throws CollectionNotFoundException
    *       if first or second collection is not found in database
    */
   public void dropDocWithDocLink(String firstCollectionName, String firstDocumentId, String secondCollectionName, String secondDocumentId) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(firstCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(firstCollectionName));
      }
      if (!dataStorage.hasCollection(secondCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(secondCollectionName));
      }
      DataDocument linkingTable = readLinkingTable(firstCollectionName, secondCollectionName);
      if (linkingTable == null) {
         // throw exception?
         return;
      }
      String colName = linkingTable.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
      dataStorage.dropManyDocuments(colName, getDoc1Doc2Filter(firstDocumentId, secondDocumentId));
      checkEmptinessAndRemoveEventually(colName);
   }

   /**
    * Drop link between document and collection
    *
    * @param firstCollectionName
    *       the name of the document's collection
    * @param firstDocumentId
    *       the id of the document
    * @param secondCollectionName
    *       the name of the collection to drop links
    * @throws CollectionNotFoundException
    *       if first or second collection is not found in database
    */
   public void dropDocWithCollectionLinks(String firstCollectionName, String firstDocumentId, String secondCollectionName) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(firstCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(firstCollectionName));
      }
      if (!dataStorage.hasCollection(secondCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(secondCollectionName));
      }
      DataDocument linkingTable = readLinkingTable(firstCollectionName, secondCollectionName);
      if (linkingTable == null) {
         // throw exception?
         return;
      }
      String colName = linkingTable.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
      removeAllDocuments(colName, firstDocumentId);
      checkEmptinessAndRemoveEventually(colName);
   }

   /**
    * Drop all links for specified collection
    *
    * @param collectionName
    *       the name of the collection to drop links
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    */
   public void dropCollectionLinks(String collectionName) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      List<DataDocument> linkingTables = readLinkingTables(collectionName);
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         dataStorage.dropCollection(colName);
      }
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL1, collectionName),
            Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL2, collectionName)
      ));
      dataStorage.dropManyDocuments(LumeerConst.Linking.MainTable.NAME, filter);
   }

   /**
    * Create link between two documents
    *
    * @param firstCollectionName
    *       the name of the first document's collection
    * @param firstDocumentId
    *       the id of the first document
    * @param secondCollectionName
    *       the name of the second document's collection
    * @param secondDocumentId
    *       the id of the second document
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if one of the documents is not in appropriate collection
    * @throws LinkAlreadyExistsException
    *       if link between documents already exists
    */
   public void createDocWithDocLink(String firstCollectionName, String firstDocumentId, String secondCollectionName, String secondDocumentId) throws CollectionNotFoundException, DocumentNotFoundException, LinkAlreadyExistsException {
      if (!dataStorage.hasCollection(firstCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(firstCollectionName));
      }
      if (!dataStorage.hasCollection(secondCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(secondCollectionName));
      }
      if (!(dataStorage.collectionHasDocument(firstCollectionName, firstDocumentId) && dataStorage.collectionHasDocument(secondCollectionName, secondDocumentId))) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      String collectionName = checkOrCreateLinkInSystemCollection(firstCollectionName, secondCollectionName);
      createLinkDocsOrThrow(collectionName, firstDocumentId, secondDocumentId);
   }

   /**
    * Create link between two documents
    *
    * @param firstCollectionName
    *       the name of the first document's collection
    * @param firstDocumentId
    *       the id of the first document
    * @param secondCollectionName
    *       the name of the second document's collection
    * @param documentIds
    *       the id of the second document
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if one of the documents is not in appropriate collection
    * @throws LinkAlreadyExistsException
    *       if link between documents already exists
    */
   public void createDocWithColletionLinks(String firstCollectionName, String firstDocumentId, String secondCollectionName, List<String> documentIds) throws CollectionNotFoundException, LinkAlreadyExistsException, DocumentNotFoundException {
      if (!dataStorage.hasCollection(firstCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(firstCollectionName));
      }
      if (!dataStorage.hasCollection(secondCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(secondCollectionName));
      }
      if (!dataStorage.collectionHasDocument(firstCollectionName, firstDocumentId)) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      String collectionName = checkOrCreateLinkInSystemCollection(firstCollectionName, secondCollectionName);
      for (String secondDocumentId : documentIds) {
         if (secondDocumentId != null) {
            if (!dataStorage.collectionHasDocument(secondCollectionName, secondDocumentId)) {
               throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
            }
            createLinkDocsOrThrow(collectionName, firstDocumentId, secondDocumentId);
         }
      }
   }

   private void createLinkDocsOrThrow(final String collectionName, final String firstDocumentId, final String secondDocumentId) throws LinkAlreadyExistsException {
      DataDocument linkingDocument = readLinkingDocument(collectionName, firstDocumentId, secondDocumentId);
      if (linkingDocument == null) {
         createLinkingDocument(collectionName, firstDocumentId, secondDocumentId);
      } else {
         throw new LinkAlreadyExistsException(ErrorMessageBuilder.linkAlreadyExists());
      }
   }

   private String checkOrCreateLinkInSystemCollection(String firstCollectionName, String secondCollectionName) {
      String collectionName;
      DataDocument linkingTable = readLinkingTable(firstCollectionName, secondCollectionName);
      if (linkingTable == null) {
         collectionName = createCollectionName(firstCollectionName, secondCollectionName);
         createLinkingTable(firstCollectionName, secondCollectionName, collectionName);
      } else {
         collectionName = linkingTable.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
      }
      return collectionName;
   }

   private DataDocument readLinkingTable(String firstCollectionName, String secondCollectionName) {
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.and(
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL1, firstCollectionName),
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL2, secondCollectionName)),
            Filters.and(Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL2, firstCollectionName),
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL1, secondCollectionName))));
      List<DataDocument> linkingTables = dataStorage.search(LumeerConst.Linking.MainTable.NAME, filter, null, 0, 0);
      return linkingTables.size() == 1 ? linkingTables.get(0) : null;
   }

   private DataDocument readLinkingDocument(final String collectionName, String firstDocumentId, String secondDocumentId) {
      List<DataDocument> linkingDocuments = dataStorage.search(collectionName, getDoc1Doc2Filter(firstDocumentId, secondDocumentId), null, 0, 0);
      return linkingDocuments.size() == 1 ? linkingDocuments.get(0) : null;
   }

   private List<DataDocument> readLinkingTables(String firstCollectionName) {
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL1, firstCollectionName),
            Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL2, firstCollectionName)));
      return dataStorage.search(LumeerConst.Linking.MainTable.NAME, filter, null, 0, 0);
   }

   private List<DataDocument> readLinkingDocuments(final String collectionName, String documentId) {
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_DOC1, documentId),
            Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_DOC2, documentId)));
      return dataStorage.search(collectionName, filter, null, 0, 0);
   }

   private List<DataDocument> readDocumentsFromLinkingDocuments(List<DataDocument> linkingDocuments, String documentId, String collectionName) {
      List<DataDocument> docs = new ArrayList<>();
      for (DataDocument ld : linkingDocuments) {
         // check for right id of linking document
         String firstDocId = ld.getString(LumeerConst.Linking.LinkingTable.ATTR_DOC1);
         String linkingDocumentId = !firstDocId.equals(documentId) ? firstDocId : ld.getString(LumeerConst.Linking.LinkingTable.ATTR_DOC2);

         DataDocument doc = dataStorage.readDocument(collectionName, linkingDocumentId);
         if (doc != null) {
            docs.add(doc);
         }
      }
      return docs;
   }

   private void removeAllDocuments(final String collectionName, final String documentId) {
      String filter = MongoUtils.convertBsonToJson(Filters.or(Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_DOC1, documentId), Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_DOC2, documentId)));
      dataStorage.dropManyDocuments(collectionName, filter);
   }

   private void checkEmptinessAndRemoveEventually(final String collectionName) {
      if (dataStorage.documentCount(collectionName) == 0) {
         dataStorage.dropCollection(collectionName);

         String filter = MongoUtils.convertBsonToJson(Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName));
         dataStorage.dropManyDocuments(LumeerConst.Linking.MainTable.NAME, filter);
      }
   }

   private void createLinkingTable(String firstCollectionName, String secondCollectionName, final String collectionName) {
      DataDocument doc = new DataDocument();
      doc.put(LumeerConst.Linking.MainTable.ATTR_COL1, firstCollectionName);
      doc.put(LumeerConst.Linking.MainTable.ATTR_COL2, secondCollectionName);
      doc.put(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName);

      dataStorage.createDocument(LumeerConst.Linking.MainTable.NAME, doc);

   }

   private void createLinkingDocument(final String collectionName, final String firstDocumentId, final String secondDocumentId) {
      DataDocument doc = new DataDocument();
      doc.put(LumeerConst.Linking.LinkingTable.ATTR_DOC1, firstDocumentId);
      doc.put(LumeerConst.Linking.LinkingTable.ATTR_DOC2, secondDocumentId);

      dataStorage.createDocument(collectionName, doc);
   }

   private String getDoc1Doc2Filter(final String firstDocumentId, final String secondDocumentId) {
      return MongoUtils.convertBsonToJson(Filters.or(
            Filters.and(
                  Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_DOC1, firstDocumentId),
                  Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_DOC2, secondDocumentId)),
            Filters.and(
                  Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_DOC1, secondDocumentId),
                  Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_DOC2, firstDocumentId))));
   }

   private String createCollectionName(final String firstCollectionName, final String secondCollectionName) {
      return LumeerConst.Linking.PREFIX + "_" + firstCollectionName + "_" + secondCollectionName;
   }
}