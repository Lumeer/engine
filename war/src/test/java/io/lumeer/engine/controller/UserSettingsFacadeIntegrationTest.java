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
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.dto.UserSettings;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 * Tests for UserSettingsFacade
 */
@RunWith(Arquillian.class)
public class UserSettingsFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private UserSettingsFacade userSettingsFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserFacade userFacade;

   @Before
   public void setUp() throws Exception {
      systemDataStorage.dropManyDocuments(LumeerConst.UserSettings.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      systemDataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      systemDataStorage.dropManyDocuments(LumeerConst.Project.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void readUserSettingsTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org1", "Organization"));
      organizationFacade.setOrganizationCode("org1");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org1", "proj1"));

      UserSettings userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org1");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");
   }

   @Test
   public void upsertUserSettingsTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org1", "Organization"));
      organizationFacade.createOrganization(new Organization("org3", "Organization"));
      organizationFacade.setOrganizationCode("org1");
      projectFacade.createProject(new Project("proj1", "Project"));
      organizationFacade.setOrganizationCode("org3");
      projectFacade.createProject(new Project("projXYZ", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org1", "proj1"));

      UserSettings userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org1");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");

      userSettingsFacade.upsertUserSettings(new UserSettings("org3", null));
      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org3");
      assertThat(userSettings.getDefaultProject()).isNull();

      userSettingsFacade.upsertUserSettings(new UserSettings("org3", "projXYZ"));
      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org3");
      assertThat(userSettings.getDefaultProject()).isEqualTo("projXYZ");
   }

   @Test
   public void removeUserSettingsTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org1", "Organization"));
      organizationFacade.setOrganizationCode("org1");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org1", "proj1"));

      assertThat(userSettingsFacade.readUserSettings().getDefaultOrganization()).isNotNull();
      userSettingsFacade.removeUserSettings();
      assertThat(userSettingsFacade.readUserSettings().getDefaultOrganization()).isNull();
   }

}
