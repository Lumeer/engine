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
import io.lumeer.engine.exception.CollectionNotFoundException;
import io.lumeer.engine.util.ConfigurationManipulator;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
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
   private static final String USER_CONFIG = "config.user";
   private static final String TEAM_CONFIG = "config.team";

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private Logger log;

   @Inject
   private UserFacade userFacade;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   /**
    * Sets default values loaded from system environment or predefined (local resources).
    */
   @PostConstruct
   private void setDefaultValues() {
      String envDefaults = System.getenv("lumeer.defaults");
      if (envDefaults == null) {
         envDefaults = DEFAULT_PROPERTY_FILE;
      }

      final Properties properties = new Properties();
      try {
         // TODO: why input returns null?
         final InputStream input = ConfigurationFacade.class.getResourceAsStream("/" + DEFAULT_PROPERTY_FILE);
         if (input != null) {
            properties.load(input);
            properties.forEach((key, value) -> DEFAULT_VALUES.put(key.toString(), value.toString()));
         }
      } catch (IOException e) {
         log.log(Level.SEVERE, "Unable to load default property values: ", e);
      }
   }

   /**
    * Returns an Optional String value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   public Optional<String> getConfigurationString(final String key) throws CollectionNotFoundException {
      return Optional.of(getConfiguration(key).toString());
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   public Optional<Integer> getConfigurationInteger(final String key) throws CollectionNotFoundException {
      return Optional.of(Integer.parseInt(getConfigurationString(key).get()));
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   public Optional<DataDocument> getConfigurationDocument(final String key) throws CollectionNotFoundException {
      final Object value = getConfiguration(key);

      if (value instanceof DataDocument) {
         DataDocument document = new DataDocument();
         document.putAll((Map<? extends String, ?>) value);
         return Optional.of(document);
      }
      return Optional.empty();
   }

   /**
    * Sets a new key-String value to configuration entry for currently logged user. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the String value of the given key
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   public void setConfigurationString(final String key, final String value) throws CollectionNotFoundException {
      setConfiguration(key, value);
   }

   /**
    * Sets a new key-Integer value to configuration entry for currently logged user. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the Integer value of the given key
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   public void setConfigurationInteger(final String key, final int value) throws CollectionNotFoundException {
      setConfiguration(key, value);
   }

   /**
    * Sets a new key-DataDocument value to configuration entry for currently logged user. If the given key exists, the document will be updated.
    *
    * @param key
    *       the name of key
    * @param document
    *       the DataDocument of the given key
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   public void setConfigurationDocument(final String key, final DataDocument document) throws CollectionNotFoundException {
      setConfiguration(key, document);
   }

   /**
    * Removes all system configuration of currently logged user. Does not delete user's configuration entry!
    *
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   public void resetConfiguration() throws CollectionNotFoundException {
      String user = userFacade.getUserEmail();
      configurationManipulator.resetConfiguration(USER_CONFIG, user);
   }

   /**
    * Removes the specified attribute located in configuration field.
    *
    * @param attributeName
    *       the name of attribute to remove
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   public void resetConfigurationAttribute(final String attributeName) throws CollectionNotFoundException {
      String user = userFacade.getUserEmail();
      configurationManipulator.resetConfigurationAttribute(USER_CONFIG, user, attributeName);
   }

   /**
    * Sets a new key-Object value to configuration entry for currently logged user. If the given key exists, the document will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the Object value of the given key
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   private void setConfiguration(final String key, final Object value) throws CollectionNotFoundException {
      final String user = userFacade.getUserEmail();

      // if default value for given key does not exist in DEFAULT_VALUES, insert the value to configuration entry
      if (!DEFAULT_VALUES.containsKey(key)) {
         configurationManipulator.setConfiguration(USER_CONFIG, user, key, value);
      }
   }

   /**
    * Returns an Object value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Objectvalue of the given key
    * @throws CollectionNotFoundException
    *       if the configuration collection does not exist
    */
   private Object getConfiguration(final String key) throws CollectionNotFoundException {
      String user = userFacade.getUserEmail();
      Object conf;

      if ((conf = configurationManipulator.getConfiguration(USER_CONFIG, user, key)) == null) {
         if ((conf = configurationManipulator.getConfiguration(TEAM_CONFIG, user, key)) == null) {
            return DEFAULT_VALUES.get(key);
         }
      }
      return conf;
   }
}
