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

import io.lumeer.api.model.User;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.codecs.UserCodec;
import io.lumeer.storage.mongodb.model.MongoUser;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoUserDao extends SystemScopedDao implements UserDao {

   private static final String COLLECTION_NAME = "users";
   private static final String ELEMENT_NAME = "group";

   @PostConstruct
   public void checkRepository() {
      if (database.getCollection(COLLECTION_NAME) == null) {
         createUsersRepository();
      }
   }

   public void createUsersRepository() {
      database.createCollection(databaseCollectionName());

      MongoCollection<Document> userCollection = database.getCollection(databaseCollectionName());
      userCollection.createIndex(Indexes.ascending(UserCodec.EMAIL), new IndexOptions().unique(true));
   }

   public void deleteUsersRepository() {
      database.getCollection(databaseCollectionName()).drop();
   }

   @Override
   public User createUser(final String organizationId, final User user) {
      MongoUser existingUser = databaseCollection().find(Filters.eq(UserCodec.EMAIL, user.getEmail())).first();
      if (existingUser != null) {
         return upsertUserGroupsForOrganization(organizationId, existingUser.getId(), user);
      }
      return createNewUser(organizationId, user);
   }

   private User createNewUser(final String organizationId, final User user) {
      try {
         MongoUser mongoUser = new MongoUser(user, organizationId, null);
         databaseCollection().insertOne(mongoUser);
         return mongoUser.toUser(organizationId);
      } catch (MongoException ex) {
         throw new StorageException("Cannot create user " + user, ex);
      }
   }

   @Override
   public User updateUser(final String organizationId, final String userId, final User user) {
      return setUserGroupsForOrganization(organizationId, userId, user);
   }

   private User upsertUserGroupsForOrganization(final String organizationId, final String userId, final User user) {
      Bson setName = Updates.set(UserCodec.NAME, user.getName());
      Bson setEmail = Updates.set(UserCodec.EMAIL, user.getEmail());
      Document groupDocument = new Document(UserCodec.ORGANIZATION_ID, organizationId)
            .append(UserCodec.GROUPS, user.getGroups());
      Bson pushGroup = Updates.push(UserCodec.ALL_GROUPS, groupDocument);
      Bson update = Updates.combine(setName, setEmail, pushGroup);

      Bson filter = Filters.and(idFilter(userId), Filters.ne(MongoUtils.concatParams(UserCodec.ALL_GROUPS, UserCodec.ORGANIZATION_ID), organizationId));
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
      try {
         MongoUser returnedUser = databaseCollection().findOneAndUpdate(filter, update, options);
         if (returnedUser == null) {
            return setUserGroupsForOrganization(organizationId, userId, user);
         }
         return returnedUser.toUser(organizationId);
      } catch (MongoException ex) {
         throw new StorageException("Cannot create user " + user, ex);
      }
   }

   private User setUserGroupsForOrganization(final String organizationId, final String userId, final User user) {
      Bson setName = Updates.set(UserCodec.NAME, user.getName());
      Bson setEmail = Updates.set(UserCodec.EMAIL, user.getEmail());
      Bson setGroups = Updates.set(MongoUtils.concatParams(UserCodec.ALL_GROUPS, "$", UserCodec.GROUPS), user.getGroups());
      Bson update = Updates.combine(setName, setEmail, setGroups);

      Bson filter = Filters.and(idFilter(userId), Filters.eq(MongoUtils.concatParams(UserCodec.ALL_GROUPS, UserCodec.ORGANIZATION_ID), organizationId));
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
      try {
         MongoUser returnedUser = databaseCollection().findOneAndUpdate(filter, update, options);
         if (returnedUser == null) {
            throw new StorageException("User '" + userId + "' has not been updated.");
         }
         return returnedUser.toUser(organizationId);
      } catch (MongoException ex) {
         throw new StorageException("Cannot update user " + user, ex);
      }

   }

   @Override
   public void deleteUser(final String organizationId, final String userId) {
      Bson pullUser = Updates.pull(UserCodec.ALL_GROUPS, Filters.eq(UserCodec.ORGANIZATION_ID, organizationId));
      try {
         UpdateResult result = databaseCollection().updateOne(idFilter(userId), pullUser);
         if (result.getModifiedCount() != 1) {
            throw new StorageException("User '" + userId + "' has not been deleted.");
         }
      } catch (MongoException ex) {
         throw new StorageException("Cannot remove user " + userId, ex);
      }
   }

   @Override
   public void deleteGroupFromUsers(final String organizationId, final String group) {
      String key = MongoUtils.concatParams(UserCodec.ALL_GROUPS, "$[" + ELEMENT_NAME + "]", UserCodec.GROUPS);
      Bson pullGroups = Updates.pull(key, group);
      UpdateOptions options = new UpdateOptions().arrayFilters(arrayFilters(organizationId));

      databaseCollection().updateMany(new BsonDocument(), pullGroups, options);
   }

   @Override
   public Optional<User> getUserByEmail(final String organizationId, final String email) {
      MongoUser mongoUser = databaseCollection().find(emailFilter(organizationId, email)).first();
      return mongoUser != null ? Optional.of(mongoUser.toUser(organizationId)) : Optional.empty();
   }

   private Bson emailFilter(final String organizationId, final String email) {
      Bson emailFilter = Filters.eq(UserCodec.EMAIL, email);
      return Filters.and(emailFilter, organizationIdFilter(organizationId));
   }

   private List<Bson> arrayFilters(final String organizationId) {
      Bson filter = Filters.eq(MongoUtils.concatParams(ELEMENT_NAME, UserCodec.ORGANIZATION_ID), organizationId);
      return Collections.singletonList(filter);
   }

   @Override
   public List<User> getAllUsers(final String organizationId) {
      return databaseCollection().find(organizationIdFilter(organizationId)).map(mongoUser -> mongoUser.toUser(organizationId)).into(new ArrayList<>());
   }

   private Bson organizationIdFilter(final String organizationId) {
      return Filters.elemMatch(UserCodec.ALL_GROUPS, Filters.eq(UserCodec.ORGANIZATION_ID, organizationId));
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<MongoUser> databaseCollection() {
      return database.getCollection(databaseCollectionName(), MongoUser.class);
   }
}
