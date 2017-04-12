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
package io.lumeer.engine.controller.configuration;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.util.Resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Manipulates specified configuration properties.
 */
@ApplicationScoped
public class ConfigurationManipulator implements Serializable {

   private static final Logger log = Resources.produceLog(ConfigurationManipulator.class.getName());

   public static final String NAME_KEY = "name";
   public static final String CONFIG_KEY = "config";
   public static final String FLAGS_KEY = "flags";

   private static final String FLAG_SEPARATOR = "_";

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   /**
    * Removes the whole attribute located in 'config' field in configuration entry.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param attributeName
    *       the name of attribute located in 'config' field
    */
   public void resetConfigurationAttribute(final String collectionName, final String nameValue, final String attributeName) {
      if (nameValue == null || attributeName == null) {
         return;
      }

      Optional<DataDocument> configuration = getConfigurationEntry(collectionName, nameValue);
      if (!configuration.isPresent()) {
         log.log(Level.FINE, "Configuration '{}' not found in collection '{}', creating configuration entry...", Arrays.asList(nameValue, collectionName));
         createEmptyConfigurationEntry(collectionName, nameValue);
         return;
      }

      DataDocument config = (DataDocument) configuration.get().get(CONFIG_KEY);
      if (!config.containsKey(attributeName)) {
         log.log(Level.FINE, "Attribute '{}' not found for configuration '{}' in collection '{}'", Arrays.asList(attributeName, nameValue, collectionName));
         return;
      }

      log.log(Level.FINE, "Dropping attribute '{}' of configuration '{}' in collection '{}'", Arrays.asList(attributeName, nameValue, collectionName));
      systemDataStorage.dropAttribute(collectionName, dataStorageDialect.documentIdFilter(configuration.get().getId()), dataStorageDialect.concatFields(CONFIG_KEY, attributeName));
   }

   /**
    * Removes all attributes and values of the 'config' field in configuration entry.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    */
   public void resetConfiguration(final String collectionName, final String nameValue) {
      if (nameValue == null) {
         return;
      }

      Optional<DataDocument> configuration = getConfigurationEntry(collectionName, nameValue);
      if (!configuration.isPresent()) {
         log.log(Level.FINE, "Configuration '{}' not found in collection '{}', creating configuration entry...", Arrays.asList(nameValue, collectionName));
         createEmptyConfigurationEntry(collectionName, nameValue);
         return;
      }

      log.log(Level.FINE, "Resetting configuration '{}' in collection '{}'", Arrays.asList(nameValue, collectionName));
      DataDocument configDocument = configuration.get();
      configDocument.remove(CONFIG_KEY);
      configDocument.put(CONFIG_KEY, new DataDocument());
      systemDataStorage.updateDocument(collectionName, configDocument, dataStorageDialect.documentIdFilter(configDocument.getId()));
   }

   /**
    * Sets a new key-value into 'config' field for given nameValue of configuration entry stored in system database. If the given key exists, its value will be updated.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param key
    *       the name of key to store in 'config' field
    * @param value
    *       the Object value of the given key
    */
   public void setConfiguration(final String collectionName, final String nameValue, final String key, final Object value) {
      setConfiguration(collectionName, nameValue, key, value, Collections.emptySet());
   }

   /**
    * Sets a new key-value into 'config' field for given nameValue of configuration entry stored in system database. If the given key exists, its value will be updated.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param key
    *       the name of key to store in 'config' field
    * @param value
    *       the Object value of the given key
    * @param flags
    *       flags to be set for the given configuration field
    */
   public void setConfiguration(final String collectionName, final String nameValue, final String key, final Object value, final Set<String> flags) {
      writeValueToDb(collectionName, nameValue, key, value, flags);
   }

   /**
    * Returns an Object value of the given key in 'config' field for unique nameValue.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param key
    *       the name of key located in 'config' field
    * @return Object value of the given key
    */
   public Object getConfiguration(final String collectionName, final String nameValue, final String key) {
      return readValueFromDb(collectionName, nameValue, key).orElse(null);
   }

   /**
    * Returns an Optional DataDocument representing configuration entry of the given nameValue stored in the given system collection.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @return configuration entry of the given nameValue in given collection
    */
   public Optional<DataDocument> getConfigurationEntry(final String collectionName, final String nameValue) {
      if (nameValue == null || !systemDataStorage.hasCollection(collectionName)) {
         return Optional.empty();
      }

      final DataFilter filter = dataStorageDialect.fieldValueFilter(NAME_KEY, nameValue);
      DataDocument config = systemDataStorage.readDocument(collectionName, filter);
      return Optional.ofNullable(config);
   }

   public boolean hasConfigurationAttributeFlag(final String collectionName, final String configName, final String attributeName, final String flagName) {
      Optional<DataDocument> configuration = getConfigurationEntry(collectionName, configName);
      if (!configuration.isPresent()) {
         return false;
      }

      List<String> flags = configuration.get().getArrayList(FLAGS_KEY, String.class);
      if (flags == null) {
         return false;
      }

      String attributeFlag = createAttributeFlag(attributeName, flagName);
      return flags.contains(attributeFlag);
   }

   /**
    * Sets a new Object value for given nameValue and key located in 'config' field in configuration entry stored in system database. If the given key exists, its value will be updated.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param key
    *       the name of key to write into 'config' field
    * @param value
    *       the Object value of the given key
    */
   private void writeValueToDb(final String collectionName, final String nameValue, final String key, final Object value, final Set<String> flags) {
      if (nameValue == null || key == null || value == null) {
         return;
      }

      List<String> newFlags = convertToAttributeFlags(key, flags);

      Optional<DataDocument> configuration = getConfigurationEntry(collectionName, nameValue);
      if (!configuration.isPresent()) {
         log.log(Level.FINE, "Configuration '{}' not found in collection '{}', creating configuration entry '{{}:{}}'", Arrays.asList(nameValue, collectionName, key, value));
         createNewConfigurationEntry(collectionName, nameValue, key, value, newFlags);
         return;
      }

      log.log(Level.FINE, "Writing '{{}:{}}' to configuration '{}' in collection '{}'", Arrays.asList(key, value, nameValue, collectionName));
      DataDocument configDocument = configuration.get();

      DataDocument configValues = (DataDocument) configDocument.get(CONFIG_KEY);
      configValues.put(key, value);

      List<String> flagsArray = configDocument.getArrayList(FLAGS_KEY, String.class);
      if (flagsArray == null) {
         flagsArray = new ArrayList<>();
      }
      flagsArray.addAll(newFlags);

      DataDocument updatedDocument = new DataDocument();
      updatedDocument.put(CONFIG_KEY, configValues);
      updatedDocument.put(FLAGS_KEY, flagsArray);

      DataFilter filter = dataStorageDialect.documentIdFilter(configDocument.getId());
      systemDataStorage.updateDocument(collectionName, updatedDocument, filter);
   }

   private static List<String> convertToAttributeFlags(final String configKey, final Set<String> flags) {
      return flags.stream()
                  .map(flag -> createAttributeFlag(configKey, flag))
                  .collect(Collectors.toList());
   }

   /**
    * Reads a value of given key located in 'config' field for given nameValue stored in system database.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param nameValue
    *       the unique name value of stored configuration entry
    * @param key
    *       the name of key in 'config' field
    * @return Optional<Object> with value of the given key
    */
   private Optional<Object> readValueFromDb(final String collectionName, final String nameValue, final String key) {
      if (nameValue == null || key == null) {
         return Optional.empty();
      }

      Optional<DataDocument> configuration = getConfigurationEntry(collectionName, nameValue);

      if (!configuration.isPresent()) {
         log.log(Level.FINE, "Configuration '{}' not found in collection '{}', creating configuration entry...", Arrays.asList(nameValue, collectionName));
         createEmptyConfigurationEntry(collectionName, nameValue);
         return Optional.empty();
      }

      DataDocument config = (DataDocument) configuration.get().get(CONFIG_KEY);

      if (!config.containsKey(key)) {
         return Optional.empty();
      }

      Object value = config.get(key);
      return Optional.ofNullable(value);
   }

   /**
    * Creates new configuration entry with '_id', 'name' and empty 'config' field.
    *
    * @param collectionName
    *       the name of collection
    * @param nameValue
    *       value of name field
    */
   private void createEmptyConfigurationEntry(final String collectionName, final String nameValue) {
      DataDocument configDocument = new DataDocument();
      configDocument.put(NAME_KEY, nameValue);
      configDocument.put(CONFIG_KEY, new DataDocument());
      configDocument.put(FLAGS_KEY, Collections.emptyList());
      systemDataStorage.createDocument(collectionName, configDocument);
   }

   private void createNewConfigurationEntry(final String collectionName, final String nameValue, final String key, final Object value, final List<String> flags) {
      DataDocument configDocument = new DataDocument();
      configDocument.put(NAME_KEY, nameValue);
      configDocument.put(CONFIG_KEY, new DataDocument(key, value));
      configDocument.put(FLAGS_KEY, flags);
      systemDataStorage.createDocument(collectionName, configDocument);
   }

   private static String createAttributeFlag(final String configKey, final String flagName) {
      return configKey + FLAG_SEPARATOR + flagName;
   }
}
