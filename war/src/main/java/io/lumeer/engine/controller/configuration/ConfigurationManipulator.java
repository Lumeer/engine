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
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.mongodb.MongoUtils;

import com.mongodb.client.model.Filters;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Manipulates specified configuration properties.
 *
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@SessionScoped
public class ConfigurationManipulator implements Serializable {

   // SYSTEM DATABASE ENTRIES STRUCTURE
   // collection _configuration_user:
   /* { _id: …,
      name: pepa@zdepa.cz ,
      config: {
         db.host: “...”,
         string_property: “value”,
         int_property: 42,
         document_property: {
            another_string_property: “...”,
            …
         }
      }
    }*/

   //collection _configuration_team:
    /* { _id: ... ,
      name: “redhat”,
      config: {
         db.host: “...”,
         string_property: “value”,
         int_property: 42,
         document_property:{
            another_string_property: “...”,
            …
         }
      }
    }
    */

   public static final String NAME_KEY = "name";
   public static final String CONFIG_KEY = "config";
   public static final String ID_KEY = "_id";

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
      if (configuration.isPresent()) {
         DataDocument config = (DataDocument) configuration.get().get(CONFIG_KEY);
         if (!config.containsKey(attributeName)) {
            return;
         }
         String id = configuration.get().getString(ID_KEY);
         systemDataStorage.dropAttribute(collectionName, id, CONFIG_KEY + "." + attributeName);
      } else {
         createSimpleConfigurationEntry(collectionName, nameValue);
      }
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
      if (configuration.isPresent()) {
         // if configuration entry exists, reset 'config' field
         String id = configuration.get().getString(ID_KEY);
         configuration.get().remove(CONFIG_KEY);
         configuration.get().put(CONFIG_KEY, new DataDocument());
         systemDataStorage.updateDocument(collectionName, configuration.get(), id);
      } else {
         createSimpleConfigurationEntry(collectionName, nameValue);
      }
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
      writeValueToDb(collectionName, nameValue, key, value);
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
      Optional<Object> conf = readValueFromDb(collectionName, nameValue, key);
      if (conf.isPresent()) {
         return conf.get();
      }
      return null;
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
      if (systemDataStorage.hasCollection(collectionName)) {
         if (nameValue == null) {
            return Optional.empty();
         }

         String filter = MongoUtils.convertBsonToJson(Filters.eq(NAME_KEY, nameValue));
         List<DataDocument> configs = systemDataStorage.search(collectionName, filter, null, 0, 0);

         if (!configs.isEmpty()) {
            return Optional.of(configs.get(0));
         }
      }

      return Optional.empty();
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
   private void writeValueToDb(final String collectionName, final String nameValue, final String key, final Object value) {
      if (nameValue == null || key == null || value == null) {
         return;
      }

      // configuration represents the configuration document of the given nameValue
      Optional<DataDocument> configuration = getConfigurationEntry(collectionName, nameValue);

      DataDocument newConfiguration = new DataDocument();
      DataDocument configDocument = new DataDocument();
      String id = "";

      if (configuration.isPresent()) {
         id = configuration.get().getString(ID_KEY);
         configDocument = (DataDocument) configuration.get().get(CONFIG_KEY);
      } else {
         newConfiguration = new DataDocument();
         newConfiguration.put(NAME_KEY, nameValue);
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

      newConfiguration.put(CONFIG_KEY, configDocument);

      if (configuration.isPresent()) {
         systemDataStorage.updateDocument(collectionName, newConfiguration, id);
      } else {
         systemDataStorage.createDocument(collectionName, newConfiguration);
      }
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

      // configuration contains only configuration document of the given nameValue. If configuration document does not exists, Optional<DataDocument> will be empty.
      Optional<DataDocument> configuration = getConfigurationEntry(collectionName, nameValue);

      if (configuration.isPresent()) {
         // config contains document of 'config' key
         DataDocument config = (DataDocument) configuration.get().get(CONFIG_KEY);

         if (!config.containsKey(key)) {
            return Optional.empty();
         }

         Object value = config.get(key);
         if (value == null) {
            return Optional.empty();
         }

         return Optional.of(value);
      }

      // if configuration document is not found, it will be created
      createSimpleConfigurationEntry(collectionName, nameValue);
      return Optional.empty();
   }

   /**
    * Creates new configuration entry with '_id', 'name' and 'config' fields.ø
    *
    * @param collectionName
    *       the name of collection
    * @param nameValue
    *       value of name field
    */
   private void createSimpleConfigurationEntry(final String collectionName, final String nameValue) {
      DataDocument configDocument = new DataDocument();
      configDocument.put(NAME_KEY, nameValue);
      configDocument.put(CONFIG_KEY, new DataDocument());
      systemDataStorage.createDocument(collectionName, configDocument);
   }
}
