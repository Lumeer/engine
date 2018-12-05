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

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Feedback;
import io.lumeer.storage.api.dao.FeedbackDao;
import io.lumeer.storage.api.exception.StorageException;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;

import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoFeedbackDao extends SystemScopedDao implements FeedbackDao {

   private static final String COLLECTION_NAME = "feedback";

   @PostConstruct
   public void checkRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(COLLECTION_NAME)) {
         createFeedbackRepository();
      }
   }

   public void createFeedbackRepository() {
      database.createCollection(databaseCollectionName());
   }

   public void deleteFeedbackRepository() {
      database.getCollection(databaseCollectionName()).drop();
   }

   @Override
   public Feedback createFeedback(final Feedback feedback) {
      try {
         databaseCollection().insertOne(feedback);
         return feedback;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create feedback " + feedback, ex);
      }
   }

   @Override
   public Feedback getFeedbackById(final String id) {
      return databaseCollection().find(idFilter(id)).first();
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<Feedback> databaseCollection() {
      return database.getCollection(databaseCollectionName(), Feedback.class);
   }

}
