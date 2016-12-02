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

import java.util.Optional;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class ConfigurationFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "ConfigurationFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-dev.properties");
   }

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

   private final String DEFAULT_DBHOST_VALUE = "ds163667.mlab.com";
   private final int DEFAULT_PORT_VALUE = 63667;
   private final String DEFAULT_NOT_EXISTED_KEY = "not_existed_key";

   private final int DUMMY_PORT_VALUE = 27017;
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

   @BeforeMethod
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
      Assert.assertEquals(defaultValue.get(), DEFAULT_DBHOST_VALUE);

      systemDataStorage.dropCollection(COLLECTION_USER_CONFIG);
      systemDataStorage.dropCollection(COLLECTION_TEAM_CONFIG);

      // #2 if the team system collection has key-value
      fillSystemCollection(COLLECTION_TEAM_CONFIG);
      Assert.assertEquals(configurationFacade.getConfigurationString(DBHOST_KEY).get(), DUMMY_DBHOST_VALUE + 0);

      // #3 if none of system values exists in collections, neither the default value
      Assert.assertEquals(configurationFacade.getConfigurationString(DEFAULT_NOT_EXISTED_KEY), Optional.empty());
   }

   @Test
   public void testGetConfigurationInteger() throws Exception {
      // #1 if both system collections are empty, default value will be returned
      Optional defaultValue = configurationFacade.getConfigurationInteger(PORT_KEY);
      Assert.assertEquals(defaultValue.get(), DEFAULT_PORT_VALUE);

      systemDataStorage.dropCollection(COLLECTION_USER_CONFIG);
      systemDataStorage.dropCollection(COLLECTION_TEAM_CONFIG);

      // #2 if the user system collection has key-value
      fillSystemCollection(COLLECTION_USER_CONFIG);
      Assert.assertTrue(configurationFacade.getConfigurationInteger(PORT_KEY).get() == DUMMY_PORT_VALUE);

      // #3 if none of system values exists in collections, neither the default value
      Assert.assertEquals(configurationFacade.getConfigurationString(DEFAULT_NOT_EXISTED_KEY), Optional.empty());

   }

   @Test
   public void testGetConfigurationDocument() throws Exception {
      // #1 if the user system collection has key-value
      fillSystemCollection(COLLECTION_USER_CONFIG);
      Assert.assertEquals(configurationFacade.getConfigurationDocument(DOCUMENT_PROPERTY_KEY).get(), dummyDataDocument);

      // #3 if none of system values exists in collections, neither the default value
      Assert.assertEquals(configurationFacade.getConfigurationDocument(DEFAULT_NOT_EXISTED_KEY), Optional.empty());
   }

   @Test
   public void testSetAndGetUserConfigurationString() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_CONFIG);

      // #1 if the default value exists
      configurationFacade.setUserConfigurationString(DBHOST_KEY, DUMMY_DBHOST_VALUE);
      Assert.assertEquals(configurationFacade.getUserConfigurationString(DBHOST_KEY), Optional.empty());
      systemDataStorage.dropCollection(COLLECTION_USER_CONFIG);

      // #2 if the system user collection is filled and key exists
      fillSystemCollection(COLLECTION_USER_CONFIG);
      configurationFacade.setUserConfigurationString(DBURL_KEY, DUMMY_VALUE);
      Assert.assertEquals(configurationFacade.getUserConfigurationString(DBURL_KEY).get(), DUMMY_VALUE);

      // #3 if none of system values exists in collections, neither the default value
      Assert.assertEquals(configurationFacade.getConfigurationString(DEFAULT_NOT_EXISTED_KEY), Optional.empty());
   }

   @Test
   public void testSetAndGetUserConfigurationInteger() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_CONFIG);

      // #1 if the default value exists
      configurationFacade.setUserConfigurationInteger(PORT_KEY, DUMMY_PORT_VALUE);
      Assert.assertEquals(configurationFacade.getUserConfigurationInteger(PORT_KEY), Optional.empty());

      // #2 if none of system values exists in collections, neither the default value
      Assert.assertEquals(configurationFacade.getConfigurationInteger(DEFAULT_NOT_EXISTED_KEY), Optional.empty());
   }

   @Test
   public void testSetAndGetUserConfigurationDocument() throws Exception {
      systemDataStorage.createCollection(COLLECTION_USER_CONFIG);

      DataDocument dummyDocument = createDummyDataDocument();
      configurationFacade.setUserConfigurationDocument(DOCUMENT_PROPERTY_KEY, dummyDocument);
      DataDocument document = configurationFacade.getUserConfigurationDocument(DOCUMENT_PROPERTY_KEY).get();

      Assert.assertEquals(dummyDocument, document);
   }

   @Test
   public void testSetAndGetTeamConfigurationString() throws Exception {
      systemDataStorage.createCollection(COLLECTION_TEAM_CONFIG);

      // #1 if the system user collection is empty
      configurationFacade.setTeamConfigurationString(DBHOST_KEY, DUMMY_DBHOST_VALUE);
      Assert.assertEquals(configurationFacade.getTeamConfigurationString(DBHOST_KEY), Optional.empty());
      systemDataStorage.dropCollection(COLLECTION_TEAM_CONFIG);

      // #2 if the system user collection is filled and key exists
      fillSystemCollection(COLLECTION_TEAM_CONFIG);
      configurationFacade.setTeamConfigurationString(DBURL_KEY, DUMMY_VALUE);
      Assert.assertEquals(configurationFacade.getTeamConfigurationString(DBURL_KEY).get(), DUMMY_VALUE);
   }

   @Test
   public void testSetAndGetTeamConfigurationInteger() throws Exception {
      systemDataStorage.createCollection(COLLECTION_TEAM_CONFIG);

      // #1 if the system team collection is empty
      configurationFacade.setTeamConfigurationInteger(PORT_KEY, DUMMY_PORT_VALUE);
      Assert.assertEquals(configurationFacade.getTeamConfigurationInteger(PORT_KEY), Optional.empty());
   }

   @Test
   public void testSetAndGetTeamConfigurationDocument() throws Exception {
      systemDataStorage.createCollection(COLLECTION_TEAM_CONFIG);

      DataDocument dummyDocument = createDummyDataDocument();
      configurationFacade.setTeamConfigurationDocument(DOCUMENT_PROPERTY_KEY, dummyDocument);
      DataDocument document = configurationFacade.getTeamConfigurationDocument(DOCUMENT_PROPERTY_KEY).get();

      Assert.assertTrue(dummyDocument.equals(document));
   }

   @Test
   public void testResetUserConfiguration() throws Exception {
      fillSystemCollection(COLLECTION_USER_CONFIG);

      Assert.assertEquals(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY)).size(), BEFORE_SIZE_RESET);
      configurationFacade.resetUserConfiguration();
      Assert.assertEquals(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY)).size(), AFTER_SIZE_RESET);
   }

   @Test
   public void testResetUserConfigurationAttribute() throws Exception {
      fillSystemCollection(COLLECTION_USER_CONFIG);

      configurationFacade.resetUserConfigurationAttribute(DBURL_KEY);
      Assert.assertFalse(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_USER_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY)).containsKey(DBURL_KEY));
   }

   @Test
   public void testResetTeamConfiguration() throws Exception {
      fillSystemCollection(COLLECTION_TEAM_CONFIG);

      Assert.assertEquals(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY)).size(), BEFORE_SIZE_RESET);
      configurationFacade.resetTeamConfiguration();
      Assert.assertEquals(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY)).size(), AFTER_SIZE_RESET);
   }

   @Test
   public void testResetTeamConfigurationAttribute() throws Exception {
      fillSystemCollection(COLLECTION_TEAM_CONFIG);

      configurationFacade.resetTeamConfigurationAttribute(DBURL_KEY);
      Assert.assertFalse(((DataDocument) configurationManipulator.getConfigurationEntry(COLLECTION_TEAM_CONFIG, userFacade.getUserEmail()).get().get(CONFIG_DOCUMENT_KEY)).containsKey(DBURL_KEY));
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