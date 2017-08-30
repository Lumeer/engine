/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.storage.mongodb.dao.collection;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Collection;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

public class MongoDataDaoTest extends MongoDbTestBase {

   private static final String COLLECTION_ID = "59a51b83d412bc2da88b010f";
   private static final String DOCUMENT_ID = "59a58ba7d412bc562eea2e6a";

   private static final String USER = "notNeeded";

   private static final String KEY1 = "A";
   private static final String KEY2 = "B";
   private static final String VALUE1 = "firstValue";
   private static final String VALUE2 = "secondValue";

   private MongoDataDao dataDao;

   @Before
   public void initDataDao() {
      Collection collection = Mockito.mock(Collection.class);
      Mockito.when(collection.getId()).thenReturn(COLLECTION_ID);

      dataDao = new MongoDataDao();
      dataDao.setDatabase(database);
      dataDao.setDatastore(datastore);

      dataDao.createDataRepository(COLLECTION_ID);
   }

   private String createDocument() {
      Document document = new Document();
      document.append(KEY1, VALUE1);
      document.append(KEY2, VALUE2);
      dataCollection().insertOne(document);
      return document.getObjectId("_id").toHexString();
   }

   private String createDocument(String key, String value) {
      Document document = new Document(key, value);
      dataCollection().insertOne(document);
      return document.getObjectId("_id").toHexString();
   }

   @Test
   public void testCreateData() {
      DataDocument data = new DataDocument().append(KEY1, VALUE1)
                                            .append(KEY2, VALUE2);
      DataDocument storedData = dataDao.createData(COLLECTION_ID, DOCUMENT_ID, data);
      assertThat(storedData).isNotNull();
      assertThat(storedData.getId()).isNull();

      MongoCursor<Document> mongoCursor = dataCollection().find().iterator();
      assertThat(mongoCursor.hasNext()).isTrue();

      Document document = mongoCursor.next();
      assertThat(document.get("_id").toString()).isNotNull().isEqualTo(DOCUMENT_ID);
      assertThat(document).containsEntry(KEY1, VALUE1);
      assertThat(document).containsEntry(KEY2, VALUE2);
   }

   @Test
   public void testCreateDataExistingDocument() {

   }

   @Test
   public void testUpdateData() {
      String id = createDocument();

      DataDocument data = new DataDocument().append(KEY1, VALUE2);
      dataDao.updateData(COLLECTION_ID, id, data);

      MongoCursor<Document> mongoCursor = dataCollection().find().iterator();
      assertThat(mongoCursor.hasNext()).isTrue();

      Document document = mongoCursor.next();
      assertThat(document).containsEntry(KEY1, VALUE2);
      assertThat(document).doesNotContainKey(KEY2);
   }

   @Test
   public void testUpdateDataNotExistingDocument() {

   }

   @Test
   public void testPatchData() {
      String id = createDocument();

      DataDocument data = new DataDocument().append(KEY1, VALUE2);
      dataDao.patchData(COLLECTION_ID, id, data);

      MongoCursor<Document> mongoCursor = dataCollection().find().iterator();
      assertThat(mongoCursor.hasNext()).isTrue();

      Document document = mongoCursor.next();
      assertThat(document).containsEntry(KEY1, VALUE2);
      assertThat(document).containsEntry(KEY2, VALUE2);
   }

   @Test
   public void testPatchNestedAttributes() {

   }

   @Test
   public void testPatchDataNotExistingDocument() {

   }

   @Test
   public void testDeleteData() {
      String id = createDocument();

      dataDao.deleteData(COLLECTION_ID, id);

      MongoCursor<Document> mongoCursor = dataCollection().find().iterator();
      assertThat(mongoCursor).isEmpty();
   }

   @Test
   public void testDeleteDataNotExistingDocument() {

   }

   @Test
   public void testGetSingleDataRecord() {
      String id = createDocument();

      DataDocument dataDocument = dataDao.getData(COLLECTION_ID, id);
      assertThat(dataDocument).containsEntry(KEY1, VALUE1);
      assertThat(dataDocument).containsEntry(KEY2, VALUE2);
   }

   @Test
   public void testGetSingleDataRecordNotExistingDocument() {

   }

   @Test
   public void testGetDataWithPagination() {

   }

   @Test
   public void testGetDataByFulltextAttributeValue() {
      String id1 = createDocument(KEY1, VALUE1);
      String id2 = createDocument(KEY1, "fulltext");
      String id3 = createDocument(KEY1, "something fulltext");
      String id4 = createDocument(KEY1, VALUE1);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER)
                                           .fulltext("fulltext")
                                           .build();
      List<DataDocument> data = dataDao.getData(COLLECTION_ID, searchQuery);
      assertThat(data).extracting(DataDocument::getId).containsOnly(id2, id3);
   }

   @Test
   @Ignore("Does not work at the moment")
   public void testGetDataByFulltextAttributeName() {
      String id1 = createDocument(KEY1, VALUE1);
      String id2 = createDocument("fulltext", VALUE1);
      String id3 = createDocument(KEY1, VALUE1);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER)
                                           .fulltext("fulltext")
                                           .build();
      List<DataDocument> data = dataDao.getData(COLLECTION_ID, searchQuery);
      assertThat(data).extracting(DataDocument::getId).containsOnly(id2);
   }

   @Test
   public void testGetDataByCollectionCodes() {

   }

   @Test
   public void testGetDataByAllParameters() {

   }

   @Test
   public void testGetDataCount() {
      createDocument();
      createDocument();

      SearchQuery searchQuery = SearchQuery.createBuilder(USER).build();
      long count = dataDao.getDataCount(COLLECTION_ID, searchQuery);
      assertThat(count).isEqualTo(2);
   }

   private MongoCollection<Document> dataCollection() {
      return dataDao.dataCollection(COLLECTION_ID);
   }

}
