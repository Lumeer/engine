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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.StorageConnection;

import com.mongodb.client.model.Filters;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class MongoDbStorageTest {

   private static final String DB_HOST = System.getProperty("lumeer.db.host", "ds163667.mlab.com");
   private static final String DB_NAME = System.getProperty("lumeer.db.name", "lumeer-test");
   private static final int DB_PORT = Integer.getInteger("lumeer.sysdb.port", 63667);
   private static final String DB_USER = System.getProperty("lumeer.db.user", "lumeer");
   private static final String DB_PASSWORD = System.getProperty("lumeer.db.passwd", "/Lumeer1");

   private final String DUMMY_KEY1 = "key1";
   private final String DUMMY_KEY2 = "key2";
   private final String DUMMY_VALUE1 = "param1";
   private final String DUMMY_VALUE2 = "param2";
   private final String DUMMY_CHANGED_VALUE1 = "changed_param1";
   private final String DUMMY_CHANGED_VALUE2 = "changed_param2";

   private final String COLLECTION_CREATE_AND_DROP_I = "collectionCreateAndDrop_I";
   private final String COLLECTION_CREATE_AND_DROP_II = "collectionCreateAndDrop_II";
   private final String COLLECTION_GET_ALL_COLLECTIONS_I = "collectionGetAllCollections_I";
   private final String COLLECTION_GET_ALL__COLLECTIONS_II = "collectionGetAllCollections_II";
   private final String COLLECTION_HAS_COLLECTION = "collectionHasCollection";
   private final String COLLECTION_COLLECTION_HAS_DOCUMENT = "collectionCollectionHasDocument";
   private final String COLLECTION_CREATE_AND_READ_DOCUMENT = "collectionCreateAndReadDocument";
   private final String COLLECTION_CREATE_AND_READ_OLD_DOCUMENT = "collectionCreateAndReadOldDocument";
   private final String COLLECTION_UPDATE_DOCUMENT = "collectionUpdateDocument";
   private final String COLLECTION_DROP_DOCUMENT = "collectionDropDocument";
   private final String COLLECTION_DROP_MANY = "collectionDropMany";
   private final String COLLECTION_DROP_ATTRIBUTE = "collectionRemoveAttribute";
   private final String COLLECTION_SEARCH = "collectionSearch";
   private final String COLLECTION_SEARCH_RAW = "collectionSearchRaw";
   private final String COLLECTION_RENAME_ATTRIBUTE = "collectionRenameAttribute";
   private final String COLLECTION_INC_ATTR_VALUE_BY = "collectionIncAttrValueBy";
   private final String COLLECTION_GET_ATTRIBUTE_VALUES = "collectionGetAttributeValues";
   private final String COLLECTION_NESTED_DOCUMENTS = "collectionNestedDocuments";

   private MongoDbStorage mongoDbStorage;

   @BeforeMethod
   public void setUp() throws Exception {
      mongoDbStorage = new MongoDbStorage();
      mongoDbStorage.connect(new StorageConnection(DB_HOST, DB_PORT, DB_USER, DB_PASSWORD), DB_NAME);

      // setup=drop collections for next use
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_DROP_I);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_DROP_II);
      mongoDbStorage.dropCollection(COLLECTION_GET_ALL_COLLECTIONS_I);
      mongoDbStorage.dropCollection(COLLECTION_GET_ALL__COLLECTIONS_II);
      mongoDbStorage.dropCollection(COLLECTION_HAS_COLLECTION);
      mongoDbStorage.dropCollection(COLLECTION_COLLECTION_HAS_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_READ_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_READ_OLD_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_UPDATE_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_DROP_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_DROP_MANY);
      mongoDbStorage.dropCollection(COLLECTION_DROP_ATTRIBUTE);
      mongoDbStorage.dropCollection(COLLECTION_SEARCH);
      mongoDbStorage.dropCollection(COLLECTION_SEARCH_RAW);
      mongoDbStorage.dropCollection(COLLECTION_RENAME_ATTRIBUTE);
      mongoDbStorage.dropCollection(COLLECTION_INC_ATTR_VALUE_BY);
      mongoDbStorage.dropCollection(COLLECTION_GET_ATTRIBUTE_VALUES);
      mongoDbStorage.dropCollection(COLLECTION_NESTED_DOCUMENTS);
   }

   @Test
   public void testCreateAndDropCollection() throws Exception {
      int numCollections = mongoDbStorage.getAllCollections().size();
      mongoDbStorage.createCollection(COLLECTION_CREATE_AND_DROP_I);
      mongoDbStorage.createCollection(COLLECTION_CREATE_AND_DROP_II);
      Assert.assertEquals(mongoDbStorage.getAllCollections().size(), numCollections + 2);

      numCollections = mongoDbStorage.getAllCollections().size();
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_DROP_I);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_DROP_II);
      Assert.assertEquals(mongoDbStorage.getAllCollections().size(), numCollections - 2);
   }

   @Test
   public void testGetAllCollections() throws Exception {
      int numCollections = mongoDbStorage.getAllCollections().size();
      mongoDbStorage.createCollection(COLLECTION_GET_ALL_COLLECTIONS_I);
      mongoDbStorage.createCollection(COLLECTION_GET_ALL__COLLECTIONS_II);

      Assert.assertEquals(mongoDbStorage.getAllCollections().size(), numCollections + 2);
   }

   @Test
   public void testHasCollection() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_HAS_COLLECTION);

      Assert.assertTrue(mongoDbStorage.hasCollection(COLLECTION_HAS_COLLECTION));
      Assert.assertFalse(mongoDbStorage.hasCollection("someNotExistingNameOfCollection"));
   }

   @Test
   public void testCollectionHasDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_COLLECTION_HAS_DOCUMENT);

      String id = mongoDbStorage.createDocument(COLLECTION_COLLECTION_HAS_DOCUMENT, createDummyDocument());

      Assert.assertTrue(mongoDbStorage.collectionHasDocument(COLLECTION_COLLECTION_HAS_DOCUMENT, id));

      String dummyId = "507f191e810c19729de860ea";

      Assert.assertFalse(mongoDbStorage.collectionHasDocument(COLLECTION_COLLECTION_HAS_DOCUMENT, dummyId));
   }

   @Test
   public void testCreateAndReadDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_CREATE_AND_READ_DOCUMENT);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(COLLECTION_CREATE_AND_READ_DOCUMENT, insertedDocument);

      DataDocument readedDocument = mongoDbStorage.readDocument(COLLECTION_CREATE_AND_READ_DOCUMENT, documentId);
      Assert.assertEquals(insertedDocument.getString(DUMMY_KEY1), readedDocument.getString(DUMMY_KEY1));
      Assert.assertEquals(insertedDocument.getString(DUMMY_KEY2), readedDocument.getString(DUMMY_KEY2));
   }

   @Test
   public void testCreateAndReadOldDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_CREATE_AND_READ_OLD_DOCUMENT);

      String dummyKey = "507f191e810c19729de860ea";

      DataDocument insertedDocument = createDummyDocument();
      mongoDbStorage.createOldDocument(COLLECTION_CREATE_AND_READ_OLD_DOCUMENT, insertedDocument, dummyKey, 1);
      DataDocument readDocument = mongoDbStorage.readOldDocument(COLLECTION_CREATE_AND_READ_OLD_DOCUMENT, dummyKey, 1);
      Assert.assertNotNull(readDocument);

      mongoDbStorage.dropOldDocument(COLLECTION_CREATE_AND_READ_OLD_DOCUMENT, dummyKey, 1);
      readDocument = mongoDbStorage.readOldDocument(COLLECTION_CREATE_AND_READ_OLD_DOCUMENT, dummyKey, 1);
      Assert.assertNull(readDocument);
   }

   @Test
   public void testUpdateDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_UPDATE_DOCUMENT);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(COLLECTION_UPDATE_DOCUMENT, insertedDocument);

      DataDocument readedDocument = mongoDbStorage.readDocument(COLLECTION_UPDATE_DOCUMENT, documentId);
      changeDummyDocumentValues(readedDocument);
      readedDocument.put(LumeerConst.METADATA_VERSION_KEY, 1);

      mongoDbStorage.updateDocument(COLLECTION_UPDATE_DOCUMENT, readedDocument, documentId, -1);
      DataDocument readedAfterInsDocument = mongoDbStorage.readDocument(COLLECTION_UPDATE_DOCUMENT, documentId);

      Assert.assertNotEquals(readedAfterInsDocument.getString(DUMMY_KEY1), DUMMY_VALUE1);
      Assert.assertNotEquals(readedAfterInsDocument.getString(DUMMY_KEY2), DUMMY_VALUE2);
      Assert.assertEquals(readedAfterInsDocument.getString(DUMMY_KEY1), DUMMY_CHANGED_VALUE1);
      Assert.assertEquals(readedAfterInsDocument.getString(DUMMY_KEY2), DUMMY_CHANGED_VALUE2);
      Assert.assertEquals(readedAfterInsDocument.getInteger(LumeerConst.METADATA_VERSION_KEY).intValue(), 1);
   }

   @Test
   public void testDropDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_DROP_DOCUMENT);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(COLLECTION_DROP_DOCUMENT, insertedDocument);
      DataDocument readedDocument = mongoDbStorage.readDocument(COLLECTION_DROP_DOCUMENT, documentId);

      Assert.assertNotNull(readedDocument);

      mongoDbStorage.dropDocument(COLLECTION_DROP_DOCUMENT, documentId);
      readedDocument = mongoDbStorage.readDocument(COLLECTION_DROP_DOCUMENT, documentId);

      Assert.assertNull(readedDocument);
   }

   @Test
   public void testDropManyDocuments() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_DROP_MANY);

      String dropManyKey = "dropManyKey";
      String value1 = "v1";
      String value2 = "v2";
      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = createDummyDocument();
         insertedDocument.put(dropManyKey, i % 2 == 0 ? value1 : value2);
         mongoDbStorage.createDocument(COLLECTION_DROP_MANY, insertedDocument);
      }

      List<DataDocument> docs = mongoDbStorage.search(COLLECTION_DROP_MANY, null, null, 0, 0);
      Assert.assertEquals(docs.size(), 1000);

      mongoDbStorage.dropManyDocuments(COLLECTION_DROP_MANY, MongoUtils.convertBsonToJson(Filters.eq(dropManyKey, value1)));

      docs = mongoDbStorage.search(COLLECTION_DROP_MANY, null, null, 0, 0);
      Assert.assertEquals(docs.size(), 500);
   }

   @Test
   public void testDropAttribute() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_DROP_ATTRIBUTE);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(COLLECTION_DROP_ATTRIBUTE, insertedDocument);
      DataDocument readedDocument = mongoDbStorage.readDocument(COLLECTION_DROP_ATTRIBUTE, documentId);
      Assert.assertEquals(readedDocument.size(), 4);

      mongoDbStorage.dropAttribute(COLLECTION_DROP_ATTRIBUTE, documentId, DUMMY_KEY1);

      readedDocument = mongoDbStorage.readDocument(COLLECTION_DROP_ATTRIBUTE, documentId);
      Assert.assertEquals(readedDocument.size(), 3);
   }

   @Test
   public void testSearch() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_SEARCH);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = createDummyDocument();
         mongoDbStorage.createDocument(COLLECTION_SEARCH, insertedDocument);
      }

      List<DataDocument> searchDocuments = mongoDbStorage.search(COLLECTION_SEARCH, null, null, 100, 100);

      Assert.assertEquals(searchDocuments.size(), 100);
   }

   @Test
   public void testRawSearch() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_SEARCH_RAW);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = createDummyDocument();
         mongoDbStorage.createDocument(COLLECTION_SEARCH_RAW, insertedDocument);
      }

      // query form: https://docs.mongodb.com/v3.2/reference/command/find/#definition
      String query = "{find: \"" + COLLECTION_SEARCH_RAW + "\"}";
      List<DataDocument> searchDocuments = mongoDbStorage.search(query);

      // search() method returns 101 entries due to it is a default value of "batchSize" query key
      Assert.assertEquals(searchDocuments.size(), 101);
   }

   @Test
   public void testRenameAttribute() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_RENAME_ATTRIBUTE);

      DataDocument insertedDocument = createDummyDocument();
      String id = mongoDbStorage.createDocument(COLLECTION_RENAME_ATTRIBUTE, insertedDocument);

      String changedAttr = "changed_" + DUMMY_KEY1;
      mongoDbStorage.renameAttribute(COLLECTION_RENAME_ATTRIBUTE, DUMMY_KEY1, changedAttr);

      DataDocument document = mongoDbStorage.readDocument(COLLECTION_RENAME_ATTRIBUTE, id);
      Assert.assertEquals(document.containsKey(DUMMY_KEY1), false);
      Assert.assertEquals(document.containsKey(changedAttr), true);
   }

   @Test
   public void testIncrementAttributeValueBy() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_INC_ATTR_VALUE_BY);

      String incAttribute = "incAttribute";

      DataDocument insertedDocument = createDummyDocument();
      String id = mongoDbStorage.createDocument(COLLECTION_INC_ATTR_VALUE_BY, insertedDocument);

      mongoDbStorage.incerementAttributeValueBy(COLLECTION_INC_ATTR_VALUE_BY, id, incAttribute, 1);

      DataDocument readDocument = mongoDbStorage.readDocument(COLLECTION_INC_ATTR_VALUE_BY, id);

      Assert.assertTrue(readDocument.containsKey(incAttribute));
      Assert.assertEquals(readDocument.getInteger(incAttribute).intValue(), 1);

      mongoDbStorage.incerementAttributeValueBy(COLLECTION_INC_ATTR_VALUE_BY, id, incAttribute, 10);

      readDocument = mongoDbStorage.readDocument(COLLECTION_INC_ATTR_VALUE_BY, id);
      Assert.assertTrue(readDocument.containsKey(incAttribute));
      Assert.assertEquals(readDocument.getInteger(incAttribute).intValue(), 11);
   }

   @Test
   public void testGetAttributeValues() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_GET_ATTRIBUTE_VALUES);

      // simple case
      int numDummyKey1 = 150;
      int numDummyKey2 = 10;
      for (int i = 0; i < numDummyKey1; i++) {
         DataDocument dataDocument = new DataDocument();
         dataDocument.put(DUMMY_KEY1, i);
         dataDocument.put(DUMMY_KEY2, i % numDummyKey2);
         mongoDbStorage.createDocument(COLLECTION_GET_ATTRIBUTE_VALUES, dataDocument);
      }

      Set<String> attributeValues1 = mongoDbStorage.getAttributeValues(COLLECTION_GET_ATTRIBUTE_VALUES, DUMMY_KEY1);
      Assert.assertEquals(attributeValues1.size(), Math.min(numDummyKey1, 100));

      Set<String> attributeValues2 = mongoDbStorage.getAttributeValues(COLLECTION_GET_ATTRIBUTE_VALUES, DUMMY_KEY2);
      Assert.assertEquals(attributeValues2.size(), Math.min(numDummyKey2, 100));

      // case when some attribute value is null

      String k1 = "k1";
      String k2 = "k2";
      String v1 = "v1";
      String v2 = "v2";
      String v3 = "v3";

      DataDocument d1 = new DataDocument();
      d1.put(k1, v1);
      d1.put(k2, v2);

      DataDocument d2 = new DataDocument();
      d2.put(k1, v3);

      mongoDbStorage.createDocument(COLLECTION_GET_ATTRIBUTE_VALUES, d1);
      mongoDbStorage.createDocument(COLLECTION_GET_ATTRIBUTE_VALUES, d2);

      Set<String> a1 = mongoDbStorage.getAttributeValues(COLLECTION_GET_ATTRIBUTE_VALUES, k1);
      Assert.assertTrue(a1.contains(v1));
      Assert.assertTrue(a1.contains(v3));
      Assert.assertFalse(a1.contains(v2));
   }

   @Test
   public void testNestedDocuments() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_NESTED_DOCUMENTS);

      DataDocument d = createDummyDocument();
      d.put("a", createDummyDocument());
      d.put("b", createDummyDocument());

      DataDocument doubleNested = createDummyDocument();
      doubleNested.put("dn", createDummyDocument());
      d.put("c", doubleNested);

      DataDocument trippleNested = createDummyDocument();
      DataDocument tn = createDummyDocument();
      tn.put("tnn", createDummyDocument());
      trippleNested.put("tn", tn);
      d.put("d", trippleNested);

      String id = mongoDbStorage.createDocument(COLLECTION_NESTED_DOCUMENTS, d);

      // use debug to see nested hierarchy works
      DataDocument nested = mongoDbStorage.readDocument(COLLECTION_NESTED_DOCUMENTS, id);

      Assert.assertNotNull(nested);
   }

   private DataDocument createDummyDocument() {
      DataDocument dataDocument = new DataDocument();
      dataDocument.put(DUMMY_KEY1, DUMMY_VALUE1);
      dataDocument.put(DUMMY_KEY2, DUMMY_VALUE2);
      dataDocument.put(LumeerConst.METADATA_VERSION_KEY, 0);
      return dataDocument;
   }

   private void changeDummyDocumentValues(DataDocument readedDocument) {
      readedDocument.replace(DUMMY_KEY1, DUMMY_CHANGED_VALUE1);
      readedDocument.replace(DUMMY_KEY2, DUMMY_CHANGED_VALUE2);
   }

}