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
import java.util.Arrays;
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
   public final List<String> COLLECTION_ATTRIBUTE_TYPE_VALUES = Arrays.asList(new String[] { "int", "long", "double", "bool", "date", "", "string", "nested" });
   public final String COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY = "constraints";
   public final String COLLECTION_ATTRIBUTE_COUNT_KEY = "count";

   public final String COLLECTION_REAL_NAME_META_TYPE_VALUE = "name";
   public final String COLLECTION_REAL_NAME_KEY = "name";

   public final String COLLECTION_LOCK_META_TYPE_VALUE = "lock";
   public final String COLLECTION_LOCK_UPDATED_KEY = "updated";

   // example of collection metadata structure:
   // -------------------------------------
   // {
   //  “meta-type” : “attributes”,
   //  “name” : “attribute1”,
   //  “type” : “int”,
   //  “constraints” : [{"gt" : 2}, {"let" : 10}], // grater than 2, less or equal than 10
   //  “child-attributes” : []
   // },
   // {
   //  “meta-type” : “attributes”,
   //  “name” : “attribute2”,
   //  “type” : “nested”
   //  “child-attributes” : [
   //    {
   //       “name” : “attribute3”,
   //       “type” : “string”,
   //       “constraints” : [{"regex" : "[a-z]*"}, {"lt" : 10}], // string shorter than 10 consisting of lowercase letters
   //    },
   //    {
   //       “name” : “attribute4”,
   //       “type” : “double”,
   //       “constraints” : ""
   //    }
   //    ]
   // },
   // {
   // “meta-type” : “name”,
   // “name” : “This is my collection name.”
   // },
   // {
   // “meta-type” : “lock”,
   // “updated” : “2016-11-08 12:23:21”
   //  }

   /**
    * Converts collection name given by user to internal representation.
    * First, the name is trimmed of whitespaces.
    * Spaces are replaced by "_". Converted to lowercase.
    * Diacritics are replaced by ASCII characters.
    * Everything except a-z, 0-9 and _ is removed.
    * Number is added to the end of the name to ensure it is unique.
    *
    * @param originalCollectionName
    *       name given by user
    * @return internal collection name
    */
   public String createInternalName(String originalCollectionName) {
      String name = originalCollectionName.trim();
      name = name.replace(' ', '_').toLowerCase();
      name = Utils.normalize(name);
      name = name.replaceAll("[^_a-z0-9]+", "");
      name = COLLECTION_NAME_PREFIX + name;
      Integer i = 0;
      while (dataStorage.getAllCollections().contains(name + "_" + i.toString())) {
         i++;
      }
      name = name + "_" + i.toString();

      return name;
   }

   /**
    * Creates initial metadata in metadata collection - adds original name and initial time lock.
    *
    * @param internalCollectionName
    *       internal collection name
    * @param collectionOriginalName
    *       name of collection given by user
    */
   public void createInitialMetadata(String internalCollectionName, String collectionOriginalName) {
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
   }

   /**
    * Returns some info about collection attributes
    *
    * @param collectionName
    *       internal collection name
    * @return map - keys are attribute names, values are types
    */
   public Map<String, String> getCollectionAttributesNamesAndTypes(String collectionName) {
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
    * Gets complete info about collection attributes
    *
    * @param collectionName
    *       internal collection name
    * @return list of DataDocuments, each with info about one attribute
    */
   public List<DataDocument> getCollectionAttributesInfo(String collectionName) {
      String query = queryCollectionAttributesInfo(collectionName);
      List<DataDocument> attributesInfoDocuments = dataStorage.search(query);
      return attributesInfoDocuments;
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
    * @return true if retype is successful, false if attribute or type does not exist
    */
   public boolean retypeCollectionAttribute(String collectionName, String attributeName, String newType) {
      if (!COLLECTION_ATTRIBUTE_TYPE_VALUES.contains(newType)) { // new type must be from our list
         return false;
      }

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

   /**
    * Adds attribute to metadata collection, if the attribute already isn't there.
    * Otherwise just increments count.
    *
    * @param collectionName
    *       internal collection name
    * @param attribute
    *       set of attributes' names
    */
   public void addOrIncrementAttribute(String collectionName, String attribute) {
      String query = queryCollectionAttributeInfo(collectionName, attribute);
      List<DataDocument> attributeInfo = dataStorage.search(query);
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      if (!attributeInfo.isEmpty()) { // attribute already exists
         DataDocument attributeDocument = attributeInfo.get(0);
         String documentId = attributeDocument.get("_id").toString();
         dataStorage.incerementAttributeValueBy(metadataCollectionName, documentId, COLLECTION_ATTRIBUTE_COUNT_KEY, 1);
      } else {
         Map<String, Object> metadata = new HashMap<>();
         metadata.put(META_TYPE_KEY, COLLECTION_ATTRIBUTES_META_TYPE_VALUE);
         metadata.put(COLLECTION_ATTRIBUTE_NAME_KEY, attribute);
         metadata.put(COLLECTION_ATTRIBUTE_TYPE_KEY, "");
         metadata.put(COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY, "");
         metadata.put(COLLECTION_ATTRIBUTE_COUNT_KEY, 1L);
         DataDocument metadataDocument = new DataDocument(metadata);
         dataStorage.createDocument(metadataCollectionName, metadataDocument);
      }
   }

   /**
    * Drops attribute if there is no document with that
    * attribute in the collection (count is 1). Otherwise just decrements count.
    *
    * @param collectionName
    *       internal collection name
    * @param attribute
    *       set of attributes' names
    */
   public void dropOrDecrementAttribute(String collectionName, String attribute) {
      String query = queryCollectionAttributeInfo(collectionName, attribute);
      List<DataDocument> attributeInfo = dataStorage.search(query);
      if (!attributeInfo.isEmpty()) { // in case somebody did that sooner, we may have nothing to remove
         DataDocument attributeDocument = attributeInfo.get(0);
         String documentId = attributeDocument.get("_id").toString();
         String metadataCollectionName = collectionMetadataCollectionName(collectionName);

         // we check if this was the last document with the attribute
         if (attributeDocument.getLong(COLLECTION_ATTRIBUTE_COUNT_KEY) == 1) {
            dataStorage.dropDocument(metadataCollectionName, documentId);
         } else {
            dataStorage.incerementAttributeValueBy(metadataCollectionName, documentId, COLLECTION_ATTRIBUTE_COUNT_KEY, -1);
         }
      }
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
      if (!countInfo.isEmpty()) {
         DataDocument countDocument = countInfo.get(0);
         return countDocument.getLong(COLLECTION_ATTRIBUTE_COUNT_KEY);
      } else {
         return 0;
      }
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
    * @param collectionInternalName
    *       internal collection name
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
    * @return true if set was succesful
    */
   public boolean setCollectionLockTime(String collectionName, String newTime) {
      if (!Utils.isValidDateFormat(newTime)) { // time format is not valid
         return false;
      }

      String query = queryCollectionLockTime(collectionName);
      List<DataDocument> lockInfo = dataStorage.search(query);
      DataDocument lockDocument = lockInfo.get(0);
      String id = lockDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(COLLECTION_LOCK_UPDATED_KEY, newTime);

      DataDocument metadataDocument = new DataDocument(metadata);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), metadataDocument, id, -1);
      return true;
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

   //   /**
   //    * Sets count for specific attribute
   //    *
   //    * @param collectionName
   //    *       internal collection name
   //    * @param attributeName
   //    *       attribute name
   //    * @param count
   //    *       count value to be set
   //    */
   //   public void setAttributeCount(String collectionName, String attributeName, long count) {
   //      String query = queryCollectionAttributeInfo(collectionName, attributeName);
   //      List<DataDocument> attributeInfo = dataStorage.search(query);
   //      DataDocument attributeDocument = attributeInfo.get(0);
   //      String id = attributeDocument.get("_id").toString();
   //
   //      Map<String, Object> metadata = new HashMap<>();
   //      metadata.put(COLLECTION_ATTRIBUTE_COUNT_KEY, count);
   //
   //      DataDocument metadataDocument = new DataDocument(metadata);
   //      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), metadataDocument, id, -1);
   //   }

   /**
    * Checks whether given attribute type is correct (if it corresponds to attribute type saved in metadata)
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       name of attribute to check
    * @param attributeType
    *       type of attribute to check
    * @return true if type is correct, false if not or if attribute doesn't exist
    */
   public boolean checkAttributeType(String collectionName, String attributeName, String attributeType) {
      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> attributeInfo = dataStorage.search(query);
      if (!attributeInfo.isEmpty()) {
         DataDocument attributeDocument = attributeInfo.get(0);
         String correctType = attributeDocument.getString(COLLECTION_ATTRIBUTE_TYPE_KEY);
         return correctType.equals(attributeType);
      } else { // attribute doesn't exist
         return false;
      }
   }

   /**
    * Checks whether value corresponds to attribute type
    * TODO: check attributes constraints
    *
    * @param collectionName
    *       internal collection name
    * @param attribute
    *       attribute name
    * @param valueString
    *       value converted to String
    * @return true if value corresponds to attribute type, false if not or attribute does not exist
    */
   public boolean checkAttributeValue(String collectionName, String attribute, String valueString) {
      String type = getCollectionAttributesNamesAndTypes(collectionName).get(attribute);

      if (type == null) {
         return false; // attribute does not exist
      }

      if (type.equals("int")) {
         try {
            Integer.parseInt(valueString);
            return true;
         } catch (NumberFormatException e) {
            return false;
         }
      }

      if (type.equals("long")) {
         try {
            Long.parseLong(valueString);
            return true;
         } catch (NumberFormatException e) {
            return false;
         }
      }

      if (type.equals("double")) {
         try {
            Double.parseDouble(valueString);
            return true;
         } catch (NumberFormatException e) {
            return false;
         }
      }

      if (type.equals("date")) { // we accept yyyy.MM.dd and yyyy.MM.dd HH.mm.ss
         return Utils.isValidDateFormatJustDate(valueString) || Utils.isValidDateFormatDateAndTimeMinutes(valueString);
      }

      if (type.equals("bool")) {
         return valueString.equals("false") || valueString.equals("true");
      }

      if (type.equals("nested")) { // we cannot add value to nested attribute, just to its children
         return false;
      }

      // we return true when type is not specified or string
      return true;
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
