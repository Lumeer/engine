/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.controller.OrganizationFacade;

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
public class OrganizationServiceIntegrationTest extends IntegrationTestBase {

   private final String TARGET_URI = "http://localhost:8080";
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/";

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Before
   public void init() {
      if (dataStorage.hasCollection(LumeerConst.Organization.COLLECTION_NAME)) {
         dataStorage.dropCollection(LumeerConst.Organization.COLLECTION_NAME);
      }
   }

   @Test
   public void testRegister() throws Exception {
      assertThat(organizationFacade).isNotNull();
   }

   @Test
   public void testGetOrganizations() throws Exception {
      String org1 = "GetOrganizations1";
      String org2 = "GetOrganizations2";
      String id1 = "GetOrganizations1_id";
      String id2 = "GetOrganizations2_id";
      organizationFacade.createOrganization(id1, org1);
      organizationFacade.createOrganization(id2, org2);

      Map<String, String> organizationsFromFacade = organizationFacade.readOrganizationsMap();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();

      Map<String, String> organizations = response.readEntity(Map.class);

      assertThat(organizations).isEqualTo(organizationsFromFacade);
      assertThat(organizations).containsEntry(id1, org1);
      assertThat(organizations).containsEntry(id2, org2);
   }

   @Test
   public void testGetOrganizationName() throws Exception {
      String org = "GetOrganizationName";
      String id = "GetOrganizationName_id";
      organizationFacade.createOrganization(id, org);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet()
                                .invoke();

      String nameFromResponse = response.readEntity(String.class);

      assertThat(nameFromResponse).isEqualTo(org);
   }

   @Test
   public void testCreateOrganization() throws Exception {
      String org = "CreateOrganization";
      String id = "CreateOrganization_id";
      organizationFacade.createOrganization(id, org);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/name/" + org)
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(organizationFacade.readOrganizationsMap()).containsEntry(id, org);
   }

   @Test
   public void testRenameOrganization() throws Exception {
      String org = "RenameOrganization";
      String newName = "RenameOrganizationNew";
      String id = "RenameOrganization_id";
      organizationFacade.createOrganization(id, org);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/name/" + newName)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(organizationFacade.readOrganizationsMap()).containsEntry(id, newName);
   }

   @Test
   public void testDropOrganization() throws Exception {
      String org = "DropOrganization";
      String id = "DropOrganization_id";
      organizationFacade.createOrganization(id, org);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete()
            .invoke();

      assertThat(organizationFacade.readOrganizationsMap()).doesNotContainEntry(id, org);
   }

   @Test
   public void testReadMetadata() throws Exception {
      String org = "ReadMetadata";
      String id = "ReadMetadata_id";
      organizationFacade.createOrganization(id, org);

      String name = "attribute";

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/meta/" + name)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      String metaValue = response.readEntity(String.class);
      assertThat(metaValue).isNull();

      String value = "value";
      organizationFacade.updateOrganizationMetadata(id, name, value);

      response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/meta/" + name)
                       .request(MediaType.APPLICATION_JSON)
                       .buildGet().invoke();
      metaValue = response.readEntity(String.class);
      assertThat(metaValue).isEqualTo(value);
   }

   @Test
   public void testUpdateMetadata() throws Exception {
      String org = "UpdateMetadata";
      String id = "UpdateMetadata_id";
      organizationFacade.createOrganization(id, org);

      String name = "attribute";
      String value = "value";

      // add new value
      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/meta/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(value, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationMetadata(id, name)).isEqualTo(value);

      String newValue = "new value";

      // update existing value
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/meta/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(newValue, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationMetadata(id, name)).isEqualTo(newValue);
   }

   @Test
   public void testDropMetadata() throws Exception {
      String org = "DropMetadata";
      String id = "DropMetadata_id";
      organizationFacade.createOrganization(id, org);

      String name = "attribute";
      String value = "value";
      organizationFacade.updateOrganizationMetadata(id, name, value);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/meta/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      assertThat(organizationFacade.readOrganizationMetadata(id, name)).isNull();
   }

   @Test
   public void testReadInfo() throws Exception {
      String org = "ReadInfo";
      String id = "ReadInfo_id";
      organizationFacade.createOrganization(id, org);

      String name = "attribute";

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/data/" + name)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      String dataValue = response.readEntity(String.class);
      assertThat(dataValue).isNull();

      String value = "value";
      organizationFacade.updateOrganizationInfoData(id, name, value);

      response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/data/" + name)
                       .request(MediaType.APPLICATION_JSON)
                       .buildGet().invoke();
      dataValue = response.readEntity(String.class);
      assertThat(dataValue).isEqualTo(value);
   }

   @Test
   public void testReadInfoDocument() throws Exception {
      String org = "ReadInfoDoc";
      String id = "ReadInfoDoc_id";
      organizationFacade.createOrganization(id, org);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/data")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      DataDocument dataValue = response.readEntity(DataDocument.class);
      assertThat(dataValue).isEmpty();

      String name1 = "attribute1";
      String name2 = "attribute2";
      String value1 = "value1";
      String value2 = "value2";
      organizationFacade.updateOrganizationInfoData(id, new DataDocument().append(name1, value1).append(name2, value2));

      response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/data")
                       .request(MediaType.APPLICATION_JSON)
                       .buildGet().invoke();
      dataValue = response.readEntity(DataDocument.class);
      assertThat(dataValue).containsEntry(name1, value1);
      assertThat(dataValue).containsEntry(name2, value2);
   }

   @Test
   public void testUpdateInfo() throws Exception {
      String org = "UpdateInfo";
      String id = "UpdateInfo_id";
      organizationFacade.createOrganization(id, org);

      String name = "attribute";
      String value = "value";

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/data/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(value, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationInfoData(id, name)).isEqualTo(value);

      String newValue = "new value";
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/data/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(newValue, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationInfoData(id, name)).isEqualTo(newValue);
   }

   @Test
   public void testDropInfo() throws Exception {
      String org = "DropInfo";
      String id = "DropInfo_id";
      organizationFacade.createOrganization(id, org);

      String name = "attribute";
      String value = "value";
      organizationFacade.updateOrganizationInfoData(id, name, value);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/data/" + name)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();

      assertThat(organizationFacade.readOrganizationInfoData(id, name)).isNull();
   }

   @Test
   public void testResetInfo() throws Exception {
      String org = "ResetInfo";
      String id = "ResetInfo_id";
      organizationFacade.createOrganization(id, org);

      String name = "attribute";
      String value = "value";
      organizationFacade.updateOrganizationInfoData(id, name, value);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/data/")
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationInfoData(id)).isEmpty();
   }

   @Test
   public void testReadUserOrganizations() throws Exception {
      String org1 = "ReadUserOrgs1";
      String id1 = "ReadUserOrgs1_id";
      organizationFacade.createOrganization(id1, org1);

      String org2 = "ReadUserOrgs2";
      String id2 = "ReadUserOrgs2_id";
      organizationFacade.createOrganization(id2, org2);

      String user = "test user";
      organizationFacade.addUserToOrganization(id1, user);
      organizationFacade.addUserToOrganization(id2, user);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "users/" + user)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      List<String> organizations = response.readEntity(List.class);
      assertThat(organizations).containsOnly(id1, id2);
   }

   @Test
   public void testReadOrganizationUsers() throws Exception {
      String org = "ReadOrgUsers";
      String id = "ReadOrgUsers_id";
      organizationFacade.createOrganization(id, org);

      String user1 = "user 1";
      organizationFacade.addUserToOrganization(id, user1);
      String user2 = "user 2";
      String role1 = "role 1";
      String role2 = "role 2";
      organizationFacade.addUserToOrganization(id, user2, Arrays.asList(role1, role2));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      Map<String, List<String>> users = response.readEntity(Map.class);
      assertThat(users).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).isEmpty();
      assertThat(users.get(user2)).containsOnly(role1, role2);
   }

   @Test
   public void testAddUserWithoutRoles() throws Exception {
      String org = "AddUserWithoutRoles";
      String id = "AddUserWithoutRoles_id";
      organizationFacade.createOrganization(id, org);

      String user = "user";
      String defaultRole = "default role";
      organizationFacade.setDefaultRoles(id, Arrays.asList(defaultRole));

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/users/" + user)
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationUsers(id)).containsOnlyKeys(user);
      assertThat(organizationFacade.readOrganizationUsers(id).get(user)).containsOnly(defaultRole);
   }

   @Test
   public void testAddUserWithRoles() throws Exception {
      String org = "AddUserWithRoles";
      String id = "AddUserWithRoles_id";
      organizationFacade.createOrganization(id, org);

      String user = "user";
      String role1 = "role 1";
      String role2 = "role 2";
      List<String> roles = Arrays.asList(role1, role2);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/users/" + user)
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(roles, MediaType.APPLICATION_JSON))
            .invoke();
      assertThat(organizationFacade.readOrganizationUsers(id)).containsOnlyKeys(user);
      assertThat(organizationFacade.readOrganizationUsers(id).get(user)).containsOnly(role1, role2);
   }

   @Test
   public void testRemoveUserFromOrganization() throws Exception {
      String org = "RemoveUser";
      String id = "RemoveUser_id";
      organizationFacade.createOrganization(id, org);

      String user = "user";
      organizationFacade.addUserToOrganization(id, user);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/users/" + user)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete().invoke();
      assertThat(organizationFacade.readOrganizationUsers(id)).isEmpty();
   }

   @Test
   public void testReadUserRoles() throws Exception {
      String org = "ReadUserRoles";
      String id = "ReadUserRoles_id";
      organizationFacade.createOrganization(id, org);

      String user = "user";
      String role1 = "role 1";
      String role2 = "role 2";
      List<String> roles = Arrays.asList(role1, role2);
      organizationFacade.addUserToOrganization(id, user, roles);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/users/" + user + "/roles")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      List<String> rolesResponse = response.readEntity(List.class);

      assertThat(rolesResponse).containsOnly(role1, role2);
   }

   @Test
   public void testAddRolesToUser() throws Exception {
      String org = "AddUserRoles";
      String id = "AddUserRoles_id";
      organizationFacade.createOrganization(id, org);

      String user = "user";
      String role1 = "role 1";
      String role2 = "role 2";
      List<String> roles = Arrays.asList(role1, role2);
      organizationFacade.addUserToOrganization(id, user);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/users/" + user + "/roles")
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(roles, MediaType.APPLICATION_JSON)).invoke();

      assertThat(organizationFacade.readUserRoles(id, user)).containsOnly(role1, role2);
   }

   @Test
   public void testRemoveRolesFromUser() throws Exception {
      String org = "RemoveUserRoles";
      String id = "RemoveUserRoles_id";
      organizationFacade.createOrganization(id, org);

      String user = "user";
      String role1 = "role 1";
      String role2 = "role 2";
      List<String> roles = Arrays.asList(role1, role2);
      organizationFacade.addUserToOrganization(id, user, roles);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/users/" + user + "/roles")
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(Arrays.asList(role1), MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(organizationFacade.readUserRoles(id, user)).containsOnly(role2);
   }

   @Test
   public void testReadDefaultRoles() throws Exception {
      String org = "ReadDefaultRoles";
      String id = "ReadDefaultRoles_id";
      organizationFacade.createOrganization(id, org);

      String role1 = "default role 1";
      String role2 = "default role 2";
      List<String> roles = Arrays.asList(role1, role2);
      organizationFacade.setDefaultRoles(id, roles);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/roles")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      List<String> rolesResponse = response.readEntity(List.class);

      assertThat(rolesResponse).containsOnly(role1, role2);
   }

   @Test
   public void testSetDefaultRoles() throws Exception {
      String org = "SetDefaultRoles";
      String id = "SetDefaultRoles_id";
      organizationFacade.createOrganization(id, org);

      String role1 = "default role 1";
      String role2 = "default role 2";
      List<String> roles = Arrays.asList(role1, role2);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI).path(PATH_PREFIX + "id/" + id + "/roles")
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(roles, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(organizationFacade.readDefaultRoles(id)).containsOnly(role1, role2);
   }
}
