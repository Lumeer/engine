/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.storage.mongodb.dao.organization;

import io.lumeer.api.model.User;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MongoUser;

import com.mongodb.WriteResult;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoUserDao extends OrganizationScopedDao implements UserDao {

   @Override
   public User createUser(final User user) {
      MongoUser mongoUser = new MongoUser(user);
      datastore.save(mongoUser);
      return mongoUser;
   }

   @Override
   public void updateUser(final String id, final User user) {
      MongoUser mongoUser = new MongoUser(user);
      mongoUser.setId(new ObjectId(id));

      datastore.save(mongoUser);
   }

   @Override
   public void deleteUser(final String id) {
      WriteResult writeResult = datastore.delete(MongoUser.class, new ObjectId(id));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   @Override
   public Optional<User> getUserByUsername(final String username) {
      User user = datastore.createQuery(MongoUser.class)
                           .field(MongoUser.USERNAME).equal(username)
                           .get();
      return Optional.ofNullable(user);
   }

   @Override
   public List<User> getAllUsers() {
      return new ArrayList<>(datastore.createQuery(MongoUser.class).asList());
   }
}
