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
import io.lumeer.engine.exception.DocumentNotFoundException;
import io.lumeer.engine.exception.UnsuccessfulOperationException;
import io.lumeer.engine.exception.VersionUpdateConflictException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import com.mongodb.MongoWriteException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
public class VersionFacade implements Serializable {
   private final String SHADOW = ".shadow";
   private final String VERSION_STRING = "_metadata-version"; //use from utils
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
    * @throws DocumentNotFoundException
    *       if document is not found in database
    */
   public int getDocumentVersion(String collectionName, String documentId) throws DocumentNotFoundException {
      DataDocument dat = dataStorage.readDocument(collectionName, documentId);
      if (dat == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return getDocumentVersion(dat);
   }

   /**
    * Return document version
    *
    * @param document
    *       document where this id is stored
    * @return integer, document version
    */
   public int getDocumentVersion(DataDocument document) {
      if (document.getInteger(VERSION_STRING) == null) {
         return 0;
      }
      return document.getInteger(VERSION_STRING);
   }

   /**
    * Create shadow collection if not created. Backup document with
    * same id as document in collection. Then replace document in
    * collection with document from input. This method is atomic.
    * As lock there is document in shadow collection.
    *
    * @param collectionName
    *       collection name, where document is stored
    * @param document
    *       document, which will be stored in SHADOW collection
    *       and then updated in new collection to new version.
    *       After that it is possible to change data and save it
    * @return integer, new version of document
    * @throws DocumentNotFoundException
    *       if document not found in database while backup
    *       was done
    * @throws UnsuccessfulOperationException
    *       if documment cannot be updated, bud was
    *       backuped in shadow collection
    */
   public int newDocumentVersion(String collectionName, DataDocument document) throws DocumentNotFoundException, UnsuccessfulOperationException, VersionUpdateConflictException {
      Object id = document.get(DOCUMENT_ID_STRING);
      int oldVersion = backUp(collectionName, document.get(DOCUMENT_ID_STRING).toString());
      createMetadata(document);
      document.replace(VERSION_STRING, oldVersion + 1);
      dataStorage.updateDocument(collectionName, document, document.get(DOCUMENT_ID_STRING).toString(), -1);
      document.put(DOCUMENT_ID_STRING, id);
      if (!document.equals(dataStorage.readDocument(collectionName, id.toString()))) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.updateDocumentUnsuccesfulString());
      }
      ;
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

   private int backUp(String collectionName, String documentId) throws DocumentNotFoundException, VersionUpdateConflictException {
      DataDocument document = dataStorage.readDocument(collectionName, documentId);
      if (document == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      createMetadata(document);
      createShadow(collectionName);
      try {
         dataStorage.createOldDocument(collectionName + SHADOW, document, documentId, getDocumentVersion(document));
      } catch (Exception e) {
         throw new VersionUpdateConflictException(e.getMessage(), e.getCause());
      }
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
    * @throws DocumentNotFoundException
    *       if input document not found in database
    * @throws UnsuccessfulOperationException
    *       if document cannot be updated
    */
   public void revertDocumentVersion(String collectionName, DataDocument document, int revertTo) throws DocumentNotFoundException, UnsuccessfulOperationException, VersionUpdateConflictException {
      Object id = document.get(DOCUMENT_ID_STRING);
      DataDocument newDocument = getOldDocumentVersion(collectionName, document, revertTo);
      int newVersion = newDocumentVersion(collectionName, document);
      newDocument.replace(VERSION_STRING, newVersion);
      Object idN = newDocument.get(DOCUMENT_ID_STRING);
      dataStorage.updateDocument(collectionName, newDocument, idN.toString(), -1);
      newDocument.put(DOCUMENT_ID_STRING, idN);
      if (!newDocument.equals(dataStorage.readDocument(collectionName, newDocument.get(DOCUMENT_ID_STRING).toString()))) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.updateDocumentUnsuccesfulString());
      }
      ;
      document.put(DOCUMENT_ID_STRING, id);
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
    * @return document from shadow with changed id
    * @throws DocumentNotFoundException
    *       if document cannot be found
    */
   public DataDocument getOldDocumentVersion(String collectionName, DataDocument document, int version) throws DocumentNotFoundException {
      Object id = document.get(DOCUMENT_ID_STRING);
      DataDocument data = dataStorage.readOldDocument(collectionName + SHADOW, id.toString(), version);
      if (data == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
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
    * @return document from shadow collection
    * @throws DocumentNotFoundException
    *       if document cannot be found
    */
   public DataDocument getOldDocumentVersion(String collectionName, String documentId, int version) throws DocumentNotFoundException {
      Object id = dataStorage.readDocument(collectionName, documentId).get(DOCUMENT_ID_STRING);
      if (id == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      DataDocument data = dataStorage.readOldDocument(collectionName + SHADOW, id.toString(), version);
      if (data == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
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
    * @return lis of documents from shadow with same id
    */
   public List<DataDocument> getDocumentVersions(String collectionName, String documentId) {
      List<DataDocument> dataDocuments = new ArrayList<DataDocument>();
      StringBuilder sb = new StringBuilder("{\"")
            .append("_id._id")
            .append("\" : ")
            .append("ObjectId(\"")
            .append(documentId)
            .append("\")}");
      String filter = sb.toString();
      dataDocuments = dataStorage.search(collectionName + SHADOW,
            filter
            , null, 0, Integer.MAX_VALUE);
      DataDocument main = dataStorage.readDocument(collectionName, documentId);
      dataDocuments.add(main);
      return dataDocuments;
   }
}
