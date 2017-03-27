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

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.VersionUpdateConflictException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Provides document versioning capabilities.
 */
@SessionScoped
public class VersionFacade implements Serializable {

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

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

      DataDocument document = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
      if (document == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }

      return getDocumentVersion(document);
   }

   /**
    * Return document version.
    *
    * @param document
    *       document where this id is stored
    * @return integer, document version
    */
   public int getDocumentVersion(DataDocument document) {
      return document.getInteger(LumeerConst.Document.METADATA_VERSION_KEY, 0);
   }

   /**
    * Move shadow collection to trash
    *
    * @param collectionName
    *       name of the collection to move
    */
   public void trashShadowCollection(final String collectionName) {
      String shadowCollectionName = buildShadowCollectionName(collectionName);
      String trashShadowCollectionName = buildTrashShadowCollectionName(shadowCollectionName);
      dataStorage.renameCollection(shadowCollectionName, trashShadowCollectionName);
   }

   /**
    * Restore shadow collection frm trash
    *
    * @param collectionName
    *       name of the collection to move
    */
   public void restoreShadowCollection(final String collectionName) {
      String shadowCollectionName = buildShadowCollectionName(collectionName);
      String trashShadowCollectionName = buildTrashShadowCollectionName(shadowCollectionName);
      dataStorage.renameCollection(trashShadowCollectionName, shadowCollectionName);
   }

   /**
    * Create shadow collection if not created. Backup document with
    * same id as document in collection. Then replace document in
    * collection with document from input. This method is atomic.
    * As lock there is document in shadow collection.
    *
    * @param collectionName
    *       collection name, where document is stored
    * @param newDocument
    *       document, which will be stored in SHADOW collection
    *       and then updated in new collection to new version.
    *       After that it is possible to change data and save it
    * @param actualDocument
    *       existing document
    * @param replace
    *       whether perform replace or update
    * @return integer, new version of document
    * @throws VersionUpdateConflictException
    *       if there are two updatest at same time, means if in
    *       shadow collection already exists document with same version
    * @throws AttributeNotFoundException
    *       if document doesnt containst id
    */
   public int newDocumentVersion(String collectionName, DataDocument actualDocument, DataDocument newDocument, boolean replace) throws VersionUpdateConflictException, AttributeNotFoundException {
      String id = actualDocument.getId();
      if (id == null) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.idNotFoundString());
      }
      createMetadata(newDocument);

      int oldVersion = backUpDocument(collectionName, actualDocument);
      int newVersion = oldVersion + 1;

      newDocument.replace(LumeerConst.Document.METADATA_VERSION_KEY, newVersion);

      if (replace) {
         dataStorage.replaceDocument(collectionName, newDocument, dataStorageDialect.documentIdFilter(id));
      } else {
         dataStorage.updateDocument(collectionName, newDocument, dataStorageDialect.documentIdFilter(id));
      }

      return newVersion;
   }

   /**
    * Create shadow collection if not created. Backup document with
    * same id as document in collection. Then replace document in
    * collection with document from input. This method is atomic.
    * As lock there is document in shadow collection.
    *
    * @param collectionName
    *       collection name, where document is stored
    * @param actualDocument
    *       document, which will be stored in SHADOW collection
    * @param attributeName
    *       name of attribute to drop
    * @return integer, new version of document
    * @throws VersionUpdateConflictException
    *       if there are two updatest at same time, means if in
    *       shadow collection already exists document with same version
    * @throws AttributeNotFoundException
    *       if document doesnt containst id
    */
   public int dropDocumentAttribute(String collectionName, DataDocument actualDocument, String attributeName) throws AttributeNotFoundException, VersionUpdateConflictException {
      String id = actualDocument.getId();
      if (id == null) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.idNotFoundString());
      }

      int oldVersion = backUpDocument(collectionName, actualDocument);
      int newVersion = oldVersion + 1;

      String documentIdFilter = dataStorageDialect.documentIdFilter(id);
      dataStorage.dropAttribute(collectionName, documentIdFilter, attributeName);
      dataStorage.incrementAttributeValueBy(collectionName, documentIdFilter, LumeerConst.Document.METADATA_VERSION_KEY, 1);

      return newVersion;
   }

   /**
    * Create metadata if not exists.
    *
    * @param document
    *       document where to create metadata
    */
   private void createMetadata(DataDocument document) {
      document.putIfAbsent(LumeerConst.Document.METADATA_VERSION_KEY, 0);
   }

   /**
    * Create shadow collection if not exists.
    *
    * @param collectionName
    *       collection name to imput.
    */
   private void createShadowCollection(String collectionName) {
      if (!dataStorage.hasCollection(buildShadowCollectionName(collectionName))) {
         dataStorage.createCollection(buildShadowCollectionName(collectionName));
      }
   }

   /**
    * Create in shadow collection backup of document from input
    *
    * @param collectionName
    *       collection where document is stored
    * @param document
    *       document to back up
    * @return return version of document stored in shadow
    * @throws VersionUpdateConflictException
    *       throws if document is already in shadow collection
    */
   public int backUpDocument(String collectionName, DataDocument document) throws VersionUpdateConflictException {
      createMetadata(document);
      createShadowCollection(collectionName);

      try {
         dataStorage.createOldDocument(buildShadowCollectionName(collectionName), document, document.getId(), getDocumentVersion(document));
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
    * @param actualDocument
    *       document to be saved with newDocumentVersion
    * @param newDocument
    *       document to be reverted to
    * @throws VersionUpdateConflictException
    *       if there already exist document in shadows collection
    *       with same id
    * @throws AttributeNotFoundException
    *       if document does not contains id
    */
   public void revertDocumentVersion(String collectionName, DataDocument actualDocument, DataDocument newDocument) throws VersionUpdateConflictException, AttributeNotFoundException {
      String id = actualDocument.getId();
      if (id == null) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.idNotFoundString());
      }

      int oldVersion = backUpDocument(collectionName, actualDocument);
      int newVersion = oldVersion + 1;

      newDocument.replace(LumeerConst.Document.METADATA_VERSION_KEY, newVersion);
      dataStorage.replaceDocument(collectionName, newDocument, dataStorageDialect.documentIdFilter(id));
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
    * @throws AttributeNotFoundException
    *       if document does not contains id
    */
   public DataDocument readOldDocumentVersion(String collectionName, DataDocument document, int version) throws DocumentNotFoundException, AttributeNotFoundException {
      String id = document.getId();
      if (id == null) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.idNotFoundString());
      }
      return readOldDocumentVersion(collectionName, id, version);
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
    */
   public DataDocument readOldDocumentVersion(String collectionName, String documentId, int version) throws DocumentNotFoundException {
      DataDocument data = dataStorage.readDocument(buildShadowCollectionName(collectionName), dataStorageDialect.documentNestedIdFilterWithVersion(documentId, version));
      if (data == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
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
    * @return list of documents from shadow with same id
    * @throws CollectionNotFoundException
    *       if collection does not exists
    */
   public List<DataDocument> getDocumentVersions(String collectionName, String documentId) throws CollectionNotFoundException {
      String filter = dataStorageDialect.documentNestedIdFilter(documentId);
      List<DataDocument> dataDocuments = dataStorage.search(buildShadowCollectionName(collectionName), filter, null, 0, 100);

      DataDocument main = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
      dataDocuments.add(main);

      return dataDocuments;
   }

   public void putInitDocumentVersionInternally(DataDocument dataDocument) {
      dataDocument.put(LumeerConst.Document.METADATA_VERSION_KEY, 0);
   }

   public String buildShadowCollectionName(String collectionName) {
      return LumeerConst.Collection.COLLECTION_SHADOW_PREFFIX + "_" + collectionName;
   }

   private String buildTrashShadowCollectionName(String shadowCollectionName) {
      return LumeerConst.Collection.COLLECTION_TRASH_PREFFIX + "_" + shadowCollectionName;
   }
}
