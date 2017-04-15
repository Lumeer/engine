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
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.event.DropDocument;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
@SessionScoped
public class DocumentFacade implements Serializable {

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private VersionFacade versionFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private Event<DropDocument> dropDocumentEvent;

   @Inject
   private UserFacade userFacade;

   /**
    * Creates and inserts a new document to specified collection and create collection if not exists
    *
    * @param collectionName
    *       the name of the collection where the document will be created
    * @param document
    *       the DataDocument object representing a document to be created
    * @return the id of the newly created document
    * @throws DbException
    *       When there is an error working with the database.
    */
   public String createDocument(final String collectionName, final DataDocument document) throws DbException {
      DataDocument documentCleaned = checkDocumentKeysValidity(document);
      // add metadata attributes
      documentMetadataFacade.putInitDocumentMetadataInternally(documentCleaned, userFacade.getUserEmail());
      versionFacade.putInitDocumentVersionInternally(documentCleaned);
      securityFacade.putFullRightsInternally(documentCleaned, userFacade.getUserEmail());

      String documentId = dataStorage.createDocument(collectionName, documentCleaned);
      if (documentId == null) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.createDocumentUnsuccesfulString());
      }

      addOrIncrementAttributes(collectionName, documentCleaned);

      collectionMetadataFacade.addRecentlyUsedDocumentId(collectionName, documentId);
      collectionMetadataFacade.setLastTimeUsedNow(collectionName);

      return documentId;
   }

   /**
    * Reads the specified document in given collection by its id.
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @return the DataDocument object representing the read document
    * @throws DbException
    *       When there is an error working with the database.
    */
   public DataDocument readDocument(final String collectionName, final String documentId) throws DbException {
      return dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
   }

   /**
    * Modifies an existing document in given collection by its id and create collection if not exists
    *
    * @param collectionName
    *       the name of the collection where the existing document is located
    * @param updatedDocument
    *       the DataDocument object representing a document with changes to update
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   public void updateDocument(final String collectionName, final DataDocument updatedDocument) throws DbException {
      DataDocument existingDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(updatedDocument.getId()));

      final DataDocument updateDocumentCleaned = cleanInvalidAttributes(updatedDocument);
      documentMetadataFacade.putUpdateDocumentMetadataInternally(updateDocumentCleaned, userFacade.getUserEmail());
      versionFacade.newDocumentVersion(collectionName, existingDocument, updateDocumentCleaned, false);

      Set<String> existingAttributes = getDocumentAttributes(existingDocument);
      Set<String> updateAttributes = getDocumentAttributes(updateDocumentCleaned);
      addOrIncrementAttributes(collectionName, updateAttributes, existingAttributes);

      collectionMetadataFacade.addRecentlyUsedDocumentId(collectionName, existingDocument.getId());
      collectionMetadataFacade.setLastTimeUsedNow(collectionName);
   }

   /**
    * Replace an existing document in given collection by its id and create collection if not exists
    *
    * @param collectionName
    *       the name of the collection where the existing document is located
    * @param replacedDocument
    *       the DataDocument object representing a replace document
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   public void replaceDocument(final String collectionName, final DataDocument replacedDocument) throws DbException {
      DataDocument existingDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(replacedDocument.getId()));
      final DataDocument replacedDocumentCleaned = cleanInvalidAttributes(replacedDocument);
      LumeerConst.Document.METADATA_KEYS.stream().filter(existingDocument::containsKey).forEach(metaKey -> replacedDocumentCleaned.put(metaKey, existingDocument.get(metaKey)));
      documentMetadataFacade.putUpdateDocumentMetadataInternally(replacedDocumentCleaned, userFacade.getUserEmail());
      versionFacade.newDocumentVersion(collectionName, existingDocument, replacedDocumentCleaned, true);

      Set<String> existingAttributes = getDocumentAttributes(existingDocument);
      Set<String> replacedAttributes = getDocumentAttributes(existingDocument);

      addOrIncrementAttributes(collectionName, replacedAttributes, existingAttributes);
      dropOrDecrementAttributes(collectionName, existingAttributes, replacedAttributes);

      collectionMetadataFacade.addRecentlyUsedDocumentId(collectionName, existingDocument.getId());
      collectionMetadataFacade.setLastTimeUsedNow(collectionName);
   }

   /**
    * Drops an existing document in given collection by its id.
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the document to drop
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropDocument(final String collectionName, final String documentId) throws DbException {
      final DataFilter documentIdFilter = dataStorageDialect.documentIdFilter(documentId);
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentIdFilter);

      versionFacade.backUpDocument(collectionName, dataDocument);
      dataStorage.dropDocument(collectionName, documentIdFilter);

      if (dataStorage.collectionHasDocument(collectionName, documentIdFilter)) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.dropDocumentUnsuccesfulString());
      } else {
         dropDocumentEvent.fire(new DropDocument(collectionName, dataDocument));
         dropOrDecrementAttributes(collectionName, dataDocument);
      }

      collectionMetadataFacade.removeRecentlyUsedDocumentId(collectionName, documentId);
      collectionMetadataFacade.setLastTimeUsedNow(collectionName);
   }

   /**
    * Reverts old version of document.
    *
    * @param collectionName
    *       the name of the collection
    * @param documentId
    *       id of document to revert
    * @param revertVersion
    *       old version to be reverted
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   public void revertDocument(final String collectionName, final String documentId, final int revertVersion) throws DbException {
      DataDocument existingDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));

      DataDocument revertDocument = versionFacade.readOldDocumentVersion(collectionName, documentId, revertVersion);
      DataDocument meta = filterAndRemoveMeta(revertDocument);
      revertDocument.putAll(meta);
      documentMetadataFacade.putUpdateDocumentMetadataInternally(revertDocument, userFacade.getUserEmail());

      versionFacade.revertDocumentVersion(collectionName, existingDocument, revertDocument);

      Set<String> existingAttributes = getDocumentAttributes(existingDocument);
      Set<String> revertedAttributes = getDocumentAttributes(existingDocument);

      addOrIncrementAttributes(collectionName, revertedAttributes, existingAttributes);
      dropOrDecrementAttributes(collectionName, existingAttributes, revertedAttributes);

      collectionMetadataFacade.addRecentlyUsedDocumentId(collectionName, documentId);
      collectionMetadataFacade.setLastTimeUsedNow(collectionName);
   }

   /**
    * Remove specific attribute of document
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the document
    * @param attributeName
    *       the name of attribute to drop
    * @throws DbException
    *       When there is an error working with the database.
    */
   public void dropAttribute(final String collectionName, final String documentId, final String attributeName) throws DbException {
      DataDocument existingDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));

      versionFacade.dropDocumentAttribute(collectionName, existingDocument, attributeName);
      collectionMetadataFacade.dropOrDecrementAttribute(collectionName, attributeName);

      collectionMetadataFacade.addRecentlyUsedDocumentId(collectionName, documentId);
      collectionMetadataFacade.setLastTimeUsedNow(collectionName);
   }

   /**
    * Read all non-metadata document attributes
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the document to drop
    * @return set containing document attributes
    * @throws DbException
    *       When there is an error working with the database.
    */
   public Set<String> getDocumentAttributes(final String collectionName, final String documentId) throws DbException {
      DataDocument dataDocument = dataStorage.readDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
      return dataDocument != null ? getDocumentAttributes(dataDocument) : null;
   }

   private Set<String> getDocumentAttributes(DataDocument dataDocument) {
      Set<String> attrs = new HashSet<>();
      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         String attributeName = entry.getKey().trim();
         if (attributeName.startsWith(LumeerConst.Document.METADATA_PREFIX)) {
            continue;
         }
         attrs.add(attributeName);
         if (isDataDocument(entry.getValue())) {
            // starts recursion
            attrs.addAll(getDocumentAttributes((DataDocument) entry.getValue(), attributeName + "."));
         }
      }
      return attrs;
   }

   private Set<String> getDocumentAttributes(DataDocument dataDocument, String prefix) {
      Set<String> attrs = new HashSet<>();
      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         String attributeName = prefix + entry.getKey().trim();
         attrs.add(attributeName);
         if (isDataDocument(entry.getValue())) {
            attrs.addAll(getDocumentAttributes((DataDocument) entry.getValue(), attributeName + "."));
         }
      }
      return attrs;
   }

   private void addOrIncrementAttributes(final String collectionName, DataDocument doc) {
      // we add all document attributes to collection metadata
      getDocumentAttributes(doc).forEach(attribute -> {
         collectionMetadataFacade.addOrIncrementAttribute(collectionName, attribute);
      });
   }

   private void addOrIncrementAttributes(final String collectionName, Set<String> attributes, Set<String> filter) {
      attributes.stream().filter(attribute -> !filter.contains(attribute)).forEach(attribute -> {
         collectionMetadataFacade.addOrIncrementAttribute(collectionName, attribute);
      });
   }

   private void dropOrDecrementAttributes(final String collectionName, DataDocument doc) {
      // we add all document attributes to collection metadata
      getDocumentAttributes(doc).forEach(attribute -> {
         collectionMetadataFacade.dropOrDecrementAttribute(collectionName, attribute);
      });
   }

   private void dropOrDecrementAttributes(final String collectionName, Set<String> attributes, Set<String> filter) {
      attributes.stream().filter(attribute -> !filter.contains(attribute)).forEach(attribute -> {
         collectionMetadataFacade.dropOrDecrementAttribute(collectionName, attribute);
      });
   }

   private DataDocument checkDocumentKeysValidity(DataDocument dataDocument) throws InvalidDocumentKeyException {
      DataDocument ndd = new DataDocument();
      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         String attributeName = entry.getKey().trim();
         if (!Utils.isAttributeNameValid(attributeName)) {
            throw new InvalidDocumentKeyException(ErrorMessageBuilder.invalidDocumentKeyString(attributeName));
         }
         Object value = entry.getValue();
         if (isDataDocument(value)) {
            ndd.put(attributeName, checkDocumentKeysValidity((DataDocument) value));
         } else if (isList(value)) {
            List l = (List) entry.getValue();
            if (!l.isEmpty() && isDataDocument(l.get(0))) {
               ArrayList<DataDocument> docs = new ArrayList<>();
               ndd.put(attributeName, docs);
               for (Object o : l) {
                  docs.add(checkDocumentKeysValidity((DataDocument) o));
               }
            } else {
               ndd.put(attributeName, l);
            }
         } else {
            ndd.put(attributeName, value);
         }
      }
      return ndd;
   }

   private DataDocument cleanInvalidAttributes(final DataDocument dataDocument) {
      final DataDocument ndd = new DataDocument();

      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         final String attributeName = entry.getKey().trim();
         if (Utils.isAttributeNameValid(attributeName)) {
            final Object value = entry.getValue();

            if (isDataDocument(value)) {
               ndd.put(attributeName, cleanInvalidAttributes((DataDocument) value));
            } else if (isList(value)) {
               List l = (List) entry.getValue();
               if (!l.isEmpty() && isDataDocument(l.get(0))) {
                  ArrayList<DataDocument> docs = new ArrayList<>();
                  ndd.put(attributeName, docs);
                  for (Object o : l) {
                     docs.add(cleanInvalidAttributes((DataDocument) o));
                  }
               } else {
                  ndd.put(attributeName, l);
               }
            } else {
               ndd.put(attributeName, value);
            }
         }
      }
      return ndd;
   }

   private DataDocument filterAndRemoveMeta(final DataDocument dataDocument) {
      DataDocument meta = new DataDocument();
      LumeerConst.Document.METADATA_KEYS.stream().filter(dataDocument::containsKey).forEach(metaKey -> {
         meta.put(metaKey, dataDocument.get(metaKey));
         dataDocument.remove(metaKey);
      });
      return meta;
   }

   private static boolean isDataDocument(Object obj) {
      return obj != null && obj instanceof DataDocument;
   }

   private static boolean isList(Object obj) {
      return obj != null && obj instanceof List;
   }

}
