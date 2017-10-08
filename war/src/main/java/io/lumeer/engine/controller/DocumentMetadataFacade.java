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
