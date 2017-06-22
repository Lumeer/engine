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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Config;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.controller.configuration.ConfigurationManipulator;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Optional;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class ConfigurationFacadeIntegrationTest extends IntegrationTestBase {

   private final String DBHOST_KEY = "db_host_test";

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Inject
   private UserFacade userFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Before
   public void setUp() throws Exception {
      projectFacade.dropProject("CFSProject");
      organizationFacade.dropOrganization("CFSOrganization");
      organizationFacade.createOrganization(new Organization("CFSOrganization", "Configuration"));
      organizationFacade.setOrganizationCode("CFSOrganization");

      projectFacade.createProject(new Project("CFSProject", "Configuration"));
      projectFacade.setCurrentProjectCode("CFSProject");

      systemDataStorage.dropManyDocuments(ConfigurationFacade.USER_CONFIG_COLLECTION, dataStorageDialect.documentFilter("{}"));
      systemDataStorage.dropManyDocuments(ConfigurationFacade.PROJECT_CONFIG_COLLECTION, dataStorageDialect.documentFilter("{}"));
      systemDataStorage.dropManyDocuments(ConfigurationFacade.ORGANIZATION_CONFIG_COLLECTION, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void testGetConfigurationString() throws Exception {
      // default value
      final String defaultString = "localhost";
      Optional<String> value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(defaultString);

      // user in global
      final String userInGlobal = "userInGlobal";
      configurationFacade.setUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL, new Config(DBHOST_KEY, userInGlobal));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(userInGlobal);

      // organization
      final String organization = "organization";
      configurationFacade.setOrganizationConfiguration(new Config(DBHOST_KEY, organization));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(organization);

      // user in organization
      final String userInOrg = "userInOrg";
      configurationFacade.setUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_ORGANIZATION, new Config(DBHOST_KEY, userInOrg));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(userInOrg);

      // project
      final String project = "project";
      configurationFacade.setProjectConfiguration(new Config(DBHOST_KEY, project));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(project);

      // user in project
      final String userInProj = "userInProj";
      configurationFacade.setUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_PROJECT, new Config(DBHOST_KEY, userInProj));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(userInProj);
   }

   @Test
   public void testGetConfigurationStringRestricted() throws Exception {
      final String organization = "organization";
      configurationFacade.setOrganizationConfiguration(new Config(DBHOST_KEY, organization, null, true));
      Optional<String> value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(organization);

      // restricted access by organization
      final String userInOrg = "userInOrg";
      configurationFacade.setUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_ORGANIZATION, new Config(DBHOST_KEY, userInOrg));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(organization);

      configurationFacade.setOrganizationConfiguration(new Config(DBHOST_KEY, organization, null, false));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(userInOrg);

      final String project = "project";
      configurationFacade.setProjectConfiguration(new Config(DBHOST_KEY, project, null, true));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(project);

      // restricted access by project
      final String userInProj = "userInProj";
      configurationFacade.setUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_PROJECT, new Config(DBHOST_KEY, userInProj));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(project);

      configurationFacade.setProjectConfiguration(new Config(DBHOST_KEY, project, null, false));
      value = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(value).contains(userInProj);

   }

   @Test
   public void testUserConfigurationManipulation() throws Exception {
      Config config1 = new Config("conf11", "value1");
      Config config2 = new Config("conf12", "value2");
      Config config3 = new Config("conf13", "value3");

      configurationFacade.setUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL, config1);
      assertThat(configurationFacade.getUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL, config1.getKey())).isNotNull();
      configurationFacade.resetUserConfigurationAttribute(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL, config1.getKey());
      assertThat(configurationFacade.getUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL, config1.getKey())).isNull();

      configurationFacade.setUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL, Arrays.asList(config1, config2, config3), true);
      assertThat(configurationFacade.getUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL))
            .extracting("key").containsOnly(config1.getKey(), config2.getKey(), config3.getKey());
      configurationFacade.resetUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL);
      assertThat(configurationFacade.getUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL))
            .isEmpty();

   }

   @Test
   public void testUserLevelsConfigurationManipulation() throws Exception {
      Config config1 = new Config("conf21", "value1");
      Config config2 = new Config("conf22", "value2");
      Config config3 = new Config("conf23", "value3");
      Config config4 = new Config("conf24", "value4");
      Config config5 = new Config("conf25", "value5");

      configurationFacade.setUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL, Arrays.asList(config1, config2, config3), true);
      configurationFacade.setUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_PROJECT, Arrays.asList(config3, config4, config5), true);
      configurationFacade.setUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_ORGANIZATION, Arrays.asList(config1, config3, config5), true);

      assertThat(configurationFacade.getUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL))
            .extracting("key").containsOnly(config1.getKey(), config2.getKey(), config3.getKey());

      assertThat(configurationFacade.getUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_PROJECT))
            .extracting("key").containsOnly(config3.getKey(), config4.getKey(), config5.getKey());

      assertThat(configurationFacade.getUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_ORGANIZATION))
            .extracting("key").containsOnly(config1.getKey(), config3.getKey(), config5.getKey());

      configurationFacade.resetUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_PROJECT);
      configurationFacade.resetUserConfigurationAttribute(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL, config3.getKey());
      configurationFacade.setUserConfiguration(ConfigurationFacade.ConfigurationLevel.USER_ORGANIZATION, config4);

      assertThat(configurationFacade.getUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_GLOBAL))
            .extracting("key").containsOnly(config1.getKey(), config2.getKey());

      assertThat(configurationFacade.getUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_PROJECT))
            .isEmpty();

      assertThat(configurationFacade.getUserConfigurations(ConfigurationFacade.ConfigurationLevel.USER_ORGANIZATION))
            .extracting("key").containsOnly(config1.getKey(), config3.getKey(), config4.getKey(), config5.getKey());

   }

   @Test
   public void testProjectConfigurationManipulation() throws Exception {
      Config config1 = new Config("conf31", "value1");
      Config config2 = new Config("conf32", "value2");
      Config config3 = new Config("conf33", "value3");

      configurationFacade.setProjectConfiguration(config1);
      assertThat(configurationFacade.getProjectConfiguration(config1.getKey())).isNotNull();
      configurationFacade.resetProjectConfigurationAttribute(config1.getKey());
      assertThat(configurationFacade.getProjectConfiguration(config1.getKey())).isNull();

      configurationFacade.setProjectConfigurations(Arrays.asList(config1, config2, config3), true);
      assertThat(configurationFacade.getProjectConfigurations())
            .extracting("key").containsOnly(config1.getKey(), config2.getKey(), config3.getKey());
      configurationFacade.resetProjectConfiguration();
      assertThat(configurationFacade.getProjectConfigurations())
            .isEmpty();
   }

   @Test
   public void testOrganizationConfigurationManipulation() throws Exception {
      Config config1 = new Config("conf41", "value1");
      Config config2 = new Config("conf42", "value2");
      Config config3 = new Config("conf43", "value3");

      configurationFacade.setOrganizationConfiguration(config1);
      assertThat(configurationFacade.getOrganizationConfiguration(config1.getKey())).isNotNull();
      configurationFacade.resetOrganizationConfigurationAttribute(config1.getKey());
      assertThat(configurationFacade.getOrganizationConfiguration(config1.getKey())).isNull();

      configurationFacade.setOrganizationConfigurations(Arrays.asList(config1, config2, config3), true);
      assertThat(configurationFacade.getOrganizationConfigurations())
            .extracting("key").containsOnly(config1.getKey(), config2.getKey(), config3.getKey());
      configurationFacade.resetOrganizationConfiguration();
      assertThat(configurationFacade.getOrganizationConfigurations())
            .isEmpty();
   }

}
