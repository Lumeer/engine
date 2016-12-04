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
import io.lumeer.engine.api.event.DropDocument;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.engine.api.exception.VersionUpdateConflictException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;
import io.lumeer.mongodb.MongoUtils;

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
   private DataStorage dataStorage;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private VersionFacade versionFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private Event<DropDocument> dropDocumentEvent;

   //@Inject
   private String userName = "testUser";

   /**
    * Creates and inserts a new document to specified collection.
    *
    * @param collectionName
    *       the name of the collection where the document will be created
    * @param document
    *       the DataDocument object representing a document to be created
    * @return the id of the newly created document
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws UnsuccessfulOperationException
    *       if create was not succesful
    * @throws InvalidDocumentKeyException
    *       if one of document's key contains illegal character
    */
   public String createDocument(final String collectionName, final DataDocument document) throws CollectionNotFoundException, UnsuccessfulOperationException, InvalidDocumentKeyException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument doc = checkDocumentKeysValidity(document);
      doc.put(LumeerConst.DOCUMENT.CREATE_DATE_KEY, Utils.getCurrentTimeString());
      doc.put(LumeerConst.DOCUMENT.CREATE_BY_USER_KEY, userName);
      doc.put(LumeerConst.METADATA_VERSION_KEY, 0);
      String documentId = dataStorage.createDocument(collectionName, doc);
      if (documentId == null) {
         throw new UnsuccessfulOperationException(ErrorMessageBuilder.createDocumentUnsuccesfulString());
      }
      // we add all document attributes to collection metadata
      for (String attribute : doc.keySet()) {
         collectionMetadataFacade.addOrIncrementAttribute(collectionName, attribute);
      }
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
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in database
    */
   public DataDocument readDocument(final String collectionName, final String documentId) throws CollectionNotFoundException, DocumentNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      for (String key : LumeerConst.DOCUMENT.METADATA_KEYS) {
         dataDocument.remove(key);
      }
      return dataDocument;
   }

   /**
    * Modifies an existing document in given collection by its id.
    *
    * @param collectionName
    *       the name of the collection where the existing document is located
    * @param updatedDocument
    *       the DataDocument object representing a document with changes to update
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in database
    * @throws UnsuccessfulOperationException
    *       if document was not updated succesfully
    * @throws InvalidDocumentKeyException
    *       if one of document's key contains illegal character
    */
   public void updateDocument(final String collectionName, final DataDocument updatedDocument) throws CollectionNotFoundException, DocumentNotFoundException, UnsuccessfulOperationException, VersionUpdateConflictException, InvalidDocumentKeyException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      String documentId = updatedDocument.getString("_id");
      DataDocument existingDocument = dataStorage.readDocument(collectionName, documentId);
      if (existingDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      DataDocument upd = checkDocumentKeysValidity(updatedDocument);
      upd.put(LumeerConst.DOCUMENT.UPDATE_DATE_KEY, Utils.getCurrentTimeString());
      upd.put(LumeerConst.DOCUMENT.UPDATED_BY_USER_KEY, userName);
      versionFacade.newDocumentVersion(collectionName, upd);

      // we add new attributes of updated document to collection metadata
      for (String attribute : upd.keySet()) {
         if (!existingDocument.containsKey(attribute)) {
            collectionMetadataFacade.addOrIncrementAttribute(collectionName, attribute);
         }
      }
   }

   /**
    * Drops an existing document in given collection by its id.
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the document to drop
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in database
    * @throws UnsuccessfulOperationException
    *       if document stay in collection after drop
    */
   public void dropDocument(final String collectionName, final String documentId) throws CollectionNotFoundException, DocumentNotFoundException, UnsuccessfulOperationException, VersionUpdateConflictException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      // TODO swap with backUp method
      versionFacade.newDocumentVersion(collectionName, dataDocument);
      dataStorage.dropDocument(collectionName, documentId);

      final DataDocument checkDataDocument = dataStorage.readDocument(collectionName, documentId);
      if (checkDataDocument != null) {
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
    * Remove specific attribute of document
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the document
    * @param attributeName
    *       the name of attribute to drop
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in database
    */
   public void dropAttribute(final String collectionName, final String documentId, final String attributeName) throws CollectionNotFoundException, DocumentNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!dataStorage.collectionHasDocument(collectionName, documentId)) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      dataStorage.dropAttribute(collectionName, documentId, attributeName);
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
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in database
    */
   public Set<String> getDocumentAttributes(final String collectionName, final String documentId) throws CollectionNotFoundException, DocumentNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument dataDocument = dataStorage.readDocument(collectionName, documentId);
      if (dataDocument == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      Set<String> documentAttributes = new HashSet<>();
      // filter out metadata attributes
      for (String key : dataDocument.keySet()) {
         if (!key.startsWith(LumeerConst.DOCUMENT.METADATA_PREFIX)) {
            documentAttributes.add(key);
         }
      }
      return documentAttributes;
   }

   private DataDocument checkDocumentKeysValidity(DataDocument dataDocument) throws InvalidDocumentKeyException {
      DataDocument ndd = new DataDocument();
      for (Map.Entry<String, Object> entry : dataDocument.entrySet()) {
         String key = entry.getKey().trim();
         if (isKeyInvalid(key)) {
            throw new InvalidDocumentKeyException(ErrorMessageBuilder.invalidDocumentKey(key));
         }
         Object value = entry.getValue();
         if (MongoUtils.isDataDocument(value)) {
            ndd.put(key, checkDocumentKeysValidity((DataDocument) value));
         } else if (MongoUtils.isList(value)) {
            List l = (List) entry.getValue();
            if (!l.isEmpty() && MongoUtils.isDataDocument(l.get(0))) {
               ArrayList<DataDocument> docs = new ArrayList<>();
               ndd.put(key, docs);
               for (Object o : l) {
                  docs.add(checkDocumentKeysValidity((DataDocument) o));
               }
            } else {
               ndd.put(key, l);
            }
         } else {
            ndd.put(key, value);
         }
      }
      return ndd;
   }

   private boolean isKeyInvalid(String key) {
      return !key.equals("_id") && (key.startsWith("$") || key.startsWith("_") || key.contains("."));
   }

}
