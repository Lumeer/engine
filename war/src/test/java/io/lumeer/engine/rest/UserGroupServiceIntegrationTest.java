package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.UserGroupFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@RunWith(Arquillian.class)
public class UserGroupServiceIntegrationTest extends IntegrationTestBase {
   @Inject
   UserGroupFacade userGroupFacade;

   @Inject
   OrganizationFacade organizationFacade;

   @Inject
   @SystemDataStorage
   DataStorage dataStorage;

   @Inject
   DataStorageDialect dataStorageDialect;

   private static final String organization = "LMR";
   private final String TARGET_URI = "http://localhost:8080";
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/" + organization + "/groups/";

   @Before
   public void init() throws Exception {
      dataStorage.dropManyDocuments(LumeerConst.UserGroup.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropManyDocuments(LumeerConst.Group.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      organizationFacade.dropOrganization(organization);
      organizationFacade.createOrganization(organization, "Lumeer");
   }

   @Test
   public void testRegister() throws Exception {
      assertThat(userGroupFacade).isNotNull();
   }

   @Test
   public void testAddUser() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      String group = "AddUser";
      userGroupFacade.addGroups(organization, group);

      client.target(TARGET_URI).path(PATH_PREFIX + "users/" + user)
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(Arrays.asList(group), MediaType.APPLICATION_JSON)).invoke();

      assertThat(userGroupFacade.getUsers(organization)).containsOnly(user);
      assertThat(userGroupFacade.getGroupsOfUser(organization, user)).containsOnly(group);
   }

   @Test
   public void testAddGroups() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String group1 = "AddGroup1";
      String group2 = "AddGroup2";

      client.target(TARGET_URI).path(PATH_PREFIX)
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(Arrays.asList(group1, group2), MediaType.APPLICATION_JSON)).invoke();

      List<String> groupsFromFacade = userGroupFacade.getGroups(organization);

      assertThat(groupsFromFacade).containsOnly(group1, group2);
   }

   @Test
   public void testAddUserToGroups() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      userGroupFacade.addUser(organization, user);
      String group1 = "AddUserToGroup1";
      String group2 = "AddUserToGroup2";
      userGroupFacade.addGroups(organization, group1, group2);

      client.target(TARGET_URI).path(PATH_PREFIX + "users/" + user)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(Arrays.asList(group1, group2), MediaType.APPLICATION_JSON)).invoke();

      List<String> groupsFromFacade = userGroupFacade.getGroupsOfUser(organization, user);

      assertThat(groupsFromFacade).containsOnly(group1, group2);
   }

   @Test
   public void testRemoveUser() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      userGroupFacade.addUser(organization, user);

      client.target(TARGET_URI).path(PATH_PREFIX + "users/" + user)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      assertThat(userGroupFacade.getUsers(organization)).isEmpty();
   }

   @Test
   public void testRemoveUserFromGroups() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      String group1 = "RemoveUserFromGroups1";
      String group2 = "RemoveUserFromGroups2";
      userGroupFacade.addGroups(organization, group1, group2);
      userGroupFacade.addUser(organization, user, group1, group2);

      client.target(TARGET_URI).path(PATH_PREFIX + "users/" + user + "/groups")
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(Arrays.asList(group1), MediaType.APPLICATION_JSON)).invoke();

      assertThat(userGroupFacade.getGroupsOfUser(organization, user)).containsOnly(group2);
   }

   // TODO: reading map from response does not work (JsonMappingException) and I don't know why
   @Test
   public void testGetUsersAndGroups() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "users").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Map<String, List<String>> usersAndGroups = response.readEntity(Map.class); // does not work
      // Map<String, List<String>> usersAndGroups = response.readEntity(new GenericType<Map<String, List<String>>>() {}); // does not work too
      assertThat(usersAndGroups).isEmpty();

      String user1 = "user1";
      String user2 = "user2";
      String group1 = "GetUsersGroups1";
      String group2 = "GetUsersGroups2";
      userGroupFacade.addGroups(organization, group1, group2);
      userGroupFacade.addUser(organization, user1, group1, group2);
      userGroupFacade.addUser(organization, user2, group1);

      response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      usersAndGroups = response.readEntity(Map.class);

      assertThat(usersAndGroups).containsOnlyKeys(user1, user2);
      assertThat(usersAndGroups.get(user1)).containsOnly(group1, group2);
      assertThat(usersAndGroups.get(user2)).containsOnly(group1);
   }

   @Test
   public void testGetGroups() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<String> groups = response.readEntity(List.class);
      assertThat(groups).isEmpty();

      String group1 = "GetGroup1";
      String group2 = "GetGroup2";
      userGroupFacade.addGroups(organization, group1, group2);

      response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      groups = response.readEntity(List.class);

      assertThat(groups).containsOnly(group1, group2);
   }

   @Test
   public void testRemoveGroups() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String group1 = "RemoveGroup1";
      String group2 = "RemoveGroup2";
      userGroupFacade.addGroups(organization, group1, group2);

      client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(Arrays.asList(group1), MediaType.APPLICATION_JSON)).invoke();

      List<String> groups = userGroupFacade.getGroups(organization);
      assertThat(groups).containsOnly(group2);

      client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(Arrays.asList(group2), MediaType.APPLICATION_JSON)).invoke();

      groups = userGroupFacade.getGroups(organization);
      assertThat(groups).isEmpty();
   }

   @Test
   public void testGetGroupsOfUser() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      String group1 = "GetGroupsOfUser1";
      String group2 = "GetGroupsOfUser2";
      userGroupFacade.addGroups(organization, group1, group2);
      userGroupFacade.addUser(organization, user, group1, group2);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + "users/" + user + "/groups/")
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<String> groups = response.readEntity(List.class);

      assertThat(groups).containsOnly(group1, group2);
   }

   @Test
   public void testGetUsersInGroup() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String group = "GetUsersInGroup";
      String user1 = "user 1";
      String user2 = "user 2";

      userGroupFacade.addUser(organization, user1, group);
      userGroupFacade.addUser(organization, user2, group);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + "groups/" + group)
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<String> users = response.readEntity(List.class);

      assertThat(users).containsOnly(user1, user2);

   }

}