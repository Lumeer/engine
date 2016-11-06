package io.lumeer.mongodb;/*
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

import io.lumeer.engine.api.data.DataDocument;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 *         <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class MongoDbStorageTest {

   private final String DUMMY_COLLECTION1 = "testCollection1";
   private final String DUMMY_COLLECTION2 = "testCollection2";
   private final String DUMMY_KEY1 = "key1";
   private final String DUMMY_KEY2 = "key2";
   private final String DUMMY_VALUE1 = "param1";
   private final String DUMMY_VALUE2 = "param2";
   private final String DUMMY_CHANGED_VALUE1 = "changed_param1";
   private final String DUMMY_CHANGED_VALUE2 = "changed_param2";

   private MongoDbStorage mongoDbStorage;

   @BeforeMethod
   public void setUp() throws Exception {
      mongoDbStorage = new MongoDbStorage();
      mongoDbStorage.connect();
   }

   @Test
   public void testCreateAndDropCollection() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);
      mongoDbStorage.createCollection(DUMMY_COLLECTION2);

      Assert.assertEquals(mongoDbStorage.getAllCollections().size(), 2);

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
      mongoDbStorage.dropCollection(DUMMY_COLLECTION2);

      Assert.assertEquals(mongoDbStorage.getAllCollections().size(), 0);
   }

   @Test
   public void testGetAllCollections() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);
      mongoDbStorage.createCollection(DUMMY_COLLECTION2);

      Assert.assertEquals(mongoDbStorage.getAllCollections().size(), 2);

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
      mongoDbStorage.dropCollection(DUMMY_COLLECTION2);
   }

   @Test
   public void testCreateAndReadDocument() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(DUMMY_COLLECTION1, insertedDocument);

      DataDocument readedDocument = mongoDbStorage.readDocument(DUMMY_COLLECTION1, documentId);
      Assert.assertEquals(insertedDocument.getString(DUMMY_KEY1), readedDocument.getString(DUMMY_KEY1));
      Assert.assertEquals(insertedDocument.getString(DUMMY_KEY2), readedDocument.getString(DUMMY_KEY2));

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testCreateAndReadOldDocument() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      DataDocument insertedDocument = createDummyDocument();
      mongoDbStorage.createOldDocument(DUMMY_COLLECTION1, insertedDocument, "507f191e810c19729de860ea", 1);

      DataDocument readedDocument = mongoDbStorage.readOldDocument(DUMMY_COLLECTION1, "507f191e810c19729de860ea", 1);

      Assert.assertNotNull(readedDocument);

      mongoDbStorage.dropOldDocument(DUMMY_COLLECTION1, "507f191e810c19729de860ea", 1);

      readedDocument = mongoDbStorage.readOldDocument(DUMMY_COLLECTION1, "507f191e810c19729de860ea", 1);

      Assert.assertNull(readedDocument);

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);

   }

   @Test
   public void testUpdateDocument() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(DUMMY_COLLECTION1, insertedDocument);

      DataDocument readedDocument = mongoDbStorage.readDocument(DUMMY_COLLECTION1, documentId);
      changeDummyDocumentValues(readedDocument);
      readedDocument.remove("_id");

      mongoDbStorage.updateDocument(DUMMY_COLLECTION1, readedDocument, documentId);
      DataDocument readedAfterInsDocument = mongoDbStorage.readDocument(DUMMY_COLLECTION1, documentId);

      Assert.assertNotEquals(readedAfterInsDocument.getString(DUMMY_KEY1), DUMMY_VALUE1);
      Assert.assertNotEquals(readedAfterInsDocument.getString(DUMMY_KEY2), DUMMY_VALUE2);
      Assert.assertEquals(readedAfterInsDocument.getString(DUMMY_KEY1), DUMMY_CHANGED_VALUE1);
      Assert.assertEquals(readedAfterInsDocument.getString(DUMMY_KEY2), DUMMY_CHANGED_VALUE2);

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testDropDocument() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(DUMMY_COLLECTION1, insertedDocument);
      DataDocument readedDocument = mongoDbStorage.readDocument(DUMMY_COLLECTION1, documentId);

      Assert.assertNotNull(readedDocument);

      mongoDbStorage.dropDocument(DUMMY_COLLECTION1, documentId);
      readedDocument = mongoDbStorage.readDocument(DUMMY_COLLECTION1, documentId);

      Assert.assertNull(readedDocument);

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testRemoveAttribute() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(DUMMY_COLLECTION1, insertedDocument);
      DataDocument readedDocument = mongoDbStorage.readDocument(DUMMY_COLLECTION1, documentId);
      Assert.assertEquals(readedDocument.size(), 3);

      mongoDbStorage.removeAttribute(DUMMY_COLLECTION1, documentId, DUMMY_KEY1);

      readedDocument = mongoDbStorage.readDocument(DUMMY_COLLECTION1, documentId);
      Assert.assertEquals(readedDocument.size(), 2);

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testSearch() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = createDummyDocument();
         mongoDbStorage.createDocument(DUMMY_COLLECTION1, insertedDocument);
      }

      List<DataDocument> searchDocuments = mongoDbStorage.search(DUMMY_COLLECTION1, null, null, 100, 100);

      Assert.assertEquals(searchDocuments.size(), 100);

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testRawSearch() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = createDummyDocument();
         mongoDbStorage.createDocument(DUMMY_COLLECTION1, insertedDocument);
      }

      // query form: https://docs.mongodb.com/v3.2/reference/command/find/#definition
      String query = "{find: \"" + DUMMY_COLLECTION1 + "\"}";
      List<DataDocument> searchDocuments = mongoDbStorage.search(query);

      // search() method returns 101 entries due to it is a default value of "batchSize" query key
      Assert.assertEquals(searchDocuments.size(), 101);

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testRenameAttribute() throws Exception {
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      DataDocument insertedDocument = createDummyDocument();
      String id = mongoDbStorage.createDocument(DUMMY_COLLECTION1, insertedDocument);

      String changedAttr = "changed_" + DUMMY_KEY1;
      mongoDbStorage.renameAttribute(DUMMY_COLLECTION1, DUMMY_KEY1, changedAttr);

      DataDocument document = mongoDbStorage.readDocument(DUMMY_COLLECTION1, id);
      Assert.assertEquals(document.containsKey(DUMMY_KEY1), false);
      Assert.assertEquals(document.containsKey(changedAttr), true);
      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
   }

   @Test
   public void testGetAttributeValues() throws Exception {
      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
      mongoDbStorage.createCollection(DUMMY_COLLECTION1);

      int numDummyKey1 = 150;
      int numDummyKey2 = 10;
      for (int i = 0; i < numDummyKey1; i++) {
         DataDocument dataDocument = new DataDocument();
         dataDocument.put(DUMMY_KEY1, i);
         dataDocument.put(DUMMY_KEY2, i % numDummyKey2);
         mongoDbStorage.createDocument(DUMMY_COLLECTION1, dataDocument);
      }

      Set<String> attributeValues1 = mongoDbStorage.getAttributeValues(DUMMY_COLLECTION1, DUMMY_KEY1);
      Assert.assertEquals(attributeValues1.size(), Math.min(numDummyKey1, 100));

      Set<String> attributeValues2 = mongoDbStorage.getAttributeValues(DUMMY_COLLECTION1, DUMMY_KEY2);
      Assert.assertEquals(attributeValues2.size(), Math.min(numDummyKey2, 100));

      mongoDbStorage.dropCollection(DUMMY_COLLECTION1);
   }

   private DataDocument createDummyDocument() {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(DUMMY_KEY1, DUMMY_VALUE1);
      dataDocument.put(DUMMY_KEY2, DUMMY_VALUE2);
      return dataDocument;
   }

   private void changeDummyDocumentValues(DataDocument readedDocument) {
      readedDocument.replace(DUMMY_KEY1, DUMMY_CHANGED_VALUE1);
      readedDocument.replace(DUMMY_KEY2, DUMMY_CHANGED_VALUE2);
   }

}