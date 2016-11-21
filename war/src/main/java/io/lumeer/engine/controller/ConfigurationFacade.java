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

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.exception.AttributeNotFoundException;
import io.lumeer.engine.exception.CollectionNotFoundException;
import io.lumeer.engine.exception.NullParameterException;
import io.lumeer.engine.util.ErrorMessageBuilder;

import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Manipulates user configuration properties.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RequestScoped
public class ConfigurationFacade implements Serializable {

   private static final Map<String, String> DEFAULT_VALUES = new HashMap<>();

   private static final String DEFAULT_PROPERTY_FILE = "defaults-dev.properties";
   private static final String USER_EMAIL_KEY = "userEmail";
   private static final String USER_CONFIG = "config.user";
   private static final String ID_KEY = "_id";

   private static final int TARGET_VERSION_DISABLED = -1;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private Logger log;

   @Inject
   private UserFacade userFacade;

   static {
      String envDefaults = System.getenv("lumeer.defaults");
      if (envDefaults == null) {
         envDefaults = DEFAULT_PROPERTY_FILE;
      }

      final Properties properties = new Properties();
      try {
         final InputStream input = ConfigurationFacade.class.getResourceAsStream("/" + DEFAULT_PROPERTY_FILE);
         properties.load(input);
         properties.forEach((key, value) -> DEFAULT_VALUES.put(key.toString(), value.toString()));
      } catch (IOException e) {
         log.log(Level.SEVERE, "Unable to load default property values: ", e);
      }
   }

   /**
    * Returns a List object of collection names in system database.
    *
    * @return the list of system collection names
    */
   // TODO: useless method?
   public List<String> getAllSystemCollections() {
      return systemDataStorage.getAllCollections();
   }

   /**
    * Returns a list of DataDocument objects representing configuration entries of given collection.
    *
    * @param collectionName
    *       the name of collection
    * @return the list of all system configuration entries
    * @throws CollectionNotFoundException
    */
   // TODO: useless method?
   public List<DataDocument> getSystemConfigurations(String collectionName) throws CollectionNotFoundException {
      if (!isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }

      return systemDataStorage.search(collectionName, null, null, 0, 0);
   }

   /**
    * Returns an Optional String with the specified present non-null value of given key in the collection.
    *
    * @param collectionName
    *       the name of collection
    * @param key
    *       the key of given collection
    * @return string value of given key
    * @throws CollectionNotFoundException
    * @throws AttributeNotFoundException
    */
   public Optional<String> getConfigurationString(final String collectionName, final String key) throws CollectionNotFoundException, AttributeNotFoundException {
      final Object value = readValueFromDb(collectionName, key);

      if (value == null && DEFAULT_VALUES.containsKey(key)) {
         return Optional.of(DEFAULT_VALUES.get(key));
      } else if (value != null) {
         return Optional.of(value.toString());
      }

      return Optional.empty();
   }

   /**
    * Returns an Optional Integer with the specified present non-null value of given key in the collection.
    *
    * @param collectionName
    *       the name of collection
    * @param key
    *       the key of given collection
    * @return integer value of given key
    * @throws CollectionNotFoundException
    * @throws AttributeNotFoundException
    */
   public Optional<Integer> getConfigurationInteger(final String collectionName, final String key) throws CollectionNotFoundException, AttributeNotFoundException {
      final Optional<String> value = getConfigurationString(collectionName, key);

      if (value.isPresent()) {
         return Optional.of(Integer.parseInt(value.get()));
      } else {
         return Optional.empty();
      }
   }

   /**
    * Returns an Optional DataDocument with the specified present non-null value of given key in the collection.
    *
    * @param collectionName
    *       the name of collection
    * @param key
    *       the key of given collection
    * @return DataDocument object of given key
    * @throws CollectionNotFoundException
    * @throws AttributeNotFoundException
    */
   public Optional<DataDocument> getConfigurationDocument(final String collectionName, final String key) throws CollectionNotFoundException, AttributeNotFoundException {
      final Object value = readValueFromDb(collectionName, key);

      if (value != null && value instanceof Document) {
         DataDocument document = new DataDocument();
         document.putAll((Map<? extends String, ?>) value);

         return Optional.of(document);
      }

      return Optional.empty();
   }

   /**
    * Sets a new String value for given key in system configuration entry. If the given key exists, its value will be updated.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param key
    *       the key of configuration field
    * @param value
    *       the String value of the given key
    * @throws CollectionNotFoundException
    * @throws NullParameterException
    */
   public void setConfigurationString(final String collectionName, final String key, final String value) throws CollectionNotFoundException, NullParameterException {
      writeValueToDb(collectionName, key, value);
   }

   /**
    * Sets a new Integer value for given key in system configuration entry. If the given key exists, its value will be updated.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param key
    *       the key of configuration field
    * @param value
    *       the Integer value of the given key
    * @throws CollectionNotFoundException
    * @throws NullParameterException
    */
   public void setConfigurationInteger(final String collectionName, final String key, final int value) throws CollectionNotFoundException, NullParameterException {
      writeValueToDb(collectionName, key, value);
   }

   /**
    * Sets a new DataDocument document for given key in system configuration entry. If the given document exists, it will be updated.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param key
    *       the key of configuration field
    * @param configurationDocument
    *       the DataDocument object representing a configuration document
    * @throws CollectionNotFoundException
    * @throws NullParameterException
    */
   public void setConfigurationDocument(final String collectionName, final String key, final DataDocument configurationDocument) throws CollectionNotFoundException, NullParameterException {
      writeValueToDb(collectionName, key, configurationDocument);
   }

   /**
    * Restores a configuration field specified by its key to default value.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param key
    *       the key of configuration field
    * @throws CollectionNotFoundException
    */
   public void resetToDefaultConfiguration(final String collectionName, final String key) throws CollectionNotFoundException {
      if (!isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }

      if (DEFAULT_VALUES.containsKey(key)) {
         String user = userFacade.getUserEmail();
         Optional<DataDocument> userConfig = getUserConfiguration(user);

         if (userConfig.isPresent()) {
            String id = userConfig.get().get(ID_KEY).toString();

            systemDataStorage.removeAttribute(collectionName, id, key);

            DataDocument defaultConfigDocument = new DataDocument();
            defaultConfigDocument.put(key, DEFAULT_VALUES.get(key));

            systemDataStorage.updateDocument(collectionName, defaultConfigDocument, id, TARGET_VERSION_DISABLED);
         } else {
            // TODO: exception?
         }
      }
   }

   /**
    * Restores all configuration fields to default values.
    *
    * @param collectionName
    *       the name of collection in system database
    * @throws CollectionNotFoundException
    */
   public void resetAllToDefaults(final String collectionName) throws CollectionNotFoundException {
      if (!isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }

      String user = userFacade.getUserEmail();
      Optional<DataDocument> userConfig = getUserConfiguration(user);

      if (userConfig.isPresent()) {
         String id = userConfig.get().get(ID_KEY).toString();
         systemDataStorage.dropDocument(collectionName, id);

         // TODO: test
         DataDocument defaultConfigDocument = new DataDocument();
         defaultConfigDocument.put(USER_EMAIL_KEY, user);

         for (String defaultKey : DEFAULT_VALUES.keySet()) {
            defaultConfigDocument.put(defaultKey, DEFAULT_VALUES.get(defaultKey));
         }

         systemDataStorage.createDocument(collectionName, defaultConfigDocument);
      } else {
         // TODO: exception?
      }
   }

   /**
    * Sets a new object value for given key in system configuration entry. If the given key exists, its value will be updated.
    *
    * @param collectionName
    *       the name of collection in system database
    * @param key
    *       the key of configuration field
    * @param value
    *       the Object value of the given key
    * @throws CollectionNotFoundException
    * @throws NullParameterException
    */
   private void writeValueToDb(final String collectionName, final String key, final Object value) throws CollectionNotFoundException, NullParameterException {
      if (!isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }

      if (key == null) {
         throw new NullParameterException(ErrorMessageBuilder.nullKey());
      }

      // if default value for given key exists, do not add this key-value to system dbs
      if (!DEFAULT_VALUES.containsKey(key)) {
         final String user = userFacade.getUserEmail();
         Optional<DataDocument> userConfig = getUserConfiguration(user);

         DataDocument configDocument;
         String id = "";

         // if user's system configuration entry does exist in database
         if (userConfig.isPresent()) {
            configDocument = userConfig.get();
            id = configDocument.get(ID_KEY).toString();
         } else {
            configDocument = new DataDocument();
            // TODO: should be primary default values inserted from DEFAULT_VALUES?
            configDocument.put(USER_EMAIL_KEY, user);
         }

         if (value instanceof String) {
            configDocument.put(key, value.toString());
         }

         if (value instanceof Integer) {
            configDocument.put(key, Integer.valueOf(value.toString()));
         }

         if (value instanceof DataDocument) {
            configDocument.put(key, value);
         }

         if (userConfig.isPresent()) {
            systemDataStorage.updateDocument(collectionName, configDocument, id, TARGET_VERSION_DISABLED);
         } else {
            systemDataStorage.createDocument(collectionName, configDocument);
         }
      }
   }

   /**
    * Reads a value of given key from system database.
    *
    * @param collectionName
    *       the collection name
    * @param key
    *       the key of given collection
    * @return the value of given key
    * @throws CollectionNotFoundException
    * @throws AttributeNotFoundException
    */
   private Object readValueFromDb(final String collectionName, final String key) throws CollectionNotFoundException, AttributeNotFoundException {
      final String user = userFacade.getUserEmail();

      if (!isDatabaseCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }

      String filter = "{" + USER_EMAIL_KEY + ": \"" + user + "\"}";
      // every single user has only one configuration entry
      DataDocument userConfig = systemDataStorage.search(collectionName, filter, null, 0, 0).get(0);

      if (!userConfig.containsKey(key)) {
         throw new AttributeNotFoundException(ErrorMessageBuilder.attributeNotFoundString(key, collectionName));
      }

      return userConfig.get(key);
   }

   /**
    * Finds out if database has given collection.
    *
    * @param collectionName
    *       name of the collection
    * @return true if database has given collection
    */
   private boolean isDatabaseCollection(final String collectionName) {
      return getAllSystemCollections().contains(collectionName);
   }

   /**
    * Returns user configuration of given user email address.ø
    *
    * @param user
    *       email address of logged user
    * @return Optional object with DataDocument configuration entry
    */
   private Optional<DataDocument> getUserConfiguration(final String user) {
      String filter = "{" + USER_EMAIL_KEY + ": \"" + user + "\"}";
      List<DataDocument> configs = systemDataStorage.search(USER_CONFIG, filter, null, 0, 0);

      if (!configs.isEmpty()) {
         return Optional.of(configs.get(0));
      }

      return Optional.empty();
   }
}
