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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.User;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaUser;

import com.mongodb.DuplicateKeyException;
import org.assertj.core.util.Sets;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MorphiaUserDaoTest extends MongoDbTestBase {

   private static final String ORGANIZATION_ID = "596e3b86d412bc5a3caaa22a";

   private static final String USERNAME = "testUser";
   private static final String USERNAME2 = "testUser2";
   private static final Set<String> GROUPS = Sets.newLinkedHashSet("testGroup1", "testGroup2");
   private static final Set<String> GROUPS2 = Sets.newLinkedHashSet("testGroup1", "testGroup2", "testGroup3");
   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   private MorphiaUserDao morphiaUserDao;

   @Before
   public void initUserDao() {
      Organization organization = Mockito.mock(Organization.class);
      Mockito.when(organization.getId()).thenReturn(ORGANIZATION_ID);

      morphiaUserDao = new MorphiaUserDao();
      morphiaUserDao.setDatabase(database);
      morphiaUserDao.setDatastore(datastore);

      morphiaUserDao.setOrganization(organization);
      morphiaUserDao.ensureIndexes();
   }

   private MorphiaUser prepareUser() {
      MorphiaUser user = new MorphiaUser();
      user.setUsername(USERNAME);
      user.setGroups(GROUPS);
      return user;
   }

   @Test
   public void testCreateUser() {
      User user = prepareUser();

      String id = morphiaUserDao.createUser(user).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      User storedUser = datastore.get(morphiaUserDao.databaseCollection(), MorphiaUser.class, new ObjectId(id));
      assertThat(storedUser).isNotNull().isEqualTo(user);
   }

   @Test
   public void testCreateUserExistingUsername() {
      User user = prepareUser();
      datastore.save(morphiaUserDao.databaseCollection(), user);

      User user2 = prepareUser();
      assertThatThrownBy(() -> morphiaUserDao.createUser(user2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testUpdateUserName() {
      MorphiaUser user = prepareUser();
      String id = datastore.save(morphiaUserDao.databaseCollection(), user).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      user.setUsername(USERNAME2);
      morphiaUserDao.updateUser(id, user);

      User storedUser = datastore.get(morphiaUserDao.databaseCollection(), MorphiaUser.class, new ObjectId(id));
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getUsername()).isEqualTo(USERNAME2);
   }

   @Test
   public void testUpdateUserGroups() {
      MorphiaUser user = prepareUser();
      String id = datastore.save(morphiaUserDao.databaseCollection(), user).getId().toString();
      assertThat(id).isNotNull().isNotEmpty();

      user.setGroups(GROUPS2);
      morphiaUserDao.updateUser(id, user);

      User storedUser = datastore.get(morphiaUserDao.databaseCollection(), MorphiaUser.class, new ObjectId(id));
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getGroups()).isEqualTo(GROUPS2);
   }

   @Test
   @Ignore("Stored anyway in the current implementation")
   public void testUpdateUserNotExisting() {
      MorphiaUser user = prepareUser();
      user.setId(NOT_EXISTING_ID);
      assertThatThrownBy(() -> morphiaUserDao.updateUser(NOT_EXISTING_ID, user))
            .isInstanceOf(WriteFailedException.class);
   }

   @Test
   public void testUpdateUserExistingUsername() {
      User user = prepareUser();
      datastore.save(morphiaUserDao.databaseCollection(), user);

      MorphiaUser user2 = new MorphiaUser();
      user2.setUsername(USERNAME2);
      datastore.save(morphiaUserDao.databaseCollection(), user2);

      user2.setUsername(USERNAME);
      assertThatThrownBy(() -> morphiaUserDao.updateUser(user2.getId(), user2))
            .isInstanceOf(DuplicateKeyException.class);
   }

   @Test
   public void testDeleteUser() {
      User user = prepareUser();
      datastore.save(morphiaUserDao.databaseCollection(), user);

      morphiaUserDao.deleteUser(user.getId());

      User storedUser = datastore.get(morphiaUserDao.databaseCollection(), MorphiaUser.class, new ObjectId(user.getId()));
      assertThat(storedUser).isNull();
   }

   @Test
   public void testDeleteUserNotExisting() {
      assertThatThrownBy(() -> morphiaUserDao.deleteUser(NOT_EXISTING_ID))
            .isInstanceOf(WriteFailedException.class);
   }

   @Test
   public void testGetUserByUsername() {
      User user = prepareUser();
      datastore.save(morphiaUserDao.databaseCollection(), user);

      Optional<User> storedUser = morphiaUserDao.getUserByUsername(USERNAME);
      assertThat(storedUser).isNotEmpty().hasValue(user);
   }

   @Test
   public void testGetUserByUsernameNotExisting() {
      Optional<User> storedUser = morphiaUserDao.getUserByUsername(USERNAME);
      assertThat(storedUser).isEmpty();
   }

   @Test
   public void testGetAllUsers() {
      User user = prepareUser();
      datastore.save(morphiaUserDao.databaseCollection(), user);

      MorphiaUser user2 = new MorphiaUser();
      user2.setUsername(USERNAME2);
      datastore.save(morphiaUserDao.databaseCollection(), user2);

      List<User> users = morphiaUserDao.getAllUsers();
      assertThat(users).isNotNull()
                       .extracting(User::getUsername).containsExactly(USERNAME, USERNAME2);
   }

   @Test
   public void testGetAllUsersEmpty() {
      List<User> users = morphiaUserDao.getAllUsers();
      assertThat(users).isEmpty();
   }

}
