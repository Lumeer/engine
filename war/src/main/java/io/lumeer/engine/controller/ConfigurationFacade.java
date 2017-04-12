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
import io.lumeer.engine.util.Resources;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Manipulates user configuration properties.
 */
@SessionScoped
public class ConfigurationFacade implements Serializable {

   public enum ConfigurationLevel {
      USER, PROJECT, ORGANISATION
   }

   private static Logger log = Resources.produceLog(ConfigurationLevel.class.getName());

   protected static final String USER_CONFIG_COLLECTION = "_config_user";
   protected static final String PROJECT_CONFIG_COLLECTION = "_config_project";
   protected static final String ORGANISATION_CONFIG_COLLECTION = "_config_organization";

   protected static final String FLAG_RESTRICTED = "restricted";

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
      return createOptionalString(getConfiguration(key));
   }

   /**
    * Returns an Optional Integer value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getConfigurationInteger(final String key) {
      return createOptionalInteger(getConfiguration(key));
   }

   /**
    * Returns an Optional DataDocument value of the given key.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getConfigurationDocument(final String key) {
      return createOptionalDataDocument(getConfiguration(key));
   }

   /**
    * Returns an Optional String value of the given key for currently logged user configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getUserConfigurationString(final String key) {
      return createOptionalString(getUserConfiguration(key));
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getUserConfigurationInteger(final String key) {
      return createOptionalInteger(getUserConfiguration(key));
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged user's configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getUserConfigurationDocument(final String key) {
      return createOptionalDataDocument(getUserConfiguration(key));
   }

   /**
    * Returns an Optional String value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getProjectConfigurationString(final String key) {
      return createOptionalString(getProjectConfiguration(key));
   }

   /**
    * Returns an Optional Integer value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getProjectConfigurationInteger(final String key) {
      return createOptionalInteger(getProjectConfiguration(key));
   }

   /**
    * Returns an Optional DataDocument value of the given key for active project configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getProjectConfigurationDocument(final String key) {
      return createOptionalDataDocument(getProjectConfiguration(key));
   }

   /**
    * Returns an Optional String value of the given key for currently logged organisation configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getOrganisationConfigurationString(final String key) {
      return createOptionalString(getOrganizationConfiguration(key));
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged organisation configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getOrganisationConfigurationInteger(final String key) {
      return createOptionalInteger(getOrganizationConfiguration(key));
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged organisation configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getOrganisationConfigurationDocument(final String key) {
      return createOptionalDataDocument(getOrganizationConfiguration(key));
   }

   /**
    * Sets a new key-value to configuration entry for currently logged user. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the value for the given key
    */
   public void setUserConfiguration(final String key, final Object value) {
      setConfiguration(ConfigurationLevel.USER, key, value, false);
   }

   /**
    * Sets a new key-value to configuration entry for active project. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the value of the given key
    * @param restricted
    *       indicates if the value of the specified field can be overridden by lower level configuration
    */
   public void setProjectConfiguration(final String key, final Object value, final boolean restricted) {
      setConfiguration(ConfigurationLevel.PROJECT, key, value, restricted);
   }

   /**
    * Sets a new key-value to configuration entry for currently logged organisation. If the given key exists, its value will be updated.
    *
    * @param key
    *       the name of key
    * @param value
    *       the value of the given key
    * @param restricted
    *       indicates if the value of the specified field can be overridden by lower level configuration
    */
   public void setOrganisationConfiguration(final String key, final Object value, final boolean restricted) {
      setConfiguration(ConfigurationLevel.ORGANISATION, key, value, restricted);
   }

   /**
    * Resets currently logged user configuration. The whole user configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetUserConfiguration() {
      resetConfiguration(ConfigurationLevel.USER);
   }

   /**
    * Removes the specified attribute located in user configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetUserConfigurationAttribute(final String attributeName) {
      resetConfigurationAttribute(ConfigurationLevel.USER, attributeName);
   }

   /**
    * Resets active project configuration. The whole project configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetProjectConfiguration() {
      resetConfiguration(ConfigurationLevel.PROJECT);
   }

   /**
    * Removes the specified attribute located in project configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetProjectConfigurationAttribute(final String attributeName) {
      resetConfigurationAttribute(ConfigurationLevel.PROJECT, attributeName);
   }

   /**
    * Resets currently logged organisation configuration. The whole organisation configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetOrganisationConfiguration() {
      resetConfiguration(ConfigurationLevel.ORGANISATION);
   }

   /**
    * Removes the specified attribute located in organisation configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetOrganisationConfigurationAttribute(final String attributeName) {
      resetConfigurationAttribute(ConfigurationLevel.ORGANISATION, attributeName);
   }

   /**
    * Resets the configuration of specified group (user or organisation).
    *
    * @param level
    *       configuration level
    */
   private void resetConfiguration(final ConfigurationLevel level) {
      final String user = userFacade.getUserEmail();
      final String organization = organizationFacade.getOrganizationId();
      final String project = projectFacade.getCurrentProjectId();

      switch (level) {
         case USER:
            String userConfigName = createConfigName(organization, project, user);
            configurationManipulator.resetConfiguration(USER_CONFIG_COLLECTION, userConfigName);
            break;
         case PROJECT:
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.resetConfiguration(PROJECT_CONFIG_COLLECTION, projectConfigName);
            break;
         case ORGANISATION:
            configurationManipulator.resetConfiguration(ORGANISATION_CONFIG_COLLECTION, organization);
            break;
      }
   }

   /**
    * Resets the configuration attribute of specified group (user or organisation).
    *
    * @param level
    *       configuration level
    * @param attributeName
    *       configuration attribute name
    */
   private void resetConfigurationAttribute(final ConfigurationLevel level, final String attributeName) {
      final String user = userFacade.getUserEmail();
      final String organization = organizationFacade.getOrganizationId();
      final String project = projectFacade.getCurrentProjectId();

      switch (level) {
         case USER:
            String userConfigName = createConfigName(organization, project, user);
            configurationManipulator.resetConfigurationAttribute(USER_CONFIG_COLLECTION, userConfigName, attributeName);
            break;
         case PROJECT:
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.resetConfigurationAttribute(PROJECT_CONFIG_COLLECTION, projectConfigName, attributeName);
            break;
         case ORGANISATION:
            configurationManipulator.resetConfigurationAttribute(ORGANISATION_CONFIG_COLLECTION, organization, attributeName);
            break;
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
      String configName = createConfigName(organizationFacade.getOrganizationId(), projectFacade.getCurrentProjectId(), userFacade.getUserEmail());
      return configurationManipulator.getConfiguration(USER_CONFIG_COLLECTION, configName, key);
   }

   /**
    * Returns an Object value of the given key for project. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   private Object getProjectConfiguration(final String key) {
      String configName = createConfigName(organizationFacade.getOrganizationId(), projectFacade.getCurrentProjectId());
      return configurationManipulator.getConfiguration(PROJECT_CONFIG_COLLECTION, configName, key);
   }

   /**
    * Returns an Object value of the given key for organisation. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   private Object getOrganizationConfiguration(final String key) {
      return configurationManipulator.getConfiguration(ORGANISATION_CONFIG_COLLECTION, organizationFacade.getOrganizationId(), key);
   }

   /**
    * Returns an Object value of the given key. First, the method checks config.user collection, then config.organisation collection - returns first non-null value of the given key. Otherwise, it will return default value. If default value does not exist, the method will return null.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   private Object getConfiguration(final String key) {
      String organization = organizationFacade.getOrganizationId();
      String project = projectFacade.getCurrentProjectId();

      String configName = createConfigName(organization, project);
      boolean projectRestricted = configurationManipulator.hasConfigurationAttributeFlag(PROJECT_CONFIG_COLLECTION, configName, key, FLAG_RESTRICTED);
      boolean organizationRestricted = configurationManipulator.hasConfigurationAttributeFlag(ORGANISATION_CONFIG_COLLECTION, organization, key, FLAG_RESTRICTED);

      Object userConfig = getUserConfiguration(key);
      if (userConfig != null && !projectRestricted && !organizationRestricted) {
         return userConfig;
      }

      Object projectConfig = getProjectConfiguration(key);
      if (projectConfig != null && !organizationRestricted) {
         return projectConfig;
      }

      Object organizationConfig = getOrganizationConfiguration(key);
      if (organizationConfig != null) {
         return organizationConfig;
      }

      return defaultConfigurationProducer.get(key);
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
    * @param restricted
    *       indicates if the value of the specified field can be overridden by lower level configuration
    */
   private void setConfiguration(final ConfigurationLevel level, final String key, final Object value, final boolean restricted) {
      final String user = userFacade.getUserEmail();
      final String organization = organizationFacade.getOrganizationId();
      final String project = projectFacade.getCurrentProjectId();

      Set<String> flags = new HashSet<>();
      if (restricted) {
         flags.add(FLAG_RESTRICTED);
      }

      switch (level) {
         case USER:
            String userConfigName = createConfigName(organization, project, user);
            configurationManipulator.setConfiguration(USER_CONFIG_COLLECTION, userConfigName, key, value, flags);
            break;
         case PROJECT:
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.setConfiguration(PROJECT_CONFIG_COLLECTION, projectConfigName, key, value, flags);
            break;
         case ORGANISATION:
            configurationManipulator.setConfiguration(ORGANISATION_CONFIG_COLLECTION, organization, key, value, flags);
            break;
      }
   }

   private static String createConfigName(String organization, String project, String user) {
      return organization + "/" + project + "/" + user;
   }

   private static String createConfigName(String organization, String project) {
      return organization + "/" + project;
   }

   private static Optional<DataDocument> createOptionalDataDocument(Object value) {
      return (value instanceof DataDocument) ? Optional.of((DataDocument) value) : Optional.empty();
   }

   private static Optional<String> createOptionalString(Object value) {
      return value != null ? Optional.of(value.toString()) : Optional.empty();
   }

   private static Optional<Integer> createOptionalInteger(Object value) {
      if (value == null) {
         return Optional.empty();
      }

      try {
         return Optional.of(Integer.parseInt(value.toString()));
      } catch (NumberFormatException ex) {
         log.log(Level.FINEST, "Cannot get integer configuration value", ex);
         return Optional.empty();
      }
   }

}
