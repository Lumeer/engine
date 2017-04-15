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

import static io.lumeer.engine.api.LumeerConst.*;

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.event.DropDocument;
import io.lumeer.engine.rest.dao.LinkDao;
import io.lumeer.engine.rest.dao.LinkTypeDao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.inject.Inject;

/**
 * Manipulates with documents links
 */
@SessionScoped
public class LinkingFacade implements Serializable {

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private ProjectFacade projectFacade;

   public void onDropDocument(@Observes(notifyObserver = Reception.IF_EXISTS) final DropDocument dropDocument) {
      dropAllDocumentLinks(dropDocument.getCollectionName(), dropDocument.getDocument().getId(), null, Linking.LinkDirection.FROM);
      dropAllDocumentLinks(dropDocument.getCollectionName(), dropDocument.getDocument().getId(), null, Linking.LinkDirection.TO);
   }

   /**
    * Read all link types for selected collection
    *
    * @param collectionName
    *       the name of the  collection
    * @param linkDirection
    *       direction of link
    * @return list of all link types
    */
   public List<LinkTypeDao> readLinkTypes(final String collectionName, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsOneWay(collectionName, null, linkDirection);

      List<LinkTypeDao> links = new ArrayList<>();
      linkingDocs.forEach(lt -> links.add(new LinkTypeDao(lt.getString(Linking.MainTable.ATTR_FROM_COLLECTION),
            lt.getString(Linking.MainTable.ATTR_TO_COLLECTION), lt.getString(Linking.MainTable.ATTR_ROLE))));

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
    */
   public List<LinkDao> readLinks(final String collectionName, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsOneWay(collectionName, role, linkDirection);

      List<LinkDao> links = new ArrayList<>();
      String linkingCollectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String fromCollection = lt.getString(Linking.MainTable.ATTR_FROM_COLLECTION);
         String toCollection = lt.getString(Linking.MainTable.ATTR_TO_COLLECTION);
         String attrRole = lt.getString(Linking.MainTable.ATTR_ROLE);

         List<DataDocument> ls = dataStorage.search(linkingCollectionName, dataStorageDialect.fieldValueFilter(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, lt.getId()), null, 0, 0);
         for (DataDocument doc : ls) {
            String fromDocumentId = linkDirection == Linking.LinkDirection.FROM ? doc.getString(Linking.LinkingTable.ATTR_FROM_ID) : doc.getString(Linking.LinkingTable.ATTR_TO_ID);
            String toDocumentId = linkDirection == Linking.LinkDirection.FROM ? doc.getString(Linking.LinkingTable.ATTR_TO_ID) : doc.getString(Linking.LinkingTable.ATTR_FROM_ID);
            links.add(new LinkDao(fromCollection, toCollection, attrRole, fromDocumentId, toDocumentId, doc.getDataDocument(Linking.LinkingTable.ATTR_ATTRIBUTES)));
         }
      }
      return links;
   }

   /**
    * Read all links between two documents
    *
    * @param firstCollectionName
    *       the name of the first document's collection
    * @param firstDocumentId
    *       the id of the first document
    * @param secondCollectionName
    *       the name of the second document's collection
    * @param secondDocumentId
    *       the id of the second document
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @return list of all links
    */
   public List<LinkDao> readDocByDocLinks(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsFromTo(firstCollectionName, secondCollectionName, role, linkDirection);

      List<LinkDao> links = new ArrayList<>();
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String fromCollection = lt.getString(Linking.MainTable.ATTR_FROM_COLLECTION);
         String toCollection = lt.getString(Linking.MainTable.ATTR_TO_COLLECTION);
         String attrRole = lt.getString(Linking.MainTable.ATTR_ROLE);

         DataDocument dc = dataStorage.readDocument(collectionName, linkingDocumentsFilter(lt.getId(), firstDocumentId, secondDocumentId, linkDirection));
         if (dc != null) {
            String fromId = linkDirection == Linking.LinkDirection.FROM ? firstDocumentId : secondDocumentId;
            String toId = linkDirection == Linking.LinkDirection.FROM ? secondDocumentId : firstDocumentId;

            links.add(new LinkDao(fromCollection, toCollection, attrRole, fromId, toId, dc.getDataDocument(Linking.LinkingTable.ATTR_ATTRIBUTES)));
         }
      }

      return links;
   }

   /**
    * Read all linking documents for specified document
    *
    * @param collectionName
    *       the name of the document's collection
    * @param documentId
    *       the id of the document to search for links
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @return list of all linked documents
    */
   public List<DataDocument> readDocumentLinksDocs(final String collectionName, final String documentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsOneWay(collectionName, role, linkDirection);

      return readDataDocumentsFromLinks(linkingDocs, documentId, linkDirection);
   }

   /**
    * Read all linking documents between two documents
    *
    * @param firstCollectionName
    *       the name of the first document's collection
    * @param firstDocumentId
    *       the id of the first document
    * @param secondCollectionName
    *       the name of the second document's collection
    * @param secondDocumentId
    *       the id of the second document
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @return list of all linked documents
    */
   public List<DataDocument> readDocByDocLinksDocs(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> links = new ArrayList<>();
      List<DataDocument> linkingDocs = readLinkingDocsFromTo(firstCollectionName, secondCollectionName, role, linkDirection);
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String readCollectionName = linkDirection == Linking.LinkDirection.FROM ? lt.getString(Linking.MainTable.ATTR_TO_COLLECTION) : lt.getString(Linking.MainTable.ATTR_FROM_COLLECTION);
         String param = linkDirection == Linking.LinkDirection.FROM ? Linking.LinkingTable.ATTR_TO_ID : Linking.LinkingTable.ATTR_FROM_ID;
         DataDocument dc = dataStorage.readDocument(collectionName, linkingDocumentsFilter(lt.getId(), firstDocumentId, secondDocumentId, linkDirection));
         if (dc != null) {
            String id = dc.getString(param);
            DataDocument doc = dataStorage.readDocument(readCollectionName, dataStorageDialect.documentIdFilter(id));
            if (doc != null) {
               links.add(doc);
            }
         }
      }
      return links;
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
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    * @return list of all linked documents
    */
   public List<DataDocument> readDocWithCollectionLinks(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsFromTo(firstCollectionName, secondCollectionName, role, linkDirection);

      return readDataDocumentsFromLinks(linkingDocs, firstDocumentId, linkDirection);
   }

   /**
    * Drop all links for specified document
    *
    * @param collectionName
    *       the name of the document's collection
    * @param documentId
    *       the id of the document to drop links
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    */
   public void dropAllDocumentLinks(final String collectionName, final String documentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsOneWay(collectionName, role, linkDirection);
      dropDocumentLinks(linkingDocs, documentId, role, linkDirection);
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
    */
   public void dropCollectionLinks(final String collectionName, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsOneWay(collectionName, role, linkDirection);
      String linkingCollectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String id = lt.getId();
         dataStorage.dropManyDocuments(linkingCollectionName, linkingDocumentsIdFilter(id));
         dataStorage.dropDocument(Linking.MainTable.NAME, dataStorageDialect.documentIdFilter(id));
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
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    */
   public void dropDocWithDocLink(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsFromTo(firstCollectionName, secondCollectionName, role, linkDirection);
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String id = lt.getId();
         dataStorage.dropManyDocuments(collectionName, linkingDocumentsFilter(id, firstDocumentId, secondDocumentId, linkDirection));
         if (linkTypeIsEmpty(id)) {
            dataStorage.dropDocument(Linking.MainTable.NAME, dataStorageDialect.documentIdFilter(id));
         }
      }
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
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    */
   public void dropDocWithCollectionLinks(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String role, final Linking.LinkDirection linkDirection) {
      List<DataDocument> linkingDocs = readLinkingDocsFromTo(firstCollectionName, secondCollectionName, role, linkDirection);
      dropDocumentLinks(linkingDocs, firstDocumentId, role, linkDirection);
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
    */
   public void createDocWithDocLink(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final DataDocument attributes, final String role, final Linking.LinkDirection linkDirection) {
      String mainTableId = createNewLinkingTypeIfNecessary(firstCollectionName, secondCollectionName, role, linkDirection);

      DataDocument dataDocument = new DataDocument(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, mainTableId)
            .append(Linking.LinkingTable.ATTR_FROM_ID, linkDirection == Linking.LinkDirection.FROM ? firstDocumentId : secondDocumentId)
            .append(Linking.LinkingTable.ATTR_TO_ID, linkDirection == Linking.LinkDirection.FROM ? secondDocumentId : firstDocumentId)
            .append(Linking.LinkingTable.ATTR_ATTRIBUTES, attributes);
      dataStorage.createDocument(buildCollectionName(), dataDocument);
   }

   /**
    * Create link from document to many documents
    *
    * @param firstCollectionName
    *       the name of the first document's collection
    * @param firstDocumentId
    *       the id of the first document
    * @param secondCollectionName
    *       the name of the second document's collection
    * @param secondDocumentsIds
    *       the ids of documents to create link
    * @param attributesList
    *       attributes of links
    * @param role
    *       role name
    * @param linkDirection
    *       direction of link
    */
   public void createDocWithDocsLinks(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final List<String> secondDocumentsIds, final List<DataDocument> attributesList, final String role, final Linking.LinkDirection linkDirection) {
      String mainTableId = createNewLinkingTypeIfNecessary(firstCollectionName, secondCollectionName, role, linkDirection);

      List<DataDocument> dataDocuments = new LinkedList<>();
      for (int i = 0; i < secondDocumentsIds.size(); i++) {
         dataDocuments.add(new DataDocument(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, mainTableId)
               .append(Linking.LinkingTable.ATTR_FROM_ID, linkDirection == Linking.LinkDirection.FROM ? firstDocumentId : secondDocumentsIds.get(i))
               .append(Linking.LinkingTable.ATTR_TO_ID, linkDirection == Linking.LinkDirection.FROM ? secondDocumentsIds.get(i) : firstDocumentId)
               .append(Linking.LinkingTable.ATTR_ATTRIBUTES, attributesList.get(i))
         );
      }
      dataStorage.createDocuments(buildCollectionName(), dataDocuments);
   }

   private List<DataDocument> readDataDocumentsFromLinks(final List<DataDocument> linkingDocs, final String documentId, final Linking.LinkDirection linkDirection) {
      List<DataDocument> links = new ArrayList<>();
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String readCollectionName = linkDirection == Linking.LinkDirection.FROM ? lt.getString(Linking.MainTable.ATTR_TO_COLLECTION) : lt.getString(Linking.MainTable.ATTR_FROM_COLLECTION);
         String param = linkDirection == Linking.LinkDirection.FROM ? Linking.LinkingTable.ATTR_TO_ID : Linking.LinkingTable.ATTR_FROM_ID;
         List<DataDocument> docs = dataStorage.search(collectionName, linkingDocumentFilter(lt.getId(), documentId, linkDirection), null, 0, 0);
         for (DataDocument dc : docs) {
            String id = dc.getString(param);
            DataDocument doc = dataStorage.readDocument(readCollectionName, dataStorageDialect.documentIdFilter(id));
            if (doc != null) {
               links.add(doc);
            }
         }
      }
      return links;
   }

   private void dropDocumentLinks(final List<DataDocument> linkingDocs, final String documentId, final String role, final Linking.LinkDirection linkDirection) {
      String collectionName = buildCollectionName();
      for (DataDocument lt : linkingDocs) {
         String id = lt.getId();
         dataStorage.dropManyDocuments(collectionName, linkingDocumentFilter(id, documentId, linkDirection));
         if (linkTypeIsEmpty(id)) {
            dataStorage.dropDocument(Linking.MainTable.NAME, dataStorageDialect.documentIdFilter(id));
         }
      }
   }

   private boolean linkTypeIsEmpty(final String id) {
      return dataStorage.search(buildCollectionName(), dataStorageDialect.fieldValueFilter(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, id), null, 0, 1).isEmpty();
   }

   private List<DataDocument> readLinkingDocsOneWay(final String collectionName, final String role, final Linking.LinkDirection linkDirection) {
      String param = linkDirection == Linking.LinkDirection.FROM ? Linking.MainTable.ATTR_FROM_COLLECTION : Linking.MainTable.ATTR_TO_COLLECTION;
      return dataStorage.search(Linking.MainTable.NAME, linkingOneWayFilter(param, collectionName, role), null, 0, 0);
   }

   private List<DataDocument> readLinkingDocsFromTo(final String firstCollectionName, final String secondCollectionName, final String role, final Linking.LinkDirection linkDirection) {
      String fromCollectionName = linkDirection == Linking.LinkDirection.FROM ? firstCollectionName : secondCollectionName;
      String toCollectionName = linkDirection == Linking.LinkDirection.FROM ? secondCollectionName : firstCollectionName;
      return dataStorage.search(Linking.MainTable.NAME, linkingFromToFilter(fromCollectionName, toCollectionName, role), null, 0, 0);
   }

   private String createNewLinkingTypeIfNecessary(final String firstCollectionName, final String secondCollectionName, final String role, final Linking.LinkDirection linkDirection) {
      String fromCollectionName = linkDirection == Linking.LinkDirection.FROM ? firstCollectionName : secondCollectionName;
      String toCollectionName = linkDirection == Linking.LinkDirection.FROM ? secondCollectionName : firstCollectionName;

      DataDocument linkingType = dataStorage.readDocument(Linking.MainTable.NAME, linkingFromToFilter(fromCollectionName, toCollectionName, role));
      if (linkingType != null) { // if linking type already exists, we return it
         return linkingType.getId();
      }

      //otherwise we create linking type and also collection for link if necessary
      DataDocument doc = new DataDocument();
      doc.put(Linking.MainTable.ATTR_FROM_COLLECTION, fromCollectionName);
      doc.put(Linking.MainTable.ATTR_TO_COLLECTION, toCollectionName);
      doc.put(Linking.MainTable.ATTR_PROJECT, projectFacade.getCurrentProjectId());
      doc.put(Linking.MainTable.ATTR_ROLE, role);

      String mainTableId = dataStorage.createDocument(Linking.MainTable.NAME, doc);

      String linkingCollectionName = buildCollectionName();
      if (!dataStorage.hasCollection(linkingCollectionName)) {
         dataStorage.createCollection(linkingCollectionName);
         dataStorage.createIndex(linkingCollectionName, new DataDocument(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, Index.ASCENDING)
               .append(Linking.LinkingTable.ATTR_FROM_ID, Index.ASCENDING)
               .append(Linking.LinkingTable.ATTR_TO_ID, Index.ASCENDING), true);
         dataStorage.createIndex(linkingCollectionName, new DataDocument(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, Index.ASCENDING)
               .append(Linking.LinkingTable.ATTR_TO_ID, Index.ASCENDING)
               .append(Linking.LinkingTable.ATTR_FROM_ID, Index.ASCENDING), true);
      }

      return mainTableId;
   }

   private DataFilter linkingDocumentsIdFilter(final String mainTableId) {
      return dataStorageDialect.fieldValueFilter(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, mainTableId);
   }

   private DataFilter linkingDocumentFilter(final String mainTableId, final String documentId, Linking.LinkDirection linkDirection) {
      Map<String, Object> fields = new HashMap<>();
      fields.put(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, mainTableId);
      fields.put(linkDirection == Linking.LinkDirection.FROM ? Linking.LinkingTable.ATTR_FROM_ID : Linking.LinkingTable.ATTR_TO_ID, documentId);
      return dataStorageDialect.multipleFieldsValueFilter(fields);
   }

   private DataFilter linkingDocumentsFilter(final String mainTableId, final String firstDocumentId, final String secondDocumentId, Linking.LinkDirection linkDirection) {
      Map<String, Object> fields = new HashMap<>();
      fields.put(Linking.LinkingTable.ATTR_MAIN_TABLE_ID, mainTableId);
      fields.put(Linking.LinkingTable.ATTR_FROM_ID, linkDirection == Linking.LinkDirection.FROM ? firstDocumentId : secondDocumentId);
      fields.put(Linking.LinkingTable.ATTR_TO_ID, linkDirection == Linking.LinkDirection.FROM ? secondDocumentId : firstDocumentId);
      return dataStorageDialect.multipleFieldsValueFilter(fields);
   }

   private DataFilter linkingOneWayFilter(final String param, final String collectionName, final String role) {
      Map<String, Object> fields = new HashMap<>();
      fields.put(LumeerConst.Linking.MainTable.ATTR_PROJECT, projectFacade.getCurrentProjectId());
      fields.put(param, collectionName);
      if (role != null) {
         fields.put(LumeerConst.Linking.MainTable.ATTR_ROLE, role);
      }
      return dataStorageDialect.multipleFieldsValueFilter(fields);
   }

   private DataFilter linkingFromToFilter(final String fromCollectionName, final String toCollectionName, final String role) {
      Map<String, Object> fields = new HashMap<>();
      fields.put(LumeerConst.Linking.MainTable.ATTR_PROJECT, projectFacade.getCurrentProjectId());
      fields.put(LumeerConst.Linking.MainTable.ATTR_FROM_COLLECTION, fromCollectionName);
      fields.put(LumeerConst.Linking.MainTable.ATTR_TO_COLLECTION, toCollectionName);
      if (role != null) {
         fields.put(LumeerConst.Linking.MainTable.ATTR_ROLE, role);
      }
      return dataStorageDialect.multipleFieldsValueFilter(fields);
   }

   private String buildCollectionName() {
      return Linking.PREFIX + "_" + projectFacade.getCurrentProjectId();
   }

}
