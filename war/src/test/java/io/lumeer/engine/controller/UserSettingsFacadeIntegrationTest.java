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
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.dto.UserSettings;
import io.lumeer.engine.api.exception.InvalidValueException;

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
      organizationFacade.createOrganization(new Organization("org11", "Organization"));
      organizationFacade.createOrganization(new Organization("org13", "Organization"));
      organizationFacade.setOrganizationCode("org11");
      projectFacade.createProject(new Project("proj1", "Project"));
      organizationFacade.setOrganizationCode("org13");
      projectFacade.createProject(new Project("projXYZ", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org11", "proj1"));

      UserSettings userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org11");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");

      userSettingsFacade.upsertUserSettings(new UserSettings("org13", null));
      userSettings = userSettingsFacade.readUserSettings();
      // same values because of bad upsert request
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org11");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");

      userSettingsFacade.upsertUserSettings(new UserSettings("org13", "projXYZ"));
      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org13");
      assertThat(userSettings.getDefaultProject()).isEqualTo("projXYZ");
   }

   @Test
   public void removeUserSettingsTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org21", "Organization"));
      organizationFacade.setOrganizationCode("org21");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org21", "proj1"));

      assertThat(userSettingsFacade.readUserSettings().getDefaultOrganization()).isNotNull();
      userSettingsFacade.removeUserSettings();
      assertThat(userSettingsFacade.readUserSettings().getDefaultOrganization()).isNull();
   }

   @Test
   public void organizationDoesntExistTest() throws Exception{
      organizationFacade.createOrganization(new Organization("org31", "Organization"));
      organizationFacade.setOrganizationCode("org31");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org31", "proj1"));

      assertThatThrownBy(() -> userSettingsFacade.upsertUserSettings(new UserSettings("org32", "proj1"))).isInstanceOf(InvalidValueException.class);
   }

   @Test
   public void projectDoesntExistTest() throws Exception{
      organizationFacade.createOrganization(new Organization("org41", "Organization"));
      organizationFacade.setOrganizationCode("org41");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org41", "proj1"));

      assertThatThrownBy(() -> userSettingsFacade.upsertUserSettings(new UserSettings("org41", "proj9"))).isInstanceOf(InvalidValueException.class);
   }

}
