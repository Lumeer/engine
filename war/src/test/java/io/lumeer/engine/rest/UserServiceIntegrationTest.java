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
public class UserServiceIntegrationTest extends IntegrationTestBase {

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
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/" + organization + "/users/";

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
   public void getUsersAndGroups() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Map<String, List<String>> usersAndGroups = response.readEntity(Map.class);

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
   public void addUser() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      String group = "AddUser";
      userGroupFacade.addGroups(organization, group);

      client.target(TARGET_URI).path(PATH_PREFIX + user)
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(Arrays.asList(group), MediaType.APPLICATION_JSON)).invoke();

      assertThat(userGroupFacade.getUsers(organization)).containsOnly(user);
      assertThat(userGroupFacade.getGroupsOfUser(organization, user)).containsOnly(group);
   }

   @Test
   public void removeUser() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      userGroupFacade.addUser(organization, user);

      client.target(TARGET_URI).path(PATH_PREFIX + user)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      assertThat(userGroupFacade.getUsers(organization)).isEmpty();
   }

   @Test
   public void getGroupsOfUser() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String user = "user";
      String group1 = "GetGroupsOfUser1";
      String group2 = "GetGroupsOfUser2";
      userGroupFacade.addGroups(organization, group1, group2);
      userGroupFacade.addUser(organization, user, group1, group2);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + user + "/groups/")
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<String> groups = response.readEntity(List.class);

      assertThat(groups).containsOnly(group1, group2);
   }

}