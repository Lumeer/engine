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
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.controller.configuration.ConfigurationManipulator;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Optional;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class ConfigurationFacadeTest extends IntegrationTestBase {

   private final String COLLECTION_USER_CONFIG = "config.user";
   private final String COLLECTION_TEAM_CONFIG = "config.team";

   private final String NAME_KEY = "name";
   private final String PORT_KEY = "db_port_test";
   private final String DBHOST_KEY = "db_host_test";
   private final String DBURL_KEY = "db_url_test";
   private final String CONFIG_DOCUMENT_KEY = "config";
   private final String DOCUMENT_PROPERTY_KEY = "document_property";

   private final String DUMMY_EMAIL_PREFIX = "pepa";
   private final String DUMMY_EMAIL_DOMAIN = "@zdepa.cz";
   private final String DUMMY_DBHOST_VALUE = "lumeer";
   private final String DUMMY_DBURL_VALUE = "mongodb://" + DUMMY_DBHOST_VALUE;
   private final String DUMMY_VALUE = "dummyValue";

   private final String DEFAULT_DBHOST_VALUE = "localhost";
   private final int DEFAULT_PORT_VALUE = 27017;
   private final String DEFAULT_NOT_EXISTED_KEY = "not_existed_key";

   private final int DUMMY_PORT_VALUE = 63667;
   private final int BEFORE_SIZE_RESET = 4;
   private final int AFTER_SIZE_RESET = 0;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Inject
   private UserFacade userFacade;

   private DataDocument dummyDataDocument;

   @Before
   public void setUp() throws Exception {
      if (isDatabaseCollection(COLLECTION_USER_CONFIG)) {
         systemDataStorage.dropCollection(COLLECTION_USER_CONFIG);
      }

      if (isDatabaseCollection(COLLECTION_TEAM_CONFIG)) {
         systemDataStorage.dropCollection(COLLECTION_TEAM_CONFIG);
      }
   }

   @Test
   public void testGetConfigurationString() throws Exception {
      // #1 if both system collections are empty, default value will be returned
      Optional defaultValue = configurationFacade.getConfigurationString(DBHOST_KEY);
      assertThat(defaultValue.get()).isEqualTo(DEFAULT_DBHOST_VALUE);

      systemDataStorage.dropCollection(COLLECTION_USER_CONFIG);
      systemDataStorage.dropCollection(COLLECTION_TEAM_CONFIG);

      // #2 if the team system collection has key-value
      fillSystemCollection(COLLECTION_TEAM_CONFIG);
      assertThat(configurationFacade.getConfigurationString(DBHOST_KEY).get()).isEqualTo(DUMMY_DBHOST_VALUE + 0);

      // #3 if none of system values exists in collections, neither the default value
      assertThat(configurationFacade.getConfigurationString(DEFAULT_NOT_EXISTED_KEY)).isEqualTo(Optional.empty());
   }

   @Test
   public void testGetConfigurationInteger() throws Exception {
      // #1 if both system collections are empty, default value will be returned
      Optional defaultValue = configurationFacade.getConfigurationInteger(PORT_KEY);
      assertThat(defaultValue.get()).isEqualTo(DEFAULT_PORT_VALUE);

      systemDataStorage.dropCollection(COLLECTION_USER_CONFIG);
      systemDataStorage.dropCollection(COLLECTION_TEAM_CONFIG);

      // #2 if the user system collection has key-value
      fillSystemCollection(COLLECTION_USER_CONFIG);
      assertThat(configurationFacade.getConfigurationInteger(PORT_KEY).get()).isEqualTo(DUMMY_PORT_VALUE);

      // #3 if none of system values exists in collections, neither the default value
      assertThat(configurationFacade.getConfigurationString(DEFAULT_NOT_EXISTED_KEY)).isEqualTo(Optional.empty());
   }

   @Test
   public void testGetConfigurationDocument() throws Exception {
      // #1 if the user system collection has key-value
      fillSystemCollection(COLLECTION_USER_CONFIG);
      assertThat(configurationFacade.getConfigurationDocument(DOCUMENT_PROPERTY_KEY).get()).isEqualTo(dummyDataDocument);

      // #3 if none of system values exists in collections, neither the default value
      assertThat(configurationFacade.getConfigurationDocument(DEFAULT_NOT_EXISTED_KEY)).isEqualTo(Optional.empty());
   }

   @Test
   public void testSetAndGetUserConfigurationString() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_CONFIG);

      // #1 if the default value exists
      configurationFacade.setUserConfigurationString(DBHOST_KEY, DUMMY_DBHOST_VALUE);
      assertThat(configurationFacade.getUserConfigurationString(DBHOST_KEY).get()).isEqualTo(DUMMY_DBHOST_VALUE);
      systemDataStorage.dropCollection(COLLECTION_USER_CONFIG);

      // #2 if the system user collection is filled and key exists
      fillSystemCollection(COLLECTION_USER_CONFIG);
      configurationFacade.setUserConfigurationString(DBURL_KEY, DUMMY_VALUE);
      assertThat(configurationFacade.getUserConfigurationString(DBURL_KEY).get()).isEqualTo(DUMMY_VALUE);

      // #3 if none of system values exists in collections, neither the default value
      assertThat(configurationFacade.getConfigurationString(DEFAULT_NOT_EXISTED_KEY)).isEqualTo(Optional.empty());
   }

   @Test
   public void testSetAndGetUserConfigurationInteger() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_CONFIG);

      // #1 if the default value exists
      configurationFacade.setUserConfigurationInteger(PORT_KEY, DUMMY_PORT_VALUE);
      assertThat(configurationFacade.getUserConfigurationInteger(PORT_KEY).get()).isEqualTo(DUMMY_PORT_VALUE);

      // #2 if none of system values exists in collections, neither the default value
      assertThat(configurationFacade.getConfigurationInteger(DEFAULT_NOT_EXISTED_KEY)).isEqualTo(Optional.empty());
   }

   @Test
   public void testSetAndGetUserConfigurationDocument() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_CONFIG);

      DataDocument dummyDocument = createDummyDataDocument();
      configurationFacade.setUserConfigurationDocument(DOCUMENT_PROPERTY_KEY, dummyDocument);
      DataDocument document = configurationFacade.getUserConfigurationDocument(DOCUMENT_PROPERTY_KEY).get();

      assertThat(dummyDocument).isEqualTo(document);
   }

   @Test
   public void testSetAndGetTeamConfigurationString() throws Exception {
      systemDataStorage.createCollection(COLLECTION_TEAM_CONFIG);

      // #1 if the system user collection is empty
      configurationFacade.setTeamConfigurationString(DBHOST_KEY, DUMMY_DBHOST_VALUE);
      assertThat(configurationFacade.getTeamConfigurationString(DBHOST_KEY).get()).isEqualTo(DUMMY_DBHOST_VALUE);
      systemDataStorage.dropCollection(COLLECTION_TEAM_CONFIG);

      // #2 if the system user collection is filled and key exists
      fillSystemCollection(COLLECTION_TEAM_CONFIG);
      configurationFacade.setTeamConfigurationString(DBURL_KEY, DUMMY_VALUE);
      assertThat(configurationFacade.getTeamConfigurationString(DBURL_KEY).get()).isEqualTo(DUMMY_VALUE);
   }

   @Test
   public void testSetAndGetTeamConfigurationInteger() throws Exception {
      systemDataStorage.createCollection(COLLECTION_TEAM_CONFIG);

      configurationFacade.setTeamConfigurationInteger(PORT_KEY, DUMMY_PORT_VALUE);
      assertThat(configurationFacade.getTeamConfigurationInteger(PORT_KEY).get()).isEqualTo(DUMMY_PORT_VALUE);
   }

   @Test
   public void testSetAndGetTeamConfigurationDocument() throws Exception {
      systemDataStorage.createCollection(COLLECTION_TEAM_CONFIG);

      DataDocument dummyDocument = createDummyDataDocument();
      configurationFacade.setTeamConfigurationDocument(DOCUMENT_PROPERTY_KEY, dummyDocument);
      DataDocument document = configurationFacade.getTeamConfigurationDocument(DOCUMENT_PROPERTY_KEY).get();

      assertThat(dummyDocument).isEqualTo(document);
   }

   @Test
   public void testResetUserConfiguration() throws Exception {
      fillSystemCollection(COLLECTION_USER_CONFIG);

      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY))).hasSize(BEFORE_SIZE_RESET);
      configurationFacade.resetUserConfiguration();
      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY))).hasSize(AFTER_SIZE_RESET);
   }

   @Test
   public void testResetUserConfigurationAttribute() throws Exception {
      fillSystemCollection(COLLECTION_USER_CONFIG);

      configurationFacade.resetUserConfigurationAttribute(DBURL_KEY);
      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY))).doesNotContainKey(DBURL_KEY);
   }

   @Test
   public void testResetTeamConfiguration() throws Exception {
      fillSystemCollection(COLLECTION_TEAM_CONFIG);

      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY))).hasSize(BEFORE_SIZE_RESET);
      configurationFacade.resetTeamConfiguration();
      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY))).hasSize(AFTER_SIZE_RESET);
   }

   @Test
   public void testResetTeamConfigurationAttribute() throws Exception {
      fillSystemCollection(COLLECTION_TEAM_CONFIG);

      configurationFacade.resetTeamConfigurationAttribute(DBURL_KEY);
      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY))).doesNotContainKey(DBURL_KEY);
   }

   private void fillSystemCollection(final String collectionName) {
      if (collectionName != null) {
         systemDataStorage.createCollection(collectionName);
         // insert user entries
         for (int i = 0; i < 5; i++) {
            DataDocument insertedDocument = new DataDocument();
            if (i == 0) {
               insertedDocument.put(NAME_KEY, userFacade.getUserEmail());
            } else {
               insertedDocument.put(NAME_KEY, DUMMY_EMAIL_PREFIX + i + DUMMY_EMAIL_DOMAIN);
            }

            DataDocument config = new DataDocument();
            config.put(DBHOST_KEY, DUMMY_DBHOST_VALUE + i);
            config.put(PORT_KEY, DUMMY_PORT_VALUE + i);
            config.put(DBURL_KEY, DUMMY_DBURL_VALUE + i);
            config.put(DOCUMENT_PROPERTY_KEY, createDummyDataDocument());

            insertedDocument.put(CONFIG_DOCUMENT_KEY, config);

            systemDataStorage.createDocument(collectionName, insertedDocument);
         }
      }
   }

   private DataDocument createDummyDataDocument() {
      String dummyKey1 = "key1";
      String dummyKey2 = "key2";
      String dummyValue1 = "param1";
      String dummyValue2 = "param2";

      dummyDataDocument = new DataDocument();
      dummyDataDocument.put(dummyKey1, dummyValue1);
      dummyDataDocument.put(dummyKey2, dummyValue2);

      return dummyDataDocument;
   }

   private boolean isDatabaseCollection(final String collectionName) {
      try {
         return systemDataStorage.hasCollection(collectionName);
      } catch (Exception e) {
         // nothing to do
      }
      return false;
   }

}