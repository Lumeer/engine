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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class UserFacadeIntegrationTest extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String USER1 = "user1@gmail.com";
   private static final String USER2 = "user2@gmail.com";
   private static final String USER3 = "user3@gmail.com";

   private static final Set<String> GROUPS;

   static {
      GROUPS = new HashSet<>(Arrays.asList("group1", "group2", "group3"));
   }

   private String organizationId1;
   private String organizationId2;
   private String organizationIdNotPermission;

   @Inject
   private UserFacade userFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Before
   public void configure() {
      JsonOrganization organization1 = new JsonOrganization();
      organization1.setCode("LMR");
      organization1.setPermissions(new JsonPermissions());
      organization1.getPermissions().updateUserPermissions(new JsonPermission(USER, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      organizationId1 = organizationDao.createOrganization(organization1).getId();

      JsonOrganization organization2 = new JsonOrganization();
      organization2.setCode("MRL");
      organization2.setPermissions(new JsonPermissions());
      organization2.getPermissions().updateUserPermissions(new JsonPermission(USER, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      organizationId2 = organizationDao.createOrganization(organization2).getId();

      JsonOrganization organization3 = new JsonOrganization();
      organization3.setCode("RML");
      organization3.setPermissions(new JsonPermissions());
      organizationIdNotPermission = organizationDao.createOrganization(organization3).getId();
   }

   @Test
   public void testCreateUser() {
      userFacade.createUser(organizationId1, prepareUser(USER1));

      User stored = getUser(organizationId1, USER1);

      assertThat(stored).isNotNull();
      assertThat(stored.getName()).isEqualTo(USER1);
      assertThat(stored.getEmail()).isEqualTo(USER1);
      assertThat(stored.getGroups()).isEqualTo(GROUPS);
   }

   @Test
   public void testCreateUserMultipleOrganizations() {
      User user11 = userFacade.createUser(organizationId1, prepareUser(USER1));
      User user21 = userFacade.createUser(organizationId1, prepareUser(USER2));
      User user12 = userFacade.createUser(organizationId2, prepareUser(USER1));
      User user32 = userFacade.createUser(organizationId2, prepareUser(USER3));

      assertThat(user11.getId()).isEqualTo(user12.getId());
      assertThat(user21.getId()).isNotEqualTo(user32.getId());
      assertThat(user21.getId()).isNotEqualTo(user11.getId());
   }

   @Test
   public void testCreateUserExistingOrganization() {
      User user1 = userFacade.createUser(organizationId1, prepareUser(USER1));
      User user2 = userFacade.createUser(organizationId1, prepareUser(USER1));
      User user3 = userFacade.createUser(organizationId1, prepareUser(USER1));

      assertThat(user1.getId()).isEqualTo(user2.getId()).isEqualTo(user3.getId());
   }

   @Test
   public void testCreateUserNotPermission() {
      assertThatThrownBy(() -> userFacade.createUser(organizationIdNotPermission, prepareUser(USER1)))
            .isInstanceOf(NoPermissionException.class);
   }

   @Test
   public void testUpdateNameAndEmail() {
      String userId = createUser(organizationId1, USER1).getId();

      User toUpdate = prepareUser(USER1);
      toUpdate.setEmail(USER3);
      toUpdate.setName("newName");
      userFacade.updateUser(organizationId1, userId, toUpdate);

      User storedNotExisting = getUser(organizationId1, USER1);
      assertThat(storedNotExisting).isNull();

      User storedExisting = getUser(organizationId1, USER3);
      assertThat(storedExisting).isNotNull();
      assertThat(storedExisting.getName()).isEqualTo("newName");
   }

   @Test
   public void testUpdateGroups() {
      String userId = createUser(organizationId1, USER1).getId();

      Set<String> newGroups = new HashSet<>(Arrays.asList("g1", "g2"));
      User toUpdate = prepareUser(USER1);
      toUpdate.setGroups(newGroups);

      userFacade.updateUser(organizationId1, userId, toUpdate);

      User stored = getUser(organizationId1, USER1);
      assertThat(stored).isNotNull();
      assertThat(stored.getName()).isEqualTo(USER1);
      assertThat(stored.getGroups()).isEqualTo(newGroups);
   }

   @Test
   public void testUpdateUserNotPermission() {
      String userId = createUser(organizationId1, USER1).getId();

      assertThatThrownBy(() -> userFacade.updateUser(organizationIdNotPermission,userId, prepareUser(USER3)))
            .isInstanceOf(NoPermissionException.class);
   }

   @Test
   public void testDeleteUser() {
      String id = userFacade.createUser(organizationId1, prepareUser(USER1)).getId();
      userFacade.createUser(organizationId1, prepareUser(USER2));
      userFacade.createUser(organizationId2, prepareUser(USER1));
      userFacade.createUser(organizationId2, prepareUser(USER3));

      assertThat( userDao.getUserByEmail(organizationId1, USER1)).isPresent();
      assertThat( userDao.getUserByEmail(organizationId2, USER1)).isPresent();

      userFacade.deleteUser(organizationId1, id);
      assertThat( userDao.getUserByEmail(organizationId1, USER1)).isNotPresent();
      assertThat( userDao.getUserByEmail(organizationId2, USER1)).isPresent();

      userFacade.deleteUser(organizationId2, id);
      assertThat( userDao.getUserByEmail(organizationId1, USER1)).isNotPresent();
      assertThat( userDao.getUserByEmail(organizationId2, USER1)).isNotPresent();
   }

   @Test
   public void testDeleteUserNoPermission() {
      String id = userFacade.createUser(organizationId1, prepareUser(USER1)).getId();

      assertThatThrownBy(() -> userFacade.deleteUser(organizationIdNotPermission, id))
            .isInstanceOf(NoPermissionException.class);
   }

   @Test
   public void testGetAllUsers() {
      String id = userFacade.createUser(organizationId1, prepareUser(USER1)).getId();
      String id2 = userFacade.createUser(organizationId1, prepareUser(USER2)).getId();
      userFacade.createUser(organizationId2, prepareUser(USER1));
      String id3 = userFacade.createUser(organizationId2, prepareUser(USER3)).getId();

      List<User> users = userFacade.getUsers(organizationId1);
      assertThat(users).extracting(User::getId).containsOnly(id, id2);

      users = userFacade.getUsers(organizationId2);
      assertThat(users).extracting(User::getId).containsOnly(id, id3);
   }

   @Test
   public void testGetAllUsersNoPermission() {
      assertThatThrownBy(() -> userFacade.getUsers(organizationIdNotPermission))
            .isInstanceOf(NoPermissionException.class);
   }

   private User createUser(String organizationId, String user){
      return userDao.createUser(organizationId, prepareUser(user));
   }

   private User getUser(String organizationId, String user){
      Optional<User> userOptional = userDao.getAllUsers(organizationId).stream().filter(u -> u.getEmail().equals(user)).findFirst();
      return userOptional.orElse(null);
   }

   private User prepareUser(String user) {
      User u = new User(user);
      u.setName(user);
      u.setGroups(GROUPS);
      return u;
   }
}
