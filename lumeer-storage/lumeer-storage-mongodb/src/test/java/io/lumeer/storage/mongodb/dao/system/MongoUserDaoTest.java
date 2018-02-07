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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.User;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.model.MongoUser;
import io.lumeer.storage.mongodb.util.MongoFilters;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MongoUserDaoTest extends MongoDbTestBase {

   private static final String ORGANIZATION_ID = "596e3b86d412bc5a3caaa22a";
   private static final String ORGANIZATION_ID2 = "596e3b86d412bc5a3caaa22b";

   private static final String USERNAME = "testUser";
   private static final String USERNAME2 = "testUser2";
   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   private static final String EMAIL = "user@email.com";
   private static final Set<String> GROUPS;

   static {
      GROUPS = new HashSet<>(Arrays.asList("group1", "group2", "group3"));
   }

   private MongoUserDao mongoUserDao;

   private Organization organization;
   private Organization organization2;

   @Before
   public void initUserDao() {
      organization = Mockito.mock(Organization.class);
      Mockito.when(organization.getId()).thenReturn(ORGANIZATION_ID);

      organization2 = Mockito.mock(Organization.class);
      Mockito.when(organization2.getId()).thenReturn(ORGANIZATION_ID2);

      mongoUserDao = new MongoUserDao();
      mongoUserDao.setDatabase(database);

      mongoUserDao.createUsersRepository();
      assertThat(database.listCollectionNames()).contains(mongoUserDao.databaseCollectionName());
   }

   @Test
   public void testDeleteRepository() {
      mongoUserDao.deleteUsersRepository();
      assertThat(database.listCollectionNames()).doesNotContain(mongoUserDao.databaseCollectionName());
   }

   @Test
   public void testCreateUser() {
      User user = prepareUser();

      String id = mongoUserDao.createUser(organization.getId(), user).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      MongoUser storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getEmail()).isEqualTo(user.getEmail());
      assertThat(storedUser.getName()).isEqualTo(user.getName());
      assertThat(storedUser.getGroups()).containsKey(organization.getId());
      assertThat(storedUser.getGroups().get(organization.getId())).containsOnlyElementsOf(GROUPS);
   }

   @Test
   public void testCreateUserAnotherOrganization() {
      User user = prepareUser();
      String id1 = mongoUserDao.createUser(organization.getId(), user).getId();
      String id2 = mongoUserDao.createUser(organization2.getId(), user).getId();
      assertThat(id1).isEqualTo(id2);

      MongoUser storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id1)).first();
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getEmail()).isEqualTo(user.getEmail());
      assertThat(storedUser.getName()).isEqualTo(user.getName());
      assertThat(storedUser.getGroups()).containsKeys(organization.getId(), organization2.getId());
      assertThat(storedUser.getGroups().get(organization.getId())).containsOnlyElementsOf(GROUPS);
      assertThat(storedUser.getGroups().get(organization2.getId())).containsOnlyElementsOf(GROUPS);
   }

   @Test
   public void testCreateUserExistingOrganization() {
      User u1 = mongoUserDao.createUser(organization.getId(),  prepareUser());
      User u2 = mongoUserDao.createUser(organization.getId(),  prepareUser());

      assertThat(u1.getId()).isEqualTo(u2.getId());
   }

   @Test
   public void testUpdateNameAndEmail() {
      User user = prepareUser();
      String id = mongoUserDao.createUser(organization.getId(), user).getId();
      assertThat(id).isNotNull().isNotEmpty();

      String anotherMail = "someother@email.com";
      user.setName(USERNAME2);
      user.setEmail(anotherMail);
      String id2 = mongoUserDao.updateUser(organization.getId(), id, user).getId();
      assertThat(id).isEqualTo(id2);

      MongoUser storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getEmail()).isEqualTo(anotherMail);
      assertThat(storedUser.getName()).isEqualTo(USERNAME2);
      assertThat(storedUser.getGroups()).containsKey(organization.getId());
      assertThat(storedUser.getGroups().get(organization.getId())).containsOnlyElementsOf(GROUPS);
   }

   @Test
   public void testUpdateGroups() {
      User user = prepareUser();
      String id = mongoUserDao.createUser(organization.getId(), user).getId();
      assertThat(id).isNotNull().isNotEmpty();

      Set<String> newGroups = new HashSet<>(Arrays.asList("groupU1", "groupU2"));
      user.setGroups(newGroups);
      mongoUserDao.updateUser(organization.getId(), id, user);

      MongoUser storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getGroups().get(organization.getId())).containsOnlyElementsOf(newGroups);
   }

   @Test
   public void testUpdateUserNotExisting() {
      User user = prepareUser();
      user.setId(NOT_EXISTING_ID);
      assertThatThrownBy(() -> mongoUserDao.updateUser(organization.getId(), NOT_EXISTING_ID, user))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testUpdateUserExistingUsername() {
      User user = prepareUser();
      mongoUserDao.createUser(organization.getId(), user);

      User user2 = new User("someother@email.com");
      user2.setName(USERNAME2);
      User returned = mongoUserDao.createUser(organization.getId(), user2);

      returned.setEmail(EMAIL);
      assertThatThrownBy(() -> mongoUserDao.updateUser(organization.getId(), returned.getId(), returned))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteUser() {
      User user = prepareUser();
      User stored = mongoUserDao.createUser(organization.getId(), user);
      mongoUserDao.createUser(organization2.getId(), user);

      MongoUser storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(stored.getId())).first();
      assertThat(storedUser.getGroups()).containsKeys(organization.getId(), organization2.getId());

      mongoUserDao.deleteUser(organization.getId(), stored.getId());
      storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(stored.getId())).first();
      assertThat(storedUser.getGroups()).containsKeys(organization2.getId());

      mongoUserDao.deleteUser(organization2.getId(), stored.getId());
      storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(stored.getId())).first();
      assertThat(storedUser.getGroups()).isEmpty();
   }

   @Test
   public void testDeleteUserNotExisting() {
      assertThatThrownBy(() -> mongoUserDao.deleteUser(organization.getId(), NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetUserByEmail() {
      User user = mongoUserDao.createUser(organization.getId(), prepareUser());

      Optional<User> storedUser = mongoUserDao.getUserByEmail(organization.getId(), EMAIL);
      assertThat(storedUser).isNotEmpty().hasValue(user);
   }

   @Test
   public void testGetUserByUsernameNotExisting() {
      Optional<User> storedUser = mongoUserDao.getUserByEmail(organization.getId(), EMAIL);
      assertThat(storedUser).isEmpty();
   }

   @Test
   public void testGetAllUsers() {
      User user = prepareUser();
      user = mongoUserDao.createUser(organization.getId(), user);

      User user2 = new User("someother@email.com");
      user2.setName(USERNAME2);
      user2 = mongoUserDao.createUser(organization.getId(), user2);

      User user3 = new User("someother@email.com");
      user3.setName(USERNAME2);
      mongoUserDao.createUser(organization2.getId(), user2);

      List<User> users = mongoUserDao.getAllUsers(organization.getId());
      assertThat(users).isNotNull()
                       .extracting(User::getId).containsOnly(user.getId(), user2.getId());
   }

   @Test
   public void testGetAllUsersEmpty() {
      List<User> users = mongoUserDao.getAllUsers(organization.getId());
      assertThat(users).isEmpty();
   }

   @Test
   public void testRemoveGroup() {
      User user = prepareUser();
      user.setGroups(new HashSet<>(Arrays.asList("g1", "g2", "g3")));
      mongoUserDao.createUser(organization.getId(), user);

      user = prepareUser();
      user.setEmail("lala@email.com");
      user.setGroups(new HashSet<>(Arrays.asList("g1", "g3", "g4")));
      mongoUserDao.createUser(organization.getId(), user);

      user = prepareUser();
      user.setEmail("lolo@email.com");
      user.setGroups(new HashSet<>(Arrays.asList("g2", "g5")));
      mongoUserDao.createUser(organization.getId(), user);

      mongoUserDao.deleteGroupFromUsers(organization.getId(), "g1");
      List<User> users = mongoUserDao.getAllUsers(organization.getId());
      users.forEach(u -> assertThat(u.getGroups()).doesNotContain("g1"));

      mongoUserDao.deleteGroupFromUsers(organization.getId(), "g2");
      users = mongoUserDao.getAllUsers(organization.getId());
      users.forEach(u -> assertThat(u.getGroups()).doesNotContain("g1"));

      mongoUserDao.deleteGroupFromUsers(organization.getId(), "g3");
      users = mongoUserDao.getAllUsers(organization.getId());
      users.forEach(u -> assertThat(u.getGroups()).doesNotContain("g1"));
   }

   private User prepareUser() {
      User user = new User(EMAIL);
      user.setName(USERNAME);
      user.setGroups(GROUPS);
      return user;
   }

}
