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
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.event.DropDocument;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.provider.DataStorageProvider;
import io.lumeer.engine.rest.dao.LinkDao;
import io.lumeer.engine.rest.dao.LinkTypeDao;

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

   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private DataStorageProvider dataStorageProvider;

   /**
    * Creates main linking table if not exists
    */
   @PostConstruct
   public void init() {
      dataStorage = dataStorageProvider.getUserStorage();

      if (!dataStorage.hasCollection(LumeerConst.Linking.MainTable.NAME)) {
         dataStorage.createCollection(LumeerConst.Linking.MainTable.NAME);
      }
   }

   public void onDropDocument(@Observes(notifyObserver = Reception.IF_EXISTS) final DropDocument dropDocument) throws DbException {
      dropAllDocumentLinks(dropDocument.getCollectionName(), dropDocument.getDocument().getId(), null, LumeerConst.Linking.LinkDirection.FROM);
      dropAllDocumentLinks(dropDocument.getCollectionName(), dropDocument.getDocument().getId(), null, LumeerConst.Linking.LinkDirection.TO);
   }

   /**
    * Read all link types for selected collection
    *
    * @param collectionName
    *       the name of the  collection
    * @param linkDirection
    *       direction of link
    * @return list of all link types
    * @throws DbException
    *       When there is an error working with the database.
    */
   public List<LinkTypeDao> readLinkTypes(final String collectionName, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      List<LinkTypeDao> links = new ArrayList<>();
      List<DataDocument> linkingTables = readLinkingTablesFrom(collectionName, null, linkDirection);

      for (DataDocument lt : linkingTables) {
         String fromCollectionName = lt.getString(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION);
         String toCollectionName = lt.getString(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION);
         String role = lt.getString(LumeerConst.Linking.MainTable.ATTR_ROLE);
         LinkTypeDao dao = new LinkTypeDao(fromCollectionName, toCollectionName, role);
         links.add(dao);
      }

      return links;
   }

   /**
    * Read all links for selected collection
    *
    * @param collectionName
    *       the name of the  collection
    * @param linkDirection
    *       direction of link
    * @param role
    *       role name
    * @return list of all links
    * @throws DbException
    *       When there is an error working with the database.
    */
   public List<LinkDao> readLinks(final String collectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      List<LinkDao> links = new ArrayList<>();
      List<DataDocument> linkingTables = readLinkingTablesFrom(collectionName, role, linkDirection);

      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         String fromCollection = lt.getString(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION);
         String toCollection = lt.getString(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION);
         List<DataDocument> ls = dataStorage.search(colName, null, null, 0, 0);
         links.addAll(convertLinkDaosFromDocuments(ls, fromCollection, toCollection, role));
      }
      return links;
   }

   /**
    * Read all links between two documents
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
    * @return list of all links
    * @throws DbException
    *       When there is an error working with the database.
    */
   public List<LinkDao> readDocByDocLinks(final String fromCollectionName, final String fromId, final String toCollectionName, final String toId, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      List<LinkDao> links = new ArrayList<>();
      List<DataDocument> linkingTables = readLinkingTablesFromTo(fromCollectionName, toCollectionName, role, linkDirection);

      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         String fromCollection = lt.getString(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION);
         String toCollection = lt.getString(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION);
         List<DataDocument> ls = readLinkingDocumentsFromTo(colName, fromId, toId, linkDirection);
         links.addAll(convertLinkDaosFromDocuments(ls, fromCollection, toCollection, role));
      }

      return links;
   }

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
   public List<DataDocument> readDocumentLinksDocs(final String fromCollectionName, final String fromDocumentId, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
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
   public List<DataDocument> readDocByDocLinksDocs(final String fromCollectionName, final String fromId, final String toCollectionName, final String toId, final String role, final LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      List<DataDocument> links = new ArrayList<>();
      List<DataDocument> linkingTables = readLinkingTablesFromTo(fromCollectionName, toCollectionName, role, linkDirection);
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
      List<DataDocument> linkingTables = readLinkingTablesFrom(collectionName, role, linkDirection);

      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.Linking.MainTable.ATTR_COL_NAME);
         dataStorage.dropCollection(colName);
      }
      dataStorage.dropManyDocuments(LumeerConst.Linking.MainTable.NAME, dataStorageDialect.linkingFromTablesFilter(collectionName, role, linkDirection));
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
      String collectionName = checkOrCreateLinkInSystemCollection(firstCollectionName, secondCollectionName, role, linkDirection);
      createLinkIfNotExists(collectionName, firstDocumentId, secondDocumentId, attributes, linkDirection);
   }

   private void createLinkIfNotExists(final String collectionName, final String firstDocumentId, final String secondDocumentId, final DataDocument attributes, final LumeerConst.Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocuments = dataStorage.search(collectionName, dataStorageDialect.linkingFromToDocumentFilter(firstDocumentId, secondDocumentId, linkDirection), null, 0, 0);
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

   private List<LinkDao> convertLinkDaosFromDocuments(final List<DataDocument> ls, final String fromCollection, final String toCollection, final String role) {
      List<LinkDao> links = new ArrayList<>();
      for (DataDocument l : ls) {
         String fromId = l.getString(LumeerConst.Linking.LinkingTable.ATTR_FROM_ID);
         String toId = l.getString(LumeerConst.Linking.LinkingTable.ATTR_TO_ID);
         DataDocument attrs = l.getDataDocument(LumeerConst.Linking.LinkingTable.ATTR_ATTRIBUTES);
         links.add(new LinkDao(fromCollection, toCollection, role, fromId, toId, attrs));
      }
      return links;
   }

   private void dropAllDocs(final String collectionName, final String documentId, final LumeerConst.Linking.LinkDirection linkDirection) {
      dataStorage.dropManyDocuments(collectionName, dataStorageDialect.linkingFromDocumentFilter(documentId, linkDirection));
   }

   private void dropAllDocs(final String collectionName, final String fromId, final String toId, final LumeerConst.Linking.LinkDirection linkDirection) {
      dataStorage.dropManyDocuments(collectionName, dataStorageDialect.linkingFromToDocumentFilter(fromId, toId, linkDirection));
   }

   private void checkEmptinessAndRemoveEventually(final String collectionName, String role) {
      if (dataStorage.documentCount(collectionName) == 0) {
         dataStorage.dropCollection(collectionName);

         dataStorage.dropManyDocuments(LumeerConst.Linking.MainTable.NAME, dataStorageDialect.linkingFromTablesColNameFilter(collectionName, role));
      }
   }

   private List<DataDocument> readLinkingTablesFromTo(final String firstCollectionName, final String secondCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      return dataStorage.search(LumeerConst.Linking.MainTable.NAME, dataStorageDialect.linkingFromToTablesFilter(firstCollectionName, secondCollectionName, role, linkDirection), null, 0, 0);
   }

   private List<DataDocument> readLinkingTablesFrom(final String fromCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection) {
      return dataStorage.search(LumeerConst.Linking.MainTable.NAME, dataStorageDialect.linkingFromTablesFilter(fromCollectionName, role, linkDirection), null, 0, 0);
   }

   private List<DataDocument> readLinkingDocumentsFrom(final String collectionName, final String fromId, final LumeerConst.Linking.LinkDirection linkDirection) {
      return dataStorage.search(collectionName, dataStorageDialect.linkingFromDocumentFilter(fromId, linkDirection), null, 0, 0);
   }

   private List<DataDocument> readLinkingDocumentsFromTo(final String collectionName, final String fromId, final String toId, final LumeerConst.Linking.LinkDirection linkDirection) {
      return dataStorage.search(collectionName, dataStorageDialect.linkingFromToDocumentFilter(fromId, toId, linkDirection), null, 0, 0);
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

}