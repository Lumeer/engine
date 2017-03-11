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
import io.lumeer.engine.api.constraint.Constraint;
import io.lumeer.engine.api.constraint.ConstraintManager;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.event.ChangeCollectionName;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.provider.DataStorageProvider;
import io.lumeer.engine.rest.dao.AccessRightsDao;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */

@SessionScoped
public class CollectionMetadataFacade implements Serializable {

   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dialect;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private Event<ChangeCollectionName> changeCollectionNameEvent;

   private ConstraintManager constraintManager;

   @Inject
   private DataStorageProvider dataStorageProvider;

   @PostConstruct
   public void init() {
      dataStorage = dataStorageProvider.getUserStorage();
      initConstraintManager();
   }

   /**
    * Initializes constraint manager.
    */
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
   @Named("systemConstraintManager")
   public ConstraintManager getConstraintManager() {
      return constraintManager;
   }

   // example of collection metadata structure:
   // -------------------------------------
   // {
   //  “meta-type” : “attributes”,
   //  “name” : “attribute1”,
   //  “type” : “number”,
   //  “constraints” : [constraintConfigurationString1, constraintConfigurationString1],
   // },
   // {
   //  “meta-type” : “attributes”,
   //  “name” : “attribute2”,
   //  “type” : “”,
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
   // “meta-type” : “rights”,
   // “create-user” : “me@mail.com”,
   // ... access rights by SecurityFacade ...
   //  }

   /**
    * Converts collection name given by user to internal representation.
    * First, the name is trimmed of whitespaces.
    * Spaces are replaced by "_". Converted to lowercase.
    * Diacritics are replaced by ASCII characters.
    * Everything except a-z, 0-9 and _ is removed.
    * Number is added to the end of the name to ensure it is unique.
    * The uniqueness of user name is not checked here, but is to be checked in CollectionFacade.
    *
    * @param originalCollectionName
    *       name given by user
    * @return internal collection name
    * @throws UserCollectionAlreadyExistsException
    *       when collection with given original name already exists
    */
   public String createInternalName(String originalCollectionName) throws UserCollectionAlreadyExistsException {
      if (checkIfUserCollectionExists(originalCollectionName)) {
         throw new UserCollectionAlreadyExistsException(ErrorMessageBuilder.userCollectionAlreadyExistsString(originalCollectionName));
      }

      String name = originalCollectionName.trim();
      name = name.replace(' ', '_');
      name = Utils.normalize(name);
      name = name.replaceAll("[^_a-z0-9]+", "");
      name = LumeerConst.Collection.COLLECTION_NAME_PREFIX + name;
      int i = 0;
      while (dataStorage.hasCollection(name + "_" + i)) {
         i++;
      }
      name = name + "_" + i;

      return name;
   }

   /**
    * Creates initial metadata in metadata collection - adds original name and initial time lock and access rights for creator.
    *
    * @param internalCollectionName
    *       internal collection name
    * @param collectionOriginalName
    *       name of collection given by user
    * @throws CollectionNotFoundException
    *       when metadata collection is not found
    */
   public void createInitialMetadata(String internalCollectionName, String collectionOriginalName) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(internalCollectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      // set name - we don't use setOriginalCollectionName, because that methods assumes document with name already exists
      DataDocument metadataName = new DataDocument(LumeerConst.Collection.META_TYPE_KEY, LumeerConst.Collection.COLLECTION_REAL_NAME_META_TYPE_VALUE)
            .append(LumeerConst.Collection.COLLECTION_REAL_NAME_KEY, collectionOriginalName);
      dataStorage.createDocument(metadataCollectionName, metadataName);

      // set create user and date and access rights for him
      String user = getCurrentUser();
      DataDocument metadataDocument = new DataDocument(LumeerConst.Collection.META_TYPE_KEY, LumeerConst.Collection.COLLECTION_RIGHTS_META_TYPE_VALUE)
            .append(LumeerConst.Collection.COLLECTION_CREATE_DATE_KEY, Utils.getCurrentTimeString())
            .append(LumeerConst.Collection.COLLECTION_CREATE_USER_KEY, user);

      securityFacade.setRightsRead(metadataDocument, user);
      securityFacade.setRightsWrite(metadataDocument, user);
      securityFacade.setRightsExecute(metadataDocument, user);
      dataStorage.createDocument(metadataCollectionName, metadataDocument);

      // set lock - we don't use setCollectionLockTime, because that methods assumes document with lock already exists
      DataDocument metadataLock = new DataDocument(LumeerConst.Collection.META_TYPE_KEY, LumeerConst.Collection.COLLECTION_LOCK_META_TYPE_VALUE)
            .append(LumeerConst.Collection.COLLECTION_LOCK_UPDATED_KEY, "");
      dataStorage.createDocument(metadataCollectionName, metadataLock);

      // we create indexes on frequently used fields
      int indexType = 1; // specifies ascending or descending index - https://docs.mongodb.com/manual/core/index-single/
      dataStorage.createIndex(metadataCollectionName, new DataDocument(LumeerConst.Collection.META_TYPE_KEY, indexType));
      dataStorage.createIndex(metadataCollectionName, new DataDocument(LumeerConst.Collection.COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY, indexType));
      dataStorage.createIndex(metadataCollectionName, new DataDocument(LumeerConst.Collection.COLLECTION_ATTRIBUTE_NAME_KEY, indexType));
      dataStorage.createIndex(metadataCollectionName, new DataDocument(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_KEY, indexType));
   }

   /**
    * Returns list of names of collection attributes.
    * We do not check access rights there, because the method is to be called only in CollectionFacade and they are checked there.
    *
    * @param collectionName
    *       internal collection name
    * @return list of collection attributes
    * @throws CollectionNotFoundException
    *       when metadata collection is not found
    */
   public List<String> getCollectionAttributesNames(String collectionName) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      List<DataDocument> attributesInfoDocuments = dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_ATTRIBUTES_META_TYPE_VALUE));

      List<String> attributes = new ArrayList<>();

      for (DataDocument attributesInfoDocument : attributesInfoDocuments) {
         String name = attributesInfoDocument.getString(LumeerConst.Collection.COLLECTION_ATTRIBUTE_NAME_KEY);
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
    *       when metadata collection is not found
    */
   public List<DataDocument> getCollectionAttributesInfo(String collectionName) throws CollectionNotFoundException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      checkIfMetadataCollectionExists(metadataCollectionName);

      return dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_ATTRIBUTES_META_TYPE_VALUE));
   }

   /**
    * Renames existing attribute in collection metadata.
    * This method should be called only when also renaming attribute in documents,
    * and access rights should be checked there so they are not checked twice.
    *
    * @param collectionName
    *       internal collection name
    * @param oldName
    *       old attribute name
    * @param newName
    *       new attribute name
    * @return true if rename is successful, false if some metadata do not exist and the rename could not be performed
    * @throws AttributeAlreadyExistsException
    *       when attribute with new name already exists
    */
   public boolean renameCollectionAttribute(String collectionName, String oldName, String newName) throws AttributeAlreadyExistsException {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);

      if (!dataStorage.hasCollection(metadataCollectionName)) { // metadata collection does not exist
         return false;
      }

      List<DataDocument> newAttributeInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, newName));

      // check if the attribute with new name already exists in the collection
      if (!newAttributeInfo.isEmpty()) {
         throw new AttributeAlreadyExistsException(ErrorMessageBuilder.attributeAlreadyExistsString(newName, collectionName));
      }

      List<DataDocument> attributeInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, oldName));

      // the old attribute does not exist
      if (attributeInfo.isEmpty()) {
         return false;
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.getId();

      if (!newName.isEmpty()) {
         dataStorage.updateDocument(metadataCollectionName, new DataDocument(LumeerConst.Collection.COLLECTION_ATTRIBUTE_NAME_KEY, newName), documentId);
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
    * @return true if retype is successful, false if new type is not valid or when metadata about attribute was not found
    */
   public boolean retypeCollectionAttribute(String collectionName, String attributeName, String newType) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);

      if (!dataStorage.hasCollection(metadataCollectionName)) { // metadata collection does not exist
         return false;
      }

      if (!LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_VALUES.contains(newType)) { // new type must be from our list
         return false;
      }

      List<DataDocument> attributeInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, attributeName));

      // attribute metadata does not exist
      if (attributeInfo.isEmpty()) {
         return false;
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.getId();

      dataStorage.updateDocument(metadataCollectionName, new DataDocument(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_KEY, newType), documentId);

      return true;
   }

   /**
    * Gets attribute type from metadata
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @return type of the attribute, default (String) if attribute is not found
    */
   public String getAttributeType(String collectionName, String attributeName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);

      if (!dataStorage.hasCollection(metadataCollectionName)) { // metadata collection does not exist
         return LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_STRING;
      }

      List<DataDocument> attributesInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, attributeName));

      if (attributesInfo.isEmpty()) { // attribute does not exist, we return default (String)
         return LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_STRING;
      }

      DataDocument attributeInfo = attributesInfo.get(0);
      String type = attributeInfo.get(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_KEY).toString();
      if (type == null) { // if type is not found, we return string as default
         return LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_STRING;
      }

      return type;
   }

   /**
    * Deletes an attribute from collection metadata. Nothing is done if attribute metadata is not found, just return.
    * This method should be called only when also renaming attribute in documents.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute to be deleted
    */
   public void dropCollectionAttribute(String collectionName, String attributeName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);

      if (!dataStorage.hasCollection(metadataCollectionName)) { // metadata collection does not exist
         return;
      }

      List<DataDocument> attributeInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, attributeName));

      // attribute metadata does not exist
      if (attributeInfo.isEmpty()) {
         return;
      }

      DataDocument attributeDocument = attributeInfo.get(0);
      String documentId = attributeDocument.getId();
      dataStorage.dropDocument(metadataCollectionName, documentId);
   }

   /**
    * Adds attribute to metadata collection, if the attribute already isn't there.
    * Otherwise just increments count. Nothing is done if metadata collection does not exist.
    * This should be called only when adding/updating document.
    *
    * @param collectionName
    *       internal collection name
    * @param attribute
    *       set of attributes' names
    */
   public void addOrIncrementAttribute(String collectionName, String attribute) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      if (!dataStorage.hasCollection(metadataCollectionName)) { // metadata collection does not exist
         return;
      }

      dataStorage.run(dialect.updateCollectionAttributeCountQuery(metadataCollectionName, attribute));
   }

   /**
    * Drops attribute if there is no document with that attribute in the collection (count is 1),
    * otherwise just decrements count. Nothing is done if attribute metadata is not found, just return.
    * This should be called only when adding/updating document.
    *
    * @param collectionName
    *       internal collection name
    * @param attribute
    *       set of attributes' names
    */
   public void dropOrDecrementAttribute(String collectionName, String attribute) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);

      if (!dataStorage.hasCollection(metadataCollectionName)) { // metadata collection does not exist
         return;
      }

      List<DataDocument> attributeInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, attribute));
      if (!attributeInfo.isEmpty()) { // in case somebody did that sooner, we may have nothing to remove
         DataDocument attributeDocument = attributeInfo.get(0);
         String documentId = attributeDocument.getId();

         // we check if this was the last document with the attribute
         if (attributeDocument.getInteger(LumeerConst.Collection.COLLECTION_ATTRIBUTE_COUNT_KEY) == 1) {
            dataStorage.dropDocument(metadataCollectionName, documentId);
         } else {
            dataStorage.incrementAttributeValueBy(metadataCollectionName, documentId, LumeerConst.Collection.COLLECTION_ATTRIBUTE_COUNT_KEY, -1);
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
    * @return attribute count, zero if the attribute does not exist
    */
   public long getAttributeCount(String collectionName, String attributeName) {
      if (!dataStorage.hasCollection(collectionMetadataCollectionName(collectionName))) { // metadata collection does not exist
         return 0;
      }

      List<DataDocument> countInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, attributeName));
      return countInfo.isEmpty() ? 0 : countInfo.get(0).getInteger(LumeerConst.Collection.COLLECTION_ATTRIBUTE_COUNT_KEY);
   }

   /**
    * Searches for original (given by user) collection name in metadata
    *
    * @param collectionName
    *       internal collection name
    * @return original collection name
    * @throws CollectionMetadataDocumentNotFoundException
    *       when document in metadata collection is not found
    * @throws CollectionNotFoundException
    *       when metadata collection is not found
    */
   public String getOriginalCollectionName(String collectionName) throws CollectionMetadataDocumentNotFoundException, CollectionNotFoundException {
      List<DataDocument> nameInfo = dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_REAL_NAME_META_TYPE_VALUE));

      if (nameInfo.isEmpty()) {
         throw new CollectionMetadataDocumentNotFoundException(ErrorMessageBuilder.collectionMetadataNotFoundString(collectionName, LumeerConst.Collection.COLLECTION_REAL_NAME_META_TYPE_VALUE));
      }

      DataDocument nameDocument = nameInfo.get(0);
      String name = nameDocument.getString(LumeerConst.Collection.COLLECTION_REAL_NAME_KEY);

      if (name == null) {
         throw new CollectionMetadataDocumentNotFoundException(ErrorMessageBuilder.collectionMetadataNotFoundString(collectionName, LumeerConst.Collection.COLLECTION_REAL_NAME_META_TYPE_VALUE));
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
    *       when collection with given user name is not found
    */
   public String getInternalCollectionName(String originalCollectionName) throws UserCollectionNotFoundException {
      List<String> collections = dataStorage.getAllCollections();
      for (String c : collections) {
         if (isUserCollection(c)) {
            try {
               if (getOriginalCollectionName(c).equals(originalCollectionName)) {
                  return c;
               }
               // we do not care if some other collection does not have original name or some problem with metadata
            } catch (CollectionMetadataDocumentNotFoundException | CollectionNotFoundException e) {
               continue;
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
    * @return true if the rename is successful, false if metadata collection was not found and the rename could not be performed
    * @throws UserCollectionAlreadyExistsException
    *       when collection with given user name already exists
    */
   public boolean setOriginalCollectionName(String collectionInternalName, String collectionOriginalName) throws UserCollectionAlreadyExistsException {
      if (checkIfUserCollectionExists(collectionOriginalName)) {
         throw new UserCollectionAlreadyExistsException(ErrorMessageBuilder.userCollectionAlreadyExistsString(collectionOriginalName));
      }

      if (!dataStorage.hasCollection(collectionMetadataCollectionName(collectionInternalName))) { // metadata collection does not exist
         return false;
      }

      List<DataDocument> nameInfo = dataStorage.run(queryDocumentFromCollectionMetadata(collectionInternalName, LumeerConst.Collection.COLLECTION_REAL_NAME_META_TYPE_VALUE));
      DataDocument nameDocument = nameInfo.get(0);
      String id = nameDocument.getId();

      DataDocument metadataDocument = new DataDocument(LumeerConst.Collection.COLLECTION_REAL_NAME_KEY, collectionOriginalName);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionInternalName), metadataDocument, id);

      changeCollectionNameEvent.fire(new ChangeCollectionName(collectionOriginalName, collectionInternalName));

      return true;
   }

   /**
    * Reads current value of collection lock
    *
    * @param collectionName
    *       internal collection name
    * @return String representation of the time of the last update of collection lock, empty string if lock is not set
    * @throws CollectionNotFoundException
    *       when metadata collection is not found
    * @throws CollectionMetadataDocumentNotFoundException
    *       when document in metadata collection is not found
    */
   public String getCollectionLockTime(String collectionName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      checkIfMetadataCollectionExists(collectionMetadataCollectionName(collectionName));

      List<DataDocument> lockInfo = dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_LOCK_META_TYPE_VALUE));

      if (lockInfo.isEmpty()) {
         throw new CollectionMetadataDocumentNotFoundException(ErrorMessageBuilder.collectionMetadataNotFoundString(collectionName, LumeerConst.Collection.COLLECTION_LOCK_META_TYPE_VALUE));
      }

      DataDocument nameDocument = lockInfo.get(0);
      return nameDocument.getString(LumeerConst.Collection.COLLECTION_LOCK_UPDATED_KEY);
   }

   /**
    * Sets collection lock to new value
    *
    * @param collectionName
    *       internal collection name
    * @param newTime
    *       String representation of the time of the last update of collection lock
    * @return true if set was successful, false if time format is wrong or metadata collection does not exist
    */
   public boolean setCollectionLockTime(String collectionName, String newTime) {
      if (!Utils.isValidDateFormat(newTime)) { // time format is not valid
         return false;
      }

      if (!dataStorage.hasCollection(collectionMetadataCollectionName(collectionName))) { // metadata collection does not exist
         return false;
      }

      List<DataDocument> lockInfo = dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_LOCK_META_TYPE_VALUE));
      DataDocument lockDocument = lockInfo.get(0);
      String id = lockDocument.getId();

      DataDocument metadataDocument = new DataDocument(LumeerConst.Collection.COLLECTION_LOCK_UPDATED_KEY, newTime);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), metadataDocument, id);
      return true;
   }

   /**
    * Gets document with custom metadata.
    *
    * @param collectionName
    *       internal name
    * @return DataDocument with all custom metadata values
    */
   public DataDocument getCustomMetadata(String collectionName) {
      if (!dataStorage.hasCollection(collectionMetadataCollectionName(collectionName))) { // metadata collection does not exist
         return new DataDocument(); // return blank document
      }

      List<DataDocument> customMetadataList = dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_CUSTOM_META_TYPE_VALUE));

      if (customMetadataList.isEmpty()) {
         return new DataDocument(); // return blank document
      }

      return customMetadataList.get(0);
   }

   /**
    * Adds all pairs key:value to custom metadata
    *
    * @param collectionName
    *       internal name
    * @param metadataDocument
    *       document with metadata values
    * @return true when update was successful, false when metadata collection does not exist
    */
   public boolean setCustomMetadata(String collectionName, DataDocument metadataDocument) {
      if (!dataStorage.hasCollection(collectionMetadataCollectionName(collectionName))) { // metadata collection does not exist
         return false;
      }

      List<DataDocument> customMetadataList = dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_CUSTOM_META_TYPE_VALUE));

      if (customMetadataList.isEmpty()) { // document with custom metadata does not exist - we create it
         metadataDocument.put(LumeerConst.Collection.META_TYPE_KEY, LumeerConst.Collection.COLLECTION_CUSTOM_META_TYPE_VALUE);
         dataStorage.createDocument(collectionMetadataCollectionName(collectionName), metadataDocument);
         return true;
      }

      DataDocument customMetadataDocument = customMetadataList.get(0);
      String id = customMetadataDocument.getId();

      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), metadataDocument, id);

      return true;
   }

   /**
    * Drops all custom metadata values associated with given keys.
    *
    * @param collectionName
    *       internal name
    * @param keys
    *       list of metadata keys to drop
    * @return false when metadata collection or custom metadata document does not exist, true otherwise
    */
   public boolean dropCustomMetadata(String collectionName, List<String> keys) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);

      if (!dataStorage.hasCollection(metadataCollectionName)) { // metadata collection does not exist
         return false;
      }

      List<DataDocument> customMetadataList = dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_CUSTOM_META_TYPE_VALUE));

      if (customMetadataList.isEmpty()) { // document with custom metadata does not exist - we have nothing to drop
         return false;
      }

      DataDocument customMetadataDocument = customMetadataList.get(0);
      String id = customMetadataDocument.getId();

      for (String key : keys) {
         dataStorage.dropAttribute(metadataCollectionName, id, key);
      }

      return true;
   }

   /**
    * @param collectionName
    *       internal collection name
    * @return name of metadata collection
    */
   public String collectionMetadataCollectionName(String collectionName) {
      return LumeerConst.Collection.COLLECTION_METADATA_PREFIX + collectionName;
   }

   /**
    * @param collectionName
    *       internal collection name
    * @return true if the name is a name of "classical" collection containing data from user
    */
   public boolean isUserCollection(String collectionName) {
      return collectionName != null && collectionName.length() >= LumeerConst.Collection.COLLECTION_NAME_PREFIX.length() && collectionName.startsWith(LumeerConst.Collection.COLLECTION_NAME_PREFIX) && !collectionName.endsWith(".shadow");
   }

   /**
    * Checks whether value is of good type and satisfies all constraints (and tries to fix it when possible).
    *
    * @param collectionName
    *       internal collection name
    * @param attribute
    *       attribute name
    * @param valueObject
    *       attribute value
    * @return null when the value is not valid, fixed value when the value is fixable, original value when the value is valid
    */
   public Object checkAndConvertAttributeValue(String collectionName, String attribute, Object valueObject) {
      List<String> constraintConfigurations = getAttributeConstraintsConfigurationsWithoutAccessRightsCheck(collectionName, attribute);
      valueObject = checkAttributeConstraints(valueObject, constraintConfigurations);
      if (valueObject == null) { // value does not satisfy constraints and could not be fixed
         return null;
      }

      String type = getAttributeType(collectionName, attribute);

      // Date type is special - we maintain it with constraint, so we have to do special check here.
      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_DATE)) {
         return checkValueDateAndConvert(valueObject.toString(), constraintConfigurations);
      }

      return checkAttributeTypeAndConvert(valueObject, type);
   }

   private Date checkValueDateAndConvert(String valueString, List<String> constraintConfigurations) {
      // If there exist no constraint for the date, it means that it passes constraint test, although it can be invalid,
      // so we will to check it against default time and date format.
      if (constraintConfigurations.isEmpty()) {
         try {
            return Utils.getDate(valueString);
         } catch (ParseException e) { // date could not be parsed
            return null;
         }
      } else {
         // we take the last constraint, because string could be fixed according to that constraint
         String constraint = constraintConfigurations.get(constraintConfigurations.size() - 1);
         String format = constraint.split(":", 2)[1];
         try {
            return new SimpleDateFormat(format).parse(valueString);
         } catch (ParseException e) { // date could not be parsed
            return null;
         }
      }
   }

   /**
    * Calls checkAndConvertAttributeValue(String collectionName, String attribute, String valueString) on every attribute of given document.
    *
    * @param collectionName
    *       internal collection name
    * @param document
    *       document with attributes and their values to check
    * @return map of results, key is attribute name and value is result of checkAndConvertAttributeValue on that attribute
    */
   public Map<String, Object> checkAndConvertAttributesValues(String collectionName, DataDocument document) {
      Map<String, Object> results = new HashMap<>();

      Set<String> attributes = document.keySet();
      for (String attribute : attributes) {
         Object value = document.get(attribute);
         results.put(attribute, checkAndConvertAttributeValue(collectionName, attribute, value));
      }

      return results;
   }

   // checks whether value is of given type and converts it to given type
   private Object checkAttributeTypeAndConvert(Object valueObject, String type) {
      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_LIST)) {
         if (valueObject instanceof List) {
            return valueObject;
         } else {
            return null;
         }
      }

      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_NESTED)) {
         if (valueObject instanceof DataDocument) {
            return valueObject;
         } else {
            return null;
         }
      }

      String valueString = valueObject.toString();

      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_INT)) {
         try {
            return Integer.parseInt(valueString);
         } catch (NumberFormatException e) {
            return null;
         }
      }

      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_LONG)) {
         try {
            return Long.parseLong(valueString);
         } catch (NumberFormatException e) {
            return null;
         }
      }

      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_DOUBLE)) {
         try {
            valueString = valueString.replace(',', '.'); // in case coma is used instead of decimal dot
            return Double.parseDouble(valueString);
         } catch (NumberFormatException e) {
            return null;
         }
      }

      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_DECIMAL)) {
         // TODO
      }

      // Date is special and its format is maintained in checkAttributeValue
      // if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_DATE))

      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_BOOLEAN)) { // we accept "true" and "false" ignoring the case
         if (LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_BOOLEAN_VALUES.contains(valueString.toLowerCase())) {
            return Boolean.parseBoolean(valueString);
         }
         return null;
      }

      return valueString; // for a string, we accept everything, so we do not check it here
   }

   /**
    * @param value
    *       value to be checked
    * @param type
    *       type to be checked
    * @return true if value satisfies given type
    */
   public Object checkValueTypeAndConvert(Object value, String type) {
      if (type.equals(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_DATE)) {
         return checkValueDateAndConvert(value.toString(), Collections.emptyList());
      }
      return checkAttributeTypeAndConvert(value, type);
   }

   // checks whether value satisfies all constraints
   private Object checkAttributeConstraints(Object valueObject, List<String> constraintConfigurations) {
      if (constraintConfigurations == null || constraintConfigurations.isEmpty()) { // there are no constraints
         return valueObject;
      }

      ConstraintManager constraintManager = null;
      try {
         constraintManager = new ConstraintManager(constraintConfigurations);
         initConstraintManager(constraintManager);
      } catch (InvalidConstraintException e) {
         throw new IllegalStateException("Illegal constraint prefix collision: ", e);
      }

      String valueString = valueObject.toString();
      Constraint.ConstraintResult result = constraintManager.isValid(valueString);

      if (result == Constraint.ConstraintResult.INVALID) {
         return null;
      }

      if (result == Constraint.ConstraintResult.FIXABLE) {
         return constraintManager.fix(valueString);
      }

      return valueString;
   }

   /**
    * Reads constraint for the given attribute.
    *
    * @param collectionName
    *       collection internal name
    * @param attributeName
    *       name of the attribute
    * @return list of constraint configurations for given attribute, empty list if constraints were not found
    */
   public List<String> getAttributeConstraintsConfigurations(String collectionName, String attributeName) {
      List<String> constraints = getAttributeConstraintsConfigurationsWithoutAccessRightsCheck(collectionName, attributeName);
      return constraints == null ? Collections.emptyList() : constraints;
   }

   // to be used only internally, when checking attribute value
   private List<String> getAttributeConstraintsConfigurationsWithoutAccessRightsCheck(String collectionName, String attributeName) {
      if (!dataStorage.hasCollection(collectionMetadataCollectionName(collectionName))) { // metadata collection does not exist
         return null;
      }

      List<DataDocument> attributesInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, attributeName));
      if (attributesInfo.isEmpty()) { // metadata for the attribute was not found
         return null;
      }

      DataDocument attributeInfo = attributesInfo.get(0);

      return attributeInfo.getArrayList(LumeerConst.Collection.COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY, String.class);
   }

   /**
    * Adds new constraint for an attribute and checks if it is valid.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       string with constraint configuration
    * @throws InvalidConstraintException
    *       when new constraint is not valid or is in conflict with existing constraints
    */
   public void addAttributeConstraint(String collectionName, String attributeName, String constraintConfiguration) throws InvalidConstraintException {
      // user may be permitted to write, but might not be permitted to read
      List<String> existingConstraints = getAttributeConstraintsConfigurationsWithoutAccessRightsCheck(collectionName, attributeName);

      ConstraintManager constraintManager = null;
      try {
         constraintManager = new ConstraintManager(existingConstraints);
         initConstraintManager(constraintManager);
      } catch (InvalidConstraintException e) { // thrown when already existing constraints are in conflict
         throw new IllegalStateException("Illegal constraint prefix collision: ", e);
      }

      constraintManager.registerConstraint(constraintConfiguration); // if this doesn't throw an exception, the constraint is valid

      // TODO: update whole array because of concurrent access?
      String attributeDocumentId = getAttributeDocumentId(collectionName, attributeName);
      dataStorage.addItemToArray(collectionMetadataCollectionName(collectionName), attributeDocumentId, LumeerConst.Collection.COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY, constraintConfiguration);
   }

   /**
    * Removes the constraint from list of constraints for given attribute
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       constraint configuration to be removed
    */
   public void dropAttributeConstraint(String collectionName, String attributeName, String constraintConfiguration) {
      String attributeDocumentId = getAttributeDocumentId(collectionName, attributeName);
      dataStorage.removeItemFromArray(collectionMetadataCollectionName(collectionName), attributeDocumentId, LumeerConst.Collection.COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY, constraintConfiguration);
   }

   /**
    * @param collectionName
    *       internal collection name
    * @param user
    *       user name
    * @return true if user can read the collection
    */
   public boolean checkCollectionForRead(String collectionName, String user) {
      DataDocument rights = getAccessRightsDocument(collectionName);
      return rights == null || securityFacade.checkForRead(rights, user);
   }

   /**
    * @param collectionName
    *       internal collection name
    * @param user
    *       user name
    * @return true if user can write to the collection
    */
   public boolean checkCollectionForWrite(String collectionName, String user) {
      DataDocument rights = getAccessRightsDocument(collectionName);
      return rights == null || securityFacade.checkForWrite(rights, user);
   }

   /**
    * @param collectionName
    *       internal collection name
    * @param user
    *       user name
    * @return true if user can change access rights to collection
    */
   public boolean checkCollectionForAccessChange(String collectionName, String user) {
      DataDocument rights = getAccessRightsDocument(collectionName);
      return rights == null || securityFacade.checkForExecute(rights, user);
   }

   /**
    * Sets read right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user to set right for
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to change rights for the collection
    */
   public void addCollectionRead(String collectionName, String user) throws UnauthorizedAccessException {
      DataDocument rights = getAccessRightsDocument(collectionName);
      securityFacade.setRightsRead(rights, user);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), rights, rights.getId());
   }

   /**
    * Sets write right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user to set right for
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to change rights for the collection
    */
   public void addCollectionWrite(String collectionName, String user) throws UnauthorizedAccessException {
      DataDocument rights = getAccessRightsDocument(collectionName);
      securityFacade.setRightsWrite(rights, user);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), rights, rights.getId());
   }

   /**
    * Sets execute (right to change rights) right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user to set right for
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to change rights for the collection
    */
   public void addCollectionAccessChange(String collectionName, String user) throws UnauthorizedAccessException {
      DataDocument rights = getAccessRightsDocument(collectionName);
      securityFacade.setRightsExecute(rights, user);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), rights, rights.getId());
   }

   /**
    * Removes read right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user whose right is removed
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to change rights for the collection
    */
   public void removeCollectionRead(String collectionName, String user) throws UnauthorizedAccessException {
      DataDocument rights = getAccessRightsDocument(collectionName);
      securityFacade.removeRightsRead(rights, user);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), rights, rights.getId());
   }

   /**
    * Removes write right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user whose right is removed
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to change rights for the collection
    */
   public void removeCollectionWrite(String collectionName, String user) throws UnauthorizedAccessException {
      DataDocument rights = getAccessRightsDocument(collectionName);
      securityFacade.removeRightsWrite(rights, user);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), rights, rights.getId());
   }

   /**
    * Removes execute (right to change rights) right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user whose right is removed
    * @throws UnauthorizedAccessException
    *       when current user is not allowed to change rights for the collection
    */
   public void removeCollectionAccessChange(String collectionName, String user) throws UnauthorizedAccessException {
      DataDocument rights = getAccessRightsDocument(collectionName);
      securityFacade.removeRightsExecute(rights, user);
      dataStorage.updateDocument(collectionMetadataCollectionName(collectionName), rights, rights.getId());
   }

   /**
    * Gets access rights for all users.
    *
    * @param collectionName
    *       internal name
    * @return list of AccessRightsDao (Daos for all users)
    */
   public List<AccessRightsDao> getAllAccessRights(String collectionName) {
      return securityFacade.getDaoList(getAccessRightsDocument(collectionName));
   }

   // returns whole access rights document - to be used only internally
   private DataDocument getAccessRightsDocument(String collectionName) {
      if (!dataStorage.hasCollection(collectionMetadataCollectionName(collectionName))) { // metadata collection does not exist
         return null;
      }

      List<DataDocument> rightsInfo = dataStorage.run(queryDocumentFromCollectionMetadata(collectionName, LumeerConst.Collection.COLLECTION_RIGHTS_META_TYPE_VALUE));

      return rightsInfo.isEmpty() ? null : rightsInfo.get(0);
   }

   // returns id of the document with info about given attribute
   private String getAttributeDocumentId(String collectionName, String attributeName) {
      List<DataDocument> attributeInfo = dataStorage.run(queryCollectionAttributeInfo(collectionName, attributeName));
      return attributeInfo.isEmpty() ? null : attributeInfo.get(0).getId();
   }

   // returns query for getting specific metadata document
   private DataDocument queryDocumentFromCollectionMetadata(String collectionName, String metaTypeValue) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      return new DataDocument()
            .append("find", metadataCollectionName)
            .append("filter",
                  new DataDocument()
                        .append(LumeerConst.Collection.META_TYPE_KEY, metaTypeValue));
   }

   // returns query for getting info about specific attribute
   private DataDocument queryCollectionAttributeInfo(String collectionName, String attributeName) {
      String metadataCollectionName = collectionMetadataCollectionName(collectionName);
      return new DataDocument()
            .append("find", metadataCollectionName)
            .append("filter",
                  new DataDocument()
                        .append(LumeerConst.Collection.META_TYPE_KEY, LumeerConst.Collection.COLLECTION_ATTRIBUTES_META_TYPE_VALUE)
                        .append(LumeerConst.Collection.COLLECTION_ATTRIBUTE_NAME_KEY, attributeName));
   }

   // checks whether collection with given user name already exists
   private boolean checkIfUserCollectionExists(String originalCollectionName) {
      dataStorage.invalidateCaches(); // cache can be 5 seconds old, so we firstly invalidate it
      List<String> collections = dataStorage.getAllCollections();
      for (String c : collections) {
         if (isUserCollection(c)) {
            try {
               if (getOriginalCollectionName(c).equals(originalCollectionName)) {
                  return true;
               }
            } catch (CollectionMetadataDocumentNotFoundException e) {
               return false; // original name for the collection c was not found, so it does not exist
            } catch (CollectionNotFoundException e) {
               return false; // metadata for the collection c was not found, so its original name does not exist
            }
         }
      }
      return false;
   }

   // checks whether metadata collection exists
   private void checkIfMetadataCollectionExists(String metadataCollectionName) throws CollectionNotFoundException {
      if (!dataStorage.hasCollection(metadataCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(metadataCollectionName));
      }
   }

   // initializes constraint manager
   private void initConstraintManager(ConstraintManager constraintManager) {
      constraintManager.setLocale(Locale.forLanguageTag(configurationFacade.getConfigurationString(LumeerConst.USER_LOCALE_PROPERTY).orElse("en-US")));
   }

   // returns current user email
   private String getCurrentUser() {
      return userFacade.getUserEmail();
   }
}
