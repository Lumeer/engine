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
package io.lumeer.engine.rest.permissions;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.exception.UserAlreadyExistsException;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.controller.UserGroupFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Ignore
@RunWith(Arquillian.class)
public class OrganizationServicePermissionsIntegrationTest extends IntegrationTestBase {

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   UserGroupFacade userGroupFacade;

   @Before
   public void clearCollectionOfOrganizationsInSystemDB() {
      dataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   private final String TARGET_URI = "http://localhost:8080";
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/";

   @Test
   public void testGetOrganizationNoRole() throws UserAlreadyExistsException {
      String name = "TestGetOrganizationNoRole";
      String code = "TestGetOrganizationNoRole_id";
      organizationFacade.createOrganization(new Organization(code, name));

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup1";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);

      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isTrue();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isTrue();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isTrue();
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isFalse();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
      client.close();
      final Client client1 = ClientBuilder.newBuilder().build();
      response = client1.target(TARGET_URI).path(PATH_PREFIX + code + "/name").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
      client1.close();
   }

   @Test
   public void testGetOrganizationReadRole() throws UserAlreadyExistsException {
      String name = "TestGetOrganizationReadRole";
      String code = "TestGetOrganizationReadRole_id";
      organizationFacade.createOrganization(new Organization(code, name));

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup2";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);

      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isFalse();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      client.close();
      final Client client1 = ClientBuilder.newBuilder().build();
      response = client1.target(TARGET_URI).path(PATH_PREFIX + code + "/name").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      client1.close();
   }

   @Test
   public void testGetOrganizationsSomeReadRoles() throws UserAlreadyExistsException {
      String name1 = "testGetOrganizationsSomeReadRoles1";
      String name2 = "testGetOrganizationsSomeReadRoles2";
      String code1 = "testGetOrganizationsSomeReadRoles1_id";
      String code2 = "testGetOrganizationsSomeReadRoles2_id";
      String name3 = "testGetOrganizationsSomeReadRoles3";
      String name4 = "testGetOrganizationsSomeReadRoles4";
      String code3 = "testGetOrganizationsSomeReadRoles3_id";
      String code4 = "testGetOrganizationsSomeReadRoles4_id";
      String nameRoleFromGroup = "testGetOrganizationsSomeReadRolesGroup";
      String codeRoleFromGroup = "testGetOrganizationsSomeReadRolesGroup_code";

      List<String> names = Arrays.asList(name1, name2, name3, name4);
      List<String> codes = Arrays.asList(code1, code2, code3, code4);

      for (int i = 0; i < codes.size(); i++) {
         organizationFacade.createOrganization(new Organization(codes.get(i), names.get(i)));
      }

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroupSomeReadRoles";

      for (int i = 0; i < codes.size(); i++) {
         userGroupFacade.addGroups(codes.get(i), groupName);
         userGroupFacade.addUser(codes.get(i), userEmail, groupName);
      }

      organizationFacade.createOrganization(new Organization(codeRoleFromGroup, nameRoleFromGroup));
      userGroupFacade.addGroups(codeRoleFromGroup, groupName);
      userGroupFacade.addUser(codeRoleFromGroup, userEmail, groupName);

      securityFacade.removeOrganizationUsersRole(codeRoleFromGroup, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(codeRoleFromGroup, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(codeRoleFromGroup, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      securityFacade.addOrganizationGroupsRole(codeRoleFromGroup, Collections.singletonList(groupName), LumeerConst.Security.ROLE_READ);
      assertThat(securityFacade.hasOrganizationRole(codeRoleFromGroup, LumeerConst.Security.ROLE_READ)).isTrue();

      securityFacade.removeOrganizationUsersRole(code1, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(code1, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code1, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      securityFacade.removeOrganizationUsersRole(code3, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(code3, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code3, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code2, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasOrganizationRole(code4, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasOrganizationRole(code1, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code3, LumeerConst.Security.ROLE_READ)).isFalse();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<Organization> organizations = response.readEntity(new GenericType<List<Organization>>(List.class) {
      });

      assertThat(organizations).extracting("code").containsOnly(code2, code4, codeRoleFromGroup);
      assertThat(organizations).extracting("name").containsOnly(name2, name4, nameRoleFromGroup);
      client.close();
   }

   @Test
   public void testUpdateOrganizationNoRole() throws UserAlreadyExistsException {
      String name = "TestUpdateOrganizationNoRole";
      String code = "TestUpdateOrganizationNoRole_id";
      String newCode = "NewTestUpdateOrganizationNoRole_id";
      organizationFacade.createOrganization(new Organization(code, name));

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup3";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);

      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isFalse();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/code/" + newCode)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
      client.close();
      final Client client1 = ClientBuilder.newBuilder().build();
      String newOrgName = "NewName1";
      response = client1.target(TARGET_URI)
                        .path(PATH_PREFIX + code)
                        .request(MediaType.APPLICATION_JSON)
                        .buildPut(Entity.json(new Organization(code, newOrgName)))
                        .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
      client1.close();
   }

   @Test
   public void testUpdateOrganizationManageRole() throws UserAlreadyExistsException {
      String name = "TestUpdateOrganizationManageRole";
      String code = "TestUpdateOrganizationManageRole_id";
      String newCode = "NewTestUpdateOrganizationManageRole_id";
      organizationFacade.createOrganization(new Organization(code, name));

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup4";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isTrue();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/code/" + newCode)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
                                .invoke();
      //204 No Content The request has been successfully processed, but is not returning any content
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
      client.close();
      final Client client1 = ClientBuilder.newBuilder().build();
      String newOrgName = "NewName2";
      response = client1.target(TARGET_URI)
                        .path(PATH_PREFIX + newCode)
                        .request(MediaType.APPLICATION_JSON)
                        .buildPut(Entity.json(new Organization(newCode, newOrgName)))
                        .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
      client1.close();
   }

   @Test
   public void testCreateProjectInOrganizationNoRole() throws UserAlreadyExistsException {
      final String project = "project1";
      final String projectName = "Project1Name";
      final String code = "TestCreateProjectInOrganizationNoRole_id";
      final String name = "TestCreateProjectInOrganizationNoRole";
      organizationFacade.createOrganization(new Organization(code, name));

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup5";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);

      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isFalse();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(projectsPathPrefix(code))
                                .request()
                                .buildPost(Entity.json(new Project(project, projectName)))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
      client.close();
   }

   @Test
   public void testCreateProjectInOrganizationWriteRole() throws UserAlreadyExistsException {
      final String project = "project2";
      final String projectName = "Project2Name";
      final String code = "TestCreateProjectInOrganizationWriteRole_id";
      final String name = "TestCreateProjectInOrganizationWriteRole";
      organizationFacade.createOrganization(new Organization(code, name));

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup6";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);

      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isTrue();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(projectsPathPrefix(code))
                                .request()
                                .buildPost(Entity.json(new Project(project, projectName)))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
      client.close();
   }

   public void testGetOrganizationGroupReadRole() throws UserAlreadyExistsException {
      String name = "testGetOrganizationGroupReadRole";
      String code = "testGetOrganizationGroupReadRole_code";

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup7";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);
      //This adds all roles (read, manage, write) for currently logged in user.
      organizationFacade.createOrganization(new Organization(code, name));

      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isFalse();
      // Add role by adding role to the whole group that the currently logged in user belongs to.
      securityFacade.addOrganizationGroupsRole(code, Collections.singletonList(groupName), LumeerConst.Security.ROLE_READ);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isFalse();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      client.close();
      final Client client1 = ClientBuilder.newBuilder().build();
      response = client1.target(TARGET_URI).path(PATH_PREFIX + code + "/name").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      client1.close();
   }

   public void testUpdateOrganizationGroupManageRole() throws UserAlreadyExistsException {
      String name = "TestUpdateOrganizationManageRole";
      String code = "TestUpdateOrganizationManageRole_id";
      String newCode = "NewTestUpdateOrganizationManageRole_id";
      organizationFacade.createOrganization(new Organization(code, name));

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup8";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);

      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isFalse();
      securityFacade.addOrganizationGroupsRole(code, Collections.singletonList(groupName), LumeerConst.Security.ROLE_READ);
      securityFacade.addOrganizationGroupsRole(code, Collections.singletonList(groupName), LumeerConst.Security.ROLE_MANAGE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isTrue();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/code/" + newCode)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
      client.close();
      final Client client1 = ClientBuilder.newBuilder().build();
      String newOrgName = "NewName2";
      response = client1.target(TARGET_URI)
                        .path(PATH_PREFIX + newCode)
                        .request(MediaType.APPLICATION_JSON)
                        .buildPut(Entity.json(new Organization(newCode, newOrgName)))
                        .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
      client1.close();
   }

   public void testCreateProjectInOrganizationGroupWriteRole() throws UserAlreadyExistsException {
      final String project = "project3";
      final String projectName = "Project3Name";
      final String code = "TestCreateProjectInOrganizationGroupWriteRole_id";
      final String name = "TestCreateProjectInOrganizationGroupWriteRole";
      organizationFacade.createOrganization(new Organization(code, name));

      String userEmail = userFacade.getUserEmail();
      String groupName = "TestGroup9";
      userGroupFacade.addGroups(code, groupName);
      userGroupFacade.addUser(code, userEmail, groupName);

      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.removeOrganizationUsersRole(code, Arrays.asList(userEmail), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_MANAGE)).isFalse();
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isFalse();
      securityFacade.addOrganizationGroupsRole(code, Collections.singletonList(groupName), LumeerConst.Security.ROLE_READ);
      securityFacade.addOrganizationGroupsRole(code, Collections.singletonList(groupName), LumeerConst.Security.ROLE_MANAGE);
      securityFacade.addOrganizationGroupsRole(code, Collections.singletonList(groupName), LumeerConst.Security.ROLE_WRITE);
      assertThat(securityFacade.hasOrganizationRole(code, LumeerConst.Security.ROLE_WRITE)).isTrue();

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(projectsPathPrefix(code))
                                .request()
                                .buildPost(Entity.json(new Project(project, projectName)))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
      client.close();
   }

   private String projectsPathPrefix(String organizationCode) {
      return PATH_CONTEXT + "/rest/organizations/" + organizationCode + "/projects/";
   }
}
