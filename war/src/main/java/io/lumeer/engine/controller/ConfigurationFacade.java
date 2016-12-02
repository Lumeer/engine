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
import io.lumeer.engine.controller.configuration.ConfigurationManipulator;
import io.lumeer.engine.controller.configuration.DefaultConfigurationProducer;

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
      return getUserConfigurationString(LumeerConst.DB_NAME_PROPERTY).orElse("lumeer");
   }

   /**
    * Returns an Optional String value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getConfigurationString(final String key) {
      try {
         String value = getConfiguration(key).toString();
         if (value != null) {
            return Optional.of(value);
         }
      } catch (Exception e) {
         // nothing to do
      }
      return Optional.empty();
   }

   /**
    * Returns an Optional Integer value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getConfigurationInteger(final String key) {
      Optional<String> value = getConfigurationString(key);
      if (value.isPresent()) {
         return Optional.of(Integer.parseInt(value.get()));
      }
      return Optional.empty();
   }

   /**
    * Returns an Optional DataDocument value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getConfigurationDocument(final String key) {
      try {
         final Object value = getConfiguration(key);

         if (value instanceof DataDocument && value != null) {
            DataDocument document = new DataDocument();
            document.putAll((Map<? extends String, ?>) value);
            return Optional.of(document);
         }
      } catch (Exception e) {
         // nothing to do
      }
      return Optional.empty();
   }

   /**
    * Returns an Optional String value of the given key for currently logged user configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getUserConfigurationString(final String key) {
      try {
         String value = getUserConfiguration(key).toString();
         if (value != null) {
            return Optional.of(value);
         }
      } catch (Exception e) {
         // nothing to do
      }
      return Optional.empty();
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getUserConfigurationInteger(final String key) {
      Optional<String> value = getUserConfigurationString(key);
      if (value.isPresent()) {
         return Optional.of(Integer.parseInt(value.get()));
      }

      return Optional.empty();
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getUserConfigurationDocument(final String key) {
      try {
         final Object value = getUserConfiguration(key);
         if (value instanceof DataDocument && value != null) {
            DataDocument document = new DataDocument();
            document.putAll((Map<? extends String, ?>) value);
            return Optional.of(document);
         }
      } catch (Exception e) {
         // nothing to do
      }
      return Optional.empty();
   }

   /**
    * Returns an Optional String value of the given key for currently logged team configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getTeamConfigurationString(final String key) {
      try {
         String value = getTeamConfiguration(key).toString();
         if (value != null) {
            return Optional.of(value);
         }
      } catch (Exception e) {
         // nothing to do
      }
      return Optional.empty();
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged team configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getTeamConfigurationInteger(final String key) {
      Optional<String> value = getTeamConfigurationString(key);
      if (value.isPresent()) {
         return Optional.of(Integer.parseInt(value.get()));
      }
      return Optional.empty();
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged team configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getTeamConfigurationDocument(final String key) {
      try {
         final Object value = getTeamConfiguration(key);
         if (value instanceof DataDocument && value != null) {
            DataDocument document = new DataDocument();
            document.putAll((Map<? extends String, ?>) value);
            return Optional.of(document);
         }
      } catch (Exception e) {
         // nothing to do
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
   public void setUserConfigurationString(final String key, final String value) {
      setConfiguration(true, key, value);
   }

   /**
    * Sets a new key-Integer value to configuration entry for currently logged user. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the Integer value of the given key
    */
   public void setUserConfigurationInteger(final String key, final int value) {
      setConfiguration(true, key, value);
   }

   /**
    * Sets a new key-DataDocument value to configuration entry for currently logged user. If the given key exists, the document will be updated.
    *
    * @param key
    *       the name of key
    * @param document
    *       the DataDocument of the given key
    */
   public void setUserConfigurationDocument(final String key, final DataDocument document) {
      setConfiguration(true, key, document);
   }

   /**
    * Sets a new key-String value to configuration entry for currently logged team. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the String value of the given key
    */
   public void setTeamConfigurationString(final String key, final String value) {
      setConfiguration(false, key, value);
   }

   /**
    * Sets a new key-Integer value to configuration entry for currently logged team. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the Integer value of the given key
    */
   public void setTeamConfigurationInteger(final String key, final int value) {
      setConfiguration(false, key, value);
   }

   /**
    * Sets a new key-DataDocument value to configuration entry for currently logged team. If the given key exists, the document will be updated.
    *
    * @param key
    *       the name of key
    * @param document
    *       the DataDocument of the given key
    */
   public void setTeamConfigurationDocument(final String key, final DataDocument document) {
      setConfiguration(false, key, document);
   }

   /**
    * Resets currently logged user configuration. The whole user configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetUserConfiguration() {
      resetConfiguration(true, null);
   }

   /**
    * Removes the specified attribute located in user configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetUserConfigurationAttribute(final String attributeName) {
      resetConfiguration(true, attributeName);
   }

   /**
    * Resets currently logged team configuration. The whole team configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetTeamConfiguration() {
      resetConfiguration(false, null);
   }

   /**
    * Removes the specified attribute located in team configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetTeamConfigurationAttribute(final String attributeName) {
      resetConfiguration(false, attributeName);
   }

   /**
    * Resets the configuration of specified group (user or team). If attribute is null, it will remove the specified attribute located in configuration entry of the given group.
    *
    * @param groupOption
    *       true = user configuration, false = team configuration
    * @param attributeName
    *       If not null, it will reset just the specified field with the given name. Else, the whole config field will be reset, but configuration entry of the given group will not be dropped.
    */
   private void resetConfiguration(final boolean groupOption, final String attributeName) {
      String user = userFacade.getUserEmail();

      if (attributeName != null) {
         // reset user configuration attribute
         if (groupOption) {
            configurationManipulator.resetConfigurationAttribute(USER_CONFIG, user, attributeName);
         } else {
            // reset team configuration attribute
            configurationManipulator.resetConfigurationAttribute(TEAM_CONFIG, user, attributeName);
         }
      } else {
         // reset currently logged user's configuration
         if (groupOption) {
            configurationManipulator.resetConfiguration(USER_CONFIG, user);
         } else {
            // reset currently logged team's configuration
            configurationManipulator.resetConfiguration(TEAM_CONFIG, user);
         }
      }
   }

   /**
    * Returns an Object value of the given key for user. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   private Object getUserConfiguration(final String key) {
      return configurationManipulator.getConfiguration(USER_CONFIG, userFacade.getUserEmail(), key);
   }

   /**
    * Returns an Object value of the given key for team. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   private Object getTeamConfiguration(final String key) {
      return configurationManipulator.getConfiguration(TEAM_CONFIG, userFacade.getUserEmail(), key);
   }

   /**
    * Returns an Object value of the given key. First, the method checks config.user collection, then config.team collection - returns first non-null value of the given key. Otherwise, it will return default value. If default value does not exist, the method will return null.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
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

   /**
    * Sets a new key-Object value to the specified configuration entry. If the given key exists, the value of specified field will be updated.
    *
    * @param groupOption
    *       true = user configuration, false = team configuration
    * @param key
    *       the name of key
    * @param value
    *       the Object value of the given key
    */
   private void setConfiguration(final boolean groupOption, final String key, final Object value) {
      final String user = userFacade.getUserEmail();

      if (groupOption) {
         configurationManipulator.setConfiguration(USER_CONFIG, user, key, value);
      } else {
         configurationManipulator.setConfiguration(TEAM_CONFIG, user, key, value);
      }
   }

}
