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

   @Before
   public void setUp() throws Exception {
      systemDataStorage.dropManyDocuments(LumeerConst.UserSettings.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void readUserSettingsTest() throws Exception {
      final String user1 = "user1";
      final String user2 = "user2";
      systemDataStorage.createDocument(LumeerConst.UserSettings.COLLECTION_NAME,
            new UserSettings(user1, "org1", "proj1").toDataDocument());
      systemDataStorage.createDocument(LumeerConst.UserSettings.COLLECTION_NAME,
            new UserSettings(user2, "org1", "proj2").toDataDocument());

      UserSettings userSettings = userSettingsFacade.readUserSettings(user1);
      assertThat(userSettings.getActiveOrganization()).isEqualTo("org1");
      assertThat(userSettings.getActiveProject()).isEqualTo("proj1");

      userSettings = userSettingsFacade.readUserSettings(user2);
      assertThat(userSettings.getActiveOrganization()).isEqualTo("org1");
      assertThat(userSettings.getActiveProject()).isEqualTo("proj2");
   }

   @Test
   public void upsertUserSettingsTest() throws Exception {
      final String user = "user11";
      systemDataStorage.createDocument(LumeerConst.UserSettings.COLLECTION_NAME,
            new UserSettings(user, "org1", "proj1").toDataDocument());

      UserSettings userSettings = userSettingsFacade.readUserSettings(user);
      assertThat(userSettings.getActiveOrganization()).isEqualTo("org1");
      assertThat(userSettings.getActiveProject()).isEqualTo("proj1");

      userSettingsFacade.upsertUserSettings(new UserSettings(user, "org3", null));
      userSettings = userSettingsFacade.readUserSettings(user);
      assertThat(userSettings.getActiveOrganization()).isEqualTo("org3");
      assertThat(userSettings.getActiveProject()).isEqualTo("proj1");

      userSettingsFacade.upsertUserSettings(new UserSettings(user, null, "projXYZ"));
      userSettings = userSettingsFacade.readUserSettings(user);
      assertThat(userSettings.getActiveOrganization()).isEqualTo("org3");
      assertThat(userSettings.getActiveProject()).isEqualTo("projXYZ");
   }

   @Test
   public void removeUserSettingsTest() throws Exception {
      final String user = "user21";
      systemDataStorage.createDocument(LumeerConst.UserSettings.COLLECTION_NAME,
            new UserSettings(user, "org1", "proj1").toDataDocument());

      assertThat(userSettingsFacade.readUserSettings(user)).isNotNull();
      userSettingsFacade.removeUserSettings(user);
      assertThat(userSettingsFacade.readUserSettings(user)).isNull();
   }

}
