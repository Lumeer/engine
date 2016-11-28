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
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.engine.controller.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.controller.configuration.ConfigurationManipulator;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Manipulates user configuration properties.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@SessionScoped
public class ConfigurationFacade implements Serializable {

   private static final String USER_CONFIG = "config.user";
   private static final String TEAM_CONFIG = "config.team";

   @Inject
   private UserFacade userFacade;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Produces
   @Named("dataStorageConnection")
   public StorageConnection getDataStorage() {
      return new StorageConnection(
            getConfigurationString(LumeerConst.DB_HOST_PROPERTY).orElse("localhost"),
            getConfigurationInteger(LumeerConst.DB_PORT_PROPERTY).orElse(27017),
            getConfigurationString(LumeerConst.DB_USER_PROPERTY).orElse(""),
            getConfigurationString(LumeerConst.DB_PASSWORD_PROPERTY).orElse(""));
   }

   @Produces
   @Named("dataStorageDatabase")
   public String getDataStorageDatabase() {
      return getConfigurationString(LumeerConst.DB_NAME_PROPERTY).orElse("lumeer");
   }

   /**
    * Returns an Optional String value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getConfigurationString(final String key) {
      return Optional.of(getConfiguration(key).toString());
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getConfigurationInteger(final String key) {
      return Optional.of(Integer.parseInt(getConfigurationString(key).get()));
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getConfigurationDocument(final String key) {
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
    */
   public void setConfigurationString(final String key, final String value) {
      setConfiguration(key, value);
   }

   /**
    * Sets a new key-Integer value to configuration entry for currently logged user. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the Integer value of the given key
    */
   public void setConfigurationInteger(final String key, final int value) {
      setConfiguration(key, value);
   }

   /**
    * Sets a new key-DataDocument value to configuration entry for currently logged user. If the given key exists, the document will be updated.
    *
    * @param key
    *       the name of key
    * @param document
    *       the DataDocument of the given key
    */
   public void setConfigurationDocument(final String key, final DataDocument document) {
      setConfiguration(key, document);
   }

   /**
    * Removes all system configuration of currently logged user. Does not delete user's configuration entry!
    */
   public void resetConfiguration() {
      String user = userFacade.getUserEmail();
      configurationManipulator.resetConfiguration(USER_CONFIG, user);
   }

   /**
    * Removes the specified attribute located in configuration field.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetConfigurationAttribute(final String attributeName){
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
    */
   private void setConfiguration(final String key, final Object value) {
      final String user = userFacade.getUserEmail();

      // if default value for given key does not exist in DEFAULT_VALUES, insert the value to configuration entry
      if (!defaultConfigurationProducer.getDefaultConfiguration().containsKey(key)) {
         configurationManipulator.setConfiguration(USER_CONFIG, user, key, value);
      }
   }

   /**
    * Returns an Object value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Objectvalue of the given key
    */
   private Object getConfiguration(final String key) {
      String user = userFacade.getUserEmail();
      Object conf;

      if ((conf = configurationManipulator.getConfiguration(USER_CONFIG, user, key)) == null) {
         if ((conf = configurationManipulator.getConfiguration(TEAM_CONFIG, user, key)) == null) {
            return defaultConfigurationProducer.getDefaultConfiguration().get(key);
         }
      }
      return conf;
   }
}
