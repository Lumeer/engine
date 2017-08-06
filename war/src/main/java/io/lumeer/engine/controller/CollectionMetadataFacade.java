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
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataSort;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Attribute;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.CollectionMetadata;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidValueException;
import io.lumeer.engine.util.ErrorMessageBuilder;

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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

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
    * @param collectionCode
    *       collection code
    * @return DataDocument with collection metadata
    * @throws CollectionMetadataDocumentNotFoundException
    *       when metadata document is not found
    */
   public DataDocument getCollectionMetadataDocument(String collectionCode) throws CollectionMetadataDocumentNotFoundException {
      DataDocument metadata = readMetadata(collectionCodeFilter(collectionCode), null);

      if (metadata == null) {
         throw new CollectionMetadataDocumentNotFoundException(ErrorMessageBuilder.collectionMetadataNotFoundString(collectionCode));
      }

      return metadata;
   }

   /**
    * Gets object with collection metadata.
    *
    * @param collectionCode
    *       collection code
    * @return object with collection metadata
    */
   public CollectionMetadata getCollectionMetadata(String collectionCode) {
      try {
         DataDocument metadata = getCollectionMetadataDocument(collectionCode);
         return new CollectionMetadata(metadata);
      } catch (CollectionMetadataDocumentNotFoundException e) {
         return new CollectionMetadata();
      }
   }

   /**
    * Gets limited info about all collections in database
    *
    * @return list with metadata
    */
   public List<Collection> getCollections() {
      DataSort sort = dialect.documentFieldSort(LumeerConst.Collection.LAST_TIME_USED, LumeerConst.SORT_DESCENDING_ORDER);
      List<String> projection = Arrays.asList(LumeerConst.Collection.CODE, LumeerConst.Collection.REAL_NAME, LumeerConst.Collection.COLOR, LumeerConst.Collection.ICON, LumeerConst.Collection.DOCUMENT_COUNT);
      return dataStorage.search(metadataCollection(), null, sort, projection, 0, 0).stream()
                        .map(Collection::new).collect(Collectors.toList());
   }

   /**
    * @return map of collection codes and their names
    */
   public Map<String, String> getCollectionsCodeName() {
      List<DataDocument> documents = dataStorage.search(metadataCollection(), null, Arrays.asList(LumeerConst.Collection.REAL_NAME, LumeerConst.Collection.CODE));
      return documents.stream()
                      .collect(Collectors.toMap(d -> d.getString(LumeerConst.Collection.CODE), d -> d.getString(LumeerConst.Collection.REAL_NAME)));
   }

   /**
    * @param collectionId
    *       collection id
    * @return collection name
    */
   public String getCollectionName(String collectionId) {
      DataDocument metaData = dataStorage.readDocumentIncludeAttrs(metadataCollection(), collectionIdFilter(collectionId), Collections.singletonList(LumeerConst.Collection.REAL_NAME));
      return metaData != null ? metaData.getString(LumeerConst.Collection.REAL_NAME) : null;
   }

   /**
    * @param collectionName
    *       collection name
    * @return collection code
    */
   public String getCollectionCodeFromName(String collectionName) {
      DataFilter filter = dialect.fieldValueFilter(LumeerConst.Collection.REAL_NAME, collectionName);
      DataDocument metaData = dataStorage.readDocumentIncludeAttrs(metadataCollection(), filter, Collections.singletonList(LumeerConst.Collection.CODE));
      return metaData != null ? metaData.getString(LumeerConst.Collection.CODE) : null;
   }

   /**
    * @param collectionId
    *       collection id
    * @return collection code
    */
   public String getCollectionCode(String collectionId) {
      DataFilter filter = collectionIdFilter(collectionId);
      DataDocument metaData = dataStorage.readDocumentIncludeAttrs(metadataCollection(), filter, Collections.singletonList(LumeerConst.Collection.CODE));
      return metaData != null ? metaData.getString(LumeerConst.Collection.CODE) : null;
   }

   /**
    * @param collectionCode
    *       collection code
    * @return collection id
    */
   public String getCollectionId(String collectionCode) {
      DataDocument metaData = dataStorage.readDocumentIncludeAttrs(metadataCollection(), collectionCodeFilter(collectionCode), Collections.emptyList());
      return metaData != null ? metaData.getId() : null;
   }

   /**
    * Creates initial metadata in metadata collection.
    *
    * @param collectionCode
    *       collection code
    * @param collection
    *       collection metadata values
    */
   public String createInitialMetadata(String collectionCode, Collection collection) {
      DataDocument toCreateDocument = collection.toDataDocument()
                                                .append(LumeerConst.Collection.CODE, collectionCode)
                                                .append(LumeerConst.Collection.ATTRIBUTES, new ArrayList<>())
                                                .append(LumeerConst.Collection.LAST_TIME_USED, new Date())
                                                .append(LumeerConst.Collection.RECENTLY_USED_DOCUMENTS, new LinkedList<>())
                                                .append(LumeerConst.Collection.CREATE_DATE, new Date())
                                                .append(LumeerConst.Collection.CREATE_USER, userFacade.getUserEmail())
                                                .append(LumeerConst.Collection.UPDATE_DATE, null)
                                                .append(LumeerConst.Collection.UPDATE_USER, null)
                                                .append(LumeerConst.Collection.CUSTOM_META, new DataDocument());
      return dataStorage.createDocument(metadataCollection(), toCreateDocument);
   }

   /**
    * Updates metadata in metadata collection.
    *
    * @param collectionCode
    *       collection code
    * @param collection
    *       collection metadata values
    */
   public void updateMetadata(String collectionCode, Collection collection) {
      DataDocument toUpdateDocument = collection.toDataDocument()
                                                .append(LumeerConst.Collection.UPDATE_USER, userFacade.getUserEmail())
                                                .append(LumeerConst.Collection.UPDATE_DATE, new Date());
      toUpdateDocument.values().removeIf(Objects::isNull);
      dataStorage.updateDocument(metadataCollection(), toUpdateDocument, collectionCodeFilter(collectionCode));
   }

   /**
    * Drops metadata in metadata collection.
    *
    * @param collectionCode
    *       collection code
    */
   public void dropMetadata(String collectionCode) {
      dataStorage.dropDocument(metadataCollection(), collectionCodeFilter(collectionCode));
   }

   /**
    * Gets set of names of collection attributes.
    *
    * @param collectionCode
    *       collection code
    * @return set of collection attributes' names
    */
   public Set<String> getAttributesNames(String collectionCode) {
      String attributeNameKey = dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, LumeerConst.Collection.ATTRIBUTE_FULL_NAME);
      DataDocument metaData = readMetadata(collectionCodeFilter(collectionCode), Collections.singletonList(attributeNameKey));

      if (metaData == null) {
         return Collections.emptySet();
      }
      return metaData.getArrayList(LumeerConst.Collection.ATTRIBUTES, DataDocument.class)
                     .stream()
                     .map(d -> d.getString(LumeerConst.Collection.ATTRIBUTE_FULL_NAME))
                     .collect(Collectors.toSet());
   }

   /**
    * Gets complete info about collection attributes.
    *
    * @param collectionCode
    *       collection code
    * @return map, keys are attributes' names, values are objects with attributes info
    */
   public Map<String, Attribute> getAttributesInfo(String collectionCode) {
      DataDocument metaData = readMetadata(collectionCodeFilter(collectionCode), Collections.singletonList(LumeerConst.Collection.ATTRIBUTES));

      if (metaData == null) {
         return Collections.emptyMap();
      }
      return metaData.getArrayList(LumeerConst.Collection.ATTRIBUTES, DataDocument.class)
                     .stream()
                     .collect(Collectors.toMap(a -> a.getString(LumeerConst.Collection.ATTRIBUTE_FULL_NAME), Attribute::new));
   }

   /**
    * Gets complete info about one attribute.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute name
    * @return Attribute object
    */
   public Attribute getAttributeInfo(String collectionCode, String attributeName) {
      DataDocument attribute = readMetadataAttribute(collectionCode, attributeName);
      return attribute != null ? new Attribute(attribute) : null;
   }

   /**
    * Renames existing attribute in collection metadata.
    * This method should be called only when also renaming attribute in all collection documents.
    *
    * @param collectionCode
    *       collection code
    * @param oldFullName
    *       old attribute name
    * @param newFullName
    *       new attribute name
    * @throws AttributeAlreadyExistsException
    *       when attribute with new name already exists
    */
   public void renameAttribute(String collectionCode, String oldFullName, String newFullName) throws AttributeAlreadyExistsException {
      if (readMetadataAttribute(collectionCode, newFullName) != null) {
         throw new AttributeAlreadyExistsException(ErrorMessageBuilder.attributeAlreadyExistsString(newFullName, collectionCode));
      }

      String fullNameKey = dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, "$", LumeerConst.Collection.ATTRIBUTE_FULL_NAME);
      String nameKey = dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, "$", LumeerConst.Collection.ATTRIBUTE_NAME);

      DataDocument renameDocument = new DataDocument(fullNameKey, newFullName)
            .append(nameKey, attributeName(newFullName));

      dataStorage.updateDocument(metadataCollection(), renameDocument, attributeFilter(collectionCode, oldFullName));
   }

   /**
    * Deletes an attribute from collection metadata. Nothing is done if attribute metadata is not found, just return.
    * This method should be called only when also dropping attribute in all collection documents.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute to be dropped
    */
   public void dropAttribute(String collectionCode, String attributeName) {
      DataFilter filter = collectionCodeFilter(collectionCode);
      List<String> attributes = Collections.singletonList(LumeerConst.Collection.ATTRIBUTES);

      DataDocument collectionToRemoveFrom = dataStorage.readDocumentIncludeAttrs(metadataCollection(), filter, attributes);

      if (collectionToRemoveFrom == null) {
         return;
      }

      collectionToRemoveFrom.getArrayList(LumeerConst.Collection.ATTRIBUTES, DataDocument.class).stream()
                            .filter(attribute -> attribute.getString(LumeerConst.Collection.ATTRIBUTE_FULL_NAME).equals(attributeName))
                            .findFirst()
                            .ifPresent(removedAttribute -> dataStorage.removeItemFromArray(metadataCollection(), filter, LumeerConst.Collection.ATTRIBUTES, removedAttribute));
   }

   /**
    * Increment number of documents by specified value
    *
    * @param collectionCode
    *       collection code
    * @param count
    *       increment by
    */
   public void incrementDocumentCount(String collectionCode, int count) {
      dataStorage.incrementAttributeValueBy(metadataCollection(), collectionCodeFilter(collectionCode), LumeerConst.Collection.DOCUMENT_COUNT, count);
   }

   /**
    * Decrement number of documents by specified value
    *
    * @param collectionCode
    *       collection code
    * @param count
    *       decrement by
    */
   public void decrementDocumentCount(String collectionCode, int count) {
      int documentCount = getDocumentCount(collectionCode);
      if (documentCount == 0) {
         return;
      }
      dataStorage.updateDocument(metadataCollection(), new DataDocument(LumeerConst.Collection.DOCUMENT_COUNT, Math.max(documentCount - count, 0)), collectionCodeFilter(collectionCode));
   }

   private int getDocumentCount(String collectionCode) {
      DataDocument document = dataStorage.readDocumentIncludeAttrs(metadataCollection(), collectionCodeFilter(collectionCode), Collections.singletonList(LumeerConst.Collection.DOCUMENT_COUNT));
      return document != null ? document.getInteger(LumeerConst.Collection.DOCUMENT_COUNT) : 0;
   }

   /**
    * Adds attribute to metadata collection, if the attribute already isn't there.
    * Otherwise just increments count.
    * This should be called only when adding/updating document.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute's name
    */
   public void addOrIncrementAttribute(String collectionCode, String attributeName) {
      Attribute attribute = getAttributeInfo(collectionCode, attributeName);

      if (attribute != null) {
         String updateKey = dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, "$", LumeerConst.Collection.ATTRIBUTE_COUNT);
         DataDocument renameDocument = new DataDocument(updateKey, attribute.getCount() + 1);
         dataStorage.updateDocument(metadataCollection(), renameDocument, attributeFilter(collectionCode, attributeName));
      } else {
         dataStorage.addItemToArray(metadataCollection(),
               collectionCodeFilter(collectionCode),
               LumeerConst.Collection.ATTRIBUTES,
               new DataDocument()
                     .append(LumeerConst.Collection.ATTRIBUTE_FULL_NAME, attributeName)
                     .append(LumeerConst.Collection.ATTRIBUTE_NAME, attributeName(attributeName))
                     .append(LumeerConst.Collection.ATTRIBUTE_CONSTRAINTS, new ArrayList<String>())
                     .append(LumeerConst.Collection.ATTRIBUTE_COUNT, 1));
      }
   }

   /**
    * Drops attribute if there is no other document with that attribute in the collection (count is 1),
    * otherwise just decrements count.
    * This should be called only when adding/updating document.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       set of attributes' names
    */
   public void dropOrDecrementAttribute(String collectionCode, String attributeName) {
      Attribute attribute = getAttributeInfo(collectionCode, attributeName);

      if (attribute == null) {
         return;
      }

      if (attribute.getCount() <= 1L) {
         dropAttribute(collectionCode, attributeName);
         return;
      }

      String updateKey = dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, "$", LumeerConst.Collection.ATTRIBUTE_COUNT);
      DataDocument renameDocument = new DataDocument(updateKey, attribute.getCount() - 1);

      dataStorage.updateDocument(metadataCollection(), renameDocument, attributeFilter(collectionCode, attributeName));
   }

   /**
    * Returns count for specific attribute.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute name
    * @return attribute count, zero if the attribute does not exist
    */
   public int getAttributeCount(String collectionCode, String attributeName) {
      Attribute attribute = getAttributeInfo(collectionCode, attributeName);
      return attribute != null ? attribute.getCount() : 0;
   }

   /**
    * Reads time of last collection usage.
    *
    * @param collectionCode
    *       collection code
    * @return String representation of the time
    */
   public Date getLastTimeUsed(String collectionCode) {
      DataDocument metaData = getMetadataKeyValue(collectionCode, LumeerConst.Collection.LAST_TIME_USED);
      return metaData != null ? metaData.getDate(LumeerConst.Collection.LAST_TIME_USED) : null;
   }

   /**
    * Sets the time of last collection usage to current time
    *
    * @param collectionCode
    *       collection code
    */
   public void setLastTimeUsedNow(String collectionCode) {
      dataStorage.updateDocument(
            metadataCollection(),
            new DataDocument(
                  LumeerConst.Collection.LAST_TIME_USED,
                  new Date()),
            collectionCodeFilter(collectionCode));
   }

   /**
    * Gets document with custom metadata.
    *
    * @param collectionCode
    *       collection code
    * @return DataDocument with all custom metadata values
    */
   public DataDocument getCustomMetadata(String collectionCode) {
      DataDocument metaData = getMetadataKeyValue(collectionCode, LumeerConst.Collection.CUSTOM_META);
      return metaData != null ? metaData.getDataDocument(LumeerConst.Collection.CUSTOM_META) : new DataDocument();
   }

   /**
    * Adds all pairs key:value to custom metadata.
    *
    * @param collectionCode
    *       collection code
    * @param metadata
    *       custom metadata
    */
   public void setCustomMetadata(String collectionCode, DataDocument metadata) {
      DataDocument metadataDocument = new DataDocument();

      for (String key : metadata.keySet()) {
         metadataDocument.append(dialect.concatFields(LumeerConst.Collection.CUSTOM_META, key), metadata.get(key));
      }

      dataStorage.updateDocument(metadataCollection(), metadataDocument, collectionCodeFilter(collectionCode));
      setLastTimeUsedNow(collectionCode);
   }

   /**
    * Drops all custom metadata value associated with given key.
    *
    * @param collectionCode
    *       collection code
    * @param key
    *       list of metadata to drop
    */
   public void dropCustomMetadata(String collectionCode, String key) {
      dataStorage.dropAttribute(
            metadataCollection(),
            collectionCodeFilter(collectionCode),
            dialect.concatFields(
                  LumeerConst.Collection.CUSTOM_META,
                  key));
      setLastTimeUsedNow(collectionCode);
   }

   /**
    * Checks whether value satisfies all constraints (and tries to fix it when possible).
    *
    * @param attribute
    *       attribute name
    * @param value
    *       attribute value
    * @param collectionCode
    *       collection code
    * @return null when the value is not valid, fixed value when the value is fixable, original value when the value is valid
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to properly encode the value.
    */
   private Object checkAndConvertAttributeValue(final Attribute attribute, final Object value, final String collectionCode) throws InvalidConstraintException, InvalidValueException {
      // if the value is DataDocument, we check it recursively
      if (value instanceof DataDocument) {

         final DataDocument beforeCheck = (DataDocument) value;
         final DataDocument afterCheck = new DataDocument();

         Set<String> names = getAttributesNames(collectionCode);

         for (String key : beforeCheck.keySet()) {
            if (!names.contains(key)) { // attribute does not exist - no need to check anything
               afterCheck.put(key, beforeCheck.get(key));
            } else {
               Object newValue = checkAndConvertAttributeValue(getAttributeInfo(collectionCode, key), beforeCheck.get(key), collectionCode);
               afterCheck.put(key, newValue);
            }
         }

         return afterCheck;
      }

      // no encoding required
      if (value == null) {
         return null;
      }

      // other types
      final List<String> constraintConfigurations = attribute.getConstraints();

      final ConstraintManager constraintManager = new ConstraintManager(constraintConfigurations);
      constraintManager.setLocale(Locale.forLanguageTag(configurationFacade.getConfigurationString(LumeerConst.USER_LOCALE_PROPERTY)
                                                                           .orElse("en-US")));

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
    * @param collectionCode
    *       collection code
    * @param document
    *       document with attributes and their values to check
    * @return map of results, key is attribute name and value is result of checkAndConvertAttributeValue on that attribute
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to properly encode the value.
    */
   public DataDocument checkAndConvertAttributesValues(final String collectionCode, final DataDocument document) throws InvalidValueException, InvalidConstraintException {
      final DataDocument results = new DataDocument();
      Set<String> names = getAttributesNames(collectionCode);

      for (Map.Entry<String, Object> entry : document.entrySet()) {
         String key = entry.getKey();
         if (!names.contains(key)) { // attribute does not exist - no need to check anything
            results.append(key, entry.getValue());
         } else {
            results.append(key, checkAndConvertAttributeValue(getAttributeInfo(collectionCode, key), entry.getValue(), collectionCode));
         }
      }

      return results;
   }

   /**
    * Decodes document attributes based on the constraints so that they can be sent to the presentation layer properly.
    *
    * @param collectionCode
    *       collection code
    * @param document
    *       The document the attributes of which should be decoded.
    * @return A new document with decoded values.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to properly decode the value.
    */
   public DataDocument decodeAttributeValues(final String collectionCode, final DataDocument document) throws InvalidConstraintException, InvalidValueException {
      final DataDocument results = new DataDocument();
      Set<String> names = getAttributesNames(collectionCode);

      for (Map.Entry<String, Object> entry : document.entrySet()) {
         String key = entry.getKey();
         if (!names.contains(key)) { // attribute does not exist - no need to check anything
            results.append(key, entry.getValue());
         } else {
            results.append(key, decodeDocumentValue(getAttributeInfo(collectionCode, key), entry.getValue(), collectionCode));
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
    * @param collectionCode
    *       collection code
    * @return Decoded value.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to properly decode the value.
    */
   private Object decodeDocumentValue(final Attribute attribute, final Object value, final String collectionCode) throws InvalidConstraintException, InvalidValueException {
      // if the value is DataDocument, we check it recursively
      if (value instanceof DataDocument) {

         final DataDocument beforeCheck = (DataDocument) value;
         final DataDocument afterCheck = new DataDocument();

         Set<String> names = getAttributesNames(collectionCode);

         for (String key : beforeCheck.keySet()) {
            if (!names.contains(key)) { // attribute does not exist - no need to check anything
               afterCheck.put(key, beforeCheck.get(key));
            } else {
               Object newValue = decodeDocumentValue(getAttributeInfo(collectionCode, key), beforeCheck.get(key), collectionCode);
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
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       name of the attribute
    * @return list of constraint configurations for given attribute, empty list if constraints were not found
    */
   public List<String> getAttributeConstraintsConfigurations(String collectionCode, String attributeName) {
      Attribute attribute = getAttributeInfo(collectionCode, attributeName);
      return attribute != null ? attribute.getConstraints() : Collections.emptyList();
   }

   /**
    * Adds new constraint for an attribute and checks if it is valid.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       string with constraint configuration
    * @throws InvalidConstraintException
    *       when new constraint is not valid or is in conflict with existing constraints
    */
   public void addAttributeConstraint(String collectionCode, String attributeName, String constraintConfiguration) throws InvalidConstraintException {
      List<String> existingConstraints = getAttributeConstraintsConfigurations(collectionCode, attributeName);

      ConstraintManager constraintManager = null;
      try {
         constraintManager = new ConstraintManager(existingConstraints);
         initConstraintManager(constraintManager);
      } catch (InvalidConstraintException e) { // thrown when already existing constraints are in conflict
         throw new IllegalStateException("Illegal constraint prefix collision: ", e);
      }

      constraintManager.registerConstraint(constraintConfiguration); // if this doesn't throw an exception, the constraint is valid

      String attrParam = dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, "$", LumeerConst.Collection.ATTRIBUTE_CONSTRAINTS);
      dataStorage.addItemToArray(metadataCollection(), attributeFilter(collectionCode, attributeName), attrParam, constraintConfiguration);

      setLastTimeUsedNow(collectionCode);
   }

   /**
    * Removes the constraint from list of constraints for given attribute.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       constraint configuration to be removed
    */
   public void dropAttributeConstraint(String collectionCode, String attributeName, String constraintConfiguration) {
      String attrParam = dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, "$", LumeerConst.Collection.ATTRIBUTE_CONSTRAINTS);
      dataStorage.removeItemFromArray(metadataCollection(), attributeFilter(collectionCode, attributeName), attrParam, constraintConfiguration);

      setLastTimeUsedNow(collectionCode);
   }

   /**
    * Gets the list of recently used documents in collection.
    *
    * @param collectionCode
    *       collection code
    * @return list of document ids sorted in descending order
    */
   public List<String> getRecentlyUsedDocumentsIds(String collectionCode) {
      DataDocument metaData = getMetadataKeyValue(collectionCode, LumeerConst.Collection.RECENTLY_USED_DOCUMENTS);
      return metaData != null ? metaData.getArrayList(LumeerConst.Collection.RECENTLY_USED_DOCUMENTS, String.class) : Collections.emptyList();
   }

   /**
    * Adds document id to the beginning of the list of recently used documents. If id is already present, it is moved to the beginning.
    *
    * @param collectionCode
    *       collection code
    * @param id
    *       document id
    */
   public void addRecentlyUsedDocumentId(String collectionCode, String id) {
      dataStorage.removeItemFromArray(
            metadataCollection(),
            collectionCodeFilter(collectionCode),
            LumeerConst.Collection.RECENTLY_USED_DOCUMENTS,
            id);

      int listSize = configurationFacade.getConfigurationInteger(LumeerConst.NUMBER_OF_RECENT_DOCS_PROPERTY)
                                        .orElse(LumeerConst.Collection.DEFAULT_NUMBER_OF_RECENT_DOCUMENTS);

      DataDocument query = dialect.addRecentlyUsedDocumentQuery(metadataCollection(), collectionCode, id, listSize);

      dataStorage.run(query);
   }

   /**
    * Removes document id from the list of recently used documents.
    *
    * @param collectionCode
    *       collection code
    * @param id
    *       document id
    */
   public void removeRecentlyUsedDocumentId(String collectionCode, String id) {
      dataStorage.removeItemFromArray(
            metadataCollection(),
            collectionCodeFilter(collectionCode),
            LumeerConst.Collection.RECENTLY_USED_DOCUMENTS,
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
      return metadataCollectionFromId(projectFacade.getProjectId(projectCode));
   }

   public String metadataCollectionFromId(String projectId) {
      return LumeerConst.Collection.METADATA_COLLECTION_PREFIX + projectId;
   }

   // initializes constraint manager
   private void initConstraintManager(ConstraintManager constraintManager) {
      constraintManager.setLocale(Locale.forLanguageTag(configurationFacade.getConfigurationString(LumeerConst.USER_LOCALE_PROPERTY).orElse("en-US")));
   }

   private DataDocument getMetadataKeyValue(String collectionCode, String key) {
      return readMetadata(collectionCodeFilter(collectionCode), Collections.singletonList(key));
   }

   private DataDocument readMetadata(DataFilter filter, List<String> projection) {
      if (projection == null || projection.isEmpty()) {
         return dataStorage.readDocument(metadataCollection(), filter);
      }
      return dataStorage.readDocumentIncludeAttrs(metadataCollection(), filter, projection);
   }

   // method avoids to throw exception, because we don't know whether collection metadata or attribute doesn't exist
   private DataDocument readMetadataAttribute(String collectionCode, String attributeName) {
      DataDocument metaData = dataStorage.readDocumentIncludeAttrs(metadataCollection(), attributeFilter(collectionCode, attributeName),
            Collections.singletonList(dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, "$")));

      return metaData != null ? metaData.getArrayList(LumeerConst.Collection.ATTRIBUTES, DataDocument.class).get(0) : null;
   }

   private DataFilter collectionCodeFilter(String collectionCode) {
      return dialect.fieldValueFilter(LumeerConst.Collection.CODE, collectionCode);
   }

   private DataFilter collectionIdFilter(final String collectionId) {
      return dialect.documentIdFilter(collectionId);
   }

   private DataFilter attributeFilter(String collectionCode, String attributeName) {
      Map<String, Object> filter = new HashMap<>();
      filter.put(LumeerConst.Collection.CODE, collectionCode);
      filter.put(dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, LumeerConst.Collection.ATTRIBUTE_FULL_NAME), attributeName);
      return dialect.multipleFieldsValueFilter(filter);
   }

   private DataFilter attributeWildcardFilter(String collectionCode, String attributeName) {
      String name = dialect.concatFields(LumeerConst.Collection.ATTRIBUTES, LumeerConst.Collection.ATTRIBUTE_FULL_NAME);
      return dialect.combineFilters(collectionCodeFilter(collectionCode), dialect.fieldValueWildcardFilterOneSided(name, attributeName));
   }

   private String attributeName(String attributeFullName) {
      int ixSeparator = attributeFullName.lastIndexOf(".");
      return ixSeparator != -1 && ixSeparator < attributeFullName.length() - 1 ? attributeFullName.substring(ixSeparator + 1) : attributeFullName;
   }

}