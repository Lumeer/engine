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
import java.util.Set;
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
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-dev.properties");
   }

   private final String DUMMY_COLLECTION1 = "collection.testcollection1_0";
   private final String DUMMY_COLLECTION1_ORIGINAL_NAME = "testCollection1";
   private final String DUMMY_COLLECTION1_METADATA_COLLECTION = "meta.collection.testcollection1_0";
   private final String DUMMY_COLLECTION2 = "collection.testcollection2_0";
   private final String DUMMY_COLLECTION2_ORIGINAL_NAME = "testCollection2";

   private final String DUMMY_KEY1 = "key1";
   private final String DUMMY_KEY2 = "key2";
   private final String DUMMY_VALUE1 = "param1";
   private final String DUMMY_VALUE2 = "param2";
   private final String DUMMY_NEW_KEY = "newKey";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Test
   public void testGetAllCollections() throws Exception {
      Assert.assertEquals(collectionFacade.getAllCollections().size(), 0);
   }

   @Test
   public void testCreateAndDropCollection() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);
      collectionFacade.createCollection(DUMMY_COLLECTION2_ORIGINAL_NAME);

      Assert.assertEquals(collectionFacade.getAllCollections().size(), 2);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);
      collectionFacade.dropCollection(DUMMY_COLLECTION2);

      Assert.assertEquals(collectionFacade.getAllCollections().size(), 0);
   }

   @Test
   public void testReadCollectionMetadata() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);

      String name = "attribute 1";
      collectionMetadataFacade.addOrIncrementAttribute(DUMMY_COLLECTION1, name);

      List<DataDocument> metadata = collectionFacade.readCollectionMetadata(DUMMY_COLLECTION1);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);

      Assert.assertEquals(metadata.size(), 3); // 3 documents: attribute, name, lock
   }

   @Test
   public void testReadCollectionAttributes() throws Exception {
      String a1 = "attribute1";
      String a2 = "attribute2";

      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);
      collectionMetadataFacade.addOrIncrementAttribute(DUMMY_COLLECTION1, a1);
      collectionMetadataFacade.addOrIncrementAttribute(DUMMY_COLLECTION1, a2);

      List<String> attributes = collectionFacade.readCollectionAttributes(DUMMY_COLLECTION1);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);

      Assert.assertTrue(attributes.contains(a1));
      Assert.assertTrue(attributes.contains(a2));
   }

   @Test
   public void testDropCollectionMetadata() throws Exception {
      boolean isDropped = true;
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);
      collectionFacade.dropCollectionMetadata(DUMMY_COLLECTION1);
      if (dataStorage.getAllCollections().contains(DUMMY_COLLECTION1_METADATA_COLLECTION)) {
         isDropped = false;
      }
      collectionFacade.dropCollection(DUMMY_COLLECTION1);

      Assert.assertTrue(isDropped);
   }

   @Test
   public void testGetAttributeValues() throws Exception {
      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);

      String a1 = "attribute";
      String a2 = "dummyattribute";
      String v1 = "hello";
      String v2 = "world";
      String v3 = "!";

      DataDocument doc1 = new DataDocument();
      doc1.put(a1, v1);
      DataDocument doc2 = new DataDocument();
      doc2.put(a1, v2);
      DataDocument doc3 = new DataDocument();
      doc3.put(a2, v3);

      dataStorage.createDocument(DUMMY_COLLECTION1, doc1);
      dataStorage.createDocument(DUMMY_COLLECTION1, doc2);
      dataStorage.createDocument(DUMMY_COLLECTION1, doc3);

      // we have to add attributes to metadata because we test them in getAttributeValues
      collectionMetadataFacade.addOrIncrementAttribute(DUMMY_COLLECTION1, a1);
      collectionMetadataFacade.addOrIncrementAttribute(DUMMY_COLLECTION1, a2);

      Set<String> values = collectionFacade.getAttributeValues(DUMMY_COLLECTION1, a1);

      collectionFacade.dropCollection(DUMMY_COLLECTION1);

      Assert.assertTrue(values.contains(v1));
      Assert.assertTrue(values.contains(v2));
      Assert.assertFalse(values.contains(v3));
   }

   //   @Test
   //   public void testAddAndDropAttribute() throws Exception {
   //      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);
   //
   //      fillDatabaseDummyEntries(DUMMY_COLLECTION1);
   //
   //      collectionFacade.addAttribute(DUMMY_COLLECTION1, DUMMY_NEW_KEY);
   //      Assert.assertTrue(isEveryDocumentFilledByNewAttribute(DUMMY_COLLECTION1, DUMMY_NEW_KEY));
   //
   //      collectionFacade.dropAttribute(DUMMY_COLLECTION1, DUMMY_NEW_KEY);
   //      Assert.assertFalse(isEveryDocumentFilledByNewAttribute(DUMMY_COLLECTION1, DUMMY_NEW_KEY));
   //
   //      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   //   }

   // TODO!!!
   //   @Test
   //   public void testRenameAttribute() throws Exception {
   //      collectionFacade.createCollection(DUMMY_COLLECTION1_ORIGINAL_NAME);
   //
   //      String name = "attribute 1";
   //      collectionMetadataFacade.addOrIncrementAttribute(DUMMY_COLLECTION1, name);
   //      collectionFacade.renameAttribute(DUMMY_COLLECTION1, name, DUMMY_NEW_KEY);
   //      Assert.assertTrue(isEveryDocumentFilledByNewAttribute(DUMMY_COLLECTION1, DUMMY_NEW_KEY));
   //
   //      collectionFacade.dropCollection(DUMMY_COLLECTION1);
   //   }

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
         dataStorage.createDocument(collectionName, insertedDocument);
      }
   }

   private boolean isEveryDocumentFilledByNewAttribute(String collection, String attributeName) {
      List<DataDocument> documents = dataStorage.search(collection, null, null, 0, 0);

      for (DataDocument document : documents) {
         if (!document.keySet().contains(attributeName)) {
            return false;
         }
      }
      return true;
   }
}