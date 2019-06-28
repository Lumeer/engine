/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.storage.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorageStats;
import io.lumeer.engine.api.data.Query;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.model.Filters;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MongoDbStorageTest extends MongoDbTestBase {

   private final String DUMMY_KEY1 = "key1";
   private final String DUMMY_KEY2 = "key2";
   private final String DUMMY_VALUE1 = "param1";
   private final String DUMMY_VALUE2 = "param2";
   private final String DUMMY_CHANGED_VALUE1 = "changed_param1";
   private final String DUMMY_CHANGED_VALUE2 = "changed_param2";

   private final String COLLECTION_CREATE_AND_DROP_I = "collectionCreateAndDrop_I";
   private final String COLLECTION_CREATE_AND_DROP_II = "collectionCreateAndDrop_II";
   private final String COLLECTION_RENAME_OLD = "collectionRenameOld";
   private final String COLLECTION_RENAME_NEW = "collectionRenameNew";
   private final String COLLECTION_GET_ALL_COLLECTIONS_I = "collectionGetAllCollections_I";
   private final String COLLECTION_GET_ALL__COLLECTIONS_II = "collectionGetAllCollections_II";
   private final String COLLECTION_HAS_COLLECTION = "collectionHasCollection";
   private final String COLLECTION_COLLECTION_HAS_DOCUMENT = "collectionCollectionHasDocument";
   private final String COLLECTION_CREATE_AND_READ_DOCUMENT = "collectionCreateAndReadDocument";
   private final String COLLECTION_CREATE_DOCUMENTS = "collectionCreateDocuments";
   private final String COLLECTION_CREATE_DOCUMENTS_EXCEPTION = "collectionCreateDocumentsWithException";
   private final String COLLECTION_CREATE_AND_READ_OLD_DOCUMENT = "collectionCreateAndReadOldDocument";
   private final String COLLECTION_UPDATE_DOCUMENT = "collectionUpdateDocument";
   private final String COLLECTION_REPLACE_DOCUMENT = "collectionReplaceDocument";
   private final String COLLECTION_DROP_DOCUMENT = "collectionDropDocument";
   private final String COLLECTION_DROP_MANY = "collectionDropMany";
   private final String COLLECTION_DROP_ATTRIBUTE = "collectionRemoveAttribute";
   private final String COLLECTION_SEARCH_ATTRS = "collectionSearchAttrs";
   private final String COLLECTION_SEARCH = "collectionSearch";
   private final String COLLECTION_SEARCH_PROJECTION = "collectionSearchWithProjection";
   private final String COLLECTION_INDEXES = "collectionIndexes";
   private final String COLLECTION_RUN = "collectionSearchRaw";
   private final String COLLECTION_RENAME_ATTRIBUTE = "collectionRenameAttribute";
   private final String COLLECTION_INC_ATTR_VALUE_BY = "collectionIncAttrValueBy";
   private final String COLLECTION_GET_ATTRIBUTE_VALUES = "collectionGetAttributeValues";
   private final String COLLECTION_NESTED_DOCUMENTS = "collectionNestedDocuments";
   private final String COLLECTION_BASIC_ARRAY_MANIPULATION = "collectionBasicArrayManipulation";
   private final String COLLECTION_COMPLEX_ARRAY_MANIPULATION = "collectionComplexArrayManipulation";
   private final String COLLECTION_AGGREGATE = "collectionAggregate";
   private final String COLLECTION_STATS = "collectionStatistics";
   private final String COLLECTION_CSTATS = "collectionCStatistics";

   private MongoDbStorageDialect mongoDbStorageDialect;

   @Before
   public void setUp() throws Exception {
      mongoDbStorageDialect = new MongoDbStorageDialect();

      // setup=drop collections for next use
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_DROP_I);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_DROP_II);
      mongoDbStorage.dropCollection(COLLECTION_RENAME_OLD);
      mongoDbStorage.dropCollection(COLLECTION_RENAME_NEW);
      mongoDbStorage.dropCollection(COLLECTION_GET_ALL_COLLECTIONS_I);
      mongoDbStorage.dropCollection(COLLECTION_GET_ALL__COLLECTIONS_II);
      mongoDbStorage.dropCollection(COLLECTION_HAS_COLLECTION);
      mongoDbStorage.dropCollection(COLLECTION_COLLECTION_HAS_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_READ_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_DOCUMENTS);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_DOCUMENTS_EXCEPTION);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_READ_OLD_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_UPDATE_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_DROP_DOCUMENT);
      mongoDbStorage.dropCollection(COLLECTION_DROP_MANY);
      mongoDbStorage.dropCollection(COLLECTION_DROP_ATTRIBUTE);
      mongoDbStorage.dropCollection(COLLECTION_SEARCH_ATTRS);
      mongoDbStorage.dropCollection(COLLECTION_SEARCH);
      mongoDbStorage.dropCollection(COLLECTION_INDEXES);
      mongoDbStorage.dropCollection(COLLECTION_RUN);
      mongoDbStorage.dropCollection(COLLECTION_RENAME_ATTRIBUTE);
      mongoDbStorage.dropCollection(COLLECTION_REPLACE_DOCUMENT);
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
      assertThat(mongoDbStorage.getAllCollections().stream().filter(s -> !"system.indexes".equals(s))).hasSize(numCollections + 2);

      numCollections = mongoDbStorage.getAllCollections().size();
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_DROP_I);
      mongoDbStorage.dropCollection(COLLECTION_CREATE_AND_DROP_II);
      assertThat(mongoDbStorage.getAllCollections()).hasSize(numCollections - 2);
   }

   @Test
   public void testRenameCollection() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_RENAME_OLD);
      if (mongoDbStorage.hasCollection(COLLECTION_RENAME_NEW)) {
         mongoDbStorage.dropCollection(COLLECTION_RENAME_NEW);
      }
      mongoDbStorage.renameCollection(COLLECTION_RENAME_OLD, COLLECTION_RENAME_NEW);
      assertThat(mongoDbStorage.hasCollection(COLLECTION_RENAME_NEW)).isTrue();
      assertThat(mongoDbStorage.hasCollection(COLLECTION_RENAME_OLD)).isFalse();
   }

   @Test
   public void testGetAllCollections() throws Exception {
      int numCollections = mongoDbStorage.getAllCollections().size();
      mongoDbStorage.createCollection(COLLECTION_GET_ALL_COLLECTIONS_I);
      mongoDbStorage.createCollection(COLLECTION_GET_ALL__COLLECTIONS_II);
      assertThat(mongoDbStorage.getAllCollections().stream().filter(s -> !"system.indexes".equals(s))).hasSize(numCollections + 2);
   }

   @Test
   public void testHasCollection() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_HAS_COLLECTION);

      assertThat(mongoDbStorage.hasCollection(COLLECTION_HAS_COLLECTION)).isTrue();
      assertThat(mongoDbStorage.hasCollection("someNotExistingNameOfCollection")).isFalse();
   }

   @Test
   public void testCollectionHasDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_COLLECTION_HAS_DOCUMENT);

      String id = mongoDbStorage.createDocument(COLLECTION_COLLECTION_HAS_DOCUMENT, createDummyDocument());
      assertThat(mongoDbStorage.collectionHasDocument(COLLECTION_COLLECTION_HAS_DOCUMENT, mongoDbStorageDialect.documentIdFilter(id))).isTrue();

      String dummyId = "507f191e810c19729de860ea";
      assertThat(mongoDbStorage.collectionHasDocument(COLLECTION_COLLECTION_HAS_DOCUMENT, mongoDbStorageDialect.documentIdFilter(dummyId))).isFalse();
   }

   @Test
   public void testCreateAndReadDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_CREATE_AND_READ_DOCUMENT);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(COLLECTION_CREATE_AND_READ_DOCUMENT, insertedDocument);

      DataDocument readedDocument = mongoDbStorage.readDocument(COLLECTION_CREATE_AND_READ_DOCUMENT, mongoDbStorageDialect.documentIdFilter(documentId));
      assertThat(insertedDocument.getString(DUMMY_KEY1)).isEqualTo(readedDocument.getString(DUMMY_KEY1));
      assertThat(insertedDocument.getString(DUMMY_KEY2)).isEqualTo(readedDocument.getString(DUMMY_KEY2));
   }

   @Test
   public void testCreateDocuments() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_CREATE_DOCUMENTS);

      List<DataDocument> documents = new LinkedList<>();
      documents.add(new DataDocument("a", "a").append("b", "a"));
      documents.add(new DataDocument("a", "a").append("b", "b"));
      documents.add(new DataDocument("a", "a").append("b", "c"));
      documents.add(new DataDocument("a", "a").append("b", "d"));
      documents.add(new DataDocument("a", "a").append("b", "a"));

      List<String> ids = mongoDbStorage.createDocuments(COLLECTION_CREATE_DOCUMENTS, documents);
      assertThat(ids).hasSize(5);

      List<DataDocument> search = mongoDbStorage.search(COLLECTION_CREATE_DOCUMENTS, null, null, 0, 0);
      assertThat(search).hasSize(5);
   }

   @Test
   public void testCreateDocumentsWithException() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_CREATE_DOCUMENTS_EXCEPTION);
      mongoDbStorage.createIndex(COLLECTION_CREATE_DOCUMENTS_EXCEPTION, new DataDocument("b", 1), true);

      List<DataDocument> documents = new LinkedList<>();
      documents.add(new DataDocument("a", "a").append("b", "a"));
      documents.add(new DataDocument("a", "a").append("b", "b"));
      documents.add(new DataDocument("a", "a").append("b", "c"));
      documents.add(new DataDocument("a", "a").append("b", "d"));
      documents.add(new DataDocument("a", "a").append("b", "a"));

      assertThatThrownBy(() -> mongoDbStorage.createDocuments(COLLECTION_CREATE_DOCUMENTS_EXCEPTION, documents)).isInstanceOf(MongoBulkWriteException.class);
      List<DataDocument> search = mongoDbStorage.search(COLLECTION_CREATE_DOCUMENTS_EXCEPTION, null, null, 0, 0);
      assertThat(search).hasSize(4);
   }

   @Test
   public void testReplaceDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_REPLACE_DOCUMENT);

      DataDocument insertedDocument = new DataDocument("a", 1).append("b", 2).append("c", 3);
      String documentId = mongoDbStorage.createDocument(COLLECTION_REPLACE_DOCUMENT, insertedDocument);
      final DataFilter documentIdFilter = mongoDbStorageDialect.documentIdFilter(documentId);

      DataDocument replaceDocument = new DataDocument("d", 4).append("e", 5).append("f", 6);
      mongoDbStorage.replaceDocument(COLLECTION_REPLACE_DOCUMENT, replaceDocument, documentIdFilter);

      DataDocument readedDocument = mongoDbStorage.readDocument(COLLECTION_REPLACE_DOCUMENT, documentIdFilter);

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(readedDocument.containsKey("a")).as("a").isFalse();
      assertions.assertThat(readedDocument.containsKey("b")).as("b").isFalse();
      assertions.assertThat(readedDocument.containsKey("c")).as("c").isFalse();
      assertions.assertThat(readedDocument.containsKey("d")).as("d").isTrue();
      assertions.assertThat(readedDocument.containsKey("e")).as("e").isTrue();
      assertions.assertThat(readedDocument.containsKey("f")).as("f").isTrue();
      assertions.assertAll();
   }

   @Test
   public void testDropDocument() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_DROP_DOCUMENT);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(COLLECTION_DROP_DOCUMENT, insertedDocument);
      final DataFilter documentIdFilter = mongoDbStorageDialect.documentIdFilter(documentId);
      DataDocument readedDocument = mongoDbStorage.readDocument(COLLECTION_DROP_DOCUMENT, documentIdFilter);

      assertThat(readedDocument).isNotNull();

      mongoDbStorage.dropDocument(COLLECTION_DROP_DOCUMENT, documentIdFilter);
      readedDocument = mongoDbStorage.readDocument(COLLECTION_DROP_DOCUMENT, documentIdFilter);

      assertThat(readedDocument).isNull();
   }

   @Test
   public void testDropManyDocuments() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_DROP_MANY);

      String dropManyKey = "dropManyKey";
      String value1 = "v1";
      String value2 = "v2";
      for (int i = 0; i < 100; i++) {
         DataDocument insertedDocument = createDummyDocument();
         insertedDocument.put(dropManyKey, i % 2 == 0 ? value1 : value2);
         mongoDbStorage.createDocument(COLLECTION_DROP_MANY, insertedDocument);
      }

      List<DataDocument> docs = mongoDbStorage.search(COLLECTION_DROP_MANY, null, null, 0, 0);
      assertThat(docs).hasSize(100);

      mongoDbStorage.dropManyDocuments(COLLECTION_DROP_MANY, mongoDbStorageDialect.fieldValueFilter(dropManyKey, value1));

      docs = mongoDbStorage.search(COLLECTION_DROP_MANY, null, null, 0, 0);
      assertThat(docs).hasSize(50);
   }

   @Test
   public void testDropAttribute() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_DROP_ATTRIBUTE);

      DataDocument insertedDocument = createDummyDocument();
      String documentId = mongoDbStorage.createDocument(COLLECTION_DROP_ATTRIBUTE, insertedDocument);
      DataDocument readedDocument = mongoDbStorage.readDocument(COLLECTION_DROP_ATTRIBUTE, mongoDbStorageDialect.documentIdFilter(documentId));
      assertThat(readedDocument).hasSize(3);

      final DataFilter documentIdFilter = mongoDbStorageDialect.documentIdFilter(documentId);

      mongoDbStorage.dropAttribute(COLLECTION_DROP_ATTRIBUTE, documentIdFilter, DUMMY_KEY1);

      readedDocument = mongoDbStorage.readDocument(COLLECTION_DROP_ATTRIBUTE, documentIdFilter);
      assertThat(readedDocument).hasSize(2);
   }

   @Test
   public void testIndexes() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_INDEXES);

      List<DataDocument> dataDocuments = mongoDbStorage.listIndexes(COLLECTION_INDEXES);
      assertThat(dataDocuments).hasSize(1); // default _id index

      mongoDbStorage.createIndex(COLLECTION_INDEXES, new DataDocument("ixs1", 1), false);
      mongoDbStorage.createIndex(COLLECTION_INDEXES, new DataDocument("ixs1", 2), true);

      dataDocuments = mongoDbStorage.listIndexes(COLLECTION_INDEXES);
      assertThat(dataDocuments).hasSize(3);
   }

   @Test
   public void testSearchAttrs() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_SEARCH_ATTRS);
      for (int i = 0; i < 200; i++) {
         DataDocument insertedDocument = new DataDocument("a", i)
               .append("b", i * 2)
               .append("c", i * 3)
               .append("d", i + 4)
               .append("e", i + 5)
               .append("f", new DataDocument("a", i)
                     .append("b", i - 2)
                     .append("c", i * i));
         mongoDbStorage.createDocument(COLLECTION_SEARCH_ATTRS, insertedDocument);
      }
      //test whether we get all documents
      List<DataDocument> docs = mongoDbStorage.search(COLLECTION_SEARCH_ATTRS, null, null);
      assertThat(docs).hasSize(200);

      final DataFilter filter = new MongoDbDataFilter(Filters.gte("a", 100));
      docs = mongoDbStorage.search(COLLECTION_SEARCH_ATTRS, filter, Arrays.asList("a", "c", "f.b"));
      assertThat(docs).hasSize(100);
      DataDocument anyDoc = docs.get(0);
      assertThat(anyDoc).containsKeys("_id", "a", "c", "f");
      assertThat(anyDoc).doesNotContainKeys("b", "d", "e");
      DataDocument nestedDoc = anyDoc.getDataDocument("f");
      assertThat(nestedDoc).containsKey("b");
      assertThat(nestedDoc).doesNotContainKeys("a", "c");

      // test if it's necceserry to include attribute which is used in filter
      docs = mongoDbStorage.search(COLLECTION_SEARCH_ATTRS, filter, Arrays.asList("b", "c", "d"));
      assertThat(docs).hasSize(100);
      anyDoc = docs.get(0);
      assertThat(anyDoc).containsKeys("_id", "b", "c", "d");
      assertThat(anyDoc).doesNotContainKeys("a", "e", "f");
   }

   @Test
   public void testSearch() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_SEARCH);

      for (int i = 0; i < 100; i++) {
         DataDocument insertedDocument = createDummyDocument();
         mongoDbStorage.createDocument(COLLECTION_SEARCH, insertedDocument);
      }

      List<DataDocument> searchDocuments = mongoDbStorage.search(COLLECTION_SEARCH, null, null, 10, 10);
      assertThat(searchDocuments).hasSize(10);
   }

   @Test
   public void testSearchWithProjection() {
      mongoDbStorage.createCollection(COLLECTION_SEARCH_PROJECTION);

      for (int i = 0; i < 10; i++) {
         DataDocument insertedDocument = createDummyDocument();
         mongoDbStorage.createDocument(COLLECTION_SEARCH_PROJECTION, insertedDocument);
      }

      List<DataDocument> searchDocuments = mongoDbStorage.search(COLLECTION_SEARCH_PROJECTION, null, null, Collections.singletonList(DUMMY_KEY1), 0, 5);
      assertThat(searchDocuments).hasSize(5);
      assertThat(searchDocuments).extracting(d -> d.getString(DUMMY_KEY2)).containsOnly((String) null);
   }

   @Test
   public void testRun() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_RUN);

      for (int i = 0; i < 200; i++) {
         DataDocument insertedDocument = createDummyDocument();
         mongoDbStorage.createDocument(COLLECTION_RUN, insertedDocument);
      }

      // query form: https://docs.mongodb.com/v3.2/reference/command/find/#definition
      String query = "{find: \"" + COLLECTION_RUN + "\"}";
      List<DataDocument> searchDocuments = mongoDbStorage.run(query);

      // run() method returns 101 entries due to it is a default value of "batchSize" query key
      assertThat(searchDocuments).hasSize(101);
   }

   @Test
   public void testRenameAttribute() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_RENAME_ATTRIBUTE);

      DataDocument insertedDocument = createDummyDocument();
      String id = mongoDbStorage.createDocument(COLLECTION_RENAME_ATTRIBUTE, insertedDocument);

      String changedAttr = "changed_" + DUMMY_KEY1;
      mongoDbStorage.renameAttribute(COLLECTION_RENAME_ATTRIBUTE, DUMMY_KEY1, changedAttr);

      DataDocument document = mongoDbStorage.readDocument(COLLECTION_RENAME_ATTRIBUTE, mongoDbStorageDialect.documentIdFilter(id));
      assertThat(document.containsKey(DUMMY_KEY1)).isFalse();
      assertThat(document.containsKey(changedAttr)).isTrue();
   }

   @Test
   public void testIncrementAttributeValueBy() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_INC_ATTR_VALUE_BY);

      String incAttribute = "incAttribute";

      DataDocument insertedDocument = createDummyDocument();
      String id = mongoDbStorage.createDocument(COLLECTION_INC_ATTR_VALUE_BY, insertedDocument);
      final DataFilter documentIdFilter = mongoDbStorageDialect.documentIdFilter(id);

      mongoDbStorage.incrementAttributeValueBy(COLLECTION_INC_ATTR_VALUE_BY, documentIdFilter, incAttribute, 1);

      DataDocument readDocument = mongoDbStorage.readDocument(COLLECTION_INC_ATTR_VALUE_BY, documentIdFilter);

      assertThat(readDocument.containsKey(incAttribute)).isTrue();
      assertThat(readDocument.getInteger(incAttribute)).isEqualTo(1);

      mongoDbStorage.incrementAttributeValueBy(COLLECTION_INC_ATTR_VALUE_BY, documentIdFilter, incAttribute, 10);

      readDocument = mongoDbStorage.readDocument(COLLECTION_INC_ATTR_VALUE_BY, documentIdFilter);
      assertThat(readDocument.containsKey(incAttribute)).isTrue();
      assertThat(readDocument.getInteger(incAttribute)).isEqualTo(11);
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
      assertThat(attributeValues1).hasSize(Math.min(numDummyKey1, 100));

      Set<String> attributeValues2 = mongoDbStorage.getAttributeValues(COLLECTION_GET_ATTRIBUTE_VALUES, DUMMY_KEY2);
      assertThat(attributeValues2).hasSize(Math.min(numDummyKey2, 100));

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
      assertThat(a1).containsExactlyInAnyOrder(v1, v3);
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
      final DataFilter documentIdFilter = mongoDbStorageDialect.documentIdFilter(id);

      mongoDbStorage.dropAttribute(COLLECTION_NESTED_DOCUMENTS, documentIdFilter, "d_d.tn.tnn." + DUMMY_KEY1);

      // use debug to see nested hierarchy works
      DataDocument nested = mongoDbStorage.readDocument(COLLECTION_NESTED_DOCUMENTS, documentIdFilter);
      assertThat(nested).isNotNull();
   }

   @Test
   public void testBasicArrayManipulation() throws Exception {
      mongoDbStorage.createCollection(COLLECTION_BASIC_ARRAY_MANIPULATION);

      DataDocument doc = createDummyDocument();
      doc.put("a", Arrays.asList(1, 2, 3, 4));

      String id = mongoDbStorage.createDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, doc);
      final DataFilter documentIdFilter = mongoDbStorageDialect.documentIdFilter(id);
      DataDocument fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("a", Integer.class)).hasSize(4);

      mongoDbStorage.addItemToArray(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter, "a", 10);
      fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("a", Integer.class)).hasSize(5);

      mongoDbStorage.addItemsToArray(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter, "a", Arrays.asList(5, 6, 7));
      fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("a", Integer.class)).hasSize(8);

      mongoDbStorage.removeItemFromArray(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter, "a", 10);
      fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("a", Integer.class)).hasSize(7);

      mongoDbStorage.removeItemsFromArray(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter, "a", Arrays.asList(5, 6, 7));
      fromDb = mongoDbStorage.readDocument(COLLECTION_BASIC_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("a", Integer.class)).hasSize(4);
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
      final DataFilter documentIdFilter = mongoDbStorageDialect.documentIdFilter(id);
      DataDocument fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("n.a", DataDocument.class)).hasSize(2);

      DataDocument d3 = new DataDocument();
      d3.put("equals", "true");
      mongoDbStorage.addItemToArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter, "n.a", d3);
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("n.a", DataDocument.class)).hasSize(3);

      DataDocument d4 = new DataDocument();
      d4.put("i", true);
      DataDocument d5 = new DataDocument();
      d4.put("p", 12.3);
      mongoDbStorage.addItemsToArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter, "n.a", Arrays.asList(d4, d5));
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("n.a", DataDocument.class)).hasSize(5);

      mongoDbStorage.removeItemsFromArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter, "n.a", Arrays.asList(d2, d3));
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("n.a", DataDocument.class)).hasSize(3);

      mongoDbStorage.removeItemFromArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter, "n.a", d2);
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("n.a", DataDocument.class)).hasSize(3);

      mongoDbStorage.removeItemFromArray(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter, "n.a", d4);
      fromDb = mongoDbStorage.readDocument(COLLECTION_COMPLEX_ARRAY_MANIPULATION, documentIdFilter);
      assertThat(fromDb.getArrayList("n.a", DataDocument.class)).hasSize(2);

   }

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
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getDataDocument("_id")).containsOnlyKeys("param1");
      assertThat(result.get(0).getString("_id.param1")).isEqualTo("a");
      assertThat(result.get(0).get("added")).isEqualTo(7);

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

      assertThat(result).hasSize(2);
      assertThat(result.get(0).get("param4")).isEqualTo(20);
      assertThat(result.get(1).get("param4")).isEqualTo(15);

      q.setSkip(1);
      result = mongoDbStorage.query(q);
      assertThat(result).hasSize(1);
      assertThat(result.get(0).get("param4")).isEqualTo(15);

      q.setSkip(0);
      q.setLimit(1);
      result = mongoDbStorage.query(q);
      assertThat(result).hasSize(1);
      assertThat(result.get(0).get("param4")).isEqualTo(20);
   }

   @Test
   public void dataStatsTest() {
      mongoDbStorage.createCollection(COLLECTION_STATS);
      mongoDbStorage.createDocument(COLLECTION_STATS, new DataDocument("stats", 4));
      mongoDbStorage.createIndex(COLLECTION_STATS, new DataDocument("stats", 1), false);

      final DataStorageStats dss = mongoDbStorage.getDbStats();

      assertThat(dss.getDatabaseName()).isEqualTo(EmbeddedMongoDb.NAME);
      assertThat(dss.getCollections()).isGreaterThan(0);
      assertThat(dss.getDocuments()).isGreaterThan(0);
      assertThat(dss.getIndexes()).isGreaterThan(0);
      assertThat(dss.getDataSize()).isGreaterThan(0);
      assertThat(dss.getStorageSize()).isGreaterThan(0);
      assertThat(dss.getIndexSize()).isGreaterThan(0);

      mongoDbStorage.dropCollection(COLLECTION_STATS);
   }

   @Test
   public void collectionStatsTest() {
      mongoDbStorage.createCollection(COLLECTION_CSTATS);
      mongoDbStorage.createDocument(COLLECTION_CSTATS, new DataDocument("stats", 4));
      mongoDbStorage.createIndex(COLLECTION_CSTATS, new DataDocument("stats", 1), false);

      final DataStorageStats dss = mongoDbStorage.getCollectionStats(COLLECTION_CSTATS);

      assertThat(dss.getDatabaseName()).isEqualTo(EmbeddedMongoDb.NAME);
      assertThat(dss.getCollectionName()).isEqualTo(COLLECTION_CSTATS);
      assertThat(dss.getDocuments()).isEqualTo(1);
      assertThat(dss.getIndexes()).isEqualTo(2);
      assertThat(dss.getDataSize()).isGreaterThan(0);
      assertThat(dss.getStorageSize()).isGreaterThan(0);
      assertThat(dss.getIndexSize()).isGreaterThan(0);

      mongoDbStorage.dropCollection(COLLECTION_CSTATS);
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