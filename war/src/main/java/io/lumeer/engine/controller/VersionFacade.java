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

import java.io.Serializable;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
public class VersionFacade implements Serializable {
   private final String SHADOW = ".shadow";
   private final String VERSION_STRING = "_metadata-version";
   private final String DOCUMENT_ID_STRING = "_id";

   @Inject
   private DataStorage dataStorage;

   public String getVersionMetadataString() {
      return VERSION_STRING;
   }

   /**
    * Return document version
    *
    * @param collectionName
    *       collection name in which document is stored
    * @param documentId
    *       the id of readed document
    * @return version of readed document as integer
    */
   public int getDocumentVersion(String collectionName, String documentId) {
      return getDocumentVersion(dataStorage.readDocument(collectionName, documentId));
   }

   /**
    * Return document version
    *
    * @param document
    *       document where this id is stored
    * @return integer, document version
    */
   public int getDocumentVersion(DataDocument document) {
      return document.getInteger(VERSION_STRING);
   }

   public boolean verifyDocumentUpdate(String collectionName, DataDocument document) {
      //HOW TO DO ? WHAT IS NEEDED ?
      return true;
   }

   /**
    * Create shadow collection if not created. Backup document with
    * same id as document in collection. Then replace document in
    * collection with document from input.
    *
    * @param collectionName
    *       collection name, where document is stored
    * @param document
    *       document, which will be stored in SHADOW collection
    *       and then updated in new collection to new version.
    *       After that it is possible to change data and save it
    * @return hmm
    */
   public int newDocumentVersion(String collectionName, DataDocument document) {
      int oldVersion = backUp(collectionName, document.get(DOCUMENT_ID_STRING).toString());
      createMetadata(document);
      document.replace(VERSION_STRING, oldVersion + 1);
      dataStorage.updateDocument(collectionName, document, document.get(DOCUMENT_ID_STRING).toString());
      return oldVersion + 1;
   }

   /**
    * Create metadata if not exists
    *
    * @param document
    *       document where to create metadata
    */
   private void createMetadata(DataDocument document) {
      if (!document.containsKey(VERSION_STRING)) {
         document.put(VERSION_STRING, 0);
      }
   }

   /**
    * Create shadow collection if not exists.
    *
    * @param collectionName
    *       collection name to imput.
    */
   private void createShadow(String collectionName) {
      if (!(dataStorage.getAllCollections().contains(collectionName + SHADOW))) {
         dataStorage.createCollection(collectionName + SHADOW);
      }
   }

   private int backUp(String collectionName, String documentId) {
      DataDocument document = dataStorage.readDocument(collectionName, documentId);
      createMetadata(document);
      createShadow(collectionName);
      dataStorage.createOldDocument(collectionName + SHADOW, document, documentId, getDocumentVersion(document));
      return getDocumentVersion(document);
   }

   /**
    * Create new version from input document. Read document from shadow collection
    * as old version and replace document in collection with document from shadow
    * collection.
    *
    * @param collectionName
    *       collection where document is stored
    * @param document
    *       document to be saved with newDocumentVersion
    * @param revertTo
    *       integer version to be reverted to
    */
   public void revertDocumentVersion(String collectionName, DataDocument document, int revertTo) {
      DataDocument newDocument = getOldDocumentVersion(collectionName, document, revertTo);
      int newVersion = newDocumentVersion(collectionName, document);
      newDocument.replace(VERSION_STRING, newVersion);
      dataStorage.updateDocument(collectionName, newDocument, newDocument.get(DOCUMENT_ID_STRING).toString());
   }

   /**
    * Parse id from document from input. Read document from shadow and
    * replace id with id specified in input document
    *
    * @param collectionName
    *       collection of document to be readed
    * @param document
    *       document containingg ID
    * @param version
    *       version to be reverted
    * @return
    */
   public DataDocument getOldDocumentVersion(String collectionName, DataDocument document, int version) {
      Object id = document.get(DOCUMENT_ID_STRING);
      DataDocument data = dataStorage.readOldDocument(collectionName + SHADOW, id.toString(), version);
      data.replace(DOCUMENT_ID_STRING, id);
      return data;
   }

   /**
    * Read document from shadow collection with specified id and version
    *
    * @param collectionName
    *       collection to read
    * @param documentId
    *       id of document
    * @param version
    *       version of document
    * @return
    */
   public DataDocument getOldDocumentVersion(String collectionName, String documentId, int version) {
      Object id = dataStorage.readDocument(collectionName, documentId).get(DOCUMENT_ID_STRING);
      DataDocument data = dataStorage.readOldDocument(collectionName + SHADOW, id.toString(), version);
      data.replace(DOCUMENT_ID_STRING, id);
      return data;
   }

   /**
    * Read all version from shadow collection and normal collection,
    * return it as list.
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @return
    */
   public List<DataDocument> getDocumentVersions(String collectionName, String documentId) {
      //to be done by JSON
      //JUST FOR TESTS
      List<DataDocument> dataDocuments = new ArrayList<DataDocument>();
      dataDocuments.add(dataStorage.readDocument(collectionName, documentId));
      for (int i = getDocumentVersion(dataDocuments.get(0)); i >= 0; i--) {
         DataDocument data = dataStorage.readOldDocument(collectionName + SHADOW, documentId, i);
         if (data != null) {
            dataDocuments.add(data);
         }
      }
      return dataDocuments;
   }
}
