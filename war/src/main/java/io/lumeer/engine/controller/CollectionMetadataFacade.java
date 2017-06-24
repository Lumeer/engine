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
import io.lumeer.engine.api.LumeerConst.Collection;
import io.lumeer.engine.api.constraint.Constraint;
import io.lumeer.engine.api.constraint.ConstraintManager;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Attribute;
import io.lumeer.engine.api.dto.CollectionMetadata;
import io.lumeer.engine.api.event.ChangeCollectionName;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidValueException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
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
import java.util.stream.Collectors;
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
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata document is not found
    */
   public DataDocument getCollectionMetadataDocument(String collectionName) throws CollectionMetadataDocumentNotFoundException {
      DataDocument metadata = readMetadata(collectionName, internalNameFilter(collectionName), null);

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
      name = Collection.NAME_PREFIX + name;
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
      DataDocument collectionMetadata = new DataDocument()
            .append(Collection.REAL_NAME_KEY, originalCollectionName)
            .append(Collection.INTERNAL_NAME_KEY, internalCollectionName)
            .append(Collection.ATTRIBUTES_KEY, new ArrayList<>())
            .append(Collection.LAST_TIME_USED_KEY, new Date())
            .append(Collection.RECENTLY_USED_DOCUMENTS_KEY, new LinkedList<>())
            .append(Collection.CREATE_DATE_KEY, new Date())
            .append(Collection.CREATE_USER_KEY, userFacade.getUserEmail())
            .append(Collection.CUSTOM_META_KEY, new DataDocument());

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
      String attributeNameKey = dialect.concatFields(Collection.ATTRIBUTES_KEY, Collection.ATTRIBUTE_FULL_NAME_KEY);
      DataDocument metaData = readMetadata(collectionName, internalNameFilter(collectionName), Collections.singletonList(attributeNameKey));

      return metaData != null ? metaData.getArrayList(Collection.ATTRIBUTES_KEY, DataDocument.class)
                                        .stream()
                                        .map(d -> d.getString(Collection.ATTRIBUTE_FULL_NAME_KEY))
                                        .collect(Collectors.toSet())
            : Collections.emptySet();
   }

   /**
    * Gets complete info about collection attributes.
    *
    * @param collectionName
    *       internal collection name
    * @return map, keys are attributes' names, values are objects with attributes info
    */
   public Map<String, Attribute> getAttributesInfo(String collectionName) {
      DataDocument metaData = readMetadata(collectionName, internalNameFilter(collectionName), Collections.singletonList(Collection.ATTRIBUTES_KEY));
      return metaData != null ? metaData.getArrayList(Collection.ATTRIBUTES_KEY, DataDocument.class)
                                        .stream()
                                        .collect(Collectors.toMap(a -> a.getString(Collection.ATTRIBUTE_FULL_NAME_KEY), Attribute::new))
            : Collections.emptyMap();
   }

   /**
    * Gets complete info about one attribute.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute name
    * @return Attribute object
    */
   public Attribute getAttributeInfo(String collectionName, String attributeName) {
      DataDocument attribute = readMetadataAttribute(collectionName, attributeName);

      return attribute != null ? new Attribute(attribute) : null;
   }

   /**
    * Renames existing attribute in collection metadata.
    * This method should be called only when also renaming attribute in all collection documents.
    *
    * @param collectionName
    *       internal collection name
    * @param oldFullName
    *       old attribute name
    * @param newFullName
    *       new attribute name
    * @throws AttributeAlreadyExistsException
    *       when attribute with new name already exists
    */
   public void renameAttribute(String collectionName, String oldFullName, String newFullName) throws AttributeAlreadyExistsException {
      if (readMetadataAttribute(collectionName, newFullName) != null) {
         throw new AttributeAlreadyExistsException(ErrorMessageBuilder.attributeAlreadyExistsString(newFullName, collectionName));
      }

      String fullNameKey = dialect.concatFields(Collection.ATTRIBUTES_KEY, "$", Collection.ATTRIBUTE_FULL_NAME_KEY);
      String nameKey = dialect.concatFields(Collection.ATTRIBUTES_KEY, "$", Collection.ATTRIBUTE_NAME_KEY);

      DataDocument renameDocument = new DataDocument(fullNameKey, newFullName)
            .append(nameKey, attributeName(newFullName));

      dataStorage.updateDocument(metadataCollection(), renameDocument, attributeFilter(collectionName, oldFullName));
   }

   /**
    * Deletes an attribute from collection metadata. Nothing is done if attribute metadata is not found, just return.
    * This method should be called only when also dropping attribute in all collection documents.
    *
    * @param collectionName
    *       internal collection name
    * @param attributeName
    *       attribute to be dropped
    */
   public void dropAttribute(String collectionName, String attributeName) {
      DataDocument removeAttributes = dataStorage.readDocumentIncludeAttrs(metadataCollection(),
            dialect.combineFilters(internalNameFilter(collectionName), attributeWildcardFilter(collectionName, attributeName)),
            Collections.singletonList(Collection.ATTRIBUTES_KEY));

      if (removeAttributes == null) {
         return;
      }

      dataStorage.removeItemsFromArray(metadataCollection(), internalNameFilter(collectionName), Collection.ATTRIBUTES_KEY,
            removeAttributes.getArrayList(Collection.ATTRIBUTES_KEY, DataDocument.class));
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
      Attribute attribute = getAttributeInfo(collectionName, attributeName);

      if (attribute != null) {
         String updateKey = dialect.concatFields(Collection.ATTRIBUTES_KEY, "$", Collection.ATTRIBUTE_COUNT_KEY);
         DataDocument renameDocument = new DataDocument(updateKey, attribute.getCount() + 1);
         dataStorage.updateDocument(metadataCollection(), renameDocument, attributeFilter(collectionName, attributeName));
      } else {
         dataStorage.addItemToArray(metadataCollection(),
               internalNameFilter(collectionName),
               Collection.ATTRIBUTES_KEY,
               new DataDocument()
                     .append(Collection.ATTRIBUTE_FULL_NAME_KEY, attributeName)
                     .append(Collection.ATTRIBUTE_NAME_KEY, attributeName(attributeName))
                     .append(Collection.ATTRIBUTE_CONSTRAINTS_KEY, new ArrayList<String>())
                     .append(Collection.ATTRIBUTE_COUNT_KEY, 1));
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
      Attribute attribute = getAttributeInfo(collectionName, attributeName);

      if (attribute == null) {
         return;
      }

      if (attribute.getCount() <= 1L) {
         dropAttribute(collectionName, attributeName);
         return;
      }

      String updateKey = dialect.concatFields(Collection.ATTRIBUTES_KEY, "$", Collection.ATTRIBUTE_COUNT_KEY);
      DataDocument renameDocument = new DataDocument(updateKey, attribute.getCount() - 1);

      dataStorage.updateDocument(metadataCollection(), renameDocument, attributeFilter(collectionName, attributeName));
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
      DataDocument metaData = getMetadataKeyValue(collectionName, Collection.REAL_NAME_KEY);
      return metaData != null ? metaData.getString(Collection.REAL_NAME_KEY) : null;
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
      DataDocument metadata = dataStorage.readDocumentIncludeAttrs(
            metadataCollection(),
            dialect.fieldValueFilter(Collection.REAL_NAME_KEY,
                  originalCollectionName),
            Collections.singletonList(Collection.INTERNAL_NAME_KEY)
      );

      if (metadata == null) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(originalCollectionName));
      }

      return metadata.getString(Collection.INTERNAL_NAME_KEY);
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
      if (checkIfUserCollectionExists(collectionOriginalName)) {
         throw new UserCollectionAlreadyExistsException(ErrorMessageBuilder.userCollectionAlreadyExistsString(collectionOriginalName));
      }

      dataStorage.updateDocument(
            metadataCollection(),
            new DataDocument(
                  Collection.REAL_NAME_KEY,
                  collectionOriginalName),
            internalNameFilter(collectionInternalName));

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
      DataDocument metaData = getMetadataKeyValue(collectionName, Collection.LAST_TIME_USED_KEY);
      return metaData != null ? metaData.getDate(Collection.LAST_TIME_USED_KEY) : null;
   }

   /**
    * Sets the time of last collection usage to current time
    *
    * @param collectionName
    *       internal collection name
    */
   public void setLastTimeUsedNow(String collectionName) {
      dataStorage.updateDocument(
            metadataCollection(),
            new DataDocument(
                  Collection.LAST_TIME_USED_KEY,
                  new Date()),
            internalNameFilter(collectionName));
   }

   /**
    * Gets document with custom metadata.
    *
    * @param collectionName
    *       internal name
    * @return DataDocument with all custom metadata values
    */
   public DataDocument getCustomMetadata(String collectionName) {
      DataDocument metaData = getMetadataKeyValue(collectionName, Collection.CUSTOM_META_KEY);
      return metaData != null ? metaData.getDataDocument(Collection.CUSTOM_META_KEY) : new DataDocument();
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
      DataDocument metadataDocument = new DataDocument();

      for (String key : metadata.keySet()) {
         metadataDocument.append(dialect.concatFields(Collection.CUSTOM_META_KEY, key), metadata.get(key));
      }

      dataStorage.updateDocument(metadataCollection(), metadataDocument, internalNameFilter(collectionName));
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
      dataStorage.dropAttribute(
            metadataCollection(),
            internalNameFilter(collectionName),
            dialect.concatFields(
                  Collection.CUSTOM_META_KEY,
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
            collectionName.length() >= Collection.NAME_PREFIX.length() &&
            collectionName.startsWith(Collection.NAME_PREFIX);
   }

   /**
    * Checks whether value satisfies all constraints (and tries to fix it when possible).
    *
    * @param attribute
    *       attribute name
    * @param value
    *       attribute value
    * @return null when the value is not valid, fixed value when the value is fixable, original value when the value is valid
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to properly encode the value.
    */
   private Object checkAndConvertAttributeValue(final Attribute attribute, final Object value, final String collection) throws InvalidConstraintException, InvalidValueException {
      // if the value is DataDocument, we check it recursively
      if (value instanceof DataDocument) {

         final DataDocument beforeCheck = (DataDocument) value;
         final DataDocument afterCheck = new DataDocument();

         Set<String> names = getAttributesNames(collection);

         for (String key : beforeCheck.keySet()) {
            if (!names.contains(key)) { // attribute does not exist - no need to check anything
               afterCheck.put(key, beforeCheck.get(key));
            } else {
               Object newValue = checkAndConvertAttributeValue(getAttributeInfo(collection, key), beforeCheck.get(key), collection);
               afterCheck.put(key, newValue);
            }
         }

         return afterCheck;
      }

      final List<String> constraintConfigurations = attribute.getConstraints();

      final ConstraintManager constraintManager = new ConstraintManager(constraintConfigurations);
      Constraint.ConstraintResult result = constraintManager.isValid(value.toString());

      if (result == Constraint.ConstraintResult.INVALID) {
         throw new InvalidValueException("Invalid value for attribute " + attribute.getName() + " given its constraints.");
      }

      final Object encoded;
      if (result == Constraint.ConstraintResult.FIXABLE) {
         encoded = constraintManager.encode(constraintManager.fix(value.toString()));
      } else {
         encoded = constraintManager.encode(value);
      }

      if (encoded == null) {
         throw new InvalidValueException("It was not possible to encode user value: " + value.toString());
      }

      return encoded;
   }

   /**
    * Checks value of every attribute of given document.
    *
    * @param collectionName
    *       internal collection name
    * @param document
    *       document with attributes and their values to check
    * @return map of results, key is attribute name and value is result of checkAndConvertAttributeValue on that attribute
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to properly encode the value.
    */
   public DataDocument checkAndConvertAttributesValues(final String collectionName, final DataDocument document) throws InvalidValueException, InvalidConstraintException {
      final DataDocument results = new DataDocument();
      Set<String> names = getAttributesNames(collectionName);

      for (Map.Entry<String, Object> entry : document.entrySet()) {
         String key = entry.getKey();
         if (!names.contains(key)) { // attribute does not exist - no need to check anything
            results.append(key, entry.getValue());
         } else {
            results.append(key, checkAndConvertAttributeValue(getAttributeInfo(collectionName, key), entry.getValue(), collectionName));
         }
      }

      return results;
   }

   /**
    * Decodes document attributes based on the constraints so that they can be sent to the presentation layer properly.
    *
    * @param collectionName
    *       Name of the collection from which the document was read.
    * @param document
    *       The document the attributes of which should be decoded.
    * @return A new document with decoded values.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to properly decode the value.
    */
   public DataDocument decodeAttributeValues(final String collectionName, final DataDocument document) throws InvalidConstraintException, InvalidValueException {
      final DataDocument results = new DataDocument();
      Set<String> names = getAttributesNames(collectionName);

      for (Map.Entry<String, Object> entry : document.entrySet()) {
         String key = entry.getKey();
         if (!names.contains(key)) { // attribute does not exist - no need to check anything
            results.append(key, entry.getValue());
         } else {
            results.append(key, decodeDocumentValue(getAttributeInfo(collectionName, key), entry.getValue(), collectionName));
         }
      }
      return results;
   }

   /**
    * Decodes a value type or the value itself from database representation to the user representation
    * based on the information in constraints.
    *
    * @param attribute
    *       The attribute meta-data.
    * @param value
    *       The attribute value.
    * @return Decoded value.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to properly decode the value.
    */
   private Object decodeDocumentValue(final Attribute attribute, final Object value, final String collection) throws InvalidConstraintException, InvalidValueException {
      // if the value is DataDocument, we check it recursively
      if (value instanceof DataDocument) {

         final DataDocument beforeCheck = (DataDocument) value;
         final DataDocument afterCheck = new DataDocument();

         Set<String> names = getAttributesNames(collection);

         for (String key : beforeCheck.keySet()) {
            if (!names.contains(key)) { // attribute does not exist - no need to check anything
               afterCheck.put(key, beforeCheck.get(key));
            } else {
               Object newValue = decodeDocumentValue(getAttributeInfo(collection, key), beforeCheck.get(key), collection);
               afterCheck.put(key, newValue);
            }
         }

         return afterCheck;
      }

      final List<String> constraintConfigurations = attribute.getConstraints();
      final ConstraintManager constraintManager = new ConstraintManager(constraintConfigurations);
      final Object decoded = constraintManager.decode(value);

      if (value != null && decoded == null) {
         throw new InvalidValueException("Unable to decode value from database: " + value.toString());
      }

      return constraintManager.decode(value);
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
      List<String> existingConstraints = getAttributeConstraintsConfigurations(collectionName, attributeName);

      ConstraintManager constraintManager = null;
      try {
         constraintManager = new ConstraintManager(existingConstraints);
         initConstraintManager(constraintManager);
      } catch (InvalidConstraintException e) { // thrown when already existing constraints are in conflict
         throw new IllegalStateException("Illegal constraint prefix collision: ", e);
      }

      constraintManager.registerConstraint(constraintConfiguration); // if this doesn't throw an exception, the constraint is valid

      String attrParam = dialect.concatFields(Collection.ATTRIBUTES_KEY, "$", Collection.ATTRIBUTE_CONSTRAINTS_KEY);
      dataStorage.addItemToArray(metadataCollection(), attributeFilter(collectionName, attributeName), attrParam, constraintConfiguration);

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
      String attrParam = dialect.concatFields(Collection.ATTRIBUTES_KEY, "$", Collection.ATTRIBUTE_CONSTRAINTS_KEY);
      dataStorage.removeItemFromArray(metadataCollection(), attributeFilter(collectionName, attributeName), attrParam, constraintConfiguration);

      setLastTimeUsedNow(collectionName);
   }

   /**
    * Gets the list of recently used documents in collection.
    *
    * @param collectionName
    *       internal collection name
    * @return list of document ids sorted in descending order
    */
   public List<String> getRecentlyUsedDocumentsIds(String collectionName) throws CollectionMetadataDocumentNotFoundException {
      return getMetadataKeyValue(collectionName, Collection.RECENTLY_USED_DOCUMENTS_KEY).getArrayList(Collection.RECENTLY_USED_DOCUMENTS_KEY, String.class);
   }

   /**
    * Adds document id to the beginning of the list of recently used documents. If id is already present, it is moved to the beginning.
    *
    * @param collectionName
    *       internal collection name
    * @param id
    *       document id
    */
   public void addRecentlyUsedDocumentId(String collectionName, String id) throws CollectionMetadataDocumentNotFoundException {
      dataStorage.removeItemFromArray(
            metadataCollection(),
            internalNameFilter(collectionName),
            Collection.RECENTLY_USED_DOCUMENTS_KEY,
            id);

      int listSize = configurationFacade.getConfigurationInteger(LumeerConst.NUMBER_OF_RECENT_DOCS_PROPERTY)
                                        .orElse(Collection.DEFAULT_NUMBER_OF_RECENT_DOCUMENTS);

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
   public void removeRecentlyUsedDocumentId(String collectionName, String id) throws CollectionMetadataDocumentNotFoundException {
      dataStorage.removeItemFromArray(
            metadataCollection(),
            internalNameFilter(collectionName),
            Collection.RECENTLY_USED_DOCUMENTS_KEY,
            id);
   }

   /**
    * @return name of metadata collection for current organisation
    */
   public String metadataCollection() {
      return metadataCollection(projectFacade.getCurrentProjectCode());
   }

   /**
    * @param projectCode
    *       project code
    * @return name of metadata collection for given project code
    */
   public String metadataCollection(String projectCode) {
      return Collection.METADATA_COLLECTION_PREFIX + projectFacade.getProjectId(projectCode);
   }

   // checks whether collection with given user name already exists
   private boolean checkIfUserCollectionExists(String originalCollectionName) {
      return dataStorage.collectionHasDocument(metadataCollection(), dialect.fieldValueFilter(Collection.REAL_NAME_KEY, originalCollectionName));
   }

   // initializes constraint manager
   private void initConstraintManager(ConstraintManager constraintManager) {
      constraintManager.setLocale(Locale.forLanguageTag(configurationFacade.getConfigurationString(LumeerConst.USER_LOCALE_PROPERTY).orElse("en-US")));
   }

   private DataDocument getMetadataKeyValue(String collection, String key) {
      return readMetadata(collection, internalNameFilter(collection), Collections.singletonList(key));
   }

   private DataDocument readMetadata(String collectionName, DataFilter filter, List<String> projection) {
      return projection == null || projection.isEmpty() ? dataStorage.readDocument(metadataCollection(), filter)
            : dataStorage.readDocumentIncludeAttrs(metadataCollection(), filter, projection);
   }

   // method avoids to throw exception, because we don't know whether collection metadata or attribute doesn't exist
   private DataDocument readMetadataAttribute(String collectionName, String attributeName) {
      DataDocument metaData = dataStorage.readDocumentIncludeAttrs(metadataCollection(), attributeFilter(collectionName, attributeName),
            Collections.singletonList(dialect.concatFields(Collection.ATTRIBUTES_KEY, "$")));

      return metaData != null ? metaData.getArrayList(Collection.ATTRIBUTES_KEY, DataDocument.class).get(0) : null;
   }

   private DataFilter internalNameFilter(String collectionName) {
      return dialect.fieldValueFilter(Collection.INTERNAL_NAME_KEY, collectionName);
   }

   private DataFilter attributeFilter(String collectionName, String attributeName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(Collection.INTERNAL_NAME_KEY, collectionName);
      filter.put(dialect.concatFields(Collection.ATTRIBUTES_KEY, Collection.ATTRIBUTE_FULL_NAME_KEY), attributeName);
      return dialect.multipleFieldsValueFilter(filter);
   }

   private DataFilter attributeWildcardFilter(String collectionName, String attributeName) {
      String name = dialect.concatFields(Collection.ATTRIBUTES_KEY, Collection.ATTRIBUTE_FULL_NAME_KEY);
      return dialect.combineFilters(internalNameFilter(collectionName), dialect.fieldValueWildcardFilterOneSided(name, attributeName));
   }

   private String attributeName(String attributeFullName) {
      int ixSeparator = attributeFullName.lastIndexOf(".");
      return ixSeparator != -1 && ixSeparator < attributeFullName.length() - 1 ? attributeFullName.substring(ixSeparator + 1) : attributeFullName;
   }

}