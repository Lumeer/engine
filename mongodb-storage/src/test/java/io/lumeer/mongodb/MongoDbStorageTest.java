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
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.data.StorageConnection;

import com.mongodb.client.model.Filters;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class MongoDbStorageTest {

   private static final String DB_HOST = System.getProperty("lumeer.db.host", "ds163667.mlab.com");
   private static final String DB_NAME = System.getProperty("lumeer.db.name", "lumeer-test");
   private static final int DB_PORT = Integer.getInteger("lumeer.db.port", 63667);
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
   private final String COLLECTION_RUN = "collectionSearchRaw";
   private final String COLLECTION_RENAME_ATTRIBUTE = "collectionRenameAttribute";
   private final String COLLECTION_INC_ATTR_VALUE_BY = "collectionIncAttrValueBy";
   private final String COLLECTION_GET_ATTRIBUTE_VALUES = "collectionGetAttributeValues";
   private final String COLLECTION_NESTED_DOCUMENTS = "collectionNestedDocuments";
   private final String COLLECTION_BASIC_ARRAY_MANIPULATION = "collectionBasicArrayManipulation";
   private final String COLLECTION_COMPLEX_ARRAY_MANIPULATION = "collectionComplexArrayManipulation";
   private final String COLLECTION_AGGREGATE = "collectionAggregate";

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
      mongoDbStorage.dropCollection(COLLECTION_RUN);
      mongoDbStorage.dropCollection(COLLECTION_RENAME_ATTRIBUTE);
      mongoDbStorage.dropCollection(COLLECTION_INC_ATTR_VALUE_BY);
      mongoDbStorage.dropCollection(COLLECTION_GET_ATTRIBUTE_VALUES);
      mongoDbStorage.dropCollection(COLLECTION_NESTED_DOCUMENTS);
      mongoDbStorage.dropCollection(COLLECTION_BASIC_ARRAY_MANIPULATION);
      mongoDbStorage.dropCollection(COLLECTION_COMPLEX_ARRAY_MANIPULATION);
      mongoDbStorage.dropCollection(COLLECTION_AGGREGATE);
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
   public void testRun() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_RUN);

      for (int i = 0; i < 1000; i++) {
         DataDocument insertedDocument = createDummyDocument();
         mongoDbStorage.createDocument(COLLECTION_RUN, insertedDocument);
      }

      // query form: https://docs.mongodb.com/v3.2/reference/command/find/#definition
      String query = "{find: \"" + COLLECTION_RUN + "\"}";
      List<DataDocument> searchDocuments = mongoDbStorage.run(query);

      // run() method returns 101 entries due to it is a default value of "batchSize" query key
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

      mongoDbStorage.incrementAttributeValueBy(COLLECTION_INC_ATTR_VALUE_BY, id, incAttribute, 1);

      DataDocument readDocument = mongoDbStorage.readDocument(COLLECTION_INC_ATTR_VALUE_BY, id);

      Assert.assertTrue(readDocument.containsKey(incAttribute));
      Assert.assertEquals(readDocument.getInteger(incAttribute).intValue(), 1);

      mongoDbStorage.incrementAttributeValueBy(COLLECTION_INC_ATTR_VALUE_BY, id, incAttribute, 10);

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
      d.put("l", Arrays.asList(1, 2, 3, 4));
      d.put("ld", Arrays.asList(createDummyDocument(), createDummyDocument(), createDummyDocument()));

      DataDocument doubleNested = createDummyDocument();
      doubleNested.put("dn", createDummyDocument());
      d.put("c", doubleNested);

      DataDocument trippleNested = createDummyDocument();
      DataDocument tn = createDummyDocument();
      tn.put("tnn", createDummyDocument());
      trippleNested.put("tn", tn);
      trippleNested.put("ld", Arrays.asList(createDummyDocument(), createDummyDocument(), createDummyDocument()));
      d.put("d_d", trippleNested);

      String id = mongoDbStorage.createDocument(COLLECTION_NESTED_DOCUMENTS, d);

      mongoDbStorage.dropAttribute(COLLECTION_NESTED_DOCUMENTS, id, "d_d.tn.tnn." + DUMMY_KEY1);

      // use debug to see nested hierarchy works
      DataDocument nested = mongoDbStorage.readDocument(COLLECTION_NESTED_DOCUMENTS, id);

      Assert.assertNotNull(nested);
   }

   @Test
   public void testBasicArrayManipulation() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_BASIC_ARRAY_MANIPULATION);

      DataDocument doc = createDummyDocument();
      doc.put("a", Arrays.asList(1, 2, 3, 4));

      String id = mongoDbStorage.createDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, doc);
      DataDocument fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, id);
      Assert.assertEquals(4, fromDb.getArrayList("a", Integer.class).size());

      mongoDbStorage.addItemToArray(COLLECTION_BASIC_ARRAY_MANIPULATION, id, "a", 10);
      fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, id);
      Assert.assertEquals(5, fromDb.getArrayList("a", Integer.class).size());

      mongoDbStorage.addItemsToArray(COLLECTION_BASIC_ARRAY_MANIPULATION, id, "a", Arrays.asList(5, 6, 7));
      fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, id);
      Assert.assertEquals(8, fromDb.getArrayList("a", Integer.class).size());

      mongoDbStorage.removeItemFromArray(COLLECTION_BASIC_ARRAY_MANIPULATION, id, "a", 10);
      fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, id);
      Assert.assertEquals(7, fromDb.getArrayList("a", Integer.class).size());

      mongoDbStorage.removeItemsFromArray(COLLECTION_BASIC_ARRAY_MANIPULATION, id, "a", Arrays.asList(5, 6, 7));
      fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, id);
      Assert.assertEquals(4, fromDb.getArrayList("a", Integer.class).size());
   }

   @Test
   public void testComplexArrayManipulation() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_COMPLEX_ARRAY_MANIPULATION);

      DataDocument d = createDummyDocument();
      DataDocument n = createDummyDocument();

      DataDocument d1 = new DataDocument();
      d1.put("gt", 10);
      DataDocument d2 = new DataDocument();
      d2.put("lt", 20);
      n.put("a", Arrays.asList(d1, d2));
      d.put("n", n);

      String id = mongoDbStorage.createDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, d);
      DataDocument fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id);
      Assert.assertEquals(2, fromDb.getArrayList("n.a", DataDocument.class).size());

      DataDocument d3 = new DataDocument();
      d3.put("equals", "true");
      mongoDbStorage.addItemToArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id, "n.a", d3);
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id);
      Assert.assertEquals(3, fromDb.getArrayList("n.a", DataDocument.class).size());

      DataDocument d4 = new DataDocument();
      d4.put("i", true);
      DataDocument d5 = new DataDocument();
      d4.put("p", 12.3);
      mongoDbStorage.addItemsToArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id, "n.a", Arrays.asList(d4, d5));
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id);
      Assert.assertEquals(5, fromDb.getArrayList("n.a", DataDocument.class).size());

      mongoDbStorage.removeItemsFromArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id, "n.a", Arrays.asList(d2, d3));
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id);
      Assert.assertEquals(3, fromDb.getArrayList("n.a", DataDocument.class).size());

      mongoDbStorage.removeItemFromArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id, "n.a", d2);
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id);
      Assert.assertEquals(3, fromDb.getArrayList("n.a", DataDocument.class).size());

      mongoDbStorage.removeItemFromArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id, "n.a", d4);
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, id);
      Assert.assertEquals(2, fromDb.getArrayList("n.a", DataDocument.class).size());

   }

   @Test
   public void testAggregate() {
      mongoDbStorage.createCollection(COLLECTION_AGGREGATE);

      mongoDbStorage.createDocument(COLLECTION_AGGREGATE, getTestDocument("a", "1", "val1"));
      mongoDbStorage.createDocument(COLLECTION_AGGREGATE, getTestDocument("a", "2", "val2"));
      mongoDbStorage.createDocument(COLLECTION_AGGREGATE, getTestDocument("a", "3", "val3"));
      mongoDbStorage.createDocument(COLLECTION_AGGREGATE, getTestDocument("a", "4", "val6"));
      mongoDbStorage.createDocument(COLLECTION_AGGREGATE, getTestDocument("b", "1", "val4"));
      mongoDbStorage.createDocument(COLLECTION_AGGREGATE, getTestDocument("c", "1", "val5"));

      final DataDocument filters = new DataDocument();
      final DataDocument condition = new DataDocument();
      condition.put("$gt", 2);
      filters.put("param2", condition);

      final DataDocument grouping = new DataDocument();
      final DataDocument groupId = new DataDocument();
      final DataDocument aggregate = new DataDocument();
      groupId.put("param1", "$param1");
      aggregate.put("$sum", "$param2");
      grouping.put("_id", groupId);
      grouping.put("added", aggregate);

      final Query q = new Query(filters);
      q.setGrouping(grouping);
      q.setCollections(Collections.singleton(COLLECTION_AGGREGATE));

      List<DataDocument> result = mongoDbStorage.query(q);
      Assert.assertEquals(result.size(), 1);
      Assert.assertEquals(result.get(0).get("_id"), "{ \"param1\" : \"a\" }");
      Assert.assertEquals(result.get(0).get("added"), 7);

      final DataDocument project = new DataDocument();
      final DataDocument multiply = new DataDocument();
      multiply.put("$multiply", Arrays.asList("$param2", 5));
      project.put("param4", multiply);

      final DataDocument sort = new DataDocument();
      sort.put("param4", -1);

      q.setGrouping(new DataDocument());
      q.setProjections(project);
      q.setSorting(sort);

      result = mongoDbStorage.query(q);

      Assert.assertEquals(result.size(), 2);
      Assert.assertEquals(result.get(0).get("param4"), 20);
      Assert.assertEquals(result.get(1).get("param4"), 15);

      q.setSkip(1);
      result = mongoDbStorage.query(q);
      Assert.assertEquals(result.size(), 1);
      Assert.assertEquals(result.get(0).get("param4"), 15);

      q.setSkip(0);
      q.setLimit(1);
      result = mongoDbStorage.query(q);
      Assert.assertEquals(result.size(), 1);
      Assert.assertEquals(result.get(0).get("param4"), 20);
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

   private DataDocument getTestDocument(final String... values) {
      final DataDocument d = new DataDocument();

      for (int i = 0; i < values.length; i++) {
         try {
            int x = Integer.parseInt(values[i]);
            d.put("param" + (i + 1), x);
         } catch (NumberFormatException nfe) {
            d.put("param" + (i + 1), values[i]);
         }
      }

      return d;
   }

}