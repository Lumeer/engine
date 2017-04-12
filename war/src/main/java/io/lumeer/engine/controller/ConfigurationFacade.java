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
import java.util.Optional;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Manipulates user configuration properties.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@SessionScoped
public class ConfigurationFacade implements Serializable {

   public enum ConfigurationLevel {
      USER, PROJECT, ORGANISATION
   }

   protected static final String USER_CONFIG_COLLECTION = "_config_user";
   protected static final String PROJECT_CONFIG_COLLECTION = "_config_project";
   protected static final String ORGANISATION_CONFIG_COLLECTION = "_config_org";

   @Inject
   private UserFacade userFacade;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   /**
    * Never ever replace the way of getting data storage here. Data storage configuration depends on this bean and this bean cannot inject it directly.
    *
    * @return Pre-configured data storage.
    */
   public StorageConnection getDataStorage() {
      return new StorageConnection(
            getConfigurationString(LumeerConst.DB_HOST_PROPERTY).orElse("localhost"),
            getConfigurationInteger(LumeerConst.DB_PORT_PROPERTY).orElse(27017),
            getConfigurationString(LumeerConst.DB_USER_PROPERTY).orElse("pepa"),
            getConfigurationString(LumeerConst.DB_PASSWORD_PROPERTY).orElse(""));
   }

   public String getDataStorageDatabase() {
      return getConfigurationString(LumeerConst.DB_NAME_PROPERTY).orElse("lumeer");
   }

   public Boolean getDataStorageUseSsl() {
      return Boolean.valueOf(getConfigurationString(LumeerConst.DB_USE_SSL).orElse("false"));
   }

   /**
    * Never ever replace the way of getting data storage here. Data storage configuration depends on this bean and this bean cannot inject it directly.
    *
    * @return Pre-configured system data storage.
    */
   public StorageConnection getSystemDataStorage() {
      return new StorageConnection(
            defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_HOST_PROPERTY),
            Integer.valueOf(defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_PORT_PROPERTY)),
            defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_USER_PROPERTY),
            defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_PASSWORD_PROPERTY));
   }

   public String getSystemDataStorageDatabase() {
      return defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_NAME_PROPERTY);
   }

   public Boolean getSystemDataStorageUseSsl() {
      return Boolean.valueOf(defaultConfigurationProducer.get(LumeerConst.SYSTEM_DB_USE_SSL));
   }

   /**
    * Returns an Optional String value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getConfigurationString(final String key) {
      final Object o;
      return (o = getConfiguration(key)) == null ? Optional.empty() : Optional.of(o.toString());
   }

   /**
    * Returns an Optional Integer value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getConfigurationInteger(final String key) {
      try {
         return getConfigurationString(key).map(Integer::parseInt);
      } catch (NumberFormatException nfe) {
         // TODO: Do not we want to at least trace log in such situations? Similar code fragments elsewhere.
         return Optional.empty();
      }
   }

   /**
    * Returns an Optional DataDocument value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getConfigurationDocument(final String key) {
      final Object value = getConfiguration(key);

      if (value != null && value instanceof DataDocument) {
         return Optional.of((DataDocument) value);
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
      final Object o;
      return (o = getUserConfiguration(key)) == null ? Optional.empty() : Optional.of(o.toString());
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getUserConfigurationInteger(final String key) {
      try {
         return getUserConfigurationString(key).map(Integer::parseInt);
      } catch (NumberFormatException nfe) {
         return Optional.empty();
      }

   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getUserConfigurationDocument(final String key) {
      final Object value = getUserConfiguration(key);
      if (value != null && value instanceof DataDocument) {
         return Optional.of((DataDocument) value);
      }

      return Optional.empty();
   }

   /**
    * Returns an Optional String value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getProjectConfigurationString(final String key) {
      final Object o;
      return (o = getProjectConfiguration(key)) == null ? Optional.empty() : Optional.of(o.toString());
   }

   /**
    * Returns an Optional Integer value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getProjectConfigurationInteger(final String key) {
      try {
         return getProjectConfigurationString(key).map(Integer::parseInt);
      } catch (NumberFormatException nfe) {
         return Optional.empty();
      }

   }

   /**
    * Returns an Optional DataDocument value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getProjectConfigurationDocument(final String key) {
      final Object value = getProjectConfiguration(key);
      if (value != null && value instanceof DataDocument) {
         return Optional.of((DataDocument) value);
      }

      return Optional.empty();
   }

   /**
    * Returns an Optional String value of the given key for currently logged organisation configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getOrganisationConfigurationString(final String key) {
      final Object o;
      return (o = getOrganisationConfiguration(key)) == null ? Optional.empty() : Optional.of(o.toString());
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged organisation configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getOrganisationConfigurationInteger(final String key) {
      try {
         return getOrganisationConfigurationString(key).map(Integer::parseInt);
      } catch (NumberFormatException nfe) {
         return Optional.empty();
      }

   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged organisation configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getOrganisationConfigurationDocument(final String key) {
      final Object value = getOrganisationConfiguration(key);
      if (value != null && value instanceof DataDocument) {
         return Optional.of((DataDocument) value);
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
      setConfiguration(ConfigurationLevel.USER, key, value);
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
      setConfiguration(ConfigurationLevel.USER, key, value);
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
      setConfiguration(ConfigurationLevel.USER, key, document);
   }

   /**
    * Sets a new key-String value to configuration entry for active project. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the String value of the given key
    */
   public void setProjectConfigurationString(final String key, final String value) {
      setConfiguration(ConfigurationLevel.PROJECT, key, value);
   }

   /**
    * Sets a new key-Integer value to configuration entry for active project. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the Integer value of the given key
    */
   public void setProjectConfigurationInteger(final String key, final int value) {
      setConfiguration(ConfigurationLevel.PROJECT, key, value);
   }

   /**
    * Sets a new key-DataDocument value to configuration entry for active project. If the given key exists, the document will be updated.
    *
    * @param key
    *       the name of key
    * @param document
    *       the DataDocument of the given key
    */
   public void setProjectConfigurationDocument(final String key, final DataDocument document) {
      setConfiguration(ConfigurationLevel.PROJECT, key, document);
   }

   /**
    * Sets a new key-String value to configuration entry for currently logged organisation. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the String value of the given key
    */
   public void setOrganisationConfigurationString(final String key, final String value) {
      setConfiguration(ConfigurationLevel.ORGANISATION, key, value);
   }

   /**
    * Sets a new key-Integer value to configuration entry for currently logged organisation. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the Integer value of the given key
    */
   public void setOrganisationConfigurationInteger(final String key, final int value) {
      setConfiguration(ConfigurationLevel.ORGANISATION, key, value);
   }

   /**
    * Sets a new key-DataDocument value to configuration entry for currently logged organisation. If the given key exists, the document will be updated.
    *
    * @param key
    *       the name of key
    * @param document
    *       the DataDocument of the given key
    */
   public void setOrganisationConfigurationDocument(final String key, final DataDocument document) {
      setConfiguration(ConfigurationLevel.ORGANISATION, key, document);
   }

   /**
    * Resets currently logged user configuration. The whole user configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetUserConfiguration() {
      resetConfiguration(ConfigurationLevel.USER, null);
   }

   /**
    * Removes the specified attribute located in user configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetUserConfigurationAttribute(final String attributeName) {
      resetConfiguration(ConfigurationLevel.USER, attributeName);
   }

   /**
    * Resets active project configuration. The whole project configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetProjectConfiguration() {
      resetConfiguration(ConfigurationLevel.PROJECT, null);
   }

   /**
    * Removes the specified attribute located in project configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetProjectConfigurationAttribute(final String attributeName) {
      resetConfiguration(ConfigurationLevel.PROJECT, attributeName);
   }

   /**
    * Resets currently logged organisation configuration. The whole organisation configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetOrganisationConfiguration() {
      // TODO: I do not like this approach. Why do we reset configuration by setting null? We can accidentally reset the configuration when null value is passed without us noticing it.
      resetConfiguration(ConfigurationLevel.ORGANISATION, null);
   }

   /**
    * Removes the specified attribute located in organisation configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetOrganisationConfigurationAttribute(final String attributeName) {
      resetConfiguration(ConfigurationLevel.ORGANISATION, attributeName);
   }

   /**
    * Resets the configuration of specified group (user or organisation). If attribute is null, it will remove the specified attribute located in configuration entry of the given group.
    *
    * @param level
    *       configuration level
    * @param attributeName
    *       If not null, it will reset just the specified field with the given name. Else, the whole config field will be reset, but configuration entry of the given group will not be dropped.
    */
   private void resetConfiguration(final ConfigurationLevel level, final String attributeName) {
      final String user = userFacade.getUserEmail();
      final String org = organizationFacade.getOrganisationId();
      final String project = projectFacade.getCurrentProjectId();

      if (attributeName != null) {
         // reset user configuration attribute
         switch (level) {
            case USER:
               configurationManipulator.resetConfigurationAttribute(USER_CONFIG_COLLECTION, org + "/" + project + "/" + user, attributeName);
               break;
            case PROJECT:
               configurationManipulator.resetConfigurationAttribute(PROJECT_CONFIG_COLLECTION, org + "/" + project, attributeName);
               break;
            case ORGANISATION:
               configurationManipulator.resetConfigurationAttribute(ORGANISATION_CONFIG_COLLECTION, org, attributeName);
               break;
         }
      } else {
         // reset currently logged user's configuration
         switch (level) {
            case USER:
               configurationManipulator.resetConfiguration(USER_CONFIG_COLLECTION, org + "/" + project + "/" + user);
               break;
            case PROJECT:
               configurationManipulator.resetConfiguration(PROJECT_CONFIG_COLLECTION, org + "/" + project);
               break;
            case ORGANISATION:
               configurationManipulator.resetConfiguration(ORGANISATION_CONFIG_COLLECTION, org);
               break;
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
      final String id = organizationFacade.getOrganisationId() + "/" + projectFacade.getCurrentProjectId() + "/" + userFacade.getUserEmail();
      return configurationManipulator.getConfiguration(USER_CONFIG_COLLECTION, id, key);
   }

   /**
    * Returns an Object value of the given key for project. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   private Object getProjectConfiguration(final String key) {
      return configurationManipulator.getConfiguration(PROJECT_CONFIG_COLLECTION, organizationFacade.getOrganisationId() + "/" + projectFacade.getCurrentProjectId(), key);
   }

   /**
    * Returns an Object value of the given key for organisation. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   private Object getOrganisationConfiguration(final String key) {
      return configurationManipulator.getConfiguration(ORGANISATION_CONFIG_COLLECTION, organizationFacade.getOrganisationId(), key);
   }

   /**
    * Returns an Object value of the given key. First, the method checks config.user collection, then config.organisation collection - returns first non-null value of the given key. Otherwise, it will return default value. If default value does not exist, the method will return null.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   private Object getConfiguration(final String key) {
      final String user = userFacade.getUserEmail();
      final String org = organizationFacade.getOrganisationId();
      final String project = projectFacade.getCurrentProjectId();
      Object conf = null;

      if ((conf = configurationManipulator.getConfiguration(USER_CONFIG_COLLECTION, org + "/" + project + "/" + user, key)) == null) {
         if ((conf = configurationManipulator.getConfiguration(PROJECT_CONFIG_COLLECTION, org + "/" + project, key)) == null) {
            if ((conf = configurationManipulator.getConfiguration(ORGANISATION_CONFIG_COLLECTION, org, key)) == null) {
               return defaultConfigurationProducer.get(key);
            }
         }
      }

      return conf;
   }

   /**
    * Sets a new key-Object value to the specified configuration entry. If the given key exists, the value of specified field will be updated.
    *
    * @param level
    *       configuration level
    * @param key
    *       the name of key
    * @param value
    *       the Object value of the given key
    */
   private void setConfiguration(final ConfigurationLevel level, final String key, final Object value) {
      final String user = userFacade.getUserEmail();
      final String org = organizationFacade.getOrganisationId();
      final String project = projectFacade.getCurrentProjectId();

      switch (level) {
         case USER:
            configurationManipulator.setConfiguration(USER_CONFIG_COLLECTION, org + "/" + project + "/" + user, key, value);
            break;
         case PROJECT:
            configurationManipulator.setConfiguration(PROJECT_CONFIG_COLLECTION, org + "/" + project, key, value);
            break;
         case ORGANISATION:
            configurationManipulator.setConfiguration(ORGANISATION_CONFIG_COLLECTION, org, key, value);
            break;
      }
   }

}
