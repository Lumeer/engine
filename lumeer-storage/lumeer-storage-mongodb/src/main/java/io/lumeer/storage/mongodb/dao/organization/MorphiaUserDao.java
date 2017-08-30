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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.User;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaUser;

import com.mongodb.WriteResult;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MorphiaUserDao extends OrganizationScopedDao implements UserDao {

   private static final String PREFIX = "users_o-";

   public void ensureIndexes() {
      datastore.ensureIndexes(databaseCollection(), MorphiaUser.class); // TODO remove after createUsersRepository() is implemented
   }

   @Override
   public void createUsersRepository(Organization organization) {
      // TODO change the way user data storage is used
   }

   @Override
   public void deleteUsersRepository(Organization organization) {
      // TODO change the way user data storage is used
   }

   @Override
   public User createUser(final User user) {
      ensureIndexes();

      MorphiaUser morphiaUser = new MorphiaUser(user);
      datastore.insert(databaseCollection(), morphiaUser);
      return morphiaUser;
   }

   @Override
   public void updateUser(final String id, final User user) {
      MorphiaUser morphiaUser = new MorphiaUser(user);
      morphiaUser.setId(id);

      datastore.save(databaseCollection(), morphiaUser);
   }

   @Override
   public void deleteUser(final String id) {
      WriteResult writeResult = datastore.delete(databaseCollection(), MorphiaUser.class, new ObjectId(id));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   @Override
   public Optional<User> getUserByUsername(final String username) {
      User user = datastore.createQuery(databaseCollection(), MorphiaUser.class)
                           .field(MorphiaUser.USERNAME).equal(username)
                           .get();
      return Optional.ofNullable(user);
   }

   @Override
   public List<User> getAllUsers() {
      return new ArrayList<>(datastore.createQuery(databaseCollection(), MorphiaUser.class).asList());
   }

   String databaseCollection() {
      if (!getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return databaseCollection(getOrganization().get());
   }

   private String databaseCollection(Organization organization) {
      return PREFIX + organization.getId();
   }
}
