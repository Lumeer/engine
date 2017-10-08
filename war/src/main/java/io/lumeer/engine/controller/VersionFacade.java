/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.controller;

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
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
    * @param collectionCode
    *       collection name in which document is stored
    * @param documentId
    *       the id of readed document
    * @return version of readed document as integer
    * @throws DocumentNotFoundException
    *       if document is not found in database
    * @throws CollectionNotFoundException
    *       if collection is not found
    */
   public int getDocumentVersion(String collectionCode, String documentId) throws DocumentNotFoundException, CollectionNotFoundException {
      if (!dataStorage.hasCollection(collectionCode)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionCode));
      }

      DataDocument document = dataStorage.readDocument(collectionCode, dataStorageDialect.documentIdFilter(documentId));
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
    * @param collectionCode
    *       name of the collection to move
    */
   public void trashShadowCollection(final String collectionCode) {
      String shadowCollectionName = buildShadowCollectionName(collectionCode);
      String trashShadowCollectionName = buildTrashShadowCollectionName(shadowCollectionName);
      dataStorage.renameCollection(shadowCollectionName, trashShadowCollectionName);
   }

   /**
    * Restore shadow collection frm trash
    *
    * @param collectionCode
    *       name of the collection to move
    */
   public void restoreShadowCollection(final String collectionCode) {
      String shadowCollectionName = buildShadowCollectionName(collectionCode);
      String trashShadowCollectionName = buildTrashShadowCollectionName(shadowCollectionName);
      dataStorage.renameCollection(trashShadowCollectionName, shadowCollectionName);
   }

   /**
    * Create shadow collection if not created. Backup document with
    * same id as document in collection. Then replace document in
    * collection with document from input. This method is atomic.
    * As lock there is document in shadow collection.
    *
    * @param collectionCode
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
   public int newDocumentVersion(String collectionCode, DataDocument actualDocument, DataDocument newDocument, boolean replace) throws VersionUpdateConflictException, AttributeNotFoundException {
      String id = actualDocument.getId();
      if (id == null) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.idNotFoundString());
      }
      createMetadata(newDocument);

      int oldVersion = backUpDocument(collectionCode, actualDocument);
      int newVersion = oldVersion + 1;

      newDocument.replace(LumeerConst.Document.METADATA_VERSION_KEY, newVersion);

      if (replace) {
         dataStorage.replaceDocument(collectionCode, newDocument, dataStorageDialect.documentIdFilter(id));
      } else {
         dataStorage.updateDocument(collectionCode, newDocument, dataStorageDialect.documentIdFilter(id));
      }

      return newVersion;
   }

   /**
    * Create shadow collection if not created. Backup document with
    * same id as document in collection. Then replace document in
    * collection with document from input. This method is atomic.
    * As lock there is document in shadow collection.
    *
    * @param collectionCode
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
   public int dropDocumentAttribute(String collectionCode, DataDocument actualDocument, String attributeName) throws AttributeNotFoundException, VersionUpdateConflictException {
      String id = actualDocument.getId();
      if (id == null) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.idNotFoundString());
      }

      int oldVersion = backUpDocument(collectionCode, actualDocument);
      int newVersion = oldVersion + 1;

      final DataFilter documentIdFilter = dataStorageDialect.documentIdFilter(id);
      dataStorage.dropAttribute(collectionCode, documentIdFilter, attributeName);
      dataStorage.incrementAttributeValueBy(collectionCode, documentIdFilter, LumeerConst.Document.METADATA_VERSION_KEY, 1);

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
    * @param collectionCode
    *       collection name to imput.
    */
   private void createShadowCollection(String collectionCode) {
      if (!dataStorage.hasCollection(buildShadowCollectionName(collectionCode))) {
         dataStorage.createCollection(buildShadowCollectionName(collectionCode));
      }
   }

   /**
    * Create in shadow collection backup of document from input
    *
    * @param collectionCode
    *       collection where document is stored
    * @param document
    *       document to back up
    * @return return version of document stored in shadow
    * @throws VersionUpdateConflictException
    *       throws if document is already in shadow collection
    */
   public int backUpDocument(String collectionCode, DataDocument document) throws VersionUpdateConflictException {
      createMetadata(document);
      createShadowCollection(collectionCode);

      try {
         dataStorage.createOldDocument(buildShadowCollectionName(collectionCode), document, document.getId(), getDocumentVersion(document));
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
    * @param collectionCode
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
   public void revertDocumentVersion(String collectionCode, DataDocument actualDocument, DataDocument newDocument) throws VersionUpdateConflictException, AttributeNotFoundException {
      String id = actualDocument.getId();
      if (id == null) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.idNotFoundString());
      }

      int oldVersion = backUpDocument(collectionCode, actualDocument);
      int newVersion = oldVersion + 1;

      newDocument.replace(LumeerConst.Document.METADATA_VERSION_KEY, newVersion);
      dataStorage.replaceDocument(collectionCode, newDocument, dataStorageDialect.documentIdFilter(id));
   }

   /**
    * Parse id from document from input. Read document from shadow and
    * replace id with id specified in input document.
    *
    * @param collectionCode
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
   public DataDocument readOldDocumentVersion(String collectionCode, DataDocument document, int version) throws DocumentNotFoundException, AttributeNotFoundException {
      String id = document.getId();
      if (id == null) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.idNotFoundString());
      }
      return readOldDocumentVersion(collectionCode, id, version);
   }

   /**
    * Read document from shadow collection with specified id and version.
    *
    * @param collectionCode
    *       collection to read
    * @param documentId
    *       id of document
    * @param version
    *       version of document
    * @return document from shadow collection
    * @throws DocumentNotFoundException
    *       if document cannot be found
    */
   public DataDocument readOldDocumentVersion(String collectionCode, String documentId, int version) throws DocumentNotFoundException {
      final DataDocument data = dataStorage.readDocument(buildShadowCollectionName(collectionCode), dataStorageDialect.documentNestedIdFilterWithVersion(documentId, version));
      if (data == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return data;
   }

   /**
    * Read all version from shadow collection and normal collection,
    * return it as list.
    *
    * @param collectionCode
    *       collection where document is stored
    * @param documentId
    *       id of document
    * @return list of documents from shadow with same id
    * @throws CollectionNotFoundException
    *       if collection does not exists
    */
   public List<DataDocument> getDocumentVersions(String collectionCode, String documentId) throws CollectionNotFoundException {
      final DataFilter filter = dataStorageDialect.documentNestedIdFilter(documentId);
      List<DataDocument> dataDocuments = dataStorage.search(buildShadowCollectionName(collectionCode), filter, null, 0, 100);

      DataDocument main = dataStorage.readDocument(collectionCode, dataStorageDialect.documentIdFilter(documentId));
      dataDocuments.add(main);

      return dataDocuments;
   }

   public void putInitDocumentVersionInternally(DataDocument dataDocument) {
      dataDocument.put(LumeerConst.Document.METADATA_VERSION_KEY, 0);
   }

   public String buildShadowCollectionName(String collectionCode) {
      return LumeerConst.Collection.COLLECTION_SHADOW_PREFFIX + "_" + collectionCode;
   }

   private String buildTrashShadowCollectionName(String shadowCollectionName) {
      return LumeerConst.Collection.COLLECTION_TRASH_PREFFIX + "_" + shadowCollectionName;
   }
}
