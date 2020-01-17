package io.lumeer.remote.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.storage.api.dao.GroupDao;
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
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class GroupServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String GROUP1 = "group1";
   private static final String GROUP2 = "group2";
   private static final String GROUP3 = "group3";

   private String urlPrefix;

   private Organization organization;

   @Inject
   private GroupDao groupDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private UserDao userDao;

   @Before
   public void configure() {
      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      Organization organization1 = new Organization();
      organization1.setCode("LMR");
      organization1.setPermissions(new Permissions());
      organization1.getPermissions().updateUserPermissions(new Permission(createdUser.getId(), Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      organization = organizationDao.createOrganization(organization1);

      groupDao.createRepository(organization);
      groupDao.setOrganization(organization);

      this.urlPrefix = organizationPath(organization) + "groups";
   }

   @Test
   public void testCreateUser() {
      Group group = new Group(GROUP1);

      Entity entity = Entity.json(group);
      Response response = client.target(urlPrefix)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Group returnedGroup = response.readEntity(Group.class);
      assertThat(returnedGroup).isNotNull();

      Group storedGroup = getGroup(GROUP1);

      assertThat(storedGroup).isNotNull();
      assertThat(storedGroup.getId()).isEqualTo(returnedGroup.getId());
      assertThat(storedGroup.getName()).isEqualTo(GROUP1);
   }

   @Test
   public void testUpdateUser() {
      groupDao.createGroup(new Group(GROUP1));
      Group storedGroup = getGroup(GROUP1);
      assertThat(storedGroup).isNotNull();

      Group updateGroup = new Group(GROUP2);
      Entity entity = Entity.json(updateGroup);
      Response response = client.target(urlPrefix).path(storedGroup.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      storedGroup = getGroup(GROUP1);
      assertThat(storedGroup).isNull();

      storedGroup = getGroup(GROUP2);
      assertThat(storedGroup).isNotNull();
   }

   @Test
   public void testDeleteUser() {
      groupDao.createGroup(new Group(GROUP1));
      Group storedGroup = getGroup(GROUP1);
      assertThat(storedGroup).isNotNull();

      Response response = client.target(urlPrefix).path(storedGroup.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      storedGroup = getGroup(GROUP1);
      assertThat(storedGroup).isNull();
   }

   @Test
   public void testGetUsers() {
      groupDao.createGroup(new Group(GROUP1));
      groupDao.createGroup(new Group(GROUP3));

      Response response = client.target(urlPrefix)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<Group> groups = response.readEntity(new GenericType<List<Group>>() {
      });

      assertThat(groups).extracting(Group::getName).containsOnly(GROUP1, GROUP3);
   }

   private Group getGroup(String group) {
      Optional<Group> groupOptional = groupDao.getAllGroups().stream().filter(g -> g.getName().equals(group)).findFirst();
      return groupOptional.orElse(null);
   }

}
