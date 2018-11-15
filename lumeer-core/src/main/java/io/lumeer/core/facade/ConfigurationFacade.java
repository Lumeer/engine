/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.core.facade;

import io.lumeer.api.model.Config;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.facade.configuration.ConfigurationManipulator;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.Resources;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.StorageConnection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * Manipulates user configuration properties.
 */
@SessionScoped
public class ConfigurationFacade implements Serializable {

   private static final String DB_HOSTS_PROPERTY = "db_hosts";
   private static final String DB_NAME_PROPERTY = "db_name";
   private static final String DB_USER_PROPERTY = "db_user";
   private static final String DB_PASSWORD_PROPERTY = "db_passwd";
   private static final String DB_USE_SSL = "db_ssl";

   private static final String SYSTEM_DB_HOSTS_PROPERTY = "sys_db_hosts";
   private static final String SYSTEM_DB_NAME_PROPERTY = "sys_db_name";
   private static final String SYSTEM_DB_USER_PROPERTY = "sys_db_user";
   private static final String SYSTEM_DB_PASSWORD_PROPERTY = "sys_db_passwd";
   private static final String SYSTEM_DB_USE_SSL = "sys_db_ssl";

   private static final String ENVIRONMENT = "environment";

   public enum ConfigurationLevel {
      USER_GLOBAL, USER_PROJECT, USER_ORGANIZATION, PROJECT, ORGANIZATION
   }

   public enum DeployEnvironment {
      DEVEL, STAGING, PRODUCTION;
   }

   private static Logger log = Resources.produceLog(ConfigurationLevel.class.getName());

   protected static final String USER_CONFIG_COLLECTION = "_config_user";
   protected static final String PROJECT_CONFIG_COLLECTION = "_config_project";
   protected static final String ORGANIZATION_CONFIG_COLLECTION = "_config_organization";

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   /**
    * Never ever replace the way of getting data storage here. Data storage configuration depends on this bean and this bean cannot inject it directly.
    *
    * @return Pre-configured data storage.
    */
   public List<StorageConnection> getDataStorage() {
      final String hosts = getSystemConfigurationString(DB_HOSTS_PROPERTY).orElse("localhost:27017");
      final String db = getSystemConfigurationString(DB_USER_PROPERTY).orElse("pepa");
      final String pwd = getSystemConfigurationString(DB_PASSWORD_PROPERTY).orElse("");

      return getStorageConnections(hosts, db, pwd);
   }

   public String getDataStorageDatabase() {
      return getSystemConfigurationString(DB_NAME_PROPERTY).orElse("lumeer");
   }

   public Boolean getDataStorageUseSsl() {
      return Boolean.valueOf(getSystemConfigurationString(DB_USE_SSL).orElse("false"));
   }

   /**
    * Never ever replace the way of getting data storage here. Data storage configuration depends on this bean and this bean cannot inject it directly.
    *
    * @return Pre-configured system data storage.
    */
   public List<StorageConnection> getSystemDataStorage() {
      final String hosts = defaultConfigurationProducer.get(SYSTEM_DB_HOSTS_PROPERTY);
      final String db = defaultConfigurationProducer.get(SYSTEM_DB_USER_PROPERTY);
      final String pwd = defaultConfigurationProducer.get(SYSTEM_DB_PASSWORD_PROPERTY);

      return getStorageConnections(hosts, db, pwd);
   }

   private static List<StorageConnection> getStorageConnections(final String hosts, final String db, final String pwd) {
      final List<StorageConnection> result = new ArrayList<>();
      Arrays.asList(hosts.split(",")).forEach(host -> {
         String[] hostParts = host.split(":", 2);
         String hostName = hostParts[0];
         int port = 27017;
         if (hostParts.length > 1) {
            try {
               port = Integer.valueOf(hostParts[1]);
            } catch (NumberFormatException nfe) {
               // just keep original port no
            }
         }
         result.add(new StorageConnection(hostName, port, db, pwd));
      });

      return result;
   }

   public String getSystemDataStorageDatabase() {
      return defaultConfigurationProducer.get(SYSTEM_DB_NAME_PROPERTY);
   }

   public Boolean getSystemDataStorageUseSsl() {
      return Boolean.valueOf(defaultConfigurationProducer.get(SYSTEM_DB_USE_SSL));
   }

   public DeployEnvironment getEnvironment() {
      final String value = defaultConfigurationProducer.get(ENVIRONMENT);

      if (value != null) {
         final DeployEnvironment env = DeployEnvironment.valueOf(value.toUpperCase());

         return env != null ? env : DeployEnvironment.DEVEL;
      }

      return DeployEnvironment.DEVEL;
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
    * @param levelIn
    *       specify type of parent resource
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getUserConfigurationString(final ConfigurationLevel levelIn, final String key) {
      return createOptionalString(getUserConfiguration(levelIn, key));
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged user's configuration.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getUserConfigurationInteger(final ConfigurationLevel levelIn, final String key) {
      return createOptionalInteger(getUserConfiguration(levelIn, key));
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged user's configuration.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getUserConfigurationDocument(final ConfigurationLevel levelIn, final String key) {
      return createOptionalDataDocument(getUserConfiguration(levelIn, key));
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
    * Returns an Optional String value of the given key for currently logged organization configuration.
    *
    * @param key
    *       the name of key
    * @return Optional String value of the given key
    */
   public Optional<String> getOrganizationConfigurationString(final String key) {
      return createOptionalString(getOrganizationConfiguration(key));
   }

   /**
    * Returns an Optional Integer value of the given key for currently logged organization configuration.
    *
    * @param key
    *       the name of key
    * @return Optional Integer value of the given key
    */
   public Optional<Integer> getOrganizationConfigurationInteger(final String key) {
      return createOptionalInteger(getOrganizationConfiguration(key));
   }

   /**
    * Returns an Optional DataDocument value of the given key for currently logged organization configuration.
    *
    * @param key
    *       the name of key
    * @return Optional DataDocument value of the given key
    */
   public Optional<DataDocument> getOrganizationConfigurationDocument(final String key) {
      return createOptionalDataDocument(getOrganizationConfiguration(key));
   }

   /**
    * Returns an Config value of the given key for user. It will return null, if the value of the given key does not exist.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param key
    *       the name of key
    * @return Config value of the given key
    */
   public Config getUserConfiguration(final ConfigurationLevel levelIn, final String key) {
      String configName = userConfigName(levelIn, getOrganizationId(), getProjectId(), getUserId());
      return configurationManipulator.getConfiguration(USER_CONFIG_COLLECTION, configName, key);
   }

   /**
    * Returns an List of user configurations at specified level
    *
    * @param levelIn
    *       specify type of parent resource
    * @return List of configurations
    */
   public List<Config> getUserConfigurations(final ConfigurationLevel levelIn) {
      String configName = userConfigName(levelIn, getOrganizationId(), getProjectId(), getUserId());
      return configurationManipulator.getConfigurations(USER_CONFIG_COLLECTION, configName);
   }

   /**
    * Returns an Config value of the given key for project. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Config value of the given key
    */
   public Config getProjectConfiguration(final String key) {
      String configName = createConfigName(getOrganizationId(), getProjectId());
      return configurationManipulator.getConfiguration(PROJECT_CONFIG_COLLECTION, configName, key);
   }

   /**
    * Returns an List of project configurations
    *
    * @return List of configurations
    */
   public List<Config> getProjectConfigurations() {
      String configName = createConfigName(getOrganizationId(), getProjectId());
      return configurationManipulator.getConfigurations(PROJECT_CONFIG_COLLECTION, configName);
   }

   /**
    * Returns an Config value of the given key for organization. It will return null, if the value of the given key does not exist.
    *
    * @param key
    *       the name of key
    * @return Object value of the given key
    */
   public Config getOrganizationConfiguration(final String key) {
      return configurationManipulator.getConfiguration(ORGANIZATION_CONFIG_COLLECTION, getOrganizationId(), key);
   }

   /**
    * Returns an List of organization configurations
    *
    * @return List of configurations
    */
   public List<Config> getOrganizationConfigurations() {
      return configurationManipulator.getConfigurations(ORGANIZATION_CONFIG_COLLECTION, getOrganizationId());
   }

   /**
    * Sets a new key-value to configuration entry for currently logged user. If the given key exists, its value will be updated.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param config
    *       config object
    */
   public void setUserConfiguration(final ConfigurationLevel levelIn, final Config config) {
      setConfiguration(levelIn, config);
   }

   /**
    * Sets a new key-values for currently logged user.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param configs
    *       configuration objects
    * @param reset
    *       indicates whether reset configuration entries or not
    */
   public void setUserConfigurations(final ConfigurationLevel levelIn, final List<Config> configs, final boolean reset) {
      setConfigurations(levelIn, configs, reset);
   }

   /**
    * Sets a new key-value to configuration entry for active project. If the given key exists, its value will be updated.
    *
    * @param config
    *       config object
    */
   public void setProjectConfiguration(final Config config) {
      setConfiguration(ConfigurationLevel.PROJECT, config);
   }

   /**
    * Sets a new key-values for currently logged user.
    *
    * @param configs
    *       configuration objects
    * @param reset
    *       indicates whether reset configuration entries or not
    */
   public void setProjectConfigurations(final List<Config> configs, final boolean reset) {
      setConfigurations(ConfigurationLevel.PROJECT, configs, reset);
   }

   /**
    * Sets a new key-value to configuration entry for currently logged organization. If the given key exists, its value will be updated.
    *
    * @param config
    *       config object
    */
   public void setOrganizationConfiguration(final Config config) {
      setConfiguration(ConfigurationLevel.ORGANIZATION, config);
   }

   /**
    * Sets a new key-values for currently logged user.
    *
    * @param configs
    *       configuration objects
    * @param reset
    *       indicates whether reset configuration entries or not
    */
   public void setOrganizationConfigurations(final List<Config> configs, final boolean reset) {
      setConfigurations(ConfigurationLevel.ORGANIZATION, configs, reset);
   }

   /**
    * Resets currently logged user configuration. The whole user configuration entry will not be deleted from the system collection, just the config field!
    *
    * @param levelIn
    *       specify type of parent resource
    */
   public void resetUserConfiguration(final ConfigurationLevel levelIn) {
      resetConfiguration(levelIn);
   }

   /**
    * Removes the specified attribute located in user configuration entry.
    *
    * @param levelIn
    *       specify type of parent resource
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetUserConfigurationAttribute(final ConfigurationLevel levelIn, final String attributeName) {
      resetConfigurationAttribute(levelIn, attributeName);
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
    * Resets currently logged organization configuration. The whole organization configuration entry will not be deleted from the system collection, just the config field!
    */
   public void resetOrganizationConfiguration() {
      resetConfiguration(ConfigurationLevel.ORGANIZATION);
   }

   /**
    * Removes the specified attribute located in organization configuration entry.
    *
    * @param attributeName
    *       the name of attribute to remove
    */
   public void resetOrganizationConfigurationAttribute(final String attributeName) {
      resetConfigurationAttribute(ConfigurationLevel.ORGANIZATION, attributeName);
   }

   /**
    * Resets the configuration of specified group (user or organization).
    *
    * @param level
    *       configuration level
    */
   private void resetConfiguration(final ConfigurationLevel level) {
      final String user = getUserId();
      final String organization = getOrganizationId();
      final String project;

      switch (level) {
         case USER_GLOBAL:
            configurationManipulator.resetConfiguration(USER_CONFIG_COLLECTION, user);
            break;
         case USER_ORGANIZATION:
            String userOrgConfigName = createConfigName(organization, user);
            configurationManipulator.resetConfiguration(USER_CONFIG_COLLECTION, userOrgConfigName);
            break;
         case USER_PROJECT:
            project = getProjectId();
            String userProjConfigName = createConfigName(organization, project, user);
            configurationManipulator.resetConfiguration(USER_CONFIG_COLLECTION, userProjConfigName);
            break;
         case PROJECT:
            project = getProjectId();
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.resetConfiguration(PROJECT_CONFIG_COLLECTION, projectConfigName);
            break;
         case ORGANIZATION:
            configurationManipulator.resetConfiguration(ORGANIZATION_CONFIG_COLLECTION, organization);
            break;
      }
   }

   /**
    * Resets the configuration attribute of specified group (user or organization).
    *
    * @param level
    *       configuration level
    * @param attributeName
    *       configuration attribute name
    */
   private void resetConfigurationAttribute(final ConfigurationLevel level, final String attributeName) {
      final String user = getUserId();
      final String organization = getOrganizationId();
      final String project;

      switch (level) {
         case USER_GLOBAL:
            configurationManipulator.resetConfigurationAttribute(USER_CONFIG_COLLECTION, user, attributeName);
            break;
         case USER_ORGANIZATION:
            String userOrgConfigName = createConfigName(organization, user);
            configurationManipulator.resetConfigurationAttribute(USER_CONFIG_COLLECTION, userOrgConfigName, attributeName);
            break;
         case USER_PROJECT:
            project = getProjectId();
            String userProjConfigName = createConfigName(organization, project, user);
            configurationManipulator.resetConfigurationAttribute(USER_CONFIG_COLLECTION, userProjConfigName, attributeName);
            break;
         case PROJECT:
            project = getProjectId();
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.resetConfigurationAttribute(PROJECT_CONFIG_COLLECTION, projectConfigName, attributeName);
            break;
         case ORGANIZATION:
            configurationManipulator.resetConfigurationAttribute(ORGANIZATION_CONFIG_COLLECTION, organization, attributeName);
            break;
      }

   }

   /**
    * Returns an Config value of the given key by priority (User, Organization, Project).
    *
    * @param key
    *       the name of key
    * @return Config value of the given key
    */
   private Config getConfiguration(final String key) {
      Config projectConfig = getProjectConfiguration(key);
      boolean projectRestricted = projectConfig != null && projectConfig.isRestricted();
      Config organizationConfig = getOrganizationConfiguration(key);
      boolean organizationRestricted = organizationConfig != null && organizationConfig.isRestricted();

      Config userConfigProject = getUserConfiguration(ConfigurationLevel.USER_PROJECT, key);
      if (userConfigProject != null && !projectRestricted && !organizationRestricted) {
         return userConfigProject;
      }

      if (projectConfig != null && !organizationRestricted) {
         return projectConfig;
      }

      Config userConfigOrganization = getUserConfiguration(ConfigurationLevel.USER_ORGANIZATION, key);
      if (userConfigOrganization != null && !organizationRestricted) {
         return userConfigOrganization;
      }

      if (organizationConfig != null) {
         return organizationConfig;
      }

      Config userConfigGlobal = getUserConfiguration(ConfigurationLevel.USER_GLOBAL, key);
      if (userConfigGlobal != null) {
         return userConfigGlobal;
      }

      return new Config(key, defaultConfigurationProducer.get(key));
   }

   /**
    * Gets configuration value either from organization and when there is none present, it backs up to property files.
    *
    * @param key
    *       Property to obtain.
    * @return Configuration value.
    */
   private Optional<String> getSystemConfigurationString(final String key) {
      final Config organizationConfig = getOrganizationConfiguration(key);

      if (organizationConfig != null) {
         return createOptionalString(organizationConfig);
      } else {
         return createOptionalString(new Config(key, defaultConfigurationProducer.get(key)));
      }
   }

   /**
    * Sets a new key-Object value to the specified configuration entry. If the given key exists, the value of specified field will be updated.
    *
    * @param level
    *       configuration level
    * @param config
    *       configuration object
    */
   private void setConfiguration(final ConfigurationLevel level, final Config config) {
      final String user = getUserId();
      final String organization = getOrganizationId();
      final String project;

      switch (level) {
         case USER_GLOBAL:
            configurationManipulator.setConfiguration(USER_CONFIG_COLLECTION, user, config);
            break;
         case USER_ORGANIZATION:
            String userOrgConfigName = createConfigName(organization, user);
            configurationManipulator.setConfiguration(USER_CONFIG_COLLECTION, userOrgConfigName, config);
            break;
         case USER_PROJECT:
            project = getProjectId();
            String userProjConfigName = createConfigName(organization, project, user);
            configurationManipulator.setConfiguration(USER_CONFIG_COLLECTION, userProjConfigName, config);
            break;
         case PROJECT:
            project = getProjectId();
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.setConfiguration(PROJECT_CONFIG_COLLECTION, projectConfigName, config);
            break;
         case ORGANIZATION:
            configurationManipulator.setConfiguration(ORGANIZATION_CONFIG_COLLECTION, organization, config);
            break;
      }

   }

   private void setConfigurations(final ConfigurationLevel level, final List<Config> configs, final boolean reset) {
      final String user = getUserId();
      final String organization = getOrganizationId();
      final String project;

      switch (level) {
         case USER_GLOBAL:
            configurationManipulator.setConfigurations(USER_CONFIG_COLLECTION, user, configs, reset);
            break;
         case USER_ORGANIZATION:
            String userOrgConfigName = createConfigName(organization, user);
            configurationManipulator.setConfigurations(USER_CONFIG_COLLECTION, userOrgConfigName, configs, reset);
            break;
         case USER_PROJECT:
            project = getProjectId();
            String userProjConfigName = createConfigName(organization, project, user);
            configurationManipulator.setConfigurations(USER_CONFIG_COLLECTION, userProjConfigName, configs, reset);
            break;
         case PROJECT:
            project = getProjectId();
            String projectConfigName = createConfigName(organization, project);
            configurationManipulator.setConfigurations(PROJECT_CONFIG_COLLECTION, projectConfigName, configs, reset);
            break;
         case ORGANIZATION:
            configurationManipulator.setConfigurations(ORGANIZATION_CONFIG_COLLECTION, organization, configs, reset);
            break;
      }

   }

   private String userConfigName(ConfigurationLevel level, String organization, String project, String user) {
      String userConfigName;
      switch (level) {
         case USER_PROJECT:
            userConfigName = createConfigName(organization, project, user);
            break;
         case USER_ORGANIZATION:
            userConfigName = createConfigName(organization, user);
            break;
         default:
            userConfigName = createConfigName(user);
      }
      return userConfigName;
   }

   private static String createConfigName(String... args) {
      if (args.length == 0) {
         return "";
      }
      String val = args[0];
      for (int i = 1; i < args.length; i++) {
         val += "/" + args[i];
      }
      return val;
   }

   private static Optional<DataDocument> createOptionalDataDocument(Config config) {
      return config != null && config.getValue() instanceof DataDocument ? Optional.of((DataDocument) config.getValue()) : Optional.empty();
   }

   private static Optional<String> createOptionalString(Config config) {
      return config != null && config.getValue() != null ? Optional.of(config.getValue().toString()) : Optional.empty();
   }

   private static Optional<Integer> createOptionalInteger(Config config) {
      if (config == null || config.getValue() == null) {
         return Optional.empty();
      }

      try {
         return Optional.of(Integer.parseInt(config.getValue().toString()));
      } catch (NumberFormatException ex) {
         log.log(Level.FINEST, "Cannot get integer configuration value", ex);
         return Optional.empty();
      }
   }

   private String getUserId() {
      return authenticatedUser.getCurrentUserId();
   }

   private String getOrganizationId() {
      return workspaceKeeper.getOrganization().isPresent() ? workspaceKeeper.getOrganization().get().getId() : null;
   }

   private String getProjectId() {
      return workspaceKeeper.getProject().isPresent() ? workspaceKeeper.getProject().get().getId() : null;
   }

}
