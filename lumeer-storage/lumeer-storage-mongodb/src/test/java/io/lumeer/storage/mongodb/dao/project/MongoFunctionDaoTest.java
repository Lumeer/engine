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
package io.lumeer.storage.mongodb.dao.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Project;
import io.lumeer.api.model.function.FunctionRow;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.codecs.FunctionRowCodec;

import com.mongodb.client.model.Filters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MongoFunctionDaoTest extends MongoDbTestBase {

   private static final String PROJECT_ID = "596e3b86d412bc5a3caaa22a";

   private static final String COLLECTION1 = "C1";
   private static final String COLLECTION2 = "C2";
   private static final String COLLECTION3 = "C3";
   private static final String ATTRIBUTE1 = "a1";
   private static final String ATTRIBUTE2 = "a2";
   private static final String ATTRIBUTE3 = "a3";
   private static final String LINK_TYPE1 = "L1";
   private static final String LINK_TYPE2 = "L2";

   private MongoFunctionDao functionDao;

   @Before
   public void initCollectionDao() {
      Project project = Mockito.mock(Project.class);
      Mockito.when(project.getId()).thenReturn(PROJECT_ID);

      functionDao = new MongoFunctionDao();
      functionDao.setDatabase(database);

      functionDao.setProject(project);
      functionDao.createRepository(project);
   }

   @Test
   public void testAddRows() {
      FunctionRow row1 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION2, ATTRIBUTE2);
      FunctionRow row2 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      FunctionRow row3 = FunctionRow.createForLink(COLLECTION1, ATTRIBUTE1, LINK_TYPE1, ATTRIBUTE2);
      List<FunctionRow> rows = Arrays.asList(row1, row2, row3);
      functionDao.createRows(rows);

      List<FunctionRow> storedRows = functionDao.databaseCollection().find(Filters.eq(FunctionRowCodec.COLLECTION_ID, COLLECTION1)).into(new ArrayList<>());
      assertThat(storedRows).hasSize(3);
      assertThat(storedRows).extracting(FunctionRow::getDependentCollectionId).contains(COLLECTION2, COLLECTION3);
      assertThat(storedRows).extracting(FunctionRow::getDependentAttributeId).contains(ATTRIBUTE2, ATTRIBUTE3);
      assertThat(storedRows).extracting(FunctionRow::getDependentLinkTypeId).contains(LINK_TYPE1);
   }

   @Test
   public void testSearchByCollection() {
      FunctionRow row1 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION2, ATTRIBUTE2);
      FunctionRow row2 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      FunctionRow row3 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION2, ATTRIBUTE2);
      FunctionRow row4 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION2, ATTRIBUTE3);
      FunctionRow row5 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION1, ATTRIBUTE2);
      FunctionRow row6 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      FunctionRow row7 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE2, COLLECTION1, ATTRIBUTE3);
      List<FunctionRow> rows = Arrays.asList(row1, row2, row3, row4, row5, row6, row7);
      functionDao.createRows(rows);

      List<FunctionRow> storedRows = functionDao.searchByCollection(COLLECTION1, ATTRIBUTE1);
      assertThat(storedRows).hasSize(2).extracting(FunctionRow::getDependentCollectionId).containsOnly(COLLECTION2, COLLECTION3);

      storedRows = functionDao.searchByCollection(COLLECTION2, ATTRIBUTE1);
      assertThat(storedRows).hasSize(2).extracting(FunctionRow::getDependentCollectionId).containsOnly(COLLECTION2, COLLECTION2);

      storedRows = functionDao.searchByCollection(COLLECTION3, ATTRIBUTE1);
      assertThat(storedRows).hasSize(2).extracting(FunctionRow::getDependentCollectionId).containsOnly(COLLECTION1, COLLECTION3);

      storedRows = functionDao.searchByCollection(COLLECTION1, null);
      assertThat(storedRows).hasSize(3).extracting(FunctionRow::getDependentCollectionId).containsOnly(COLLECTION1, COLLECTION2, COLLECTION3);

      storedRows = functionDao.searchByCollection(COLLECTION3, ATTRIBUTE2);
      assertThat(storedRows).isEmpty();
   }

   @Test
   public void testSearchByAnyCollection() {
      FunctionRow row1 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION2, ATTRIBUTE2);
      FunctionRow row2 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      FunctionRow row3 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION1, ATTRIBUTE2);
      FunctionRow row4 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION2, ATTRIBUTE3);
      FunctionRow row5 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION1, ATTRIBUTE1);
      FunctionRow row6 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      List<FunctionRow> rows = Arrays.asList(row1, row2, row3, row4, row5, row6);
      functionDao.createRows(rows);

      List<FunctionRow> storedRows = functionDao.searchByAnyCollection(COLLECTION1, ATTRIBUTE1);
      assertThat(storedRows).hasSize(3).extracting(FunctionRow::getCollectionId).containsOnly(COLLECTION1, COLLECTION1, COLLECTION3);

      storedRows = functionDao.searchByAnyCollection(COLLECTION1, ATTRIBUTE2);
      assertThat(storedRows).hasSize(1).extracting(FunctionRow::getCollectionId).containsOnly(COLLECTION2);

      storedRows = functionDao.searchByAnyCollection(COLLECTION1, null);
      assertThat(storedRows).hasSize(4).extracting(FunctionRow::getCollectionId).contains(COLLECTION1, COLLECTION2, COLLECTION3);

      storedRows = functionDao.searchByAnyCollection(COLLECTION1, ATTRIBUTE3);
      assertThat(storedRows).isEmpty();
   }

   @Test
   public void testSearchByDependentCollection() {
      FunctionRow row1 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION2, ATTRIBUTE2);
      FunctionRow row2 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      FunctionRow row3 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION1, ATTRIBUTE2);
      FunctionRow row4 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION2, ATTRIBUTE3);
      FunctionRow row5 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION1, ATTRIBUTE1);
      FunctionRow row6 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      List<FunctionRow> rows = Arrays.asList(row1, row2, row3, row4, row5, row6);
      functionDao.createRows(rows);

      List<FunctionRow> storedRows = functionDao.searchByDependentCollection(COLLECTION3, ATTRIBUTE3);
      assertThat(storedRows).hasSize(2).extracting(FunctionRow::getCollectionId).containsOnly(COLLECTION1, COLLECTION3);

      storedRows = functionDao.searchByDependentCollection(COLLECTION1, ATTRIBUTE1);
      assertThat(storedRows).hasSize(1).extracting(FunctionRow::getCollectionId).containsOnly(COLLECTION3);

      storedRows = functionDao.searchByDependentCollection(COLLECTION2, null);
      assertThat(storedRows).hasSize(2).extracting(FunctionRow::getCollectionId).containsOnly(COLLECTION1, COLLECTION2);

      storedRows = functionDao.searchByDependentCollection(COLLECTION2, ATTRIBUTE1);
      assertThat(storedRows).isEmpty();
   }

   @Test
   public void testSearchByLinkType() {
      FunctionRow row1 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION2, ATTRIBUTE2);
      FunctionRow row2 = FunctionRow.createForLink(COLLECTION1, ATTRIBUTE1, LINK_TYPE1, ATTRIBUTE1);
      FunctionRow row3 = FunctionRow.createForLink(COLLECTION1, ATTRIBUTE1, LINK_TYPE1, ATTRIBUTE2);
      FunctionRow row4 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION2, ATTRIBUTE3);
      FunctionRow row5 = FunctionRow.createForLink(COLLECTION2, ATTRIBUTE1, LINK_TYPE2, ATTRIBUTE2);
      FunctionRow row6 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION1, ATTRIBUTE1);
      FunctionRow row7 = FunctionRow.createForLink(COLLECTION3, ATTRIBUTE1, LINK_TYPE1, ATTRIBUTE2);
      List<FunctionRow> rows = Arrays.asList(row1, row2, row3, row4, row5, row6, row7);
      functionDao.createRows(rows);

      List<FunctionRow> storedRows = functionDao.searchByDependentLinkType(LINK_TYPE1, ATTRIBUTE2);
      assertThat(storedRows).hasSize(2).extracting(FunctionRow::getCollectionId).containsOnly(COLLECTION1, COLLECTION3);

      storedRows = functionDao.searchByDependentLinkType(LINK_TYPE2, ATTRIBUTE2);
      assertThat(storedRows).hasSize(1).extracting(FunctionRow::getCollectionId).containsOnly(COLLECTION2);

      storedRows = functionDao.searchByDependentLinkType(LINK_TYPE1, null);
      assertThat(storedRows).hasSize(3);

      storedRows = functionDao.searchByDependentLinkType(LINK_TYPE2, ATTRIBUTE3);
      assertThat(storedRows).isEmpty();
   }

   @Test
   public void testDeleteByCollections() {
      FunctionRow row1 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION2, ATTRIBUTE2);
      FunctionRow row2 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      FunctionRow row3 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION1, ATTRIBUTE2);
      FunctionRow row4 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION2, ATTRIBUTE3);
      FunctionRow row5 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION1, ATTRIBUTE1);
      FunctionRow row6 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION3, ATTRIBUTE3);
      List<FunctionRow> rows = Arrays.asList(row1, row2, row3, row4, row5, row6);
      functionDao.createRows(rows);

      List<FunctionRow> storedRows = functionDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(storedRows).hasSize(6).extracting(FunctionRow::getCollectionId).contains(COLLECTION1, COLLECTION2, COLLECTION3);

      functionDao.deleteByCollections(COLLECTION1, COLLECTION3);
      storedRows = functionDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(storedRows).hasSize(2).extracting(FunctionRow::getCollectionId).contains(COLLECTION2);

      functionDao.deleteByCollections(COLLECTION2);
      storedRows = functionDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(storedRows).isEmpty();
   }

   @Test
   public void testDeleteByCollection() {
      FunctionRow row1 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE1, COLLECTION2, ATTRIBUTE2);
      FunctionRow row2 = FunctionRow.createForCollection(COLLECTION1, ATTRIBUTE2, COLLECTION3, ATTRIBUTE3);
      FunctionRow row3 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE1, COLLECTION1, ATTRIBUTE2);
      FunctionRow row4 = FunctionRow.createForCollection(COLLECTION2, ATTRIBUTE3, COLLECTION2, ATTRIBUTE3);
      FunctionRow row5 = FunctionRow.createForCollection(COLLECTION3, ATTRIBUTE1, COLLECTION1, ATTRIBUTE1);
      List<FunctionRow> rows = Arrays.asList(row1, row2, row3, row4, row5);
      functionDao.createRows(rows);

      List<FunctionRow> storedRows = functionDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(storedRows).hasSize(5).extracting(FunctionRow::getCollectionId).contains(COLLECTION1, COLLECTION2, COLLECTION3);

      functionDao.deleteByCollection(COLLECTION1, ATTRIBUTE2);
      storedRows = functionDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(storedRows).hasSize(4).extracting(FunctionRow::getCollectionId).contains(COLLECTION1, COLLECTION2, COLLECTION3);

      functionDao.deleteByCollection(COLLECTION1, ATTRIBUTE1);
      storedRows = functionDao.databaseCollection().find().into(new ArrayList<>());
      assertThat(storedRows).hasSize(3).extracting(FunctionRow::getCollectionId).contains(COLLECTION2, COLLECTION3);
   }

}
