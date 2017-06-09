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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.UserSettings;
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

   private final String TARGET_URI = "http://localhost:8080";
   private final String PATH_PREFIX = PATH_CONTEXT + "/rest/settings/user/";

   @Before
   public void init() throws Exception {
      dataStorage.dropDocument(LumeerConst.UserSettings.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void readUserSettingsTest() throws Exception {
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
   }

   @Test
   public void upsertUserSettingsTest() throws Exception {
      userSettingsFacade.upsertUserSettings(new UserSettings("org1", "proj1"));

      UserSettings userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org1");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");

      ClientBuilder.newBuilder().build()
                   .target(TARGET_URI)
                   .path(PATH_PREFIX)
                   .request(MediaType.APPLICATION_JSON)
                   .buildPut(Entity.json(new UserSettings("org3", null)))
                   .invoke();

      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org3");
      assertThat(userSettings.getDefaultProject()).isEqualTo("proj1");

      ClientBuilder.newBuilder().build()
                   .target(TARGET_URI)
                   .path(PATH_PREFIX)
                   .request(MediaType.APPLICATION_JSON)
                   .buildPut(Entity.json(new UserSettings(null, "projXYZ")))
                   .invoke();

      userSettings = userSettingsFacade.readUserSettings();
      assertThat(userSettings.getDefaultOrganization()).isEqualTo("org3");
      assertThat(userSettings.getDefaultProject()).isEqualTo("projXYZ");
   }

   @Test
   public void removeUserSettingsTest() throws Exception {
      userSettingsFacade.upsertUserSettings(new UserSettings( "org1", "proj1"));
      assertThat(userSettingsFacade.readUserSettings()).isNotNull();

      ClientBuilder.newBuilder().build()
                   .target(TARGET_URI)
                   .path(PATH_PREFIX)
                   .request(MediaType.APPLICATION_JSON)
                   .buildDelete()
                   .invoke();

      assertThat(userSettingsFacade.readUserSettings()).isNull();
   }

}
