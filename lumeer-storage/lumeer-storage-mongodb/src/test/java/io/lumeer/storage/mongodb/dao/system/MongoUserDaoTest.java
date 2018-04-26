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
import io.lumeer.storage.mongodb.util.MongoFilters;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

      String id = mongoUserDao.createUser(user).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      User storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getEmail()).isEqualTo(user.getEmail());
      assertThat(storedUser.getName()).isEqualTo(user.getName());
      assertThat(storedUser.getGroups()).containsKey(organization.getId());
      assertThat(storedUser.getGroups().get(organization.getId())).containsOnlyElementsOf(GROUPS);
   }

   @Test
   public void testCreateUserAnotherOrganization() {
      User user = prepareUser();
      User stored = mongoUserDao.createUser(user);

      Map<String, Set<String>> groups = new HashMap<>(stored.getGroups());
      groups.put(organization2.getId(), GROUPS);
      user.setGroups(groups);
      mongoUserDao.updateUser(stored.getId(), user);

      User storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(stored.getId())).first();
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getEmail()).isEqualTo(user.getEmail());
      assertThat(storedUser.getName()).isEqualTo(user.getName());
      assertThat(storedUser.getGroups()).containsKeys(organization.getId(), organization2.getId());
      assertThat(storedUser.getGroups().get(organization.getId())).containsOnlyElementsOf(GROUPS);
      assertThat(storedUser.getGroups().get(organization2.getId())).containsOnlyElementsOf(GROUPS);
   }

   @Test
   public void testCreateExistingUser() {
      mongoUserDao.createUser(prepareUser());

      assertThatThrownBy(() -> mongoUserDao.createUser(prepareUser()))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testUpdateNameAndEmail() {
      User user = prepareUser();
      String id = mongoUserDao.createUser(user).getId();
      assertThat(id).isNotNull().isNotEmpty();

      String anotherMail = "someother@email.com";
      user.setName(USERNAME2);
      user.setEmail(anotherMail);
      String id2 = mongoUserDao.updateUser(id, user).getId();
      assertThat(id).isEqualTo(id2);

      User storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getEmail()).isEqualTo(anotherMail);
      assertThat(storedUser.getName()).isEqualTo(USERNAME2);
      assertThat(storedUser.getGroups()).containsKey(organization.getId());
      assertThat(storedUser.getGroups().get(organization.getId())).containsOnlyElementsOf(GROUPS);
   }

   @Test
   public void testUpdateGroups() {
      User user = prepareUser();
      String id = mongoUserDao.createUser(user).getId();
      assertThat(id).isNotNull().isNotEmpty();

      Set<String> newGroups = new HashSet<>(Arrays.asList("groupU1", "groupU2"));
      user.setGroups(Collections.singletonMap(organization.getId(), newGroups));
      mongoUserDao.updateUser(id, user);

      User storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getGroups().get(organization.getId())).containsOnlyElementsOf(newGroups);
   }

   @Test
   public void testUpdateUserExistingUsername() {
      User user = prepareUser();
      mongoUserDao.createUser(user);

      User user2 = new User("someother@email.com");
      user2.setName(USERNAME2);
      User returned = mongoUserDao.createUser(user2);

      returned.setEmail(EMAIL);
      assertThatThrownBy(() -> mongoUserDao.updateUser(returned.getId(), returned))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteUserGroups() {
      User user = prepareUser();
      User stored = mongoUserDao.createUser(user);

      Map<String, Set<String>> groups = new HashMap<>(stored.getGroups());
      groups.put(organization2.getId(), GROUPS);
      user.setGroups(groups);
      mongoUserDao.updateUser(stored.getId(), user);

      User storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(stored.getId())).first();
      assertThat(storedUser.getGroups()).containsKeys(organization.getId(), organization2.getId());

      mongoUserDao.deleteUserGroups(organization.getId(), stored.getId());
      storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(stored.getId())).first();
      assertThat(storedUser.getGroups()).containsKeys(organization2.getId());

      mongoUserDao.deleteUserGroups(organization2.getId(), stored.getId());
      storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(stored.getId())).first();
      assertThat(storedUser.getGroups()).isEmpty();
   }

   @Test
   public void testDeleteUsersGroups() {
      User usr1 = prepareUser("email1");
      Map<String, Set<String>> groups = new HashMap<>(usr1.getGroups());
      groups.put(organization2.getId(), GROUPS);
      usr1.setGroups(groups);
      User stored1 = mongoUserDao.createUser(usr1);

      User stored2 = mongoUserDao.createUser(prepareUser("email2"));

      User usr3 = prepareUser("email3");
      Map<String, Set<String>> groups2 = new HashMap<>(usr3.getGroups());
      groups2.put(organization2.getId(), GROUPS);
      usr3.setGroups(groups2);
      User stored3 = mongoUserDao.createUser(usr3);

      assertThat(stored1.getGroups().keySet()).containsOnly(organization.getId(), organization2.getId());
      assertThat(stored2.getGroups().keySet()).containsOnly(organization.getId());
      assertThat(stored3.getGroups().keySet()).containsOnly(organization.getId(), organization2.getId());

      mongoUserDao.deleteUsersGroups(organization.getId());

      stored1 = mongoUserDao.getUserById(stored1.getId());
      stored2 = mongoUserDao.getUserById(stored2.getId());
      stored3 = mongoUserDao.getUserById(stored3.getId());

      assertThat(stored1.getGroups().keySet()).containsOnly(organization2.getId());
      assertThat(stored2.getGroups().keySet()).isEmpty();
      assertThat(stored3.getGroups().keySet()).containsOnly(organization2.getId());

   }

   @Test
   public void testDeleteUser() {
      String id = mongoUserDao.createUser(prepareUser()).getId();

      User storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedUser).isNotNull();

      mongoUserDao.deleteUser(id);
      storedUser = mongoUserDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedUser).isNull();
   }

   @Test
   public void testDeleteUserNotExisting() {
      assertThatThrownBy(() -> mongoUserDao.deleteUser(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetUserByEmail() {
      User user = mongoUserDao.createUser(prepareUser());

      User storedUser = mongoUserDao.getUserByEmail(user.getEmail());
      assertThat(storedUser).isNotNull();
   }

   @Test
   public void testGetUserById() {
      User user = mongoUserDao.createUser(prepareUser());

      User storedUser = mongoUserDao.getUserById(user.getId());
      assertThat(storedUser).isNotNull();
   }

   @Test
   public void testGetUserByUsernameNotExisting() {
      User storedUser = mongoUserDao.getUserByEmail(EMAIL);
      assertThat(storedUser).isNull();
   }

   @Test
   public void testGetAllUsers() {
      User user = prepareUser();
      user = mongoUserDao.createUser(user);

      User user2 = prepareUser();
      user2.setName(USERNAME2);
      user2.setEmail("someother@email.com");
      user2 = mongoUserDao.createUser(user2);

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
      user.setGroups(Collections.singletonMap(organization.getId(), new HashSet<>(Arrays.asList("g1", "g2", "g3"))));
      mongoUserDao.createUser(user);

      user = prepareUser();
      user.setEmail("lala@email.com");
      user.setGroups(Collections.singletonMap(organization.getId(), new HashSet<>(Arrays.asList("g1", "g3", "g4"))));
      mongoUserDao.createUser(user);

      user = prepareUser();
      user.setEmail("lolo@email.com");
      user.setGroups(Collections.singletonMap(organization.getId(), new HashSet<>(Arrays.asList("g2", "g5"))));
      mongoUserDao.createUser(user);

      mongoUserDao.deleteGroupFromUsers(organization.getId(), "g1");
      List<User> users = mongoUserDao.getAllUsers(organization.getId());
      users.forEach(u -> assertThat(u.getGroups().get(organization.getId())).doesNotContain("g1"));

      mongoUserDao.deleteGroupFromUsers(organization.getId(), "g2");
      users = mongoUserDao.getAllUsers(organization.getId());
      users.forEach(u -> assertThat(u.getGroups().get(organization.getId())).doesNotContain("g1"));

      mongoUserDao.deleteGroupFromUsers(organization.getId(), "g3");
      users = mongoUserDao.getAllUsers(organization.getId());
      users.forEach(u -> assertThat(u.getGroups().get(organization.getId())).doesNotContain("g1"));
   }

   private User prepareUser() {
      return prepareUser(EMAIL);
   }

   private User prepareUser(String email) {
      User user = new User(email);
      user.setName(USERNAME);
      user.setGroups(Collections.singletonMap(organization.getId(), GROUPS));
      return user;
   }

}
