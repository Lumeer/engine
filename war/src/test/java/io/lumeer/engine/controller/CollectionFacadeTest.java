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

import java.util.List;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 * @author <a href="mailto:alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class CollectionFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "CollectionFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
   }

   private final String DUMMY_COLLECTION1 = "testcollection1";
   private final String DUMMY_COLLECTION2 = "testcollection2";
   private final String DUMMY_COLLECTION1_METADATA_COLLECTION = "meta.collection.testcollection1";

   private final String DUMMY_KEY1 = "key1";
   private final String DUMMY_KEY2 = "key2";
   private final String DUMMY_VALUE1 = "param1";
   private final String DUMMY_VALUE2 = "param2";
   private final String DUMMY_NEW_KEY = "newKey";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   @Test
   public void testGetAllCollections() throws Exception {
      Assert.assertEquals(collectionFacade.getAllCollections().size(), 0);
   }

   @Test
   public void testCreateAndDropCollection() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1);
      collectionFacade.createCollection(DUMMY_COLLECTION2);

      Assert.assertEquals(collectionFacade.getAllCollections().size(), 2);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
      collectionFacade.dropCollection(DUMMY_COLLECTION2);

      Assert.assertEquals(collectionFacade.getAllCollections().size(), 0);
   }

   @Test
   public void testDropCollectionMetadata() throws Exception {
      boolean isDropped = true;
      collectionFacade.createCollection(DUMMY_COLLECTION1);
      collectionFacade.dropCollectionMetadata(DUMMY_COLLECTION1);
      if (dataStorage.getAllCollections().contains(DUMMY_COLLECTION1_METADATA_COLLECTION)) {
         isDropped = false;
      }
      dataStorage.dropCollection("collection." + DUMMY_COLLECTION1);

      Assert.assertTrue(isDropped);
   }

   @Test
   public void testGetAttributeValues() throws Exception {
      // TODO
   }

   @Test
   public void testAddAndDropColumn() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1);

      fillDatabaseDummyEntries(DUMMY_COLLECTION1);

      collectionFacade.addColumn(DUMMY_COLLECTION1, DUMMY_NEW_KEY);
      Assert.assertTrue(isEveryDocumentFilledByNewColumn(DUMMY_COLLECTION1, DUMMY_NEW_KEY));

      collectionFacade.dropColumn(DUMMY_COLLECTION1, DUMMY_NEW_KEY);
      Assert.assertFalse(isEveryDocumentFilledByNewColumn(DUMMY_COLLECTION1, DUMMY_NEW_KEY));

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testRenameColumn() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1);

      fillDatabaseDummyEntries(DUMMY_COLLECTION1);

      collectionFacade.renameColumn(DUMMY_COLLECTION1, DUMMY_KEY1, DUMMY_NEW_KEY);
      Assert.assertTrue(isEveryDocumentFilledByNewColumn(DUMMY_COLLECTION1, DUMMY_NEW_KEY));

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testOnCollectionEvent() throws Exception {

   }

   private DataDocument createDummyDocument() {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(DUMMY_KEY1, DUMMY_VALUE1);
      dataDocument.put(DUMMY_KEY2, DUMMY_VALUE2);

      return dataDocument;
   }

   private void fillDatabaseDummyEntries(String collectionName) {
      for (int i = 0; i < 100; i++) {
         DataDocument insertedDocument = createDummyDocument();
         dataStorage.createDocument("collection." + collectionName, insertedDocument);
      }
   }

   private boolean isEveryDocumentFilledByNewColumn(String collection, String columnName) {
      List<DataDocument> documents = dataStorage.search("collection." + collection, null, null, 0, 0);

      for (DataDocument document : documents) {
         if (!document.keySet().contains(columnName)) {
            return false;
         }
      }
      return true;
   }
}