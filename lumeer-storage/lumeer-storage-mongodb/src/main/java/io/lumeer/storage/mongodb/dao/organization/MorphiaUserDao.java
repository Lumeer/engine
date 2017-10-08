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
