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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.dto.UserSettings;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.UserSettingsFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User settings service tests.
 */
@RunWith(Arquillian.class)
public class UserSettingsServiceIntegrationTest extends IntegrationTestBase {

   @Inject
   private UserSettingsFacade userSettingsFacade;

   @Inject
   @SystemDataStorage
   DataStorage dataStorage;

   @Inject
   DataStorageDialect dataStorageDialect;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   private final String TARGET_URI = "http://localhost:8080";
   private final String PATH_PREFIX = PATH_CONTEXT + "/rest/settings/user/";

   @Before
   public void init() throws Exception {
      dataStorage.dropDocument(LumeerConst.UserSettings.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropDocument(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropDocument(LumeerConst.Project.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void readUserSettingsTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org1", "Organization"));
      organizationFacade.setOrganizationCode("org1");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org1", "proj1"));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(PATH_PREFIX)
            .request(MediaType.APPLICATION_JSON)
            .buildGet()
            .invoke();

      UserSettings userSettings = response.readEntity(UserSettings.class);
      assertThat(userSettings).isNotNull();
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

      ClientBuilder.newBuilder().build()
                   .target(TARGET_URI)
                   .path(PATH_PREFIX)
                   .request(MediaType.APPLICATION_JSON)
                   .buildPut(Entity.json(new UserSettings("org13", null)))
                   .invoke();

      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org11");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");

      ClientBuilder.newBuilder().build()
                   .target(TARGET_URI)
                   .path(PATH_PREFIX)
                   .request(MediaType.APPLICATION_JSON)
                   .buildPut(Entity.json(new UserSettings("org13", "projXYZ")))
                   .invoke();

      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org13");
      assertThat(userSettings.getDefaultProject()).isEqualTo("projXYZ");
   }

   @Test
   public void organizationDoesntExistTest() throws Exception {
      organizationFacade.createOrganization(new Organization("org21", "Organization"));
      organizationFacade.setOrganizationCode("org21");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org21", "proj1"));

      Response response = ClientBuilder.newBuilder().build()
                                      .target(TARGET_URI)
                                      .path(PATH_PREFIX)
                                      .request(MediaType.APPLICATION_JSON)
                                      .buildPut(Entity.json(new UserSettings("org23", "projXYZ")))
                                      .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
   }

   @Test
   public void projectDoesntExistTest() throws Exception{
      organizationFacade.createOrganization(new Organization("org31", "Organization"));
      organizationFacade.setOrganizationCode("org31");
      projectFacade.createProject(new Project("proj1", "Project"));
      userSettingsFacade.upsertUserSettings(new UserSettings("org31", "proj1"));

      Response response = ClientBuilder.newBuilder().build()
                                       .target(TARGET_URI)
                                       .path(PATH_PREFIX)
                                       .request(MediaType.APPLICATION_JSON)
                                       .buildPut(Entity.json(new UserSettings("org31", "projXYZ")))
                                       .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

   }


}
