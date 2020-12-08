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
package io.lumeer.storage.mongodb.dao.system;

import io.lumeer.api.model.DelayedAction;
import io.lumeer.api.model.NotificationType;
import io.lumeer.api.model.Organization;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.exception.StorageException;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoDelayedActionDao extends MongoSystemScopedDao implements DelayedActionDao {

   public static final String COLLECTION_NAME = "delayed_actions";

   @PostConstruct
   public void checkRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(databaseCollectionName())) {
         createDelayedActionsRepository();
      }
   }

   public void createDelayedActionsRepository() {
      database.createCollection(databaseCollectionName());

      MongoCollection<Document> actionsCollection = database.getCollection(databaseCollectionName());
      actionsCollection.createIndex(Indexes.ascending(DelayedAction.CHECK_AFTER), new IndexOptions().unique(false));
      actionsCollection.createIndex(Indexes.ascending(DelayedAction.STARTED_PROCESSING), new IndexOptions().unique(false));
      actionsCollection.createIndex(Indexes.ascending(DelayedAction.COMPLETED), new IndexOptions().unique(false));
      actionsCollection.createIndex(Indexes.ascending(DelayedAction.INITIATOR), new IndexOptions().unique(false));
      actionsCollection.createIndex(Indexes.ascending(DelayedAction.RECEIVER), new IndexOptions().unique(false));
      actionsCollection.createIndex(Indexes.ascending(DelayedAction.RESOURCE_PATH), new IndexOptions().unique(false));
      actionsCollection.createIndex(Indexes.ascending(DelayedAction.NOTIFICATION_TYPE), new IndexOptions().unique(false));
   }

   @Override
   public List<DelayedAction> getActions() {
      return databaseCollection().find().into(new ArrayList<>());
   }

   @Override
   public void deleteScheduledActions(final String resourcePath, final NotificationType notificationType) {
      databaseCollection().deleteMany(
            Filters.and(
                  Filters.eq(DelayedAction.RESOURCE_PATH, resourcePath),
                  Filters.eq(DelayedAction.NOTIFICATION_TYPE, notificationType),
                  Filters.not(Filters.exists(DelayedAction.STARTED_PROCESSING)),
                  Filters.not(Filters.eq(DelayedAction.COMPLETED, true))
            )
      );
   }

   @Override
   public DelayedAction scheduleAction(final DelayedAction delayedAction) {
      try {
         databaseCollection().insertOne(delayedAction);
         return delayedAction;
      } catch (MongoException ex) {
         throw new StorageException("Cannot schedule action: " + delayedAction, ex);
      }
   }

   @Override
   public List<DelayedAction> scheduleActions(final List<DelayedAction> delayedActions) {
      try {
         databaseCollection().insertMany(delayedActions);
         return delayedActions;
      } catch (MongoException ex) {
         throw new StorageException("Cannot schedule actions: " + delayedActions, ex);
      }
   }

   public void deleteDelayedActionsRepository() {
      database.getCollection(databaseCollectionName()).drop();
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<DelayedAction> databaseCollection() {
      return database.getCollection(databaseCollectionName(), DelayedAction.class);
   }
}