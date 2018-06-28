package io.lumeer.remote.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class UserServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String USER1 = "user1@gmail.com";
   private static final String USER2 = "user2@gmail.com";
   private static final String USER3 = "user3@gmail.com";

   private static final Set<String> GROUPS = new HashSet<>(Arrays.asList("group1", "group2", "group3"));

   private static final String URL_PREFIX = "http://localhost:8080/" + PATH_CONTEXT + "/rest/organizations/";

   private String organizationId1;
   private String organizationId2;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Before
   public void configure() {
      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      JsonOrganization organization1 = new JsonOrganization();
      organization1.setCode("LMR");
      organization1.setPermissions(new JsonPermissions());
      organization1.getPermissions().updateUserPermissions(new JsonPermission(createdUser.getId(), Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      organizationId1 = organizationDao.createOrganization(organization1).getId();

      JsonOrganization organization2 = new JsonOrganization();
      organization2.setCode("MRL");
      organization2.setPermissions(new JsonPermissions());
      organization2.getPermissions().updateUserPermissions(new JsonPermission(createdUser.getId(), Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      organizationId2 = organizationDao.createOrganization(organization2).getId();
   }

   @Test
   public void testCreateUser() {
      User user = prepareUser(organizationId1, USER1);

      Entity entity = Entity.json(user);
      Response response = client.target(getPath(organizationId1))
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      User returnedUser = response.readEntity(User.class);
      assertThat(returnedUser).isNotNull();

      User storedUser = getUser(organizationId1, USER1);

      assertThat(storedUser).isNotNull();
      assertThat(storedUser.getId()).isEqualTo(returnedUser.getId());
      assertThat(storedUser.getName()).isEqualTo(USER1);
      assertThat(storedUser.getEmail()).isEqualTo(USER1);
      assertThat(storedUser.getGroups().get(organizationId1)).isEqualTo(GROUPS);

   }

   @Test
   public void testUpdateUser() {
      createUser(organizationId1, USER1);
      User storedUser = getUser(organizationId1, USER1);
      assertThat(storedUser).isNotNull();

      User updateUser = prepareUser(organizationId1, USER2);
      Entity entity = Entity.json(updateUser);
      Response response = client.target(getPath(organizationId1)).path(storedUser.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      storedUser = getUser(organizationId1, USER1);
      assertThat(storedUser).isNull();

      storedUser = getUser(organizationId1, USER2);
      assertThat(storedUser).isNotNull();
   }

   @Test
   public void testDeleteUser() {
      createUser(organizationId1, USER1);
      User storedUser = getUser(organizationId1, USER1);
      assertThat(storedUser).isNotNull();

      Response response = client.target(getPath(organizationId1)).path(storedUser.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      storedUser = getUser(organizationId1, USER1);
      assertThat(storedUser).isNull();
   }

   @Test
   public void testGetUsers() {
      createUser(organizationId1, USER1);
      createUser(organizationId1, USER2);
      createUser(organizationId2, USER1);
      createUser(organizationId2, USER3);

      Response response = client.target(getPath(organizationId2))
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<User> users = response.readEntity(new GenericType<List<User>>() {
      });

      assertThat(users).extracting(User::getEmail).containsOnly(USER1, USER3);
   }

   private String getPath(String organizationId) {
      return URL_PREFIX + organizationId + "/users";
   }

   private User createUser(String organizationId, String user) {
      return userDao.createUser(prepareUser( organizationId, user));
   }

   private User getUser(String organizationId, String user) {
      Optional<User> userOptional = userDao.getAllUsers(organizationId).stream().filter(u -> u.getEmail().equals(user)).findFirst();
      return userOptional.orElse(null);
   }

   private User prepareUser(String organizationId, String user) {
      User u = new User(user);
      u.setName(user);
      u.setGroups(Collections.singletonMap(organizationId,GROUPS));
      return u;
   }

}
