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
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   // do not change collection names, because it can mess up internal name creation in method internalName()
   private final String COLLECTION_GET_ALL_COLLECTIONS = "CollectionFacadeCollectionGetAllCollections";
   private final String COLLECTION_CREATE_AND_DROP = "CollectionFacadeCollectionCreateAndDrop";
   private final String COLLECTION_READ_COLLECTION_METADATA = "CollectionFacadeReadCollectionCollectionMetadata";
   private final String COLLECTION_READ_COLLECTION_ATTRIBUTES = "CollectionFacadeReadCollectionCollectionAttributes";
   private final String COLLECTION_DROP_COLLECTION_ATTRIBUTE = "CollectionFacadeCollectionDropCollectionAttribute";
   private final String COLLECTION_GET_ATTRIBUTE_VALUES = "CollectionFacadeCollectionGetAttributeValues";
   private final String COLLECTION_RENAME_ATTRIBUTE = "CollectionFacadeCollectionRenameAttribute";

   private final String DUMMY_KEY1 = "key1";
   private final String DUMMY_KEY2 = "key2";
   private final String DUMMY_VALUE1 = "param1";
   private final String DUMMY_VALUE2 = "param2";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Test
   public void testGetAllCollections() throws Exception {
      setUpCollection(COLLECTION_GET_ALL_COLLECTIONS);

      collectionFacade.createCollection(COLLECTION_GET_ALL_COLLECTIONS);
      Assert.assertTrue(collectionFacade.getAllCollections().keySet().contains(internalName(COLLECTION_GET_ALL_COLLECTIONS)));
   }

   @Test
   public void testCreateAndDropCollection() throws Exception {
      setUpCollection(COLLECTION_CREATE_AND_DROP);

      Assert.assertFalse(collectionFacade.getAllCollections().keySet().contains(internalName(COLLECTION_CREATE_AND_DROP)));

      collectionFacade.createCollection(COLLECTION_CREATE_AND_DROP);
      Assert.assertTrue(collectionFacade.getAllCollections().keySet().contains(internalName(COLLECTION_CREATE_AND_DROP)));

      collectionFacade.dropCollection(internalName(COLLECTION_CREATE_AND_DROP));
      Assert.assertFalse(collectionFacade.getAllCollections().keySet().contains(internalName(COLLECTION_CREATE_AND_DROP)));
   }

   @Test
   public void testReadCollectionMetadata() throws Exception {
      setUpCollection(COLLECTION_READ_COLLECTION_METADATA);

      collectionFacade.createCollection(COLLECTION_READ_COLLECTION_METADATA);

      String name = "attribute 1";
      String collection = internalName(COLLECTION_READ_COLLECTION_METADATA);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      List<DataDocument> metadata = collectionFacade.readCollectionMetadata(collection);

      Assert.assertEquals(metadata.size(), 4); // 4 documents: attribute, name, lock, rights
   }

   @Test
   public void testReadCollectionAttributes() throws Exception {
      setUpCollection(COLLECTION_READ_COLLECTION_ATTRIBUTES);

      String a1 = "attribute1";
      String a2 = "attribute2";
      String collection = internalName(COLLECTION_READ_COLLECTION_ATTRIBUTES);

      collectionFacade.createCollection(COLLECTION_READ_COLLECTION_ATTRIBUTES);
      collectionMetadataFacade.addOrIncrementAttribute(collection, a1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, a2);

      List<String> attributes = collectionFacade.readCollectionAttributes(collection);

      Assert.assertTrue(attributes.contains(a1));
      Assert.assertTrue(attributes.contains(a2));
   }

   @Test
   public void testGetAttributeValues() throws Exception {
      setUpCollection(COLLECTION_GET_ATTRIBUTE_VALUES);

      collectionFacade.createCollection(COLLECTION_GET_ATTRIBUTE_VALUES);
      String collection = internalName(COLLECTION_GET_ATTRIBUTE_VALUES);

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

      dataStorage.createDocument(collection, doc1);
      dataStorage.createDocument(collection, doc2);
      dataStorage.createDocument(collection, doc3);

      // we have to add attributes to metadata because we test them in getAttributeValues
      collectionMetadataFacade.addOrIncrementAttribute(collection, a1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, a2);

      Set<String> values = collectionFacade.getAttributeValues(collection, a1);

      Assert.assertTrue(values.contains(v1));
      Assert.assertTrue(values.contains(v2));
      Assert.assertFalse(values.contains(v3));
   }

   @Test
   public void testDropAttribute() throws Exception {
      setUpCollection(COLLECTION_DROP_COLLECTION_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_DROP_COLLECTION_ATTRIBUTE);
      String collection = internalName(COLLECTION_DROP_COLLECTION_ATTRIBUTE);

      String attribute1 = "attribute-to-drop";
      String attribute2 = "attribute";
      String value = "value";

      DataDocument doc1 = new DataDocument();
      doc1.put(attribute1, value);
      doc1.put(attribute2, value);
      DataDocument doc2 = new DataDocument();
      doc2.put(attribute1, value);
      doc2.put(attribute2, value);
      DataDocument doc3 = new DataDocument();
      doc3.put(attribute1, value);
      doc3.put(attribute2, value);

      dataStorage.createDocument(collection, doc1);
      dataStorage.createDocument(collection, doc2);
      dataStorage.createDocument(collection, doc3);

      // we have to add attributes to metadata because we test them in getAttributeValues
      for (int i = 0; i < 3; i++) {
         collectionMetadataFacade.addOrIncrementAttribute(collection, attribute1);
         collectionMetadataFacade.addOrIncrementAttribute(collection, attribute2);
      }

      collectionFacade.dropAttribute(collection, attribute1);

      List<DataDocument> documents = dataStorage.search(collection, null, null, 0, 0);
      for (int i = 0; i < 3; i++) {
         Assert.assertFalse(documents.get(i).containsKey(attribute1));
      }
   }

   @Test
   public void testRenameAttribute() throws Exception {
      setUpCollection(COLLECTION_RENAME_ATTRIBUTE);

      collectionFacade.createCollection(COLLECTION_RENAME_ATTRIBUTE);
      String collection = internalName(COLLECTION_RENAME_ATTRIBUTE);

      String name = "attribute 1";
      String newName = "new attribute 1";

      DataDocument doc1 = new DataDocument();
      doc1.put(name, DUMMY_VALUE1);
      DataDocument doc2 = new DataDocument();
      doc2.put(name, DUMMY_VALUE1);
      DataDocument doc3 = new DataDocument();
      doc3.put(name, DUMMY_VALUE1);

      dataStorage.createDocument(collection, doc1);
      dataStorage.createDocument(collection, doc2);
      dataStorage.createDocument(collection, doc3);

      // we have to increment 3 times, because we added 3 documents
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      collectionFacade.renameAttribute(collection, name, newName);

      Assert.assertTrue(isEveryDocumentFilledByNewAttribute(collection, newName));
   }

   @Test
   public void testOnCollectionEvent() throws Exception {

   }

   private String internalName(String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

   private DataDocument createDummyDocument() {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(DUMMY_KEY1, DUMMY_VALUE1);
      dataDocument.put(DUMMY_KEY2, DUMMY_VALUE2);

      return dataDocument;
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

   private void setUpCollection(String originalCollectionName) {
      dataStorage.dropCollection(internalName(originalCollectionName));
      dataStorage.dropCollection("meta." + internalName(originalCollectionName));
   }
}