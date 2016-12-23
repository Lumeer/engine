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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.engine.api.exception.VersionUpdateConflictException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.mongodb.MongoUtils;

import com.mongodb.client.model.Filters;

import org.bson.types.ObjectId;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
@SessionScoped
public class VersionFacade implements Serializable {

   private final String SHADOW = ".shadow";

   public static final String METADATA_ID_KEY = "_id";

   @Inject
   private DataStorage dataStorage;

   public String getVersionMetadataString() {
      return LumeerConst.METADATA_VERSION_KEY;
   }

   /**
    * Return document version.
    *
    * @param collectionName
    *       collection name in which document is stored
    * @param documentId
    *       the id of readed document
    * @return version of readed document as integer
    * @throws DocumentNotFoundException
    *       if document is not found in database
    * @throws CollectionNotFoundException
    *       if collection is not found
    */
   public int getDocumentVersion(String collectionName, String documentId) throws DocumentNotFoundException, CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument dat = dataStorage.readDocument(collectionName, documentId);
      if (dat == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return getDocumentVersion(dat);
   }

   /**
    * Return document version.
    *
    * @param document
    *       document where this id is stored
    * @return integer, document version
    */
   public int getDocumentVersion(DataDocument document) {
      if (document.getInteger(LumeerConst.METADATA_VERSION_KEY) == null) {
         return 0;
      }
      return document.getInteger(LumeerConst.METADATA_VERSION_KEY);
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
    * @throws VersionUpdateConflictException
    *       if there are two updatest at same time, means if in
    *       shadow collection already exists document with same version
    * @throws InvalidDocumentKeyException
    *       if document doesnt containst id
    * @throws CollectionNotFoundException
    *       if collection not found
    */
   public int newDocumentVersion(String collectionName, DataDocument document) throws DocumentNotFoundException, UnsuccessfulOperationException, VersionUpdateConflictException, InvalidDocumentKeyException, CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      String id = document.getId();
      if (id == null) {
         throw new InvalidDocumentKeyException("no document id");
      }
      createMetadata(document);
      int oldVersion = backUp(collectionName, id);
      document.replace(LumeerConst.METADATA_VERSION_KEY, oldVersion + 1);
      dataStorage.updateDocument(collectionName, document, document.getId());
      DataDocument readed = dataStorage.readDocument(collectionName, id);
      if (!readed.keySet().containsAll(document.keySet())) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.updateDocumentUnsuccesfulString());
      }
      return oldVersion + 1;
   }

   /**
    * Create metadata if not exists.
    *
    * @param document
    *       document where to create metadata
    */
   private void createMetadata(DataDocument document) {
      if (!document.containsKey(LumeerConst.METADATA_VERSION_KEY)) {
         document.put(LumeerConst.METADATA_VERSION_KEY, 0);
      }
   }

   /**
    * Create shadow collection if not exists.
    *
    * @param collectionName
    *       collection name to imput.
    */
   private void createShadow(String collectionName) {
      if (!(dataStorage.hasCollection(collectionName + SHADOW))) {
         dataStorage.createCollection(collectionName + SHADOW);
      }
   }

   /**
    * Create in shadow collection backup of document from input
    *
    * @param collectionName
    *       collection where document is stored
    * @param documentId
    *       document id
    * @return return version of document stored in shadow
    * @throws DocumentNotFoundException
    *       throws if document not found in collection (collecitonName)
    * @throws VersionUpdateConflictException
    *       throws if document is already in shadow collection
    * @throws CollectionNotFoundException
    *       throws if collection not found
    */
   public int backUp(String collectionName, String documentId) throws DocumentNotFoundException, VersionUpdateConflictException, CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
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
    * @throws VersionUpdateConflictException
    *       if there already exist document in shadows collection
    *       with same id
    * @throws InvalidDocumentKeyException
    *       if document does not contains id
    * @throws CollectionNotFoundException
    *       if coolection Not found
    */
   public void revertDocumentVersion(String collectionName, DataDocument document, int revertTo) throws DocumentNotFoundException, UnsuccessfulOperationException, VersionUpdateConflictException, InvalidDocumentKeyException, CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      String id = document.getId();
      if (id == null) {
         throw new InvalidDocumentKeyException("no document id");
      }
      DataDocument newDocument = readOldDocumentVersion(collectionName, document, revertTo);
      int newVersion = newDocumentVersion(collectionName, document);
      newDocument.replace(LumeerConst.METADATA_VERSION_KEY, newVersion);
      String idN = newDocument.getId();
      dataStorage.updateDocument(collectionName, newDocument, idN);
      newDocument.put(METADATA_ID_KEY, idN);
      DataDocument readed = dataStorage.readDocument(collectionName, newDocument.getId());
      if (!readed.keySet().containsAll(newDocument.keySet())) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.updateDocumentUnsuccesfulString());
      }
      document.setId(id);
   }

   /**
    * Parse id from document from input. Read document from shadow and
    * replace id with id specified in input document.
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
    * @throws InvalidDocumentKeyException
    *       if document does not contains id
    * @throws CollectionNotFoundException
    *       if collection not found
    */
   public DataDocument readOldDocumentVersion(String collectionName, DataDocument document, int version) throws DocumentNotFoundException, InvalidDocumentKeyException, CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      String id = document.getId();
      if (id == null) {
         throw new InvalidDocumentKeyException("no document id");
      }
      DataDocument data = dataStorage.readOldDocument(collectionName + SHADOW, id, version);
      if (data == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      data.setId(id);
      return data;
   }

   /**
    * Read document from shadow collection with specified id and version.
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
    * @throws CollectionNotFoundException
    *       if collection not found
    */
   public DataDocument readOldDocumentVersion(String collectionName, String documentId, int version) throws DocumentNotFoundException, CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }

      DataDocument data = dataStorage.readOldDocument(collectionName + SHADOW, documentId, version);
      if (data == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      data.setId(documentId);
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
    * @throws CollectionNotFoundException
    *       if collection does not exists
    */
   public List<DataDocument> getDocumentVersions(String collectionName, String documentId) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      List<DataDocument> dataDocuments;
      dataDocuments = dataStorage.search(collectionName + SHADOW,
            MongoUtils.convertBsonToJson(Filters.eq("_id._id", new ObjectId(documentId)))
            , null, 0, 100);
      DataDocument main = dataStorage.readDocument(collectionName, documentId);
      dataDocuments.add(main);
      return dataDocuments;
   }

   /**
    * Add .delete tag to shadow collection
    *
    * @param collectionName
    *       collection to delete
    */
   public void deleteVersionCollection(String collectionName) {
      StringBuilder sb = new StringBuilder("{ renameCollection: \"")
            .append(collectionName + SHADOW)
            .append("\", to: \"")
            .append(collectionName + SHADOW + ".delete")
            .append("\"}");
      dataStorage.run(sb.toString());
   }
}
