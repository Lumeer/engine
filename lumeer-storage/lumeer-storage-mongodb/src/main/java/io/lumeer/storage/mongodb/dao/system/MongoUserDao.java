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

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
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
   public User createUser(final User user) {
      try {
         databaseCollection().insertOne(user);
         return user;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create User " + user, ex);
      }
   }

   @Override
   public User updateUser(final String id, final User user) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         User returnedUser = databaseCollection().findOneAndReplace(idFilter(id), user, options);
         if (returnedUser == null) {
            throw new StorageException("User '" + id + "' has not been updated.");
         }
         return returnedUser;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update user " + user, ex);
      }
   }

   @Override
   public void deleteUser(final String id) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(id));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("User '" + id + "' has not been deleted.");
      }
   }

   @Override
   public void deleteUserGroups(final String organizationId, final String userId) {
      Bson pullUser = Updates.pull(UserCodec.ALL_GROUPS, Filters.eq(UserCodec.ORGANIZATION_ID, organizationId));
      try {
         UpdateResult result = databaseCollection().updateOne(idFilter(userId), pullUser);
         if (result.getModifiedCount() != 1) {
            throw new StorageException("User '" + userId + "' has not been deleted.");
         }
      } catch (MongoException ex) {
         throw new StorageException("Cannot remove organization " + organizationId + "from user " + userId, ex);
      }
   }

   @Override
   public void deleteUsersGroups(final String organizationId) {
      Bson pullUser = Updates.pull(UserCodec.ALL_GROUPS, Filters.eq(UserCodec.ORGANIZATION_ID, organizationId));
      try {
         databaseCollection().updateMany(new BsonDocument(), pullUser);
      } catch (MongoException ex) {
         throw new StorageException("Cannot remove organization " + organizationId + " from users", ex);
      }
   }

   @Override
   public void deleteGroupFromUsers(final String organizationId, final String group) {
      String key = MongoUtils.concatParams(UserCodec.ALL_GROUPS, "$[" + ELEMENT_NAME + "]", UserCodec.GROUPS);
      Bson pullGroups = Updates.pull(key, group);
      UpdateOptions options = new UpdateOptions().arrayFilters(arrayFilters(organizationId));

      databaseCollection().updateMany(new BsonDocument(), pullGroups, options);
   }

   private List<Bson> arrayFilters(final String organizationId) {
      Bson filter = Filters.eq(MongoUtils.concatParams(ELEMENT_NAME, UserCodec.ORGANIZATION_ID), organizationId);
      return Collections.singletonList(filter);
   }

   @Override
   public User getUserByEmail(final String email) {
      Bson emailFilter = Filters.eq(UserCodec.EMAIL, email);

      return databaseCollection().find(emailFilter).first();
   }

   @Override
   public User getUserByAuthId(final String authId) {
      Bson authIdFilter = Filters.eq(UserCodec.AUTH_ID, authId);

      return databaseCollection().find(authIdFilter).first();
   }

   @Override
   public User getUserById(final String id) {
      return databaseCollection().find(idFilter(id)).first();
   }

   @Override
   public List<User> getAllUsers(final String organizationId) {
      return databaseCollection().find(organizationIdFilter(organizationId)).into(new ArrayList<>());
   }

   @Override
   public long getAllUsersCount(final String organizationId) {
      return databaseCollection().count(organizationIdFilter(organizationId));
   }

   private Bson organizationIdFilter(final String organizationId) {
      return Filters.elemMatch(UserCodec.ALL_GROUPS, Filters.eq(UserCodec.ORGANIZATION_ID, organizationId));
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<User> databaseCollection() {
      return database.getCollection(databaseCollectionName(), User.class);
   }
}
