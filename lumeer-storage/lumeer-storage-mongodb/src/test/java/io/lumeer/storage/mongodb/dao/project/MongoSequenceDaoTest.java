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

import io.lumeer.api.model.Project;
import io.lumeer.storage.mongodb.MongoDbTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MongoSequenceDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private MongoSequenceDao sequenceDao;

   @BeforeEach
   public void initSequenceDao() {
      Project project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      sequenceDao = new MongoSequenceDao();
      sequenceDao.setDatabase(database);
      sequenceDao.setProject(project);
      sequenceDao.createRepository(project);
   }

   @Test
   public void testGetNextSequenceNo() {
      var indexName = "sequence";
      for (int i = 0; i < 10; i++) {
         assertThat(sequenceDao.getNextSequenceNo(indexName)).isEqualTo(i);
      }
   }

}
