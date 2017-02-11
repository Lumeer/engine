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
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
@SessionScoped
public class DocumentMetadataFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   // example of document metadata structure:
   // -------------------------------------
   // {
   //	“_meta-create-date” : date,
   //	“_meta-update-date” : date,
   //	“_meta-creator-user” : user_name,
   //	“_meta-update-user” : user_name,
   //	“_meta-rights” : [
   //      user_name1 : 1  //execute permissions
   //      user_name2 : 2  //write permissions
   //      user_name3 : 4  //read permissions
   //      user_name4 : 3  //execute and write permissions = 1 + 2
   //      user_name5 : 5  //read and execute permissions = 1 + 4
   //      user_name6 : 6  //write and read permissions = 2 + 4
   //      user_name7 : 7  //full permissions = 1 + 2 + 4
   //      others     : 0  //others is forced to be there, maybe if not presented, that means 0 permissions
   //    ],
   // “_meta-group-rights” : [
   //      group_name1: 1  //execute permissions
   //      group_name2: 2  //write permissions
   //      group_name3: 4  //read permissions
   //      group_name4: 3  //execute and write permissions = 1 + 2
   //      group_name5: 5  //read and execute permissions = 1 + 4
   //      group_name6: 6  //write and read permissions = 2 + 4
   //      group_name7: 7  //full permissions = 1 + 2 + 4
   //	   ]
   //	… (rest of the document) …
   // }

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
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in collection
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public Object getDocumentMetadata(String collectionName, String documentId, String key) throws CollectionNotFoundException, DocumentNotFoundException, IllegalArgumentException {
      if (!key.startsWith(LumeerConst.Document.METADATA_PREFIX)) {
         throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKeyString(key));
      }
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument documentMetadata = dataStorage.readDocumentIncludeAttrs(collectionName, documentId, Collections.singletonList(key));
      if (documentMetadata == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return documentMetadata.get(key);
   }

   /**
    * Reads the metadata keys and values of specified document
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @return the map where key is name of metadata attribute and its value
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in collection
    */
   public Map<String, Object> readDocumentMetadata(String collectionName, String documentId) throws CollectionNotFoundException, DocumentNotFoundException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      DataDocument documentMetadata = dataStorage.readDocumentIncludeAttrs(collectionName, documentId, LumeerConst.Document.METADATA_KEYS);
      if (documentMetadata == null) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      return documentMetadata;
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
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in collection
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public void putDocumentMetadata(String collectionName, String documentId, String key, Object value) throws CollectionNotFoundException, DocumentNotFoundException, IllegalArgumentException {
      if (!key.startsWith(LumeerConst.Document.METADATA_PREFIX)) {
         throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKeyString(key));
      }
      updateDocumentMetadata(collectionName, documentId, new DataDocument(key, value));
   }

   /**
    * Put attributes and its values to document metadata
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @param metadata
    *       map with medatadata attributes and its values
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in collection
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public void updateDocumentMetadata(String collectionName, String documentId, DataDocument metadata) throws CollectionNotFoundException, DocumentNotFoundException, IllegalArgumentException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!dataStorage.collectionHasDocument(collectionName, documentId)) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      for (String key : metadata.keySet()) {
         if (!key.startsWith(LumeerConst.Document.METADATA_PREFIX)) {
            throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKeyString(key));
         }
      }
      dataStorage.updateDocument(collectionName, metadata, documentId);
   }

   /**
    * Remove single metadata attribute for the document
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @param key
    *       the id of the read document
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in collection
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public void dropDocumentMetadata(String collectionName, String documentId, String key) throws CollectionNotFoundException, DocumentNotFoundException, IllegalArgumentException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!dataStorage.collectionHasDocument(collectionName, documentId)) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (!key.startsWith(LumeerConst.Document.METADATA_PREFIX)) {
         throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKeyString(key));
      }
      dataStorage.dropAttribute(collectionName, documentId, key);
   }

}
