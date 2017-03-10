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
package io.lumeer.engine.controller.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class ConfigurationManipulatorIntegrationTest extends IntegrationTestBase {

   private final String COLLECTION_USER_SET_CONFIGURATION = "config.user_setConfiguration";
   private final String COLLECTION_USER_GET_CONFIGURATION = "config.user_getConfiguration";
   private final String COLLECTION_USER_RESET_CONFIGURATION = "config.user_resetConfiguration";
   private final String COLLECTION_USER_RESET_CONFIGURATION_BY_KEY = "config.user_resetConfigurationAttribute";
   private final String COLLECTION_TEAM_SET_CONFIGURATION = "config.team_setConfiguration";
   private final String COLLECTION_TEAM_GET_CONFIGURATION = "config.team_getConfiguration";
   private final String COLLECTION_TEAM_RESET_CONFIGURATION = "config.team_resetConfiguration";
   private final String COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY = "config.team_resetConfigurationAttribute";

   private final String PORT_KEY = "db_port";
   private final String DBHOST_KEY = "db_host";
   private final String DBURL_KEY = "db_url";
   private final String CONFIG_DOCUMENT_KEY = "config";
   private final String DOCUMENT_PROPERTY_KEY = "document_property";

   private final String DUMMY_EMAIL_PREFIX = "pepa";
   private final String DUMMY_EMAIL_DOMAIN = "@zdepa.cz";
   private final String DUMMY_DBHOST_VALUE = "lumeer";
   private final String DUMMY_DBURL_VALUE = "mongodb://" + DUMMY_DBHOST_VALUE;
   private final int DUMMY_PORT_VALUE = 27017;

   private final String DUMMY_USER_NAME_VALUE = "pepa2@zdepa.cz";
   private final String DUMMY_TEAM_NAME_VALUE = "redhat";

   private final String DUMMY_KEY = "dummyKey";
   private final String DUMMY_VALUE = "dummyValue";

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private ConfigurationManipulator configurationManipulator;

   @Before
   public void setUp() throws Exception {
      if (isDatabaseCollection(COLLECTION_USER_SET_CONFIGURATION)) {
         systemDataStorage.dropCollection(COLLECTION_USER_SET_CONFIGURATION);
      }

      if (isDatabaseCollection(COLLECTION_USER_GET_CONFIGURATION)) {
         systemDataStorage.dropCollection(COLLECTION_USER_GET_CONFIGURATION);
      }

      if (isDatabaseCollection(COLLECTION_TEAM_SET_CONFIGURATION)) {
         systemDataStorage.dropCollection(COLLECTION_TEAM_SET_CONFIGURATION);
      }

      if (isDatabaseCollection(COLLECTION_TEAM_GET_CONFIGURATION)) {
         systemDataStorage.dropCollection(COLLECTION_TEAM_GET_CONFIGURATION);
      }

      if (isDatabaseCollection(COLLECTION_USER_RESET_CONFIGURATION)) {
         systemDataStorage.dropCollection(COLLECTION_USER_RESET_CONFIGURATION);
      }

      if (isDatabaseCollection(COLLECTION_TEAM_RESET_CONFIGURATION)) {
         systemDataStorage.dropCollection(COLLECTION_TEAM_RESET_CONFIGURATION);
      }

      if (isDatabaseCollection(COLLECTION_USER_RESET_CONFIGURATION_BY_KEY)) {
         systemDataStorage.dropCollection(COLLECTION_USER_RESET_CONFIGURATION_BY_KEY);
      }

      if (isDatabaseCollection(COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY)) {
         systemDataStorage.dropCollection(COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY);
      }
   }

   @Test
   public void testSetConfiguration() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_SET_CONFIGURATION);
      systemDataStorage.createCollection(COLLECTION_TEAM_SET_CONFIGURATION);

      // #1 if the system collection is empty
      configurationManipulator.setConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, PORT_KEY, DUMMY_PORT_VALUE);
      configurationManipulator.setConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, PORT_KEY, DUMMY_PORT_VALUE);

      assertThat(configurationManipulator.getConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, PORT_KEY).toString()).isEqualTo(String.valueOf(DUMMY_PORT_VALUE));
      assertThat(configurationManipulator.getConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, PORT_KEY).toString()).isEqualTo(String.valueOf(DUMMY_PORT_VALUE));

      systemDataStorage.dropCollection(COLLECTION_USER_SET_CONFIGURATION);
      systemDataStorage.dropCollection(COLLECTION_TEAM_SET_CONFIGURATION);

      // #2 if the system collection is filled
      fillSystemDatabase(COLLECTION_USER_SET_CONFIGURATION, COLLECTION_TEAM_SET_CONFIGURATION);

      configurationManipulator.setConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DUMMY_KEY, DUMMY_VALUE);
      configurationManipulator.setConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DUMMY_KEY, DUMMY_VALUE);

      assertThat(configurationManipulator.getConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DUMMY_KEY).toString()).isEqualTo(String.valueOf(DUMMY_VALUE));
      assertThat(configurationManipulator.getConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DUMMY_KEY).toString()).isEqualTo(String.valueOf(DUMMY_VALUE));

      // #3 if key exists, value will be updated
      configurationManipulator.setConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DBURL_KEY, DUMMY_VALUE);
      configurationManipulator.setConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DBURL_KEY, DUMMY_VALUE);

      assertThat(configurationManipulator.getConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DBURL_KEY).toString()).isEqualTo(String.valueOf(DUMMY_VALUE));
      assertThat(configurationManipulator.getConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DBURL_KEY).toString()).isEqualTo(String.valueOf(DUMMY_VALUE));
   }

   @Test
   public void testGetConfiguration() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_GET_CONFIGURATION);
      systemDataStorage.createCollection(COLLECTION_TEAM_GET_CONFIGURATION);

      // #1 if the system collection is empty = key-value does not exist
      assertThat(configurationManipulator.getConfiguration(COLLECTION_USER_GET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DUMMY_KEY)).isNull();
      assertThat(configurationManipulator.getConfiguration(COLLECTION_TEAM_GET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DUMMY_KEY)).isNull();

      systemDataStorage.dropCollection(COLLECTION_USER_GET_CONFIGURATION);
      systemDataStorage.dropCollection(COLLECTION_TEAM_GET_CONFIGURATION);

      // #2 if the system collection is filled
      fillSystemDatabase(COLLECTION_USER_GET_CONFIGURATION, COLLECTION_TEAM_GET_CONFIGURATION);

      assertThat(configurationManipulator.getConfiguration(COLLECTION_USER_GET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DBURL_KEY)).isEqualTo(DUMMY_DBURL_VALUE + 2);
      assertThat(configurationManipulator.getConfiguration(COLLECTION_TEAM_GET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DBURL_KEY)).isEqualTo(DUMMY_DBURL_VALUE);
   }

   @Test
   public void testResetConfiguration() throws Exception {
      fillSystemDatabase(COLLECTION_USER_RESET_CONFIGURATION, COLLECTION_TEAM_RESET_CONFIGURATION);

      configurationManipulator.resetConfiguration(COLLECTION_USER_RESET_CONFIGURATION, DUMMY_USER_NAME_VALUE);
      configurationManipulator.resetConfiguration(COLLECTION_TEAM_RESET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE);

      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_RESET_CONFIGURATION, DUMMY_USER_NAME_VALUE).get().get(CONFIG_DOCUMENT_KEY))).hasSize(0);
      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_RESET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE).get().get(CONFIG_DOCUMENT_KEY))).hasSize(0);
   }

   @Test
   public void testResetConfigurationAttribute() throws Exception {
      fillSystemDatabase(COLLECTION_USER_RESET_CONFIGURATION_BY_KEY, COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY);

      configurationManipulator.resetConfigurationAttribute(COLLECTION_USER_RESET_CONFIGURATION_BY_KEY, DUMMY_USER_NAME_VALUE, DBURL_KEY);
      configurationManipulator.resetConfigurationAttribute(COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY, DUMMY_TEAM_NAME_VALUE, DBURL_KEY);

      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_RESET_CONFIGURATION_BY_KEY, DUMMY_USER_NAME_VALUE).get().get(CONFIG_DOCUMENT_KEY))).doesNotContainKey(DBURL_KEY);
      assertThat(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY, DUMMY_TEAM_NAME_VALUE).get().get(CONFIG_DOCUMENT_KEY))).doesNotContainKey(DBURL_KEY);
   }

   private void fillSystemDatabase(final String collectionUser, final String collectionTeam) {
      systemDataStorage.createCollection(collectionUser);
      systemDataStorage.createCollection(collectionTeam);

      // insert user entries
      for (int i = 0; i < 5; i++) {
         DataDocument insertedDocument = new DataDocument();
         insertedDocument.put(ConfigurationManipulator.NAME_KEY, DUMMY_EMAIL_PREFIX + i + DUMMY_EMAIL_DOMAIN);

         DataDocument config = new DataDocument();
         config.put(DBHOST_KEY, DUMMY_DBHOST_VALUE + i);
         config.put(PORT_KEY, DUMMY_PORT_VALUE + i);
         config.put(DBURL_KEY, DUMMY_DBURL_VALUE + i);
         config.put(DOCUMENT_PROPERTY_KEY, createDummyDataDocument());

         insertedDocument.put(CONFIG_DOCUMENT_KEY, config);

         systemDataStorage.createDocument(collectionUser, insertedDocument);
      }

      // insert single team entry
      DataDocument insertedDocument = new DataDocument();
      insertedDocument.put(ConfigurationManipulator.NAME_KEY, DUMMY_TEAM_NAME_VALUE);

      DataDocument config = new DataDocument();
      config.put(DBHOST_KEY, DUMMY_DBHOST_VALUE);
      config.put(PORT_KEY, DUMMY_PORT_VALUE);
      config.put(DBURL_KEY, DUMMY_DBURL_VALUE);
      config.put(DOCUMENT_PROPERTY_KEY, createDummyDataDocument());

      insertedDocument.put(CONFIG_DOCUMENT_KEY, config);

      systemDataStorage.createDocument(collectionTeam, insertedDocument);
   }

   public static DataDocument createDummyDataDocument() {
      final String dummyKey1 = "key1";
      final String dummyKey2 = "key2";
      final String dummyValue1 = "param1";
      final String dummyValue2 = "param2";

      final DataDocument dummyDataDocument = new DataDocument();
      dummyDataDocument.put(dummyKey1, dummyValue1);
      dummyDataDocument.put(dummyKey2, dummyValue2);

      return dummyDataDocument;
   }

   private boolean isDatabaseCollection(final String collectionName) {
      return systemDataStorage.hasCollection(collectionName);
   }
}
