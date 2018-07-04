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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.dto.Config;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class ConfigurationFacadeIT extends IntegrationTestBase {

   private final String DBHOST_KEY = "db_host_test";

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void setUp() {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      JsonProject project = new JsonProject();
      project.setCode(PROJECT_CODE);

      JsonPermissions projectPermissions = new JsonPermissions();
      projectPermissions.updateUserPermissions(new JsonPermission(createdUser.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      project.setPermissions(projectPermissions);
      projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);
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