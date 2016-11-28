package io.lumeer.engine.util;/*
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

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.controller.configuration.ConfigurationManipulator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class ConfigurationManipulatorTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "ConfigurationManipulatorTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-dev.properties");
   }

   private final String COLLECTION_USER_SET_CONFIGURATION = "config.user_setConfiguration";
   private final String COLLECTION_USER_GET_CONFIGURATION = "config.user_getConfiguration";
   private final String COLLECTION_USER_RESET_CONFIGURATION = "config.user_resetConfiguration";
   private final String COLLECTION_USER_RESET_CONFIGURATION_BY_KEY = "config.user_resetConfigurationAttribute";
   private final String COLLECTION_TEAM_SET_CONFIGURATION = "config.team_setConfiguration";
   private final String COLLECTION_TEAM_GET_CONFIGURATION = "config.team_getConfiguration";
   private final String COLLECTION_TEAM_RESET_CONFIGURATION = "config.team_resetConfiguration";
   private final String COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY = "config.team_resetConfigurationAttribute";

   private final String NAME_KEY = "name";
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

   private DataDocument dummyDataDocument;

   @BeforeMethod
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

      // #1 if system database is empty
      configurationManipulator.setConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, PORT_KEY, DUMMY_PORT_VALUE);
      configurationManipulator.setConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, PORT_KEY, DUMMY_PORT_VALUE);

      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, PORT_KEY).toString(), String.valueOf(DUMMY_PORT_VALUE));
      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, PORT_KEY).toString(), String.valueOf(DUMMY_PORT_VALUE));

      systemDataStorage.dropCollection(COLLECTION_USER_SET_CONFIGURATION);
      systemDataStorage.dropCollection(COLLECTION_TEAM_SET_CONFIGURATION);

      // #2 if system database is filled
      fillSystemDatabase(COLLECTION_USER_SET_CONFIGURATION, COLLECTION_TEAM_SET_CONFIGURATION);

      configurationManipulator.setConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DUMMY_KEY, DUMMY_VALUE);
      configurationManipulator.setConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DUMMY_KEY, DUMMY_VALUE);

      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DUMMY_KEY).toString(), String.valueOf(DUMMY_VALUE));
      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DUMMY_KEY).toString(), String.valueOf(DUMMY_VALUE));

      // #3 if key exists, value will be updated
      configurationManipulator.setConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DBURL_KEY, DUMMY_VALUE);
      configurationManipulator.setConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DBURL_KEY, DUMMY_VALUE);

      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_USER_SET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DBURL_KEY).toString(), String.valueOf(DUMMY_VALUE));
      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_TEAM_SET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DBURL_KEY).toString(), String.valueOf(DUMMY_VALUE));
   }

   @Test
   public void testGetConfiguration() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_GET_CONFIGURATION);
      systemDataStorage.createCollection(COLLECTION_TEAM_GET_CONFIGURATION);

      // #1 if system database is empty = key-value does not exist
      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_USER_GET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DUMMY_KEY), null);
      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_TEAM_GET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DUMMY_KEY), null);

      systemDataStorage.dropCollection(COLLECTION_USER_GET_CONFIGURATION);
      systemDataStorage.dropCollection(COLLECTION_TEAM_GET_CONFIGURATION);

      // #2 if system database is filled
      fillSystemDatabase(COLLECTION_USER_GET_CONFIGURATION, COLLECTION_TEAM_GET_CONFIGURATION);

      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_USER_GET_CONFIGURATION, DUMMY_USER_NAME_VALUE, DBURL_KEY), DUMMY_DBURL_VALUE + 2);
      Assert.assertEquals(configurationManipulator.getConfiguration(COLLECTION_TEAM_GET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE, DBURL_KEY), DUMMY_DBURL_VALUE);
   }

   @Test
   public void testResetConfiguration() throws Exception {
      fillSystemDatabase(COLLECTION_USER_RESET_CONFIGURATION, COLLECTION_TEAM_RESET_CONFIGURATION);

      configurationManipulator.resetConfiguration(COLLECTION_USER_RESET_CONFIGURATION, DUMMY_USER_NAME_VALUE);
      configurationManipulator.resetConfiguration(COLLECTION_TEAM_RESET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE);

      Assert.assertEquals(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_RESET_CONFIGURATION, DUMMY_USER_NAME_VALUE).get().get(CONFIG_DOCUMENT_KEY)).size(), 0);
      Assert.assertEquals(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_RESET_CONFIGURATION, DUMMY_TEAM_NAME_VALUE).get().get(CONFIG_DOCUMENT_KEY)).size(), 0);
   }

   @Test
   public void testResetConfigurationAttribute() throws Exception {
      fillSystemDatabase(COLLECTION_USER_RESET_CONFIGURATION_BY_KEY, COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY);

      configurationManipulator.resetConfigurationAttribute(COLLECTION_USER_RESET_CONFIGURATION_BY_KEY, DUMMY_USER_NAME_VALUE, DBURL_KEY);
      configurationManipulator.resetConfigurationAttribute(COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY, DUMMY_TEAM_NAME_VALUE, DBURL_KEY);

      Assert.assertFalse(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_RESET_CONFIGURATION_BY_KEY, DUMMY_USER_NAME_VALUE).get().get(CONFIG_DOCUMENT_KEY)).containsKey(DBURL_KEY));
      Assert.assertFalse(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_RESET_CONFIGURATION_BY_KEY, DUMMY_TEAM_NAME_VALUE).get().get(CONFIG_DOCUMENT_KEY)).containsKey(DBURL_KEY));
   }

   private void fillSystemDatabase(final String collectionUser, final String collectionTeam) {
      systemDataStorage.createCollection(collectionUser);
      systemDataStorage.createCollection(collectionTeam);

      // insert user entries
      for (int i = 0; i < 5; i++) {
         DataDocument insertedDocument = new DataDocument();
         insertedDocument.put(NAME_KEY, DUMMY_EMAIL_PREFIX + i + DUMMY_EMAIL_DOMAIN);

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
      insertedDocument.put(NAME_KEY, DUMMY_TEAM_NAME_VALUE);

      DataDocument config = new DataDocument();
      config.put(DBHOST_KEY, DUMMY_DBHOST_VALUE);
      config.put(PORT_KEY, DUMMY_PORT_VALUE);
      config.put(DBURL_KEY, DUMMY_DBURL_VALUE);
      config.put(DOCUMENT_PROPERTY_KEY, createDummyDataDocument());

      insertedDocument.put(CONFIG_DOCUMENT_KEY, config);

      systemDataStorage.createDocument(collectionTeam, insertedDocument);
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
         return systemDataStorage.getAllCollections().contains(collectionName);
      } catch (Exception e) {
         // nothing to do
      }
      return false;
   }
}