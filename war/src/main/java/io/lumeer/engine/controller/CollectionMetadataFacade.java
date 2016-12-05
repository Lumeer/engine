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
import io.lumeer.engine.api.constraint.ConstraintManager;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.CollectionMetadataNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */

@SessionScoped
public class CollectionMetadataFacade implements Serializable {

   @Inject
   private DataStorage dataStorage;

   @Inject
   private ConfigurationFacade configurationFacade;

   // table name prefixes, attribute names and other constants used in metadata

   private static final String META_TYPE_KEY = "meta-type";
   private static final String COLLECTION_NAME_PREFIX = "collection.";
   private static final String COLLECTION_METADATA_PREFIX = "meta.";

   private static final String COLLECTION_ATTRIBUTES_META_TYPE_VALUE = "attributes";

   private static final String COLLECTION_ATTRIBUTE_NAME_KEY = "name";
   private static final String COLLECTION_ATTRIBUTE_TYPE_KEY = "type";

   // attribute types according to DataDocument methods, empty is default and is considered String
   public static final String COLLECTION_ATTRIBUTE_TYPE_INT = "int";
   public static final String COLLECTION_ATTRIBUTE_TYPE_LONG = "long";
   public static final String COLLECTION_ATTRIBUTE_TYPE_DOUBLE = "double";
   public static final String COLLECTION_ATTRIBUTE_TYPE_BOOLEAN = "bool";
   public static final String COLLECTION_ATTRIBUTE_TYPE_DATE = "date";
   public static final String COLLECTION_ATTRIBUTE_TYPE_STRING = "";
   public static final String COLLECTION_ATTRIBUTE_TYPE_NESTED = "nested";
   private static final List<String> COLLECTION_ATTRIBUTE_TYPE_VALUES =
         Arrays.asList(new String[] {
               COLLECTION_ATTRIBUTE_TYPE_INT,
               COLLECTION_ATTRIBUTE_TYPE_LONG,
               COLLECTION_ATTRIBUTE_TYPE_DOUBLE,
               COLLECTION_ATTRIBUTE_TYPE_BOOLEAN,
               COLLECTION_ATTRIBUTE_TYPE_DATE,
               COLLECTION_ATTRIBUTE_TYPE_STRING,
               COLLECTION_ATTRIBUTE_TYPE_NESTED
         });

   private static final String COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY = "constraints";

   //   public static final String COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_GT = "gt";
   //   public static final String COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_GTE = "gtt";
   //   public static final String COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_LT = "lt";
   //   public static final String COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_LTE = "lte";
   //   public static final String COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_REGEX = "regex";
   //   private static final List<String> COLLECTION_CONSTRAINT_TYPE_VALUES =
   //         Arrays.asList(new String[] {
   //               COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_GT,
   //               COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_LT,
   //               COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_GTE,
   //               COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_LTE,
   //               COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_REGEX
   //         });

   private static final String COLLECTION_ATTRIBUTE_COUNT_KEY = "count";

   private static final String COLLECTION_REAL_NAME_META_TYPE_VALUE = "name";
   private static final String COLLECTION_REAL_NAME_KEY = "name";

   private static final String COLLECTION_LOCK_META_TYPE_VALUE = "lock";
   private static final String COLLECTION_LOCK_UPDATED_KEY = "updated";

   private ConstraintManager constraintManager;

   /**
    * Initializes constraint manager.
    */
   @PostConstruct
   public void initConstraintManager() {
      try {
         constraintManager = new ConstraintManager();
         constraintManager.setLocale(Locale.forLanguageTag(configurationFacade.getConfigurationString(LumeerConst.USER_LOCALE_PROPERTY).orElse("en-US")));
      } catch (InvalidConstraintException e) {
         throw new IllegalStateException("Illegal constraint prefix collision: ", e);
      }
   }

   /**
    * Gets active constraint manager.
    *
    * @return The active constraint manager.
    */
   @Produces
   @Named
   public ConstraintManager getConstraintManager() {
      return constraintManager;
   }

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
    * @throws UserCollectionAlreadyExistsException
    * @throws CollectionNotFoundException
    * @throws CollectionMetadataNotFoundException
    */
   public String createInternalName(String originalCollectionName) throws UserCollectionAlreadyExistsException, CollectionMetadataNotFoundException, CollectionNotFoundException {
      if (checkIfUserCollectionExists(originalCollectionName)) {
         throw new UserCollectionAlreadyExistsException(ErrorMessageBuilder.userCollectionAlreadyExistsString(originalCollectionName));
      }

      String name = originalCollectionName.trim();
      name = name.replace(' ', '_').toLowerCase();
      name = Utils.normalize(name);
      name = name.replaceAll("[^_a-z0-9]+", "");
      name = COLLECTION_NAME_PREFIX + name;
      int i = 0;
      while (dataStorage.getAllCollections().contains(name + "_" + i)) {
         i++;
      }
      name = name + "_" + i;

      return name;
   }

   /**
    * Creates initial metadata in metadata collection - adds original name and initial time lock.
    *
    * @param internalCollectionName
    *       internal collection name
    * @param collectionOriginalName
    *       name of collection given by user
    * @throws CollectionNotFoundException
    */
   public void createInitialMetadata(String internalCollectionName, String collectionOriginalName) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(internalCollectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

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
    * Returns list of names of collection attributes
    *
    * @param collectionName
    *       internal collection name
    * @return list of collection attributes
    * @throws CollectionNotFoundException
    */
   public List<String> getCollectionAttributesNames(String collectionName) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      String query = queryOneValueFromCollectionMetadata(collectionName, COLLECTION_ATTRIBUTES_META_TYPE_VALUE);
      List<DataDocument> attributesInfoDocuments = dataStorage.run(query);

      List<String> attributes = new ArrayList<>();

      for (int i = 0; i < attributesInfoDocuments.size(); i++) {
         String name = attributesInfoDocuments.get(i).getString(COLLECTION_ATTRIBUTE_NAME_KEY);
         attributes.add(name);
      }

      return attributes;
   }

   /**
    * Gets complete info about collection attributes
    *
    * @param collectionName
    *       internal collection name
    * @return list of DataDocuments, each with info about one attribute
    * @throws CollectionNotFoundException
    */
   public List<DataDocument> getCollectionAttributesInfo(String collectionName) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      String query = queryOneValueFromCollectionMetadata(collectionName, COLLECTION_ATTRIBUTES_META_TYPE_VALUE);
      List<DataDocument> attributesInfoDocuments = dataStorage.run(query);
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
    * @return true if rename is successful, false if attribute already exists
    * @throws CollectionNotFoundException
    * @throws CollectionMetadataNotFoundException
    */
   public boolean renameCollectionAttribute(String collectionName, String oldName, String newName) throws CollectionMetadataNotFoundException, CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      String query = queryCollectionAttributeInfo(collectionName, newName);
      List<DataDocument> newAttributeInfo = dataStorage.run(query);
      // check if the attribute with new name already exists in the collection
      if (!newAttributeInfo.isEmpty()) {
         // TODO Add exception?
         return false;
      }

      query = queryCollectionAttributeInfo(collectionName, oldName);
      List<DataDocument> attributeInfo = dataStorage.run(query);

      // the attribute does not exist
      if (attributeInfo.isEmpty()) {
         throw new CollectionMetadataNotFoundException(ErrorMessageBuilder.attributeMetadataDocumentNotFoundString(collectionName, oldName));
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      if (!newName.isEmpty()) {
         metadata.put(COLLECTION_ATTRIBUTE_NAME_KEY, newName);
         DataDocument metadataDocument = new DataDocument(metadata);
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
    * @throws CollectionNotFoundException
    * @throws CollectionMetadataNotFoundException
    */
   public boolean retypeCollectionAttribute(String collectionName, String attributeName, String newType) throws CollectionNotFoundException, CollectionMetadataNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      if (!COLLECTION_ATTRIBUTE_TYPE_VALUES.contains(newType)) { // new type must be from our list
         return false;
      }

      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> attributeInfo = dataStorage.run(query);

      // attribute metadata does not exist
      if (attributeInfo.isEmpty()) {
         throw new CollectionMetadataNotFoundException(ErrorMessageBuilder.attributeMetadataDocumentNotFoundString(collectionName, attributeName));
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.get("_id").toString();

      Map<String, Object> metadata = new HashMap<>();
      metadata.put(COLLECTION_ATTRIBUTE_TYPE_KEY, newType);
      DataDocument metadataDocument = new DataDocument(metadata);
      dataStorage.updateDocument(metadataCollectionName, metadataDocument, documentId, -1);

      return true;
   }

   /**
    * Gets attribute type from metadata
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @return type of the attribute
    * @throws CollectionNotFoundException
    * @throws CollectionMetadataNotFoundException
    */
   public String getAttributeType(String collectionName, String attributeName) throws CollectionNotFoundException, CollectionMetadataNotFoundException {
      List<DataDocument> attributesInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, attributeName));
      if (attributesInfo.isEmpty()) {
         throw new CollectionMetadataNotFoundException(ErrorMessageBuilder.attributeMetadataDocumentNotFoundString(collectionName, attributeName));
      }

      DataDocument attributeInfo = attributesInfo.get(0);
      String type = attributeInfo.get(COLLECTION_ATTRIBUTE_TYPE_KEY).toString();
      if (type == null) {
         throw new CollectionMetadataNotFoundException(ErrorMessageBuilder.attributeMetadataNotFoundString(collectionName, attributeName, COLLECTION_ATTRIBUTE_TYPE_KEY));
      }

      return type;
   }

   /**
    * Deletes an attribute from collection metadata
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute to be deleted
    * @return true if delete is successful, false if attribute does not exist
    * @throws CollectionNotFoundException
    * @throws CollectionMetadataNotFoundException
    */
   public boolean dropCollectionAttribute(String collectionName, String attributeName) throws CollectionNotFoundException, CollectionMetadataNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> attributeInfo = dataStorage.run(query);

      // attribute metadata does not exist
      if (attributeInfo.isEmpty()) {
         throw new CollectionMetadataNotFoundException(ErrorMessageBuilder.attributeMetadataDocumentNotFoundString(collectionName, attributeName));
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.get("_id").toString();
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
    * @throws CollectionNotFoundException
    */
   public void addOrIncrementAttribute(String collectionName, String attribute) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      String query = queryCollectionAttributeInfo(collectionName, attribute);
      List<DataDocument> attributeInfo = dataStorage.run(query);
      if (!attributeInfo.isEmpty()) { // attribute already exists
         DataDocument attributeDocument = attributeInfo.get(0);
         String documentId = attributeDocument.get("_id").toString();
         dataStorage.incrementAttributeValueBy(metadataCollectionName, documentId, COLLECTION_ATTRIBUTE_COUNT_KEY, 1);
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
    * @throws CollectionNotFoundException
    */
   public void dropOrDecrementAttribute(String collectionName, String attribute) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      String query = queryCollectionAttributeInfo(collectionName, attribute);
      List<DataDocument> attributeInfo = dataStorage.run(query);
      if (!attributeInfo.isEmpty()) { // in case somebody did that sooner, we may have nothing to remove
         DataDocument attributeDocument = attributeInfo.get(0);
         String documentId = attributeDocument.get("_id").toString();

         // we check if this was the last document with the attribute
         if (attributeDocument.getLong(COLLECTION_ATTRIBUTE_COUNT_KEY) == 1) {
            dataStorage.dropDocument(metadataCollectionName, documentId);
         } else {
            dataStorage.incrementAttributeValueBy(metadataCollectionName, documentId, COLLECTION_ATTRIBUTE_COUNT_KEY, -1);
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
    * @throws CollectionNotFoundException
    */
   public long getAttributeCount(String collectionName, String attributeName) throws CollectionNotFoundException {
      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> countInfo = dataStorage.run(query);
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
    * @throws CollectionMetadataNotFoundException
    * @throws CollectionNotFoundException
    */
   public String getOriginalCollectionName(String collectionName) throws CollectionMetadataNotFoundException, CollectionNotFoundException {
      String query = queryOneValueFromCollectionMetadata(collectionName, COLLECTION_REAL_NAME_META_TYPE_VALUE);
      List<DataDocument> nameInfo = dataStorage.run(query);

      if (nameInfo.isEmpty()) {
         throw new CollectionMetadataNotFoundException(ErrorMessageBuilder.collectionMetadataNotFoundString(collectionName, COLLECTION_REAL_NAME_META_TYPE_VALUE));
      }

      DataDocument nameDocument = nameInfo.get(0);
      String name = nameDocument.getString(COLLECTION_REAL_NAME_KEY);

      if (name == null) {
         throw new CollectionMetadataNotFoundException(ErrorMessageBuilder.collectionMetadataNotFoundString(collectionName, COLLECTION_REAL_NAME_META_TYPE_VALUE));
      }

      return name;
   }

   /**
    * Searches for internal representation of collection name
    *
    * @param originalCollectionName
    *       original collection name
    * @return internal representation of collection name
    * @throws UserCollectionNotFoundException
    * @throws CollectionNotFoundException
    * @throws CollectionMetadataNotFoundException
    */
   public String getInternalCollectionName(String originalCollectionName) throws CollectionNotFoundException, CollectionMetadataNotFoundException {
      List<String> collections = dataStorage.getAllCollections();
      for (String c : collections) {
         if (isUserCollection(c)) {
            if (getOriginalCollectionName(c).equals(originalCollectionName)) {
               return c;
            }
         }
      }
      throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(originalCollectionName));
   }

   /**
    * Sets original (given by user) collection name in metadata
    *
    * @param collectionInternalName
    *       internal collection name
    * @param collectionOriginalName
    *       name given by user
    * @throws CollectionNotFoundException
    * @throws UserCollectionAlreadyExistsException
    * @throws CollectionMetadataNotFoundException
    */
   public void setOriginalCollectionName(String collectionInternalName, String collectionOriginalName) throws CollectionNotFoundException, UserCollectionAlreadyExistsException, CollectionMetadataNotFoundException {
      if (checkIfUserCollectionExists(collectionOriginalName)) {
         throw new UserCollectionAlreadyExistsException(ErrorMessageBuilder.userCollectionAlreadyExistsString(collectionOriginalName));
      }

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
    * @throws CollectionNotFoundException
    * @throws CollectionMetadataNotFoundException
    */
   public String getCollectionLockTime(String collectionName) throws CollectionNotFoundException, CollectionMetadataNotFoundException {
      String query = queryOneValueFromCollectionMetadata(collectionName, COLLECTION_LOCK_META_TYPE_VALUE);
      List<DataDocument> lockInfo = dataStorage.run(query);

      if (lockInfo.isEmpty()) {
         throw new CollectionMetadataNotFoundException(ErrorMessageBuilder.collectionMetadataNotFoundString(collectionName, COLLECTION_LOCK_META_TYPE_VALUE));
      }

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
    * @return true if set was successful
    * @throws CollectionNotFoundException
    */
   public boolean setCollectionLockTime(String collectionName, String newTime) throws CollectionNotFoundException {
      if (!Utils.isValidDateFormat(newTime)) { // time format is not valid
         return false;
      }

      String query = queryOneValueFromCollectionMetadata(collectionName, COLLECTION_LOCK_META_TYPE_VALUE);
      List<DataDocument> lockInfo = dataStorage.run(query);
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
      if (collectionName.length() < COLLECTION_NAME_PREFIX.length()) {
         return false;
      }
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
    * @throws CollectionNotFoundException
    * @throws CollectionMetadataNotFoundException
    */
   public boolean checkAttributeValue(String collectionName, String attribute, String valueString) throws CollectionNotFoundException, CollectionMetadataNotFoundException {
      String type = getAttributeType(collectionName, attribute);

      if (type.equals(COLLECTION_ATTRIBUTE_TYPE_INT)) {
         try {
            Integer.parseInt(valueString);
            return true;
         } catch (NumberFormatException e) {
            return false;
         }
      }

      if (type.equals(COLLECTION_ATTRIBUTE_TYPE_LONG)) {
         try {
            Long.parseLong(valueString);
            return true;
         } catch (NumberFormatException e) {
            return false;
         }
      }

      if (type.equals(COLLECTION_ATTRIBUTE_TYPE_DOUBLE)) {
         try {
            Double.parseDouble(valueString);
            return true;
         } catch (NumberFormatException e) {
            return false;
         }
      }

      if (type.equals(COLLECTION_ATTRIBUTE_TYPE_DATE)) { // we accept yyyy.MM.dd and yyyy.MM.dd HH.mm.ss
         return Utils.isValidDateFormatJustDate(valueString) || Utils.isValidDateFormatDateAndTimeMinutes(valueString);
      }

      if (type.equals(COLLECTION_ATTRIBUTE_TYPE_BOOLEAN)) {
         return valueString.equals("false") || valueString.equals("true");
      }

      if (type.equals(COLLECTION_ATTRIBUTE_TYPE_NESTED)) { // we cannot add value to nested attribute, just to its children
         return false;
      }

      // we return true when type is not specified or string
      return true;
   }

   //   public List<DataDocument> getAttributeConstraints(String collectionName, String attributeName) throws CollectionNotFoundException {
   //      String query = queryCollectionAttributeInfo(collectionName, attributeName);
   //      List<DataDocument> attributesInfo = dataStorage.run(query);
   //      if (attributesInfo.isEmpty()) {
   //         // TODO
   //         return null;
   //      }
   //
   //      DataDocument attributeInfo = attributesInfo.get(0);
   //      List<DataDocument> constraints = (List<DataDocument>) (attributeInfo.get(COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY));
   //
   //      return constraints;
   //   }

   //   public boolean addAttributeConstraint(String collectionName, String attributeName, String constraintType, String constraintValueString) {
   //      String id = getAttributeDocumentId(collectionName, attributeName);
   //
   //      String type = getCollectionAttributesNames(collectionName).get(attributeName);
   //      if (type == null) {
   //         return false; // attribute does not exist
   //      }
   //
   //      if (!COLLECTION_CONSTRAINT_TYPE_VALUES.contains(constraintType)) {
   //         return false; // attribute constraint type is invalid
   //      }
   //
   //      DataDocument constraintDocument;
   //      Map<String, Object> constraintEntry = new HashMap<String, Object>();
   //
   //      if (constraintType.equals(COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_GT) ||
   //            constraintType.equals(COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_LT) ||
   //            constraintType.equals(COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_GTE) ||
   //            constraintType.equals(COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_LTE)) {
   //         if (type.equals(COLLECTION_ATTRIBUTE_TYPE_INT)) {
   //            try {
   //               int value = Integer.parseInt(constraintValueString);
   //               constraintEntry.put(constraintType, value);
   //            } catch (NumberFormatException e) {
   //               return false;
   //            }
   //         } else if (type.equals(COLLECTION_ATTRIBUTE_TYPE_LONG)) {
   //            try {
   //               long value = Long.parseLong(constraintValueString);
   //               constraintEntry.put(constraintType, value);
   //            } catch (NumberFormatException e) {
   //               return false;
   //            }
   //         } else if (type.equals(COLLECTION_ATTRIBUTE_TYPE_DOUBLE)) {
   //            try {
   //               double value = Double.parseDouble(constraintValueString);
   //               constraintEntry.put(constraintType, value);
   //            } catch (NumberFormatException e) {
   //               return false;
   //            }
   //
   //         } else if (type.equals(COLLECTION_ATTRIBUTE_TYPE_DATE)) {
   //            if (Utils.isValidDateFormatJustDate(constraintValueString) || Utils.isValidDateFormatDateAndTimeMinutes(constraintValueString)) {
   //               constraintEntry.put(constraintType, constraintValueString);
   //            }
   //         } else if (type.equals(COLLECTION_ATTRIBUTE_TYPE_STRING)) {
   //            try {
   //               int value = Integer.parseInt(constraintValueString);
   //               constraintEntry.put(constraintType, value);
   //            } catch (NumberFormatException e) {
   //               return false;
   //            }
   //         }
   //      } else if (constraintType.equals(COLLECTION_ATTRIBUTE_CONSTRAINT_TYPE_REGEX)) {
   //         if (type.equals(COLLECTION_ATTRIBUTE_TYPE_STRING)) {
   //            constraintEntry.put(constraintType, constraintValueString);
   //         }
   //      }
   //
   //      if (constraintEntry.isEmpty()) {
   //         // we return false if the constraint is not valid
   //         return false;
   //      }
   //
   //      constraintDocument = new DataDocument(constraintEntry);
   //      // so far we can add only one constraint
   //      DataDocument attributeDocument = createNestedDocumentWithAttributeConstraint(attributeName, constraintDocument);
   //      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), attributeDocument, id, -1);
   //      return true;
   //   }
   //
   //   public boolean dropAttributeConstraint(String collectionName, String attributeName, String constraintType) {
   //      // TODO
   //      return false;
   //   }
   //
   //   private DataDocument createNestedDocumentWithAttributeConstraint(String attributeName, DataDocument constraintDocument) {
   //      Map<String, Object> attributeMap = new HashMap<String, Object>();
   //      List<DataDocument> constraintsList = new ArrayList<>();
   //      // TODO add already existing constraints
   //      constraintsList.add(constraintDocument);
   //      attributeMap.put(COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY, constraintDocument);
   //      return new DataDocument(attributeMap);
   //   }

   // returns id of the document with info about given attribute
   private String getAttributeDocumentId(String collectionName, String attributeName) throws CollectionNotFoundException {
      String query = queryCollectionAttributeInfo(collectionName, attributeName);
      List<DataDocument> attributeInfo = dataStorage.run(query);
      if (!attributeInfo.isEmpty()) {
         DataDocument attributeDocument = attributeInfo.get(0);
         return attributeDocument.getId();
      } else { // attribute doesn't exist
         return null;
      }
   }

   // returns MongoDb query for getting specific metadata value
   private String queryOneValueFromCollectionMetadata(String collectionName, String metaTypeValue) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      StringBuilder sb = new StringBuilder("{find:\"")
            .append(metadataCollectionName)
            .append("\",filter:{\"")
            .append(META_TYPE_KEY)
            .append("\":\"")
            .append(metaTypeValue)
            .append("\"}}");
      String query = sb.toString();
      return query;
   }

   // returns MongoDb query for getting info about specific attribute
   private String queryCollectionAttributeInfo(String collectionName, String attributeName) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

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

   private boolean checkIfUserCollectionExists(String originalCollectionName) throws CollectionMetadataNotFoundException, CollectionNotFoundException {
      List<String> collections = dataStorage.getAllCollections();
      for (String c : collections) {
         if (isUserCollection(c)) {
            if (getOriginalCollectionName(c).equals(originalCollectionName)) {
               return true;
            }
         }
      }
      return false;
   }

   private void checkIfMetadataCollectionExists(String metadataCollectionName) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(metadataCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(metadataCollectionName));
      }
   }
}
