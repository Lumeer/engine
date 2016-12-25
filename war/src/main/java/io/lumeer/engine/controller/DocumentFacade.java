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
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.event.DropDocument;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;
import io.lumeer.mongodb.MongoUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
@SessionScoped
public class DocumentFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

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
    * @throws InvalidConstraintException
    *       if one of document's value doesn't satisfy constraint or type
    */
   public String createDocument(final String collectionName, final DataDocument document) throws DbException, InvalidConstraintException {
      checkCollectionForWriteBenevolent(collectionName);
      DataDocument doc = checkDocumentKeysValidity(document);
      // check constraints
      checkConstraintsAndConvert(collectionName, doc);
      // add metadata attributes
      doc.put(LumeerConst.Document.CREATE_DATE_KEY, Utils.getCurrentTimeString());
      doc.put(LumeerConst.Document.CREATE_BY_USER_KEY, userFacade.getUserEmail());
      doc.put(LumeerConst.METADATA_VERSION_KEY, 0);
      doc.put(LumeerConst.Document.USER_RIGHTS, Collections.singletonList(new DataDocument(LumeerConst.Security.USER_ID, userFacade.getUserEmail()).append(LumeerConst.Security.RULE, LumeerConst.Security.WRITE + LumeerConst.Security.EXECUTE + LumeerConst.Security.READ)));

      String documentId = dataStorage.createDocument(collectionName, doc);
      if (documentId == null) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.createDocumentUnsuccesfulString());
      }
      // we add all document attributes to collection metadata
      doc.keySet().stream().filter(attribute -> !LumeerConst.Document.METADATA_KEYS.contains(attribute)).forEach(attribute -> {
         collectionMetadataFacade.addOrIncrementAttribute(collectionName, attribute);
      });
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
    * @throws UnauthorizedAccessException
    *       if user doesn't have rights to read document
    */
   public DataDocument readDocument(final String collectionName, final String documentId) throws DbException {
      checkCollectionForRead(collectionName);
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (!securityFacade.checkForRead(dataDocument, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
      LumeerConst.Document.PURGE_METADATA_KEYS.forEach(dataDocument::remove);
      return dataDocument;
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
    * @throws InvalidConstraintException
    *       if one of document's value doesn't satisfy constraint or type
    */
   public void updateDocument(final String collectionName, final DataDocument updatedDocument) throws DbException, InvalidConstraintException {
      checkCollectionForWriteBenevolent(collectionName);

      String documentId = updatedDocument.getId();
      DataDocument existingDocument = dataStorage.readDocument(collectionName, documentId);
      if (existingDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (!securityFacade.checkForWrite(existingDocument, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
      final DataDocument upd = cleanInvalidAttributes(updatedDocument);
      checkConstraintsAndConvert(collectionName, updatedDocument);
      upd.put(LumeerConst.Document.UPDATE_DATE_KEY, Utils.getCurrentTimeString());
      upd.put(LumeerConst.Document.UPDATED_BY_USER_KEY, userFacade.getUserEmail());
      versionFacade.newDocumentVersion(collectionName, upd);

      // we add new attributes of updated document to collection metadata
      upd.keySet().stream().filter(attribute -> !existingDocument.containsKey(attribute) && !LumeerConst.Document.METADATA_KEYS.contains(attribute)).forEach(attribute -> {
         collectionMetadataFacade.addOrIncrementAttribute(collectionName, attribute);
      });
   }

   /**
    * Replace an existing document in given collection by its id and create collection if not exists
    *
    * @param collectionName
    *       the name of the collection where the existing document is located
    * @param replaceDocument
    *       the DataDocument object representing a replace document
    * @throws DbException
    *       When there is an error working with the data storage.
    * @throws InvalidConstraintException
    *       if one of document's value doesn't satisfy constraint or type
    */
   public void replaceDocument(final String collectionName, final DataDocument replaceDocument) throws DbException, InvalidConstraintException {
      checkCollectionForWriteBenevolent(collectionName);

      String documentId = replaceDocument.getId();
      DataDocument existingDocument = dataStorage.readDocument(collectionName, documentId);
      if (existingDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (!securityFacade.checkForWrite(existingDocument, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
      final DataDocument repl = cleanInvalidAttributes(replaceDocument);
      checkConstraintsAndConvert(collectionName, replaceDocument);
      LumeerConst.Document.METADATA_KEYS.stream().filter(existingDocument::containsKey).forEach(metaKey -> {
         repl.put(metaKey, existingDocument.get(metaKey));
      });
      repl.put(LumeerConst.Document.UPDATE_DATE_KEY, Utils.getCurrentTimeString());
      repl.put(LumeerConst.Document.UPDATED_BY_USER_KEY, userFacade.getUserEmail());
      versionFacade.backUp(collectionName, documentId);
      dataStorage.replaceDocument(collectionName, repl, documentId);
      dataStorage.incrementAttributeValueBy(collectionName, documentId, LumeerConst.METADATA_VERSION_KEY, 1);

      // add new attributes of updated document to collection metadata
      repl.keySet().stream().filter(attribute -> !existingDocument.containsKey(attribute) && !LumeerConst.Document.METADATA_KEYS.contains(attribute)).forEach(attribute -> {
         collectionMetadataFacade.addOrIncrementAttribute(collectionName, attribute);
      });

      existingDocument.keySet().stream().filter(attribute -> !repl.containsKey(attribute) && !LumeerConst.Document.METADATA_KEYS.contains(attribute)).forEach(attribute -> {
         collectionMetadataFacade.dropOrDecrementAttribute(collectionName, attribute);
      });
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
      checkCollectionForWrite(collectionName);

      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (!securityFacade.checkForWrite(dataDocument, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }

      versionFacade.backUp(collectionName, documentId);
      dataStorage.dropDocument(collectionName, documentId);

      if (dataStorage.collectionHasDocument(collectionName, documentId)) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.dropDocumentUnsuccesfulString());
      } else {
         dropDocumentEvent.fire(new DropDocument(collectionName, dataDocument));
         // we drop all attributes of dropped document from collection metadata
         for (String attributeName : dataDocument.keySet()) {
            collectionMetadataFacade.dropOrDecrementAttribute(collectionName, attributeName);
         }
      }
   }

   /**
    * Revert old version of document
    *
    * @param collectionName
    *       the name of the collection
    * @param documentId
    *       id of document to revert
    * @param revertVersion
    *       old version to be reverted
    * @throws DbException
    *       When there is an error working with the data storage.
    * @throws InvalidConstraintException
    *       if one of document's value doesn't satisfy constraint or type
    */
   public void revertDocument(final String collectionName, final String documentId, final int revertVersion) throws DbException, InvalidConstraintException {
      checkCollectionForWrite(collectionName);

      DataDocument existingDocument = dataStorage.readDocument(collectionName, documentId);
      if (existingDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }

      DataDocument revertDocument = versionFacade.readOldDocumentVersion(collectionName, documentId, revertVersion);
      checkConstraintsAndConvert(collectionName, revertDocument);

      versionFacade.revertDocumentVersion(collectionName, existingDocument, revertVersion);

      // add new attributes of updated document to collection metadata
      revertDocument.keySet().stream().filter(attribute -> !existingDocument.containsKey(attribute) && !LumeerConst.Document.METADATA_KEYS.contains(attribute)).forEach(attribute -> {
         collectionMetadataFacade.addOrIncrementAttribute(collectionName, attribute);
      });

      existingDocument.keySet().stream().filter(attribute -> !revertDocument.containsKey(attribute) && !LumeerConst.Document.METADATA_KEYS.contains(attribute)).forEach(attribute -> {
         collectionMetadataFacade.dropOrDecrementAttribute(collectionName, attribute);
      });
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
      checkCollectionForWrite(collectionName);
      if (!dataStorage.collectionHasDocument(collectionName, documentId)) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (!securityFacade.checkForWrite(collectionName, documentId, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
      versionFacade.backUp(collectionName, documentId);
      dataStorage.dropAttribute(collectionName, documentId, attributeName);
      dataStorage.incrementAttributeValueBy(collectionName, documentId, LumeerConst.METADATA_VERSION_KEY, 1);
      collectionMetadataFacade.dropOrDecrementAttribute(collectionName, attributeName);
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
      checkCollectionForRead(collectionName);
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (!securityFacade.checkForRead(dataDocument, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
      Set<String> documentAttributes = new HashSet<>();
      // filter out metadata attributes
      documentAttributes.addAll(dataDocument.keySet().stream().filter(key -> !key.startsWith(LumeerConst.Document.METADATA_PREFIX)).collect(Collectors.toList()));
      return documentAttributes;
   }

   private void checkCollectionForRead(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!collectionMetadataFacade.checkCollectionForRead(collectionName, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
   }

   private void checkCollectionForWriteBenevolent(final String collectionName) throws UnauthorizedAccessException {
      if (!dataStorage.hasCollection(collectionName)) {
         return;
      }
      if (!collectionMetadataFacade.checkCollectionForWrite(collectionName, userFacade.getUserEmail())) {
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

   private void checkConstraintsAndConvert(final String collectionName, final DataDocument doc) throws InvalidConstraintException {
      for (String attribute : doc.keySet()) {
         Object value = collectionMetadataFacade.checkAndConvertAttributeValue(collectionName, attribute, doc.get(attribute).toString());
         if (value == null) {
            throw new InvalidConstraintException(ErrorMessageBuilder.invalidConstraintKey(attribute));
         } else {
            doc.replace(attribute, value);
         }
      }
   }

   private DataDocument checkDocumentKeysValidity(DataDocument dataDocument) throws InvalidDocumentKeyException {
      DataDocument ndd = new DataDocument();
      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         String attributeName = entry.getKey().trim();
         if (!Utils.isAttributeNameValid(attributeName)) {
            throw new InvalidDocumentKeyException(ErrorMessageBuilder.invalidDocumentKey(attributeName));
         }
         Object value = entry.getValue();
         if (MongoUtils.isDataDocument(value)) {
            ndd.put(attributeName, checkDocumentKeysValidity((DataDocument) value));
         } else if (MongoUtils.isList(value)) {
            List l = (List) entry.getValue();
            if (!l.isEmpty() && MongoUtils.isDataDocument(l.get(0))) {
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

            if (MongoUtils.isDataDocument(value)) {
               ndd.put(attributeName, cleanInvalidAttributes((DataDocument) value));
            } else if (MongoUtils.isList(value)) {
               List l = (List) entry.getValue();
               if (!l.isEmpty() && MongoUtils.isDataDocument(l.get(0))) {
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

}
