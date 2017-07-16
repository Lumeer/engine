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
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;

import java.io.Serializable;
import java.util.Collections;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Manipulates with document metadata
 */
@SessionScoped
public class DocumentMetadataFacade implements Serializable {

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   /**
    * Reads specified metadata value
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @param key
    *       the id of the read document
    * @return value of specified metadata attribute
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public Object getDocumentMetadata(String collectionName, String documentId, String key) throws IllegalArgumentException {
      if (!LumeerConst.Document.METADATA_KEYS.contains(key)) {
         throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKeyString(key));
      }
      return dataStorage.readDocumentIncludeAttrs(collectionName, dataStorageDialect.documentIdFilter(documentId), Collections.singletonList(key)).get(key);
   }

   /**
    * Reads the metadata keys and values of specified document
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @return the map where key is name of metadata attribute and its value
    */
   public DataDocument readDocumentMetadata(String collectionName, String documentId) {
      return dataStorage.readDocumentIncludeAttrs(collectionName, dataStorageDialect.documentIdFilter(documentId), LumeerConst.Document.METADATA_KEYS);
   }

   /**
    * Put attribute and value to document metadata
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @param key
    *       the meta attribute to put
    * @param value
    *       the meta value of the given attribute
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public void putDocumentMetadata(String collectionName, String documentId, String key, Object value) throws IllegalArgumentException {
      updateDocumentMetadata(collectionName, documentId, new DataDocument(key, value));
   }

   /**
    * Put attributes and its values to document metadata
    *
    * @param collectionCode
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @param metadata
    *       map with medatadata attributes and its values
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public void updateDocumentMetadata(String collectionCode, String documentId, DataDocument metadata) throws IllegalArgumentException {
      for (String key : metadata.keySet()) {
         if (!LumeerConst.Document.METADATA_KEYS.contains(key)) {
            throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKeyString(key));
         }
      }
      dataStorage.updateDocument(collectionCode, metadata, dataStorageDialect.documentIdFilter(documentId));
   }

   /**
    * Remove single metadata attribute for the document
    *
    * @param collectionCode
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @param key
    *       the id of the read document
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public void dropDocumentMetadata(String collectionCode, String documentId, String key) throws IllegalArgumentException {
      if (!LumeerConst.Document.METADATA_KEYS.contains(key)) {
         throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKeyString(key));
      }
      dataStorage.dropAttribute(collectionCode, dataStorageDialect.documentIdFilter(documentId), key);
   }

   public void putInitDocumentMetadataInternally(DataDocument dataDocument, String userEmail) {
      dataDocument.put(LumeerConst.Document.CREATE_DATE_KEY, Utils.getCurrentTimeString());
      dataDocument.put(LumeerConst.Document.CREATE_BY_USER_KEY, userEmail);
   }

   public void putUpdateDocumentMetadataInternally(DataDocument dataDocument, String userEmail) {
      dataDocument.put(LumeerConst.Document.UPDATE_DATE_KEY, Utils.getCurrentTimeString());
      dataDocument.put(LumeerConst.Document.UPDATED_BY_USER_KEY, userEmail);
   }

}
