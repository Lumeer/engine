package io.lumeer.engine.controller;/*
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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class ConfigurationFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "ConfigurationFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
   }

   private final String DUMMY_USER_COLLECTION = "config.user";
   private final String DUMMY_TEAM_COLLECTION = "config.team";

   private final String DUMMY_EMAIL_KEY = "userEmail";
   private final String DUMMY_PORT_KEY = "dbPort";
   private final String DUMMY_DBNAME_KEY = "dbName";
   private final String DUMMY_DBURL_KEY = "dbURL";
   private final String DUMMY_DOCUMENT_KEY = "dummyDocument";

   private final String DUMMY_EMAIL_FIRSTNAME = "pepa";
   private final String DUMMY_EMAIL_VALUE = "@zdepa.cz";
   private final String DUMMY_DBNAME_VALUE = "lumeer";
   private final String DUMMY_DBURL_VALUE = "mongodb://" + DUMMY_DBNAME_VALUE;
   private final int DUMMY_PORT_VALUE = 27017;

   private final String DUMMY_KEY_1 = "dummyKey1";
   private final String DUMMY_KEY_2 = "dummyKey2";
   private final String DUMMY_VALUE_1 = "dummyValue1";
   private final String DUMMY_VALUE_2 = "dummyValue2";

   private DataDocument dummyDataDocument;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Test
   public void testGetConfigurationString() throws Exception {
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);
      fillDatabaseDummySystemConfigEntries(DUMMY_USER_COLLECTION);

      Assert.assertEquals(configurationFacade.getConfigurationString(DUMMY_USER_COLLECTION, DUMMY_DBNAME_KEY).get(), DUMMY_DBNAME_VALUE + '0');

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);
   }

   @Test
   public void testGetConfigurationInteger() throws Exception {
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);
      fillDatabaseDummySystemConfigEntries(DUMMY_USER_COLLECTION);

      Assert.assertEquals(configurationFacade.getConfigurationInteger(DUMMY_USER_COLLECTION, DUMMY_PORT_KEY).get().intValue(), DUMMY_PORT_VALUE);

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);
   }

   @Test
   public void testGetConfigurationDocument() throws Exception {
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);
      fillDatabaseDummySystemConfigEntries(DUMMY_USER_COLLECTION);

      Assert.assertEquals(configurationFacade.getConfigurationDocument(DUMMY_USER_COLLECTION, DUMMY_DOCUMENT_KEY).get(), dummyDataDocument);

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);
   }

   @Test
   public void testSetConfigurationDocument() throws Exception {
      // #1: if configuration entry has not existed so far - dbs is empty
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);

      DataDocument insertedDocument = new DataDocument();
      insertedDocument.put(DUMMY_KEY_1, DUMMY_VALUE_1);
      insertedDocument.put(DUMMY_KEY_2, DUMMY_VALUE_2);

      configurationFacade.setConfigurationDocument(DUMMY_USER_COLLECTION, DUMMY_DOCUMENT_KEY, insertedDocument);

      DataDocument configDocument = configurationFacade.getConfigurationDocument(DUMMY_USER_COLLECTION, DUMMY_DOCUMENT_KEY).get();
      Assert.assertEquals(configDocument.get(DUMMY_KEY_1), insertedDocument.get(DUMMY_KEY_1));
      Assert.assertEquals(configDocument.get(DUMMY_KEY_2), insertedDocument.get(DUMMY_KEY_2));

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);

      // #2: if configuration entry has already existed - dbs is filled
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);
      fillDatabaseDummySystemConfigEntries(DUMMY_USER_COLLECTION);

      configurationFacade.setConfigurationDocument(DUMMY_USER_COLLECTION, DUMMY_DOCUMENT_KEY, insertedDocument);

      DataDocument reconfigDocument = configurationFacade.getConfigurationDocument(DUMMY_USER_COLLECTION, DUMMY_DOCUMENT_KEY).get();
      Assert.assertEquals(reconfigDocument.get(DUMMY_KEY_1), insertedDocument.get(DUMMY_KEY_1));
      Assert.assertEquals(reconfigDocument.get(DUMMY_KEY_2), insertedDocument.get(DUMMY_KEY_2));

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);
   }

   @Test
   public void testSetConfigurationString() throws Exception {
      final String DUMMY_STRING_KEY = "stringKey";
      final String DUMMY_STRING_VALUE = "stringValue";
      final String DUMMY_DBURL_VALUE = "mongodb://localhost";

      // #1: if configuration entry has not existed so far - dbs is empty
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);
      configurationFacade.setConfigurationString(DUMMY_USER_COLLECTION, DUMMY_STRING_KEY, DUMMY_STRING_VALUE);

      String configString = configurationFacade.getConfigurationString(DUMMY_USER_COLLECTION, DUMMY_STRING_KEY).get();
      Assert.assertEquals(configString, DUMMY_STRING_VALUE);

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);

      // #2: if configuration entry has already existed - dbs is filled
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);
      fillDatabaseDummySystemConfigEntries(DUMMY_USER_COLLECTION);

      configurationFacade.setConfigurationString(DUMMY_USER_COLLECTION, DUMMY_DBURL_KEY, DUMMY_DBURL_VALUE);

      String reconfigString = configurationFacade.getConfigurationString(DUMMY_USER_COLLECTION, DUMMY_DBURL_KEY).get();
      Assert.assertEquals(reconfigString, DUMMY_DBURL_VALUE);

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);
   }

   @Test
   public void testSetConfigurationInteger() throws Exception {
      final String DUMMY_INT_KEY = "integerKey";
      final int DUMMY_PORT_VALUE = 12345;

      // #1: if configuration entry has not existed so far - dbs is empty
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);
      configurationFacade.setConfigurationInteger(DUMMY_USER_COLLECTION, DUMMY_INT_KEY, DUMMY_PORT_VALUE);

      int configInt = configurationFacade.getConfigurationInteger(DUMMY_USER_COLLECTION, DUMMY_INT_KEY).get();
      Assert.assertEquals(configInt, DUMMY_PORT_VALUE);

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);

      // #2: if configuration entry has already existed - dbs is filled
      systemDataStorage.createCollection(DUMMY_USER_COLLECTION);
      fillDatabaseDummySystemConfigEntries(DUMMY_USER_COLLECTION);

      configurationFacade.setConfigurationInteger(DUMMY_USER_COLLECTION, DUMMY_PORT_KEY, DUMMY_PORT_VALUE);

      int reconfigInt = configurationFacade.getConfigurationInteger(DUMMY_USER_COLLECTION, DUMMY_PORT_KEY).get();
      Assert.assertEquals(reconfigInt, DUMMY_PORT_VALUE);

      systemDataStorage.dropCollection(DUMMY_USER_COLLECTION);
   }

   @Test
   public void testResetToDefaultConfiguration() throws Exception {
      // TODO:
   }

   @Test
   public void testResetAllToDefaults() throws Exception {
      // TODO:
   }

   private void fillDatabaseDummySystemConfigEntries(String collectionName) {
      for (int i = 0; i < 20; i++) {
         DataDocument insertedDocument = new DataDocument();
         insertedDocument.put(DUMMY_EMAIL_KEY, DUMMY_EMAIL_FIRSTNAME + i + DUMMY_EMAIL_VALUE);
         insertedDocument.put(DUMMY_DBNAME_KEY, DUMMY_DBNAME_VALUE + i);
         insertedDocument.put(DUMMY_PORT_KEY, DUMMY_PORT_VALUE + i);
         insertedDocument.put(DUMMY_DBURL_KEY, DUMMY_DBURL_VALUE + i);
         insertedDocument.put(DUMMY_DOCUMENT_KEY, createDummyDataDocument());

         systemDataStorage.createDocument(collectionName, insertedDocument);
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

}