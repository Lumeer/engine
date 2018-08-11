/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.storage.mongodb.dao.collection;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.model.MorphiaCollection;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
   private CollectionDao collectionDao;

   @Before
   public void initDataDao() {
      Collection col = new MorphiaCollection();
      col.setId(COLLECTION_ID);
      col.setAttributes(Collections.EMPTY_SET);
      col.setLastAttributeNum(0);

      collectionDao = Mockito.mock(CollectionDao.class);
      Mockito.when(collectionDao.getCollectionById(COLLECTION_ID)).thenReturn(col);

      dataDao = new MongoDataDao();
      dataDao.setDatabase(database);
      dataDao.setDatastore(datastore);
      dataDao.setCollectionDao(collectionDao);

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
      Collection collection = collectionDao.getCollectionById(COLLECTION_ID);
      final String id = key; // use the same document id for simplicity in tests
      if (!collection.getAttributes().stream().anyMatch(attr -> attr.getName().equals(key))) {
         collection.createAttribute(new JsonAttribute(id, key, Collections.emptySet(), 1));
         collection.setLastAttributeNum(collection.getLastAttributeNum() + 1);
         collectionDao.updateCollection(COLLECTION_ID, collection);
      } else {
         Attribute attr = collection.getAttributes().stream().filter(a -> a.getName().equals(key)).findFirst().get();
         attr.setUsageCount(attr.getUsageCount() + 1);
         collectionDao.updateCollection(COLLECTION_ID, collection);
      }

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
   public void testGetDataByDocumenstIds() {
      String id1 = createDocument(KEY1, VALUE1);
      String id2 = createDocument(KEY1, VALUE2);
      String id3 = createDocument(KEY1, VALUE1);
      String id4 = createDocument(KEY1, VALUE2);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER)
                                           .documentIds(Collections.singleton(id2))
                                           .build();
      List<DataDocument> data = dataDao.getData(COLLECTION_ID, searchQuery);
      assertThat(data).extracting(DataDocument::getId).containsOnly(id2);

      searchQuery = SearchQuery.createBuilder(USER)
                               .documentIds(new HashSet<>(Arrays.asList(id1, id3, id4)))
                               .build();
      data = dataDao.getData(COLLECTION_ID, searchQuery);
      assertThat(data).extracting(DataDocument::getId).containsOnly(id1, id3, id4);
   }

   @Test
   public void testGetDataByFulltextAttributeValueAndDocumentsIds() {
      String id1 = createDocument(KEY1, VALUE1);
      String id2 = createDocument(KEY1, "fulltext");
      String id3 = createDocument(KEY1, "something fulltext");
      String id4 = createDocument(KEY1, VALUE1);

      SearchQuery searchQuery = SearchQuery.createBuilder(USER)
                                           .fulltext("fulltext")
                                           .documentIds(new HashSet<>(Arrays.asList(id1, id2)))
                                           .build();
      List<DataDocument> data = dataDao.getData(COLLECTION_ID, searchQuery);
      assertThat(data).extracting(DataDocument::getId).containsOnly(id2);
   }

   @Test
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
