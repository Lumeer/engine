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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.mongodb.MongoDbTestBase;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class MongoLinkDataDaoTest extends MongoDbTestBase {

   private static final String LINK_TYPE_ID = "59a51b83d412bc2da88b010f";
   private static final String LINK_INSTANCE_ID = "59a58ba7d412bc562eea2e6a";
   private static final String LINK_INSTANCE_ID2 = "59a58ba7d412bc562eea2e6b";
   private static final String LINK_INSTANCE_ID3 = "59a58ba7d412bc562eea2e6c";

   private MongoLinkDataDao dataDao;

   @Before
   public void initDataDao() {
      dataDao = new MongoLinkDataDao();
      dataDao.setDatabase(database);

      dataDao.createDataRepository(LINK_TYPE_ID);
   }

   @Test
   public void testCreateData() {
      DataDocument data = new DataDocument().append("k1", "v1").append("k2", "v2");
      DataDocument storedData = dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID, data);
      assertThat(storedData).isNotNull();
      assertThat(storedData.getId()).isNull();

      MongoCursor<Document> mongoCursor = dataCollection().find().iterator();
      assertThat(mongoCursor.hasNext()).isTrue();

      Document document = mongoCursor.next();
      assertThat(document.get("_id").toString()).isNotNull().isEqualTo(LINK_INSTANCE_ID);
      assertThat(document).containsEntry("k1", "v1");
      assertThat(document).containsEntry("k2", "v2");
   }

   @Test
   public void testGetData() {
      DataDocument data = new DataDocument().append("k1", "v1").append("k2", "v2");
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID, data);

      DataDocument storedData = dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID);
      assertThat(storedData).containsEntry("k1", "v1");
      assertThat(storedData).containsEntry("k2", "v2");
   }

   @Test
   public void testUpdateData() {
      DataDocument data = new DataDocument().append("k1", "v1").append("k2", "v2");
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID, data);

      DataDocument storedData = dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID);
      assertThat(storedData).containsEntry("k1", "v1");
      assertThat(storedData).containsEntry("k2", "v2");

      dataDao.updateData(LINK_TYPE_ID, LINK_INSTANCE_ID, new DataDocument().append("k3", "v3").append("k4", "v4"));
      DataDocument updatedData = dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID);
      assertThat(updatedData).containsEntry("k3", "v3");
      assertThat(updatedData).containsEntry("k4", "v4");
      assertThat(updatedData).doesNotContainEntry("k1", "v1");
      assertThat(updatedData).doesNotContainEntry("k2", "v2");
   }

   @Test
   public void testPatchData() {
      DataDocument data = new DataDocument().append("k1", "v1").append("k2", "v2");
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID, data);

      DataDocument storedData = dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID);
      assertThat(storedData).containsEntry("k1", "v1");
      assertThat(storedData).containsEntry("k2", "v2");

      dataDao.patchData(LINK_TYPE_ID, LINK_INSTANCE_ID, new DataDocument().append("k3", "v3").append("k4", "v4"));
      DataDocument patchedData = dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID);
      assertThat(patchedData).containsEntry("k3", "v3");
      assertThat(patchedData).containsEntry("k4", "v4");
      assertThat(patchedData).containsEntry("k1", "v1");
      assertThat(patchedData).containsEntry("k2", "v2");
   }

   @Test
   public void testDeleteData() {
      DataDocument data = new DataDocument().append("k1", "v1").append("k2", "v2");
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID, data);

      DataDocument storedData = dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID);
      assertThat(storedData).containsEntry("k1", "v1");
      assertThat(storedData).containsEntry("k2", "v2");

      dataDao.deleteData(LINK_TYPE_ID, LINK_INSTANCE_ID);
      DataDocument newData = dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID);
      assertThat(newData).isEmpty();
   }

   @Test
   public void testDeleteAttribute(){
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID, new DataDocument().append("k1", "v1").append("k2", "v2"));
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID2, new DataDocument().append("k1", "v41").append("k3", "v3"));

      assertThat(dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID)).containsKey("k1");
      assertThat(dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID2)).containsKey("k1");

      dataDao.deleteAttribute(LINK_TYPE_ID, "k1");

      assertThat(dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID)).doesNotContainKey("k1");
      assertThat(dataDao.getData(LINK_TYPE_ID, LINK_INSTANCE_ID2)).doesNotContainKey("k1");
   }

   @Test
   public void testDeleteDataMultiple() {
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID, new DataDocument().append("k1", "v1").append("k2", "v2"));
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID2, new DataDocument().append("k1", "v41").append("k3", "v3"));
      dataDao.createData(LINK_TYPE_ID, LINK_INSTANCE_ID3, new DataDocument().append("k1", "v41").append("k3", "v3"));

      assertThat(dataCollection().find().into(new ArrayList<>())).hasSize(3);

      dataDao.deleteData(LINK_TYPE_ID, new HashSet<>(Arrays.asList(LINK_INSTANCE_ID, LINK_INSTANCE_ID3)));

      ArrayList<Document> documents = dataCollection().find().into(new ArrayList<>());
      assertThat(documents).hasSize(1);
      assertThat(documents.get(0).getObjectId("_id").toString()).isEqualTo(LINK_INSTANCE_ID2);
   }

   private MongoCollection<Document> dataCollection() {
      return dataDao.linkDataCollection(LINK_TYPE_ID);
   }
}
