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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.exception.CollectionNotFoundException;
import io.lumeer.engine.exception.DocumentNotFoundException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
public class DocumentMetadataFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   public final String DOCUMENT_METADATA_PREFIX = "meta-";
   public final String DOCUMENT_CREATE_DATE_KEY = DOCUMENT_METADATA_PREFIX + "create-date";
   public final String DOCUMENT_UPDATE_DATE_KEY = DOCUMENT_METADATA_PREFIX + "update-date";
   public final String DOCUMENT_CREATE_BY_USER_KEY = DOCUMENT_METADATA_PREFIX + "create-user";
   public final String DOCUMENT_UPDATED_BY_USER_KEY = DOCUMENT_METADATA_PREFIX + "update-user";
   public final String DOCUMENT_RIGHTS_KEY = DOCUMENT_METADATA_PREFIX + "rights";

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
      if (!key.startsWith(DOCUMENT_METADATA_PREFIX)) {
         throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKey(key));
      }
      return readDocumentMetadata(collectionName, documentId).get(key);
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
      DataDocument document = readDocument(collectionName, documentId);
      Iterator<Map.Entry<String, Object>> iter = document.entrySet().iterator();
      // filter out non-metadata attributes with  values
      Map<String, Object> documentMetadata = new HashMap<>();
      while (iter.hasNext()) {
         Map.Entry<String, Object> entry = iter.next();
         if (entry.getKey().startsWith(DOCUMENT_METADATA_PREFIX)) {
            documentMetadata.put(entry.getKey(), entry.getValue());
         }
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
    *       the id of the read document
    * @param value
    *       the id of the read document
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in collection
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public void putDocumentMetadata(String collectionName, String documentId, String key, Object value) throws CollectionNotFoundException, DocumentNotFoundException, IllegalArgumentException {
      if (!key.startsWith(DOCUMENT_METADATA_PREFIX)) {
         throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKey(key));
      }
      Map<String, Object> metadata = new HashMap<>();
      metadata.remove(key, value);
      updateDocumentMetadata(collectionName, documentId, metadata);
   }

   /**
    * Put attributes and its values to document metadata
    *
    * @param collectionName
    *       the name of the collection where the document is located
    * @param documentId
    *       the id of the read document
    * @param metadata
    *       map with medatada attributes and its values
    * @throws CollectionNotFoundException
    *       if collection is not found in database
    * @throws DocumentNotFoundException
    *       if document is not found in collection
    * @throws IllegalArgumentException
    *       if key is not metadata attribute
    */
   public void updateDocumentMetadata(String collectionName, String documentId, Map<String, Object> metadata) throws CollectionNotFoundException, DocumentNotFoundException, IllegalArgumentException {
      readDocument(collectionName, documentId);
      for (String key : metadata.keySet()) {
         if (!key.startsWith(DOCUMENT_METADATA_PREFIX)) {
            throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKey(key));
         }
      }
      dataStorage.updateDocument(collectionName, new DataDocument(metadata), documentId, -1);
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
      readDocument(collectionName, documentId);
      if (!key.startsWith(DOCUMENT_METADATA_PREFIX)) {
         throw new IllegalArgumentException(ErrorMessageBuilder.invalidMetadataKey(key));
      }
      dataStorage.removeAttribute(collectionName, documentId, key);
   }

   private DataDocument readDocument(final String collectionName, final String documentId) throws CollectionNotFoundException, DocumentNotFoundException {
      return documentFacade.readDocument(collectionName, documentId);
   }

}
