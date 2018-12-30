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
import io.lumeer.engine.api.event.CreateOrUpdateUserNotification;
import io.lumeer.engine.api.event.RemoveUserNotification;
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
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class MongoUserNotificationDao extends SystemScopedDao implements UserNotificationDao {

   public static final String COLLECTION_NAME = "userNotifications";

   @Inject
   private Event<CreateOrUpdateUserNotification> createOrUpdateUserNotificationEvent;

   @Inject
   private Event<RemoveUserNotification> removeUserNotificationEvent;

   @PostConstruct
   public void initDb() {
      createUserNotificationsRepository();
   }

   @Override
   public List<UserNotification> getRecentNotifications(final String userId) {
      Bson idFilter = Filters.eq(UserNotificationCodec.USER_ID, userId);

      return databaseCollection()
            .find(idFilter)
            .sort(Sorts.descending(UserNotificationCodec.CREATED_AT))
            .limit(500)
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
         if (createOrUpdateUserNotificationEvent != null) {
            createOrUpdateUserNotificationEvent.fire(new CreateOrUpdateUserNotification(returnedNotification));
         }
         return returnedNotification;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update notification " + notification, ex);
      }
   }

   @Override
   public void updateNotifications(final String searchField, final String searchId, final Map<String, String> updates) {
      final List<Bson> sets = updates.entrySet().stream().map(e -> Updates.set(e.getKey(), e.getValue())).collect(Collectors.toList());
      final UpdateResult result = databaseCollection().updateMany(Filters.eq(searchField, searchId), Updates.combine(sets));

      if (result.getModifiedCount() > 0 && createOrUpdateUserNotificationEvent != null) {
         final List<UserNotification> updatedNotifications = databaseCollection().find(Filters.eq(searchField, searchId)).into(new ArrayList<>());
         updatedNotifications.forEach(notification -> createOrUpdateUserNotificationEvent.fire(new CreateOrUpdateUserNotification(notification)));
      }
   }

   @Override
   public void removeNotifications(final String searchField, final String searchId, final Set<String> users) {
      Bson filter = createRemoveNotificationsFilter(searchField, searchId, users);
      final List<UserNotification> deletedNotifications = databaseCollection().find(filter).into(new ArrayList<>());
      final DeleteResult result = databaseCollection().deleteMany(filter);

      if (result.getDeletedCount() > 0 && removeUserNotificationEvent != null) {
         deletedNotifications.forEach(notification -> removeUserNotificationEvent.fire(new RemoveUserNotification(notification)));
      }
   }

   private Bson createRemoveNotificationsFilter(final String searchField, final String searchId, final Set<String> users) {
      Bson searchFilter = Filters.eq(searchField, searchId);
      if (users.isEmpty()) {
         return searchFilter;
      }
      Bson userFilter = Filters.in(UserNotificationCodec.USER_ID, users);
      return Filters.and(searchFilter, userFilter);
   }

   @Override
   public void deleteNotification(final UserNotification notification) {
      final UserNotification userNotification = databaseCollection().findOneAndDelete(idFilter(notification.getId()));
      if (userNotification == null) {
         throw new StorageException("User notification '" + notification.getUserId() + "' has not been deleted.");
      }
      if (removeUserNotificationEvent != null) {
         removeUserNotificationEvent.fire(new RemoveUserNotification(userNotification));
      }
   }

   @Override
   public List<UserNotification> createNotificationsBatch(final List<UserNotification> notifications) {
      if (notifications.size() > 0) {
         databaseCollection().insertMany(notifications);

         if (createOrUpdateUserNotificationEvent != null) {
            notifications.forEach(notification -> createOrUpdateUserNotificationEvent.fire(new CreateOrUpdateUserNotification(notification)));
         }
      }

      return notifications;
   }

   @Override
   public void createUserNotificationsRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(COLLECTION_NAME)) {
         database.createCollection(COLLECTION_NAME);

         MongoCollection<Document> userNotificationCollection = database.getCollection(COLLECTION_NAME);
         userNotificationCollection.createIndex(Indexes.ascending(UserNotification.USER_ID, UserNotification.CREATED_AT), new IndexOptions().unique(false));
         userNotificationCollection.createIndex(Indexes.ascending(UserNotification.DATA + "." + UserNotification.OrganizationShared.ORGANIZATION_ID));
         userNotificationCollection.createIndex(Indexes.ascending(UserNotification.DATA + "." + UserNotification.ProjectShared.PROJECT_ID));
         userNotificationCollection.createIndex(Indexes.ascending(UserNotification.DATA + "." + UserNotification.CollectionShared.COLLECTION_ID));
         userNotificationCollection.createIndex(Indexes.ascending(UserNotification.DATA + "." + UserNotification.ViewShared.VIEW_CODE));
      }
   }

   private MongoCollection<UserNotification> databaseCollection() {
      return database.getCollection(COLLECTION_NAME, UserNotification.class);
   }
}
