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
import io.lumeer.engine.util.Utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class CollectionMetadataFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   // table name prefixes, attribute names and other constants used in metadata

   public final String META_TYPE_KEY = "meta-type";
   public final String COLLECTION_NAME_PREFIX = "collection.";
   public final String COLLECTION_METADATA_PREFIX = "meta.";

   public final String COLLECTION_ATTRIBUTES_META_TYPE_VALUE = "attributes";
   public final String COLLECTION_ATTRIBUTE_NAME_KEY = "name";

   public final String COLLECTION_ATTRIBUTE_TYPE_KEY = "type";
   // attribute types according to DataDocument methods, empty is default and is considered String
   // TODO: What about nested attributes? Should we return them as a string?
   public final String[] COLLECTION_ATTRIBUTE_TYPE_VALUES = { "int", "long", "double", "boolean", "date", "" };

   public final String COLLECTION_REAL_NAME_META_TYPE_VALUE = "name";
   public final String COLLECTION_REAL_NAME_KEY = "name";

   public final String COLLECTION_LOCK_META_TYPE_VALUE = "lock";
   public final String COLLECTION_LOCK_UPDATED_KEY = "updated";
   // TODO: access rights

   // example of collection metadata structure:
   // -------------------------------------
   // {
   //  “meta-type” : “attributes”,
   //  “name” : “attributes1”,
   //  “type” : “int”
   // },
   // {
   //  “meta-type” : “attributes”,
   //  “name” : “attributes2”,
   //  “type” : “”
   // },
   // {
   // “meta-type” : “name”,
   // “name” : “This is my collection name.”
   // }
   // {
   // “meta-type” : “lock”,
   // “updated” : “2016-11-08 12:23:21”
   //  }

   /**
    * Converts collection name given by user to internal representation.
    * First, spaces, diacritics, special characters etc. are removed from name
    * which is converted to lowercase. Secondly, we add collection prefix.
    *
    * @param originalCollectionName
    *       name given by user
    * @return internal collection name
    */
   public String collectionNameToInternalForm(String originalCollectionName) {
      String name = originalCollectionName.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
      return COLLECTION_NAME_PREFIX + name;
   }

   /**
    * Creates initial metadata in metadata collection - adds original name and initial time lock.
    *
    * @param collectionOriginalName
    *       name of collection given by user
    */
   public void createInitialMetadata(String collectionOriginalName) {
      String internalCollectionName = collectionNameToInternalForm(collectionOriginalName);
      setOriginalCollectionName(internalCollectionName, collectionOriginalName);

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(META_TYPE_KEY, COLLECTION_LOCK_META_TYPE_VALUE);
      metadata.put(COLLECTION_LOCK_UPDATED_KEY, Utils.getCurrentTimeString());
      DataDocument metadataDocument = new DataDocument(metadata);

      String metadataCollectionName = collectionMetadataCollectionName(internalCollectionName);
      dataStorage.createDocument(metadataCollectionName, metadataDocument);
   }

   /**
    * Returns info about collection attributes
    *
    * @param collectionName
    *       internal collection name
    * @return map - keys are attribute names, values are types
    */
   public Map<String, String> getCollectionAttributesInfo(String collectionName) {
      String query = queryCollectionAttributesInfo(collectionName);
      List<DataDocument> attributesInfoDocuments = dataStorage.search(query);

      Map<String, String> attributesInfo = new HashMap<>();

      for (int i = 0; i < attributesInfoDocuments.size(); i++) {
         String name = attributesInfoDocuments.get(i).getString(COLLECTION_ATTRIBUTE_NAME_KEY);
         String type = attributesInfoDocuments.get(i).getString(COLLECTION_ATTRIBUTE_TYPE_KEY);
         attributesInfo.put(name, type);
      }

      return attributesInfo;
   }

   /**
    * Adds an attribute to collection metadata.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       added attribute name
    * @param attributeType
    *       added attribute type
    * @return true if add is successful, false if attribute already exists
    */
   public boolean addCollectionAttribute(String collectionName, String attributeName, String attributeType) {
      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> attributeInfo = dataStorage.search(query);

      // return false if the attribute already exists
      if (!attributeInfo.isEmpty()) {
         return false;
      }

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(META_TYPE_KEY, COLLECTION_ATTRIBUTES_META_TYPE_VALUE);
      metadata.put(COLLECTION_ATTRIBUTE_NAME_KEY, attributeName);
      metadata.put(COLLECTION_ATTRIBUTE_TYPE_KEY, attributeType);
      DataDocument metadataDocument = new DataDocument(metadata);
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      dataStorage.createDocument(metadataCollectionName, metadataDocument);

      return true;
   }

   /**
    * Renames existing attribute in collection metadata.
    *
    * @param collectionName
    *       internal collection name
    * @param oldName
    *       old attribute name
    * @param newName
    *       new attribute name
    * @return true if rename is successful, false if attribute does not exist
    */
   public boolean renameCollectionAttribute(String collectionName, String oldName, String newName) {
      String query = queryCollectionAttributeInfo(collectionName, oldName);
      List<DataDocument> attributeInfo = dataStorage.search(query);

      // the attribute does not exist
      if (attributeInfo.isEmpty()) {
         return false;
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      if (!newName.isEmpty()) {
         metadata.put(COLLECTION_ATTRIBUTE_NAME_KEY, newName);
         DataDocument metadataDocument = new DataDocument(metadata);
         String metadataCollectionName = collectionMetadataCollectionName(collectionName);
         dataStorage.updateDocument(metadataCollectionName, metadataDocument, documentId);
         return true;
      }

      return false;
   }

   /**
    * Changes attribute type in metadata.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @param newType
    *       new attribute type
    * @return true if retype is successful, false if attribute does not exist
    */
   public boolean retypeCollectionAttribute(String collectionName, String attributeName, String newType) {
      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> attributeInfo = dataStorage.search(query);

      // the attribute does not exist
      if (attributeInfo.isEmpty()) {
         return false;
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(COLLECTION_ATTRIBUTE_TYPE_KEY, newType);
      DataDocument metadataDocument = new DataDocument(metadata);
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      dataStorage.updateDocument(metadataCollectionName, metadataDocument, documentId);

      return true;
   }

   /**
    * Deletes an attribute from collection metadata
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute to be deleted
    * @return true if delete is successful, false if attribute does not exist
    */
   public boolean dropCollectionAttribute(String collectionName, String attributeName) {
      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> attributeInfo = dataStorage.search(query);

      // the attribute does not exist
      if (attributeInfo.isEmpty()) {
         return false;
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.get("_id").toString();
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      dataStorage.dropDocument(metadataCollectionName, documentId);

      return true;
   }

   /**
    * Searches for original (given by user) collection name in metadata
    *
    * @param collectionName
    *       internal collection name
    * @return original collection name
    */
   public String getOriginalCollectionName(String collectionName) {
      String query = queryCollectionNameInfo(collectionName);
      List<DataDocument> nameInfo = dataStorage.search(query);

      DataDocument nameDocument = nameInfo.get(0);
      String name = nameDocument.getString(COLLECTION_REAL_NAME_KEY);

      return name;
   }

   /**
    * Sets original (given by user) collection name in metadata
    *
    * @param collectionOriginalName
    *       name given by user
    */
   public void setOriginalCollectionName(String collectionInternalName, String collectionOriginalName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionInternalName);
      Map<String, Object> metadata = new HashMap<>();
      metadata.put(META_TYPE_KEY, COLLECTION_REAL_NAME_META_TYPE_VALUE);
      metadata.put(COLLECTION_REAL_NAME_KEY, collectionOriginalName);

      DataDocument metadataDocument = new DataDocument(metadata);
      dataStorage.createDocument(metadataCollectionName, metadataDocument);
   }

   /**
    * Reads current value of collection lock
    *
    * @param collectionName
    *       internal collection name
    * @return String representation of the time of the last update of collection lock
    */
   public String getCollectionLockTime(String collectionName) {
      String query = queryCollectionLockTime(collectionName);
      List<DataDocument> lockInfo = dataStorage.search(query);

      DataDocument nameDocument = lockInfo.get(0);
      String lock = nameDocument.getString(COLLECTION_LOCK_UPDATED_KEY);
      return lock;
   }

   /**
    * Sets collection lock to new value
    *
    * @param collectionName
    *       internal collection name
    * @param newTime
    *       String representation of the time of the last update of collection lock
    */
   public void setCollectionLockTime(String collectionName, String newTime) {
      String query = queryCollectionLockTime(collectionName);
      List<DataDocument> lockInfo = dataStorage.search(query);
      DataDocument lockDocument = lockInfo.get(0);
      String id = lockDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(COLLECTION_LOCK_UPDATED_KEY, newTime);

      DataDocument metadataDocument = new DataDocument(metadata);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), metadataDocument, id);
   }

   /**
    * @param collectionName
    *       internal collection name
    * @return name of metadata collection
    */
   public String collectionMetadataCollectionName(String collectionName) {
      return COLLECTION_METADATA_PREFIX + collectionName;
   }

   /**
    * @param collectionName
    *       internal collection name
    * @return true if the name is a name of "classical" collection containing data from user
    */
   public boolean isUserCollection(String collectionName) {
      String prefix = collectionName.substring(0, COLLECTION_NAME_PREFIX.length());
      return COLLECTION_NAME_PREFIX.equals(prefix);
   }

   // returns MongoDb query for getting real collection name
   private String queryCollectionNameInfo(String collectionName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(COLLECTION_REAL_NAME_META_TYPE_VALUE)
            .append("\"}}");
      String findNameQuery = sb.toString();
      return findNameQuery;
   }

   // returns MongoDb query for getting info about all attributes
   private String queryCollectionAttributesInfo(String collectionName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(COLLECTION_ATTRIBUTES_META_TYPE_VALUE)
            .append("\"}}");
      String findAttributeQuery = sb.toString();
      return findAttributeQuery;
   }

   // returns MongoDb query for getting info about specific attribute
   private String queryCollectionAttributeInfo(String collectionName, String attributeName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(COLLECTION_ATTRIBUTES_META_TYPE_VALUE)
            .append("\",\"")
            .append(COLLECTION_ATTRIBUTE_NAME_KEY)
            .append("\":\"")
            .append(attributeName)
            .append("\"}}");
      String findAttributeQuery = sb.toString();
      return findAttributeQuery;
   }

   // returns MongoDb query for getting collection lock time
   private String queryCollectionLockTime(String collectionName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(COLLECTION_LOCK_META_TYPE_VALUE)
            .append("\"}}");
      String findNameQuery = sb.toString();
      return findNameQuery;
   }
}
