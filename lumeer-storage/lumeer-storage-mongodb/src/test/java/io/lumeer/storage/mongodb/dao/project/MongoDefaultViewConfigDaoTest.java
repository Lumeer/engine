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
package io.lumeer.storage.mongodb.dao.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.DefaultViewConfig;
import io.lumeer.api.model.Project;
import io.lumeer.storage.mongodb.MongoDbTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;

public class MongoDefaultViewConfigDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String USER_ID1 = "someUser1";
   private static final String USER_ID2 = "someUser2";

   private static final String COLLECTION_ID1 = "someCollection1";
   private static final String COLLECTION_ID2 = "someCollection2";

   private MongoDefaultViewConfigDao defaultViewConfigDao;

   @BeforeEach
   public void initSequenceDao() {
      Project project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      defaultViewConfigDao = new MongoDefaultViewConfigDao();
      defaultViewConfigDao.setDatabase(database);
      defaultViewConfigDao.setProject(project);
      defaultViewConfigDao.createRepository(project);
   }

   @Test
   public void testUpdateConfig() {
      assertThat(defaultViewConfigDao.databaseCollection().countDocuments()).isEqualTo(0);

      var config = defaultViewConfigDao.updateConfig(createConfig(USER_ID1, COLLECTION_ID1));
      assertThat(config).isNotNull();
      assertThat(defaultViewConfigDao.databaseCollection().countDocuments()).isEqualTo(1);

      defaultViewConfigDao.updateConfig(createConfig(USER_ID1, COLLECTION_ID1));
      assertThat(defaultViewConfigDao.databaseCollection().countDocuments()).isEqualTo(1);
   }

   @Test
   public void testDeleteByCollection() {
      defaultViewConfigDao.updateConfig(createConfig(USER_ID1, COLLECTION_ID1));
      defaultViewConfigDao.updateConfig(createConfig(USER_ID1, COLLECTION_ID2));
      defaultViewConfigDao.updateConfig(createConfig(USER_ID2, COLLECTION_ID1));
      defaultViewConfigDao.updateConfig(createConfig(USER_ID2, COLLECTION_ID2));

      assertThat(defaultViewConfigDao.getConfigs(USER_ID1)).hasSize(2);
      assertThat(defaultViewConfigDao.getConfigs(USER_ID2)).hasSize(2);

      defaultViewConfigDao.deleteByCollection(COLLECTION_ID1);

      assertThat(defaultViewConfigDao.getConfigs(USER_ID1)).hasSize(1);
      assertThat(defaultViewConfigDao.getConfigs(USER_ID2)).hasSize(1);

      defaultViewConfigDao.deleteByCollection(COLLECTION_ID2);

      assertThat(defaultViewConfigDao.getConfigs(USER_ID1)).isEmpty();
      assertThat(defaultViewConfigDao.getConfigs(USER_ID2)).isEmpty();
   }

   private DefaultViewConfig createConfig(String userId, String collectionId) {
      var config = new DefaultViewConfig(collectionId, "perspective", new org.bson.Document(), ZonedDateTime.now());
      config.setUserId(userId);
      return config;
   }

}
