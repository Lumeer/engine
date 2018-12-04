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

import io.lumeer.api.model.UserNotification;
import io.lumeer.storage.api.dao.UserNotificationDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.UserNotificationCodec;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class MongoUserNotificationDao extends SystemScopedDao implements UserNotificationDao {

   public static final String COLLECTION_NAME = "userNotifications";

   @PostConstruct
   public void initDb() {
      createUserNotificationsRepository();
   }

   @Override
   public List<UserNotification> getRecentNotifications(final String userId) {
      Bson idFilter = Filters.eq(UserNotificationCodec.USER_ID, userId);

      return databaseCollection()
            .find(idFilter)
            .limit(500)
            .sort(Sorts.descending(UserNotificationCodec.CREATED_AT))
            .into(new ArrayList<>());
   }

   @Override
   public UserNotification getNotificationById(final String notificationId) {
      return databaseCollection().find(idFilter(notificationId)).first();
   }

   @Override
   public UserNotification updateNotification(final UserNotification notification) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         UserNotification returnedNotification = databaseCollection().findOneAndReplace(idFilter(notification.getId()), notification, options);
         if (returnedNotification == null) {
            throw new StorageException("Notification '" + notification.getId() + "' has not been updated.");
         }
         // TODO send push notification
         return returnedNotification;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update notification " + notification, ex);
      }
   }

   @Override
   public List<UserNotification> createNotificationsBatch(final List<UserNotification> notifications) {
      databaseCollection().insertMany(notifications);
      return notifications;
   }

   @Override
   public void createUserNotificationsRepository() {
      if (database.getCollection(COLLECTION_NAME) == null) {
         database.createCollection(COLLECTION_NAME);

         MongoCollection<Document> userNotificationCollection = database.getCollection(COLLECTION_NAME);
         userNotificationCollection.createIndex(Indexes.ascending(UserNotification.USER_ID, UserNotification.CREATED_AT), new IndexOptions().unique(false));
      }
   }

   MongoCollection<UserNotification> databaseCollection() {
      return database.getCollection(COLLECTION_NAME, UserNotification.class);
   }
}
