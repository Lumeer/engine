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

import java.util.List;
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
public class GroupServiceIntegrationTest extends IntegrationTestBase {

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
   public void getGroups() throws Exception {
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
   public void addGroup() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String group = "AddGroup";

      client.target(TARGET_URI).path(PATH_PREFIX + group)
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();

      List<String> groupsFromFacade = userGroupFacade.getGroups(organization);

      assertThat(groupsFromFacade).containsOnly(group);
   }

   @Test
   public void removeGroup() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String group1 = "RemoveGroup1";
      String group2 = "RemoveGroup2";
      userGroupFacade.addGroups(organization, group1, group2);

      client.target(TARGET_URI).path(PATH_PREFIX + group1).request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      List<String> groups = userGroupFacade.getGroups(organization);
      assertThat(groups).containsOnly(group2);

      client.target(TARGET_URI).path(PATH_PREFIX + group2).request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      groups = userGroupFacade.getGroups(organization);
      assertThat(groups).isEmpty();
   }

   @Test
   public void getUsersInGroup() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String group = "GetUsersInGroup";
      String user1 = "user 1";
      String user2 = "user 2";

      userGroupFacade.addUser(organization, user1, group);
      userGroupFacade.addUser(organization, user2, group);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + group + "/users")
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<String> users = response.readEntity(List.class);

      assertThat(users).containsOnly(user1, user2);
   }

   @Test
   public void addUserToGroup() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      userGroupFacade.addUser(organization, user);
      String group = "AddUserToGroup";
      userGroupFacade.addGroups(organization, group);

      client.target(TARGET_URI).path(PATH_PREFIX + group + "/users/" + user)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(group, MediaType.APPLICATION_JSON)).invoke();

      List<String> groupsFromFacade = userGroupFacade.getGroupsOfUser(organization, user);

      assertThat(groupsFromFacade).containsOnly(group);
   }

   @Test
   public void removeUserFromGroup() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      String group1 = "RemoveUserFromGroups1";
      String group2 = "RemoveUserFromGroups2";
      userGroupFacade.addGroups(organization, group1, group2);
      userGroupFacade.addUser(organization, user, group1, group2);

      client.target(TARGET_URI).path(PATH_PREFIX + group1 + "/users/" + user)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      assertThat(userGroupFacade.getGroupsOfUser(organization, user)).containsOnly(group2);
   }

}