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
import io.lumeer.engine.api.constraint.Constraint;
import io.lumeer.engine.api.constraint.ConstraintManager;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.event.ChangeCollectionName;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.rest.dao.AccessRightsDao;
import io.lumeer.engine.rest.dao.Attribute;
import io.lumeer.engine.rest.dao.CollectionMetadata;
import io.lumeer.engine.util.ErrorMessageBuilder;
import io.lumeer.engine.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
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

   @Inject
   @UserDataStorage
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
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private Event<ChangeCollectionName> changeCollectionNameEvent;

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
   @Named("systemConstraintManager")
   public ConstraintManager getConstraintManager() {
      return constraintManager;
   }

   /**
    * Gets metadata document of given collection.
    *
    * @param collectionName
    *       internal collection name
    * @return DataDocument with collection metadata
    * @throws CollectionMetadataDocumentNotFoundException when metadata document is not found
    */
   public DataDocument getCollectionMetadataDocument(String collectionName) throws CollectionMetadataDocumentNotFoundException {
      DataDocument metadata = dataStorage.readDocument(
            metadataCollection(),
            dialect.fieldValueFilter(LumeerConst.Collection.INTERNAL_NAME_KEY, collectionName));

      if (metadata == null) {
         throw new CollectionMetadataDocumentNotFoundException(ErrorMessageBuilder.collectionMetadataNotFoundString(collectionName));
      }

      return metadata;
   }

   /**
    * Gets object with collection metadata.
    *
    * @param collectionName
    *       internal collection name
    * @return object with collection metadata
    */
   public CollectionMetadata getCollectionMetadata(String collectionName) {
      try {
         DataDocument metadata = getCollectionMetadataDocument(collectionName);
         return new CollectionMetadata(metadata);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return new CollectionMetadata();
      }
   }

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
      name = LumeerConst.Collection.NAME_PREFIX + name;
      int i = 0;
      while (dataStorage.hasCollection(name + "_" + i)) {
         i++;
      }
      name = name + "_" + i;

      return name;
   }

   /**
    * Creates initial metadata in metadata collection.
    *
    * @param internalCollectionName
    *       internal collection name
    * @param originalCollectionName
    *       name of collection given by user
    */
   public void createInitialMetadata(String internalCollectionName, String originalCollectionName) {
      String user = getCurrentUser();
      DataDocument collectionMetadata = new DataDocument()
            .append(LumeerConst.Collection.REAL_NAME_KEY, originalCollectionName)
            .append(LumeerConst.Collection.INTERNAL_NAME_KEY, internalCollectionName)
            .append(LumeerConst.Collection.ATTRIBUTES_KEY, new DataDocument())
            .append(LumeerConst.Collection.LAST_TIME_USED_KEY, new Date())
            .append(LumeerConst.Collection.RECENTLY_USED_DOCUMENTS_KEY, new LinkedList<>())
            .append(LumeerConst.Collection.CREATE_DATE_KEY, new Date())
            .append(LumeerConst.Collection.CREATE_USER_KEY, user)
            .append(LumeerConst.Collection.CUSTOM_META_KEY, new DataDocument());

      securityFacade.setRightsRead(collectionMetadata, user);
      securityFacade.setRightsWrite(collectionMetadata, user);
      securityFacade.setRightsExecute(collectionMetadata, user);
      dataStorage.createDocument(metadataCollection(), collectionMetadata);
   }

   /**
    * Gets set of names of collection attributes.
    *
    * @param collectionName
    *       internal collection name
    * @return set of collection attributes' names
    */
   public Set<String> getAttributesNames(String collectionName) {
      return getAttributesInfo(collectionName).keySet();
   }

   /**
    * Gets complete info about collection attributes.
    *
    * @param collectionName
    *       internal collection name
    * @return map, keys are attributes' names, values are objects with attributes info
    */
   public Map<String, Attribute> getAttributesInfo(String collectionName) {
      return getCollectionMetadata(collectionName).getAttributes();
   }

   /**
    * Gets complete info about one attribute.
    *
    * @param collection
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @return Attribute object
    */
   public Attribute getAttributeInfo(String collection, String attributeName) {
      List<String> keys = divideAttributeName(attributeName);
      if (keys.isEmpty()) {
         return null;
      }

      Attribute attribute = getCollectionMetadata(collection).getAttributes().get(keys.get(0));

      for (int i = 1; i < keys.size(); i++) {
         if (attribute == null) {
            break;
         }
         attribute = attribute.getChildAttributes().get(keys.get(i));
      }

      return attribute;
   }

   private List<String> divideAttributeName(String attribute) {
      return Arrays.asList(attribute.split("\\."));
   }

   private String attributePath(String attribute) {
      return attribute.replaceAll("\\.",
            "."
                  + LumeerConst.Collection.ATTRIBUTE_CHILDREN_KEY
                  + ".");
   }

   /**
    * Renames existing attribute in collection metadata.
    * This method should be called only when also renaming attribute in all collection documents.
    *
    * @param collectionName
    *       internal collection name
    * @param oldName
    *       old attribute name
    * @param newName
    *       new attribute name
    * @throws AttributeAlreadyExistsException
    *       when attribute with new name already exists
    */
   public void renameAttribute(String collectionName, String oldName, String newName) throws AttributeAlreadyExistsException {
      if (getAttributeInfo(collectionName, newName) != null) {
         throw new AttributeAlreadyExistsException(ErrorMessageBuilder.attributeAlreadyExistsString(newName, collectionName));
      }

      String oldNamePath = attributePath(oldName);
      String newNamePath = attributePath(newName);

      DataDocument renameQuery = dialect.renameAttributeQuery(metadataCollection(), collectionName, oldNamePath, newNamePath);

      dataStorage.run(renameQuery);
   }

   /**
    * Deletes an attribute from collection metadata. Nothing is done if attribute metadata is not found, just return.
    * This method should be called only when also renaming attribute in all collection documents.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute to be dropped
    */
   public void dropAttribute(String collectionName, String attributeName) {
      try {
         dataStorage.dropAttribute(
               metadataCollection(),
               dialect.documentIdFilter(getCollectionMetadataDocument(collectionName).getId()),
               dialect.concatFields(
                     LumeerConst.Collection.ATTRIBUTES_KEY,
                     attributePath(attributeName)));
      } catch (CollectionMetadataDocumentNotFoundException e) {
         // do nothing
      }
   }

   /**
    * Adds attribute to metadata collection, if the attribute already isn't there.
    * Otherwise just increments count.
    * This should be called only when adding/updating document.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute's name
    */
   public void addOrIncrementAttribute(String collectionName, String attributeName) {
      String documentId = null;
      try {
         documentId = getCollectionMetadataDocument(collectionName).getId();
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      Attribute attribute = getAttributeInfo(collectionName, attributeName);

      if (attribute != null) {
         dataStorage.incrementAttributeValueBy(
               metadataCollection(),
               dialect.documentIdFilter(documentId),
               dialect.concatFields(
                     LumeerConst.Collection.ATTRIBUTES_KEY,
                     attributePath(attributeName),
                     LumeerConst.Collection.ATTRIBUTE_COUNT_KEY),
               1);
      } else {
         List<String> dividedName = divideAttributeName(attributeName);
         dataStorage.updateDocument(
               metadataCollection(),
               new DataDocument(
                     dialect.concatFields(
                           LumeerConst.Collection.ATTRIBUTES_KEY,
                           attributePath(attributeName)),
                     new DataDocument()
                           .append(
                                 LumeerConst.Collection.ATTRIBUTE_NAME_KEY,
                                 dividedName.get(dividedName.size() - 1))
                           .append(
                                 LumeerConst.Collection.ATTRIBUTE_CONSTRAINTS_KEY,
                                 new ArrayList<String>())
                           .append(
                                 LumeerConst.Collection.ATTRIBUTE_COUNT_KEY, 1)
                           .append(
                                 LumeerConst.Collection.ATTRIBUTE_CHILDREN_KEY,
                                 new HashMap<String, Attribute>())
               ),
               dialect.documentIdFilter(documentId));
      }
   }

   /**
    * Drops attribute if there is no other document with that attribute in the collection (count is 1),
    * otherwise just decrements count.
    * This should be called only when adding/updating document.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       set of attributes' names
    */
   public void dropOrDecrementAttribute(String collectionName, String attributeName) {
      String documentId = null;
      try {
         documentId = getCollectionMetadataDocument(collectionName).getId();
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      Attribute attribute = getAttributeInfo(collectionName, attributeName);

      if (attribute == null) {
         return;
      }

      if (attribute.getCount() <= 1L) {
         dropAttribute(collectionName, attributeName);
         return;
      }

      dataStorage.incrementAttributeValueBy(
            metadataCollection(),
            dialect.documentIdFilter(documentId),
            dialect.concatFields(
                  LumeerConst.Collection.ATTRIBUTES_KEY,
                  attributePath(attributeName),
                  LumeerConst.Collection.ATTRIBUTE_COUNT_KEY),
            -1);
   }

   /**
    * Returns count for specific attribute.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @return attribute count, zero if the attribute does not exist
    */
   public int getAttributeCount(String collectionName, String attributeName) {
      Attribute attribute = getAttributeInfo(collectionName, attributeName);
      return attribute == null ? 0 : attribute.getCount();
   }

   /**
    * Searches for original (given by user) collection name in metadata.
    *
    * @param collectionName
    *       internal collection name
    * @return original collection name
    */
   public String getOriginalCollectionName(String collectionName) {
      return getCollectionMetadata(collectionName).getName();
   }

   /**
    * Searches for internal representation of collection name.
    *
    * @param originalCollectionName
    *       original collection name
    * @return internal representation of collection name
    * @throws UserCollectionNotFoundException
    *       when collection with given user name is not found
    */
   public String getInternalCollectionName(String originalCollectionName) throws UserCollectionNotFoundException {
      DataDocument metadata = dataStorage.readDocument(
            metadataCollection(),
            dialect.fieldValueFilter(LumeerConst.Collection.REAL_NAME_KEY,
                  originalCollectionName));

      if (metadata == null) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(originalCollectionName));
      }

      return metadata.getString(LumeerConst.Collection.INTERNAL_NAME_KEY);
   }

   /**
    * Sets original (given by user) collection name in metadata.
    *
    * @param collectionInternalName
    *       internal collection name
    * @param collectionOriginalName
    *       name given by user
    * @throws UserCollectionAlreadyExistsException
    *       when collection with given user name already exists
    */
   public void setOriginalCollectionName(String collectionInternalName, String collectionOriginalName) throws UserCollectionAlreadyExistsException {
      DataDocument collectionInfo = null;
      try {
         collectionInfo = getCollectionMetadataDocument(collectionInternalName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      if (checkIfUserCollectionExists(collectionOriginalName)) {
         throw new UserCollectionAlreadyExistsException(ErrorMessageBuilder.userCollectionAlreadyExistsString(collectionOriginalName));
      }

      String documentId = collectionInfo.getId();
      dataStorage.updateDocument(
            metadataCollection(),
            new DataDocument(
                  LumeerConst.Collection.REAL_NAME_KEY,
                  collectionOriginalName),
            dialect.documentIdFilter(documentId));

      setLastTimeUsedNow(collectionInternalName);
      changeCollectionNameEvent.fire(new ChangeCollectionName(collectionOriginalName, collectionInternalName));
   }

   /**
    * Reads time of last collection usage.
    *
    * @param collectionName
    *       internal collection name
    * @return String representation of the time
    */
   public Date getLastTimeUsed(String collectionName) {
      return getCollectionMetadata(collectionName).getLastTimeUsed();
   }

   /**
    * Sets the time of last collection usage to current time
    *
    * @param collectionName
    *       internal collection name
    */
   public void setLastTimeUsedNow(String collectionName) {
      DataDocument collectionInfo = null;
      try {
         collectionInfo = getCollectionMetadataDocument(collectionName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return; // do nothing
      }

      String documentId = collectionInfo.getId();
      dataStorage.updateDocument(
            metadataCollection(),
            new DataDocument(
                  LumeerConst.Collection.LAST_TIME_USED_KEY,
                  new Date()),
            dialect.documentIdFilter(documentId));
   }

   /**
    * Gets document with custom metadata.
    *
    * @param collectionName
    *       internal name
    * @return DataDocument with all custom metadata values
    */
   public DataDocument getCustomMetadata(String collectionName) {
      return getCollectionMetadata(collectionName).getCustomMetadata();
   }

   /**
    * Adds all pairs key:value to custom metadata.
    *
    * @param collectionName
    *       internal name
    * @param metadata
    *       custom metadata
    */
   public void setCustomMetadata(String collectionName, DataDocument metadata) {
      DataDocument collectionInfo = null;
      try {
         collectionInfo = getCollectionMetadataDocument(collectionName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }
      String documentId = collectionInfo.getId();

      DataDocument metadataDocument = new DataDocument();

      for (String key : metadata.keySet()) {
         metadataDocument.append(dialect.concatFields(LumeerConst.Collection.CUSTOM_META_KEY, key), metadata.get(key));
      }

      dataStorage.updateDocument(metadataCollection(), metadataDocument, dialect.documentIdFilter(documentId));
      setLastTimeUsedNow(collectionName);
   }

   /**
    * Drops all custom metadata value associated with given key.
    *
    * @param collectionName
    *       internal name
    * @param key
    *       list of metadata to drop
    */
   public void dropCustomMetadata(String collectionName, String key) {
      String documentId = null;
      try {
         documentId = getCollectionMetadataDocument(collectionName).getId();
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      dataStorage.dropAttribute(
            metadataCollection(),
            dialect.documentIdFilter(documentId),
            dialect.concatFields(
                  LumeerConst.Collection.CUSTOM_META_KEY,
                  key));
      setLastTimeUsedNow(collectionName);
   }

   /**
    * Check whether the name is name of user collection.
    *
    * @param collectionName
    *       internal collection name
    * @return true if the name is a name of "classical" collection containing data from user
    */
   public boolean isUserCollection(String collectionName) {
      return collectionName != null &&
            collectionName.length() >= LumeerConst.Collection.NAME_PREFIX.length() &&
            collectionName.startsWith(LumeerConst.Collection.NAME_PREFIX);
   }

   /**
    * Checks whether value satisfies all constraints (and tries to fix it when possible).
    *
    * @param attribute
    *       attribute name
    * @param valueObject
    *       attribute value
    * @return null when the value is not valid, fixed value when the value is fixable, original value when the value is valid
    */
   private Object checkAndConvertAttributeValue(Attribute attribute, Object valueObject) {
      // if the value is DataDocument, we check it recursively
      if (valueObject instanceof DataDocument) {

         DataDocument beforeCheck = (DataDocument) valueObject;
         DataDocument afterCheck = new DataDocument();

         Map<String, Attribute> attributes = attribute.getChildAttributes();

         for (String key : beforeCheck.keySet()) {
            if (!attributes.keySet().contains(key)) { // attribute does not exist - no need to check anything
               afterCheck.put(key, beforeCheck.get(key));
            } else {
               Object newValue = checkAndConvertAttributeValue(attributes.get(key), beforeCheck.get(key));
               afterCheck.put(key, newValue);
            }
         }

         return afterCheck;
      }

      List<String> constraintConfigurations = attribute.getConstraints();
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
    * Checks value of every attribute of given document.
    *
    * @param collectionName
    *       internal collection name
    * @param document
    *       document with attributes and their values to check
    * @return map of results, key is attribute name and value is result of checkAndConvertAttributeValue on that attribute
    */
   public DataDocument checkAndConvertAttributesValues(String collectionName, DataDocument document) {
      DataDocument results = new DataDocument();

      Set<String> attributes = document.keySet();
      Map<String, Attribute> attributesMetadata = getCollectionMetadata(collectionName).getAttributes();
      for (String a : attributes) {
         Object value = document.get(a);
         if (!attributesMetadata.keySet().contains(a)) { // attribute does not exist - no need to check anything
            results.append(a, value);
         } else {
            results.append(a, checkAndConvertAttributeValue(attributesMetadata.get(a), value));
         }
      }

      return results;
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
      Attribute attribute = getAttributeInfo(collectionName, attributeName);

      if (attribute == null) {
         return Collections.emptyList();
      }

      return attribute.getConstraints();
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
      String docId = null;
      try {
         docId = getCollectionMetadataDocument(collectionName).getId();
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      // user may be permitted to write, but might not be permitted to read
      List<String> existingConstraints = getAttributeConstraintsConfigurations(collectionName, attributeName);

      ConstraintManager constraintManager = null;
      try {
         constraintManager = new ConstraintManager(existingConstraints);
         initConstraintManager(constraintManager);
      } catch (InvalidConstraintException e) { // thrown when already existing constraints are in conflict
         throw new IllegalStateException("Illegal constraint prefix collision: ", e);
      }

      constraintManager.registerConstraint(constraintConfiguration); // if this doesn't throw an exception, the constraint is valid

      dataStorage.addItemToArray(
            metadataCollection(),
            dialect.documentIdFilter(docId),
            dialect.concatFields(
                  LumeerConst.Collection.ATTRIBUTES_KEY, attributePath(attributeName),
                  LumeerConst.Collection.ATTRIBUTE_CONSTRAINTS_KEY
            ),
            constraintConfiguration);

      setLastTimeUsedNow(collectionName);
   }

   /**
    * Removes the constraint from list of constraints for given attribute.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       constraint configuration to be removed
    */
   public void dropAttributeConstraint(String collectionName, String attributeName, String constraintConfiguration) {
      String docId = null;
      try {
         docId = getCollectionMetadataDocument(collectionName).getId();
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      dataStorage.removeItemFromArray(
            metadataCollection(),
            dialect.documentIdFilter(docId),
            dialect.concatFields(
                  LumeerConst.Collection.ATTRIBUTES_KEY, attributePath(attributeName),
                  LumeerConst.Collection.ATTRIBUTE_CONSTRAINTS_KEY
            ),
            constraintConfiguration);

      setLastTimeUsedNow(collectionName);
   }

   /**
    * Gets the list of recently used documents in collection.
    *
    * @param collectionName
    *       internal collection name
    * @return list of document ids sorted in descending order
    */
   public List<String> getRecentlyUsedDocumentsIds(String collectionName) {
      return getCollectionMetadata(collectionName).getRecentlyUsedDocumentIds();
   }

   /**
    * Adds document id to the beginning of the list of recently used documents. If id is already present, it is moved to the beginning.
    *
    * @param collectionName
    *       internal collection name
    * @param id document id
    */
   public void addRecentlyUsedDocumentId(String collectionName, String id) {
      String docId = null;
      try {
         docId = getCollectionMetadataDocument(collectionName).getId();
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      dataStorage.removeItemFromArray(
            metadataCollection(),
            dialect.documentIdFilter(docId),
            LumeerConst.Collection.RECENTLY_USED_DOCUMENTS_KEY,
            id);

      int listSize = configurationFacade.getConfigurationInteger(LumeerConst.NUMBER_OF_RECENT_DOCS_PROPERTY)
                                        .orElse(LumeerConst.Collection.DEFAULT_NUMBER_OF_RECENT_DOCUMENTS);

      DataDocument query = dialect.addRecentlyUsedDocumentQuery(metadataCollection(), collectionName, id, listSize);

      dataStorage.run(query);
   }

   /**
    * Removes document id from the list of recently used documents.
    *
    * @param collectionName
    *       internal collection name
    * @param id
    *       document id
    */
   public void removeRecentlyUsedDocumentId(String collectionName, String id) {
      String docId = null;
      try {
         docId = getCollectionMetadataDocument(collectionName).getId();
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }
      dataStorage.removeItemFromArray(
            metadataCollection(),
            dialect.documentIdFilter(docId),
            LumeerConst.Collection.RECENTLY_USED_DOCUMENTS_KEY,
            id);
   }

   /**
    * @param collectionName
    *       internal collection name
    * @param user
    *       user name
    * @return true if user can read the collection
    */
   public boolean checkCollectionForRead(String collectionName, String user) {
      try {
         return securityFacade.checkForRead(getCollectionMetadataDocument(collectionName), user);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return true; // if metadata is not found, we allow access
      }
   }

   /**
    * @param collectionName
    *       internal collection name
    * @param user
    *       user name
    * @return true if user can write to the collection
    */
   public boolean checkCollectionForWrite(String collectionName, String user) {
      try {
         return securityFacade.checkForWrite(getCollectionMetadataDocument(collectionName), user);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return true; // if metadata is not found, we allow access
      }
   }

   /**
    * @param collectionName
    *       internal collection name
    * @param user
    *       user name
    * @return true if user can change access rights to collection
    */
   public boolean checkCollectionForAccessChange(String collectionName, String user) {
      try {
         return securityFacade.checkForExecute(getCollectionMetadataDocument(collectionName), user);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return true; // if metadata is not found, we allow access
      }

   }

   /**
    * Sets read right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user to set right for
    */
   public void addCollectionRead(String collectionName, String user) {
      DataDocument collectionMetadata = null;
      try {
         collectionMetadata = getCollectionMetadataDocument(collectionName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      securityFacade.setRightsRead(collectionMetadata, user);
      dataStorage.updateDocument(metadataCollection(), collectionMetadata, dialect.documentIdFilter(collectionMetadata.getId()));
      setLastTimeUsedNow(collectionName);
   }

   /**
    * Sets write right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user to set right for
    */
   public void addCollectionWrite(String collectionName, String user) {
      DataDocument collectionMetadata = null;
      try {
         collectionMetadata = getCollectionMetadataDocument(collectionName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      securityFacade.setRightsWrite(collectionMetadata, user);
      dataStorage.updateDocument(metadataCollection(), collectionMetadata, dialect.documentIdFilter(collectionMetadata.getId()));
      setLastTimeUsedNow(collectionName);
   }

   /**
    * Sets right to change rights.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user to set right for
    */
   public void addCollectionAccessChange(String collectionName, String user) {
      DataDocument collectionMetadata = null;
      try {
         collectionMetadata = getCollectionMetadataDocument(collectionName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      securityFacade.setRightsExecute(collectionMetadata, user);
      dataStorage.updateDocument(metadataCollection(), collectionMetadata, dialect.documentIdFilter(collectionMetadata.getId()));
      setLastTimeUsedNow(collectionName);
   }

   /**
    * Removes read right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user whose right is removed
    */
   public void removeCollectionRead(String collectionName, String user) {
      DataDocument collectionMetadata = null;
      try {
         collectionMetadata = getCollectionMetadataDocument(collectionName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      securityFacade.removeRightsRead(collectionMetadata, user);
      dataStorage.updateDocument(metadataCollection(), collectionMetadata, dialect.documentIdFilter(collectionMetadata.getId()));
      setLastTimeUsedNow(collectionName);
   }

   /**
    * Removes write right.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user whose right is removed
    */
   public void removeCollectionWrite(String collectionName, String user) {
      DataDocument collectionMetadata = null;
      try {
         collectionMetadata = getCollectionMetadataDocument(collectionName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      securityFacade.removeRightsWrite(collectionMetadata, user);
      dataStorage.updateDocument(metadataCollection(), collectionMetadata, dialect.documentIdFilter(collectionMetadata.getId()));
      setLastTimeUsedNow(collectionName);
   }

   /**
    * Removes right to change rights.
    *
    * @param collectionName
    *       internal name
    * @param user
    *       user whose right is removed
    */
   public void removeCollectionAccessChange(String collectionName, String user) {
      DataDocument collectionMetadata = null;
      try {
         collectionMetadata = getCollectionMetadataDocument(collectionName);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return;
      }

      securityFacade.removeRightsExecute(collectionMetadata, user);
      dataStorage.updateDocument(metadataCollection(), collectionMetadata, dialect.documentIdFilter(collectionMetadata.getId()));
      setLastTimeUsedNow(collectionName);
   }

   /**
    * Gets access rights for all users.
    *
    * @param collectionName
    *       internal name
    * @return list of AccessRightsDao (Daos for all users)
    */
   public List<AccessRightsDao> getAllAccessRights(String collectionName) {
      try {
         return securityFacade.getDaoList(getCollectionMetadataDocument(collectionName));
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return Collections.emptyList();
      }
   }

   // checks whether collection with given user name already exists
   private boolean checkIfUserCollectionExists(String originalCollectionName) {
      return dataStorage.collectionHasDocument(metadataCollection(), dialect.fieldValueFilter(LumeerConst.Collection.REAL_NAME_KEY, originalCollectionName));
   }

   // initializes constraint manager
   private void initConstraintManager(ConstraintManager constraintManager) {
      constraintManager.setLocale(Locale.forLanguageTag(configurationFacade.getConfigurationString(LumeerConst.USER_LOCALE_PROPERTY).orElse("en-US")));
   }

   // returns current user email
   private String getCurrentUser() {
      return userFacade.getUserEmail();
   }

   /**
    * @return name of metadata collection for current organisation
    */
   public String metadataCollection() {
      return metadataCollection(projectFacade.getCurrentProjectId());
   }

   /**
    * @param projectId
    *       project id
    * @return name of metadata collection for given project id
    */
   public static String metadataCollection(String projectId) {
      return LumeerConst.Collection.METADATA_COLLECTION_PREFIX + projectId;
   }
}