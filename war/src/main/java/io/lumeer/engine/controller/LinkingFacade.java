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
import io.lumeer.mongodb.MongoUtils;

import com.mongodb.client.model.Filters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
public class LinkingFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   public List<DataDocument> readAllDocumentLinks(String collectionName, String documentId) {
      List<DataDocument> linkingTables = readLinkingTables(collectionName);
      List<DataDocument> docLinks = new ArrayList<>();
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL_NAME);
         List<DataDocument> linkingDocuments = readLinkingDocuments(colName, documentId);

         String firstColName = lt.getString(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL1);
         String linkingCollectionName = firstColName.equals(collectionName) ? firstColName : lt.getString(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL2);

         docLinks.addAll(readDocumentsFromLinkingDocuments(linkingDocuments, documentId, linkingCollectionName));
      }
      return docLinks;
   }

   public List<DataDocument> readDocWithCollectionLinks(String firstCollectionName, String firstDocumentId, String secondCollectionName) {
      DataDocument linkingTable = readLinkingTable(firstCollectionName, secondCollectionName);
      if (linkingTable == null) {
         return new ArrayList<>();
      }

      List<DataDocument> docLinks = new ArrayList<>();
      String colName = linkingTable.getString(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL_NAME);
      List<DataDocument> linkingDocuments = readLinkingDocuments(colName, firstDocumentId);

      return readDocumentsFromLinkingDocuments(linkingDocuments, firstDocumentId, secondCollectionName);
   }

   public void dropAllDocumentLinks(String collectionName, String documentId) {
      List<DataDocument> linkingTables = readLinkingTables(collectionName);
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL_NAME);
         removeAllDocuments(colName, documentId);
      }
   }

   public void dropDocWithDocLink(String firstCollectionName, String firstDocumentId, String secondCollectionName, String secondDocumentId) {
      DataDocument linkingTable = readLinkingTable(firstCollectionName, secondCollectionName);
      if (linkingTable == null) {
         // throw exception?
         return;
      }
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.and(
                  Filters.eq(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC1, firstDocumentId),
                  Filters.eq(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC2, secondDocumentId)),
            Filters.and(
                  Filters.eq(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC1, secondDocumentId),
                  Filters.eq(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC2, firstDocumentId))));

      dataStorage.dropManyDocuments(linkingTable.getString(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL_NAME), filter);
   }

   public void dropDocWithCollectionLinks(String firstCollectionName, String firstDocumentId, String secondCollectionName) {
      DataDocument linkingTable = readLinkingTable(firstCollectionName, secondCollectionName);
      if (linkingTable == null) {
         // throw exception?
         return;
      }
      String colName = linkingTable.getString(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL_NAME);
      removeAllDocuments(colName, firstDocumentId);
   }

   public void dropCollectionLinks(String collectionName) {
      List<DataDocument> linkingTables = readLinkingTables(collectionName);
      for (DataDocument lt : linkingTables) {
         String colName = lt.getString(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL_NAME);
         dataStorage.dropCollection(colName);
      }
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.eq(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL1, collectionName),
            Filters.eq(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL2, collectionName)
      ));
      dataStorage.dropManyDocuments(collectionName, filter);
   }

   public void createDocWithDocLink(String firstCollectionName, String firstDocumentId, String secondCollectionName, String secondDocumentId) {
      // TODO
      throw new UnsupportedOperationException();
   }

   public void createDocWithColletionLinks(String firstCollectionName, String firstDocumentId, String secondCollectionName, List<String> documentIds) {
      // TODO
      throw new UnsupportedOperationException();
   }

   private DataDocument readLinkingTable(String firstCollectionName, String secondCollectionName) {
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.and(
                  Filters.eq(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL1, firstCollectionName),
                  Filters.eq(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL2, secondCollectionName)),
            Filters.and(Filters.eq(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL2, firstCollectionName),
                  Filters.eq(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL1, secondCollectionName))));
      List<DataDocument> linkingTables = dataStorage.search(LumeerConst.LINKING.MAIN_TABLE.NAME, filter, null, 0, 0);
      return linkingTables.size() == 1 ? linkingTables.get(0) : null;
   }

   private List<DataDocument> readLinkingTables(String firstCollectionName) {
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.eq(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL1, firstCollectionName),
            Filters.eq(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL2, firstCollectionName)));
      return dataStorage.search(LumeerConst.LINKING.MAIN_TABLE.NAME, filter, null, 0, 0);
   }

   private List<DataDocument> readLinkingDocuments(final String collectionName, String documentId) {
      String filter = MongoUtils.convertBsonToJson(Filters.or(
            Filters.eq(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC1, documentId),
            Filters.eq(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC2, documentId)));
      return dataStorage.search(collectionName, filter, null, 0, 0);
   }

   private List<DataDocument> readDocumentsFromLinkingDocuments(List<DataDocument> linkingDocuments, String documentId, String collectionName) {
      List<DataDocument> docs = new ArrayList<>();
      for (DataDocument ld : linkingDocuments) {
         String firstDocId = ld.getString(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC1);
         String linkingDocumentId = firstDocId.equals(documentId) ? firstDocId : ld.getString(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC2);

         DataDocument doc = dataStorage.readDocument(collectionName, linkingDocumentId);
         if (doc != null) {
            docs.add(doc);
         }
      }
      return docs;
   }

   private void removeAllDocuments(final String collectionName, final String documentId) {
      String filter = Filters.or(Filters.eq(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC1, documentId), Filters.eq(LumeerConst.LINKING.LINKING_TABLE.ATTR_DOC2, documentId)).toString();
      dataStorage.dropManyDocuments(collectionName, filter);
   }

   private void createLinkingTable(String firstCollectionName, String secondCollectionName) {
      DataDocument doc = new DataDocument();
      doc.put(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL1, firstCollectionName);
      doc.put(LumeerConst.LINKING.MAIN_TABLE.ATTR_COL2, secondCollectionName);

      dataStorage.createDocument(LumeerConst.LINKING.MAIN_TABLE.NAME, doc);

   }
}