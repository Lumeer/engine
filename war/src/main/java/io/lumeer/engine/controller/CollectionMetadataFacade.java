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
import java.util.Set;
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
   public final String COLLECTION_ATTRIBUTE_COUNT_KEY = "count";

   public final String COLLECTION_REAL_NAME_META_TYPE_VALUE = "name";
   public final String COLLECTION_REAL_NAME_KEY = "name";

   public final String COLLECTION_LOCK_META_TYPE_VALUE = "lock";
   public final String COLLECTION_LOCK_UPDATED_KEY = "updated";

   public final String COLLECTION_COUNT_META_TYPE_VALUE = "count";
   public final String COLLECTION_COUNT_KEY = "count";
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
   // },
   // {
   // “meta-type” : “lock”,
   // “updated” : “2016-11-08 12:23:21”
   //  },
   // {
   // “meta-type” : “count”,
   // “count” : “23”
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
      String metadataCollectionName = collectionMetadataCollectionName(internalCollectionName);

      // set name - we don't use setOriginalCollectionName, because that methods assumes document with name already exists
      Map<String, Object> metadataName = new HashMap<>();
      metadataName.put(META_TYPE_KEY, COLLECTION_REAL_NAME_META_TYPE_VALUE);
      metadataName.put(COLLECTION_REAL_NAME_KEY, collectionOriginalName);
      dataStorage.createDocument(metadataCollectionName, new DataDocument(metadataName));

      // set lock - we don't use setCollectionLockTime, because that methods assumes document with lock already exists
      Map<String, Object> metadataLock = new HashMap<>();
      metadataLock.put(META_TYPE_KEY, COLLECTION_LOCK_META_TYPE_VALUE);
      metadataLock.put(COLLECTION_LOCK_UPDATED_KEY, Utils.getCurrentTimeString());
      dataStorage.createDocument(metadataCollectionName, new DataDocument(metadataLock));

      // set count - we don't use setCollectionCountTime, because that methods assumes document with count already exists
      Map<String, Object> metadataCount = new HashMap<>();
      metadataCount.put(META_TYPE_KEY, COLLECTION_COUNT_META_TYPE_VALUE);
      metadataCount.put(COLLECTION_COUNT_KEY, 0L);
      dataStorage.createDocument(metadataCollectionName, new DataDocument(metadataCount));
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
    * @param attributeCount
    *       added attribute count. If -1, then count is the number of documents in the collection
    * @return true if add is successful, false if attribute already exists
    */
   public boolean addCollectionAttribute(String collectionName, String attributeName, String attributeType, long attributeCount) {
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
      if (attributeCount == -1) {
         metadata.put(COLLECTION_ATTRIBUTE_COUNT_KEY, getCollectionCount(collectionName));
      } else {
         metadata.put(COLLECTION_ATTRIBUTE_COUNT_KEY, attributeCount);
      }
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
         dataStorage.updateDocument(metadataCollectionName, metadataDocument, documentId, -1);
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
      dataStorage.updateDocument(metadataCollectionName, metadataDocument, documentId, -1);

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

   // NOT TESTED YET
   //   /**
   //    * Adds attributes from given document to metadata collection, if the attribute already isn't there.
   //    * Otherwise just increments count.
   //    *
   //    * @param collectionName
   //    *       internal collection name
   //    * @param attributes
   //    *       set of attributes' names
   //    */
   //   public void addDocumentAttributes(String collectionName, Set<String> attributes) {
   //      Set<String> collectionAttributes = getCollectionAttributesInfo(collectionName).keySet();
   //      for (String attribute : attributes) {
   //         if (!collectionAttributes.contains(attribute)) {
   //            addCollectionAttribute(collectionName, attribute, "", 1);
   //         } else {
   //            incrementAttributeCount(collectionName, attribute);
   //         }
   //      }
   //   }

   // NOT TESTED YET
   //   /**
   //    * Drops attributes from given document, when there is no document with that
   //    * attribute in the collection (count is 1). Otherwise just decrements count.
   //    *
   //    * @param collectionName
   //    *       internal collection name
   //    * @param attributes
   //    *       set of attributes' names
   //    */
   //   public void dropDocumentAttributes(String collectionName, Set<String> attributes) {
   //      Set<String> collectionAttributes = getCollectionAttributesInfo(collectionName).keySet();
   //      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
   //      for (String attribute : attributes) {
   //         String query = queryCollectionAttributeInfo(collectionName, attribute);
   //         List<DataDocument> attributeInfo = dataStorage.search(query);
   //
   //         DataDocument attributeDocument = attributeInfo.get(0);
   //
   //         String documentId = attributeDocument.get("_id").toString();
   //         long attributeCount = attributeDocument.getLong(COLLECTION_ATTRIBUTE_COUNT_KEY);
   //         if (attributeCount == 1) { // document is the last one with the attribute
   //            String attributeName = attributeDocument.getString(COLLECTION_ATTRIBUTE_NAME_KEY);
   //            dropCollectionAttribute(collectionName, attributeName);
   //         } else {
   //            decrementAttributeCount(collectionName, attribute);
   //         }
   //      }
   //   }

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
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), metadataDocument, id, -1);
   }

   public long getCollectionCount(String collectionName) {
      String query = queryCollectionCount(collectionName);
      List<DataDocument> countInfo = dataStorage.search(query);

      DataDocument countDocument = countInfo.get(0);
      return countDocument.getLong(COLLECTION_COUNT_KEY); // getLong() throws ClassCastException: "java.lang.Integer cannot be cast to java.lang.Long"
   }

   public void setCollectionCount(String collectionName, long count) {
      String query = queryCollectionCount(collectionName);
      List<DataDocument> countInfo = dataStorage.search(query);
      DataDocument countDocument = countInfo.get(0);
      String id = countDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(COLLECTION_COUNT_KEY, count);

      DataDocument metadataDocument = new DataDocument(metadata);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), metadataDocument, id, -1);
   }

   /**
    * Increments collection count by 1
    *
    * @param collectionName
    *       internal collection name
    */
   public void incrementCollectionCount(String collectionName) {
      long count = getCollectionCount(collectionName);
      count++;
      setCollectionCount(collectionName, count);
   }

   /**
    * Decrements collection count by 1
    *
    * @param collectionName
    *       internal collection name
    */
   public void decrementCollectionCount(String collectionName) {
      long count = getCollectionCount(collectionName);
      count--;
      setCollectionCount(collectionName, count);
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
      return COLLECTION_NAME_PREFIX.equals(prefix) && !collectionName.endsWith(".shadow"); // VersionFacade adds suffix
   }

   /**
    * Returns count for specific attribute
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @return attribute count
    */
   public long getAttributeCount(String collectionName, String attributeName) {
      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> countInfo = dataStorage.search(query);
      DataDocument countDocument = countInfo.get(0);
      return countDocument.getLong(COLLECTION_ATTRIBUTE_COUNT_KEY);
   }

   /**
    * Sets count for specific attribute
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @param count
    *       count value to be set
    */
   public void setAttributeCount(String collectionName, String attributeName, long count) {
      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> attributeInfo = dataStorage.search(query);
      DataDocument attributeDocument = attributeInfo.get(0);
      String id = attributeDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(COLLECTION_ATTRIBUTE_COUNT_KEY, count);

      DataDocument metadataDocument = new DataDocument(metadata);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), metadataDocument, id, -1);
   }

   public void incrementAttributeCount(String collectionName, String attribute) {
      long count = getAttributeCount(collectionName, attribute);
      count++;
      setAttributeCount(collectionName, attribute, count);
   }

   public void decrementAttributeCount(String collectionName, String attribute) {
      long count = getAttributeCount(collectionName, attribute);
      count--;
      setAttributeCount(collectionName, attribute, count);
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

   // returns MongoDb query for getting collection count
   private String queryCollectionCount(String collectionName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(COLLECTION_COUNT_META_TYPE_VALUE)
            .append("\"}}");
      String findCountQuery = sb.toString();
      return findCountQuery;
   }
}
