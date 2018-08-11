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

package io.lumeer.storage.mongodb.dao.system;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Feedback;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.util.MongoFilters;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class MongoFeedbackDaoTest extends MongoDbTestBase {

   private static final String USER_ID = "596e3b86d412bc5a3caaa22a";
   private static final ZonedDateTime CREATION_TIME = ZonedDateTime.now();
   private static final String MESSAGE = "This application is shit!";

   private MongoFeedbackDao mongoFeedbackDao;

   @Before
   public void initUserDao() {
      mongoFeedbackDao = new MongoFeedbackDao();
      mongoFeedbackDao.setDatabase(database);

      mongoFeedbackDao.createFeedbackRepository();
      assertThat(database.listCollectionNames()).contains(mongoFeedbackDao.databaseCollectionName());
   }

   @Test
   public void testCreateFeedback() {
      Feedback feedback = prepareFeedback();

      Feedback returnedFeedback = mongoFeedbackDao.createFeedback(feedback);
      assertThat(returnedFeedback).isNotNull();
      String id = returnedFeedback.getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Feedback storedFeedback = mongoFeedbackDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedFeedback).isNotNull();
      assertThat(storedFeedback.getUserId()).isEqualTo(feedback.getUserId());
      assertThat(storedFeedback.getCreationTime()).isEqualTo(feedback.getCreationTime().truncatedTo(ChronoUnit.MILLIS));
      assertThat(storedFeedback.getMessage()).isEqualTo(feedback.getMessage());
   }

   @Test
   public void testGetFeedbackById() {
      Feedback feedback = prepareFeedback();
      mongoFeedbackDao.databaseCollection().insertOne(feedback);

      Feedback storedFeedback = mongoFeedbackDao.getFeedbackById(feedback.getId());
      assertThat(storedFeedback).isNotNull();
      assertThat(storedFeedback.getId()).isEqualTo(feedback.getId());
      assertThat(storedFeedback.getUserId()).isEqualTo(feedback.getUserId());
      assertThat(storedFeedback.getCreationTime()).isEqualTo(feedback.getCreationTime().truncatedTo(ChronoUnit.MILLIS));
      assertThat(storedFeedback.getMessage()).isEqualTo(feedback.getMessage());
   }

   @Test
   public void testGetFeedbackByIdNotExisting() {
      Feedback storedFeedback = mongoFeedbackDao.getFeedbackById("596e3b86d412bc5a3caaa22a");
      assertThat(storedFeedback).isNull();
   }

   private Feedback prepareFeedback() {
      return new Feedback(USER_ID, CREATION_TIME, MESSAGE);
   }

}
