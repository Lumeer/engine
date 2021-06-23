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

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.User;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.UserCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoUserDao extends MongoSystemScopedDao implements UserDao {

   private static final String COLLECTION_NAME = "users";

   @PostConstruct
   public void checkRepository() {
      createUsersRepository();
   }

   public void createUsersRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(databaseCollectionName())) {
         database.createCollection(databaseCollectionName());
      }

      MongoCollection<Document> userCollection = database.getCollection(databaseCollectionName());
      userCollection.createIndex(Indexes.ascending(UserCodec.EMAIL), new IndexOptions().unique(true));
      userCollection.createIndex(Indexes.ascending(UserCodec.REFERRAL), new IndexOptions().unique(false));
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
   public User getUserByEmail(final String email) {
      Bson emailFilter = Filters.or(Filters.eq(UserCodec.EMAIL, email), Filters.eq(UserCodec.EMAIL, email.toLowerCase()));

      return databaseCollection().find(emailFilter).first();
   }

   @Override
   public List<User> getUsersByEmails(final Set<String> emails) {
      if (emails.isEmpty()) {
         return Collections.emptyList();
      }
      Bson emailFilter = Filters.or(Filters.in(UserCodec.EMAIL, emails), Filters.eq(UserCodec.EMAIL, emails.stream().map(String::toLowerCase).collect(Collectors.toSet())));

      return databaseCollection().find(emailFilter).into(new ArrayList<>());
   }

   @Override
   public long getReferralsCount(final String referral) {
      Bson referralFilter = Filters.eq(UserCodec.REFERRAL, referral);

      return databaseCollection().countDocuments(referralFilter);
   }

   @Override
   public User getUserByAuthId(final String authId) {
      Bson authIdFilter = Filters.eq(UserCodec.AUTH_IDS, authId);

      return databaseCollection().find(authIdFilter).first();
   }

   @Override
   public User getUserById(final String id) {
      return databaseCollection().find(idFilter(id)).first();
   }

   @Override
   public List<User> getUserByIds(final Set<String> ids) {
      Bson filter = MongoFilters.idsFilter(ids);
      if (filter == null) {
         return Collections.emptyList();
      }
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<User> getAllUsers(final String organizationId) {
      return databaseCollection().find(organizationIdFilter(organizationId)).into(new ArrayList<>());
   }

   @Override
   public long getAllUsersCount(final String organizationId) {
      return databaseCollection().countDocuments(organizationIdFilter(organizationId));
   }

   private Bson organizationIdFilter(final String organizationId) {
      return Filters.or(
            Filters.in(UserCodec.ORGANIZATIONS, organizationId),
            Filters.elemMatch(UserCodec.ALL_GROUPS, Filters.eq("organizationId", organizationId))
      );
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<User> databaseCollection() {
      return database.getCollection(databaseCollectionName(), User.class);
   }
}
