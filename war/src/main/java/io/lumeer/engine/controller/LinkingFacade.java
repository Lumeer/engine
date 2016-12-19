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
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.mongodb.MongoUtils;

import com.mongodb.client.model.Filters;

import org.bson.conversions.Bson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
      attributes - "fromCollection" name of first collection
                 - "toCollection" name of second collection
                 - "collection_name" name of collection, where links are located
                 - "role" number of links in collection

      Collection linking table
      attributes - "fromId" id of first document
                 - "toId" id of second document
                 - "attributes" some attributes...

    */

   @Inject
   private DataStorage dataStorage;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private UserFacade userFacade;

   /**
    * Creates main linking table if not exists
    */
   @PostConstruct
   public void init() {
      if (!dataStorage.hasCollection(LumeerConst.Linking.MainTable.NAME)) {
         dataStorage.createCollection(LumeerConst.Linking.MainTable.NAME);
      }
   }

   public void onDropDocument(@Observes(notifyObserver = Reception.IF_EXISTS) final DropDocument dropDocument) throws DbException {
      dropAllDocumentLinks(dropDocument.getCollectionName(), dropDocument.getDocument().getId(), null, LumeerConst.Linking.LinkDirection.FROM);
      dropAllDocumentLinks(dropDocument.getCollectionName(), dropDocument.getDocument().getId(), null, LumeerConst.Linking.LinkDirection.TO);
   }

   // TODO onDropCollection

   /**
    * Read all linking documents for specified document
    *
    * @param fromCollectionName
    *       the name of the document's collection
    * @param fromDocumentId
    *       the id of the document to search for links
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @return list of all linked documents
    * @throws DbException
    *       When there is an error working with the database.
    */
   public List<DataDocument> readDocumentLinks(final String fromCollectionName, final String fromDocumentId, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      checkCollectionForRead(fromCollectionName);

      List<DataDocument> links = new ArrayList<>();
      List<DataDocument> linkingTables = readLinkingTablesFrom(fromCollectionName, role, linkDirection);
      for (DataDocument lt : linkingTables) {
         links.addAll(getDataDocumentsFromLinks(fromDocumentId, linkDirection, lt));
      }
      return links;
   }

   /**
    * Read all linking documents between two documents
    *
    * @param fromCollectionName
    *       the name of the first document's collection
    * @param fromId
    *       the id of the first document
    * @param toCollectionName
    *       the name of the second document's collection
    * @param toId
    *       the id of the second document
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @return list of all linked documents
    * @throws DbException
    *       When there is an error working with the database.
    */
   public List<DataDocument> readDocByDocLinks(final String fromCollectionName, final String fromId, final String toCollectionName, final String toId, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      checkCollectionForRead(fromCollectionName);

      List<DataDocument> links = new ArrayList<>();
      List<DataDocument> linkingTables = readLinkingTablesFromTo(fromCollectionName, toCollectionName, role, linkDirection);
      System.out.println(linkingTables.size());
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         List<DataDocument> linkingDocuments = readLinkingDocumentsFromTo(colName, fromId, toId, linkDirection);
         String readCollectionName = linkDirection == LumeerConst.Linking.LinkDirection.FROM ? lt.getString(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION) : lt.getString(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION);
         links.addAll(readDocumentsFromLinkingDocumentsFrom(linkingDocuments, readCollectionName, linkDirection));
      }
      return links;
   }

   /**
    * Read all linking documents for specified document and collection
    *
    * @param fromCollectionName
    *       the name of the document's collection
    * @param fromDocumentId
    *       the id of the document to search for links
    * @param toCollectionName
    *       the name of the collection to search for linking documents
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @return list of all linked documents
    * @throws DbException
    *       When there is an error working with the database.
    */
   public List<DataDocument> readDocWithCollectionLinks(final String fromCollectionName, final String fromDocumentId, final String toCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      checkCollectionForRead(fromCollectionName);
      checkCollectionForRead(toCollectionName);

      List<DataDocument> links = new ArrayList<>();
      List<DataDocument> linkingTables = readLinkingTablesFromTo(fromCollectionName, toCollectionName, role, linkDirection);
      for (DataDocument lt : linkingTables) {
         links.addAll(getDataDocumentsFromLinks(fromDocumentId, linkDirection, lt));
      }
      return links;
   }

   /**
    * Drop all links for specified document
    *
    * @param fromCollectionName
    *       the name of the document's collection
    * @param documentId
    *       the id of the document to drop links
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropAllDocumentLinks(final String fromCollectionName, final String documentId, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      checkCollectionForWrite(fromCollectionName);

      List<DataDocument> linkingTables = readLinkingTablesFrom(fromCollectionName, role, linkDirection);
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         dropAllDocs(colName, documentId, linkDirection);
         checkEmptinessAndRemoveEventually(colName, role);
      }
   }

   /**
    * Drop all links for specified collection
    *
    * @param collectionName
    *       the name of the collection to drop links
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropCollectionLinks(final String collectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      checkCollectionForWrite(collectionName);

      List<DataDocument> linkingTables = readLinkingTablesFrom(collectionName, role, linkDirection);
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         dataStorage.dropCollection(colName);
      }
      dataStorage.dropManyDocuments(LumeerConst.Linking.MainTable.NAME, fromTablesFilter(collectionName, role, linkDirection));
   }

   /**
    * Drop link between two documents
    *
    * @param fromCollectionName
    *       the name of the first document's collection
    * @param fromId
    *       the id of the first document
    * @param toCollectionName
    *       the name of the second document's collection
    * @param toId
    *       the id of the second document
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropDocWithDocLink(final String fromCollectionName, final String fromId, final String toCollectionName, final String toId, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      checkCollectionForWrite(fromCollectionName);
      checkCollectionForWrite(toCollectionName);

      List<DataDocument> linkingTables = readLinkingTablesFromTo(fromCollectionName, toCollectionName, role, linkDirection);
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         dropAllDocs(colName, fromId, toId, linkDirection);
         checkEmptinessAndRemoveEventually(colName, role);
      }
   }

   /**
    * Drop link between document and collection
    *
    * @param fromCollectionName
    *       the name of the document's collection
    * @param fromId
    *       the id of the document
    * @param toCollectionName
    *       the name of the collection to drop links
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropDocWithCollectionLinks(final String fromCollectionName, final String fromId, final String toCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      checkCollectionForWrite(fromCollectionName);
      checkCollectionForWrite(toCollectionName);

      List<DataDocument> linkingTables = readLinkingTablesFromTo(fromCollectionName, toCollectionName, role, linkDirection);
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         dropAllDocs(colName, fromId, linkDirection);
         checkEmptinessAndRemoveEventually(colName, role);
      }
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
    * @param attributes
    *       attributes of link
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void createDocWithDocLink(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final DataDocument attributes, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      checkCollectionForWrite(firstCollectionName);
      checkCollectionForWrite(secondCollectionName);

      if (!(dataStorage.collectionHasDocument(firstCollectionName, firstDocumentId) && dataStorage.collectionHasDocument(secondCollectionName, secondDocumentId))) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (role == null) {
         throw new IllegalArgumentException(ErrorMessageBuilder.paramCanNotBeNull(LumeerConst.Linking.MainTable.ATTR_ROLE));
      }
      String collectionName = checkOrCreateLinkInSystemCollection(firstCollectionName, secondCollectionName, role, linkDirection);
      createLinkIfNotExists(collectionName, firstDocumentId, secondDocumentId, attributes, linkDirection);

   }

   private void createLinkIfNotExists(final String collectionName, final String firstDocumentId, final String secondDocumentId, final DataDocument attributes, final LumeerConst.Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocuments = dataStorage.search(collectionName, fromToDocumentFilter(firstDocumentId, secondDocumentId, linkDirection), null, 0, 0);
      if (linkingDocuments.isEmpty()) {
         String fromId;
         String toId;
         if (linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
            fromId = firstDocumentId;
            toId = secondDocumentId;
         } else {
            fromId = secondDocumentId;
            toId = firstDocumentId;
         }
         DataDocument dataDocument = new DataDocument(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, fromId)
               .append(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, toId)
               .append(LumeerConst.Linking.LinkingTable.ATTR_ATTRIBUTES, attributes);
         dataStorage.createDocument(collectionName, dataDocument);
      }
   }

   private String checkOrCreateLinkInSystemCollection(final String firstCollectionName, final String secondCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      String collectionName;
      List<DataDocument> linkingTable = readLinkingTablesFromTo(firstCollectionName, secondCollectionName, role, linkDirection);
      // role cannot be null there so the size of list is 1 or 0
      if (linkingTable.isEmpty()) {
         collectionName = buildCollectionName(firstCollectionName, secondCollectionName, role, linkDirection);
         createLinkingTable(firstCollectionName, secondCollectionName, role, collectionName, linkDirection);
      } else {
         collectionName = linkingTable.get(0).getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
      }
      return collectionName;
   }

   private List<DataDocument> getDataDocumentsFromLinks(final String fromDocumentId, final LumeerConst.Linking.LinkDirection linkDirection, final DataDocument lt) {
      String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
      List<DataDocument> linkingDocuments = readLinkingDocumentsFrom(colName, fromDocumentId, linkDirection);
      String readCollectionName = linkDirection == LumeerConst.Linking.LinkDirection.FROM ? lt.getString(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION) : lt.getString(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION);
      return readDocumentsFromLinkingDocumentsFrom(linkingDocuments, readCollectionName, linkDirection);
   }

   private void dropAllDocs(final String collectionName, final String documentId, final LumeerConst.Linking.LinkDirection linkDirection) {
      dataStorage.dropManyDocuments(collectionName, fromDocumentFilter(documentId, linkDirection));
   }

   private void dropAllDocs(final String collectionName, final String fromId, final String toId, final LumeerConst.Linking.LinkDirection linkDirection) {
      dataStorage.dropManyDocuments(collectionName, fromToDocumentFilter(fromId, toId, linkDirection));
   }

   private void checkEmptinessAndRemoveEventually(final String collectionName, String role) {
      if (dataStorage.documentCount(collectionName) == 0) {
         dataStorage.dropCollection(collectionName);

         dataStorage.dropManyDocuments(LumeerConst.Linking.MainTable.NAME, fromTablesColNameFilter(collectionName, role));
      }
   }

   private List<DataDocument> readLinkingTablesFromTo(final String firstCollectionName, final String secondCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      return dataStorage.search(LumeerConst.Linking.MainTable.NAME, fromToTablesFilter(firstCollectionName, secondCollectionName, role, linkDirection), null, 0, 0);
   }

   private List<DataDocument> readLinkingTablesFrom(final String fromCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      return dataStorage.search(LumeerConst.Linking.MainTable.NAME, fromTablesFilter(fromCollectionName, role, linkDirection), null, 0, 0);
   }

   private List<DataDocument> readLinkingDocumentsFrom(final String collectionName, final String fromId, final LumeerConst.Linking.LinkDirection linkDirection) {
      return dataStorage.search(collectionName, fromDocumentFilter(fromId, linkDirection), null, 0, 0);
   }

   private List<DataDocument> readLinkingDocumentsFromTo(final String collectionName, final String fromId, final String toId, final LumeerConst.Linking.LinkDirection linkDirection) {
      return dataStorage.search(collectionName, fromToDocumentFilter(fromId, toId, linkDirection), null, 0, 0);
   }

   private List<DataDocument> readDocumentsFromLinkingDocumentsFrom(final List<DataDocument> linkingDocuments, final String collectionName, final LumeerConst.Linking.LinkDirection linkDirection) {
      List<DataDocument> docs = new ArrayList<>();
      String keyParam = linkDirection == LumeerConst.Linking.LinkDirection.TO ? LumeerConst.Linking.LinkingTable.ATTR_FROM_ID : LumeerConst.Linking.LinkingTable.ATTR_TO_ID;
      for (DataDocument ld : linkingDocuments) {
         // check for right id of linking document
         String linkingDocumentId = ld.getString(keyParam);

         DataDocument doc = dataStorage.readDocument(collectionName, linkingDocumentId);
         if (doc != null) {
            docs.add(doc);
         }
      }
      return docs;
   }

   private String fromTablesColNameFilter(final String collectionName, final String role) {
      Bson filterRaw = role == null || role.isEmpty() ? Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName) :
            Filters.and(
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName),
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_ROLE, role));
      return MongoUtils.convertBsonToJson(filterRaw);
   }

   private String fromTablesFilter(final String firstCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      String collParam = linkDirection == LumeerConst.Linking.LinkDirection.FROM ? LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION : LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION;
      Bson filterRaw = role == null || role.isEmpty() ? Filters.eq(collParam, firstCollectionName) :
            Filters.and(
                  Filters.eq(collParam, firstCollectionName),
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_ROLE, role)
            );
      return MongoUtils.convertBsonToJson(filterRaw);
   }

   private String fromToTablesFilter(final String firstCollectionName, final String secondCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      String fromCollectionName;
      String toCollectionName;
      if (linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         fromCollectionName = firstCollectionName;
         toCollectionName = secondCollectionName;
      } else {
         fromCollectionName = secondCollectionName;
         toCollectionName = firstCollectionName;
      }
      Bson filterRaw = role == null ?
            Filters.and(
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION, fromCollectionName),
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION, toCollectionName)) :
            Filters.and(
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION, fromCollectionName),
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION, toCollectionName),
                  Filters.eq(LumeerConst.Linking.MainTable.ATTR_ROLE, role));
      return MongoUtils.convertBsonToJson(filterRaw);
   }

   private String fromToDocumentFilter(final String fromId, final String toId, final LumeerConst.Linking.LinkDirection linkDirection) {
      Bson filterRaw = linkDirection == LumeerConst.Linking.LinkDirection.FROM ?
            Filters.and(
                  Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, fromId),
                  Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, toId)
            ) :
            Filters.and(
                  Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, toId),
                  Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, fromId)
            );
      return MongoUtils.convertBsonToJson(filterRaw);
   }

   private String fromDocumentFilter(final String fromId, final LumeerConst.Linking.LinkDirection linkDirection) {
      Bson filterRaw = linkDirection == LumeerConst.Linking.LinkDirection.FROM ?
            Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID, fromId)
            :
            Filters.eq(LumeerConst.Linking.LinkingTable.ATTR_TO_ID, fromId);
      return MongoUtils.convertBsonToJson(filterRaw);
   }

   private void createLinkingTable(final String firstCollectionName, final String secondCollectionName, final String role, final String collectionName, final LumeerConst.Linking.LinkDirection linkDirection) {
      String fromCollectionName;
      String toCollectionName;
      if (linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         fromCollectionName = firstCollectionName;
         toCollectionName = secondCollectionName;
      } else {
         fromCollectionName = secondCollectionName;
         toCollectionName = firstCollectionName;
      }
      DataDocument doc = new DataDocument();
      doc.put(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION, fromCollectionName);
      doc.put(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION, toCollectionName);
      doc.put(LumeerConst.Linking.MainTable.ATTR_COL_NAME, collectionName);
      doc.put(LumeerConst.Linking.MainTable.ATTR_ROLE, role);

      dataStorage.createDocument(LumeerConst.Linking.MainTable.NAME, doc);

   }

   private String buildCollectionName(final String firstCollectionName, final String secondCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      String fromCollectionName;
      String toCollectionName;
      if (linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         fromCollectionName = firstCollectionName;
         toCollectionName = secondCollectionName;
      } else {
         fromCollectionName = secondCollectionName;
         toCollectionName = firstCollectionName;
      }
      return LumeerConst.Linking.PREFIX + "_" + fromCollectionName + "_" + toCollectionName + "_" + role;
   }

   private void checkCollectionForRead(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!collectionMetadataFacade.checkCollectionForRead(collectionName, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
   }

   private void checkCollectionForWrite(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!collectionMetadataFacade.checkCollectionForWrite(collectionName, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
   }
}