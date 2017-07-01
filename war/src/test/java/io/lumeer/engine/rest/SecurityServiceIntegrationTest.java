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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.dto.Role;
import io.lumeer.engine.controller.DatabaseInitializer;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.controller.UserGroupFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class SecurityServiceIntegrationTest extends IntegrationTestBase {

   private final String TARGET_URI = "http://localhost:8080";
   private final String PUT_METHOD_NAME = "PUT";

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;
   @Inject
   @SystemDataStorage
   private DataStorage sysDataStorage;
   @Inject
   private DataStorageDialect dataStorageDialect;
   @Inject
   private OrganizationFacade organizationFacade;
   @Inject
   private UserGroupFacade userGroupFacade;
   @Inject
   private SecurityFacade securityFacade;
   @Inject
   private ProjectFacade projectFacade;
   @Inject
   private DatabaseInitializer databaseInitializer;
   @Inject
   private UserFacade userFacade;

   private final String USER_X = "user X";
   private final String USER_Y = "userY";
   private final String GROUP_A = "groupA";
   private final String GROUP_B = "groupB";
   private final String organizationCode = "LMR";
   private final String ORGANIZATION_NAME = "Lumeer";
   private final String projectCode = "PR";
   private final String PROJECT_NAME = "project";
   private final String COLLECTION_NAME = "collection name";
   private final Integer VIEW_ID = 42;

   private final String USERS_QP = "users";
   private final String GROUPS_QP = "groups";

   private final String EMPTY_FILTER = "{}";

   private String user;

   @Before
   public void init() {
      sysDataStorage.dropManyDocuments(
            LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME,
            dataStorageDialect.documentFilter(EMPTY_FILTER));
      dataStorage.dropManyDocuments(
            LumeerConst.Security.ROLES_COLLECTION_NAME,
            dataStorageDialect.documentFilter(EMPTY_FILTER));

      organizationFacade.dropOrganization(organizationCode);
      organizationFacade.createOrganization(new Organization(organizationCode, ORGANIZATION_NAME));
      organizationFacade.setOrganizationCode(organizationCode);

      projectFacade.createProject(new Project(projectCode, PROJECT_NAME));

      databaseInitializer.onCollectionCreated(projectCode, COLLECTION_NAME);
      databaseInitializer.onViewCreated(projectCode, VIEW_ID);
   }

   @Test
   public void testAddOrganizationUsersGroupsRoleBasic() {
      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_MANAGE;
      // PUT
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isTrue();
      assertThat(groups.contains(GROUP_A)).isTrue();
      assertThat(groups.contains(GROUP_B)).isTrue();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
   }

   @Test
   public void testAddOrganizationUsersGroupsRoleNotAllowedRoles() {
      final Client client = ClientBuilder.newBuilder().build();
      final String role = "thisIsNotAllowedRole";

      Response response = client.target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
   }

   @Test
   public void testAddOrganizationUsersGroupsRoleCallTwoTimes() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addOrganizationUserRole(organizationCode, USER_X, role);
      securityFacade.addOrganizationUserRole(organizationCode, USER_Y, role);
      securityFacade.addOrganizationGroupRole(organizationCode, GROUP_A, role);
      securityFacade.addOrganizationGroupRole(organizationCode, GROUP_B, role);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isTrue();
      assertThat(groups.contains(GROUP_A)).isTrue();
      assertThat(groups.contains(GROUP_B)).isTrue();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
   }

   @Test
   public void testGetOrganizationRolesNoQueryParam() {
      final List<Role> before = securityFacade.getOrganizationRoles(organizationCode);

      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_MANAGE;

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      final List<Role> after = securityFacade.getOrganizationRoles(organizationCode);

      assertThat(before.size()).isEqualTo(after.size());
      for (Role r : before) {
         assertThat(after.contains(r));
      }
   }

   @Test
   public void testGetOrganizationRolesBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addOrganizationUserRole(organizationCode, USER_Y, role);
      securityFacade.addOrganizationGroupRole(organizationCode, GROUP_B, role);
      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.ROLES_KEY))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(List.class);
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testGetOrganizationRolesNothingAdded() {
      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.ROLES_KEY))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(List.class);
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testRemoveOrganizationUsersGroupsRoleBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addOrganizationUserRole(organizationCode, USER_X, role);
      securityFacade.addOrganizationUserRole(organizationCode, USER_Y, role);
      securityFacade.addOrganizationGroupRole(organizationCode, GROUP_A, role);
      securityFacade.addOrganizationGroupRole(organizationCode, GROUP_B, role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isFalse();
      assertThat(groups.contains(GROUP_A)).isFalse();
      assertThat(groups.contains(GROUP_B)).isFalse();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      response.close();
   }

   @Test
   public void testRemoveOrganizationUsersGroupsRoleNotExistingUsers() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addOrganizationUserRole(organizationCode, USER_X, role);
      securityFacade.addOrganizationGroupRole(organizationCode, GROUP_A, role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users).contains(USER_X);
      assertThat(groups.contains(GROUP_A)).isFalse();
      assertThat(groups.contains(GROUP_B)).isFalse();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      response.close();
   }

   @Test
   public void testAddProjectUsersGroupsRoleBasic() {
      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_MANAGE;
      // PUT
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isTrue();
      assertThat(groups.contains(GROUP_A)).isTrue();
      assertThat(groups.contains(GROUP_B)).isTrue();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
   }

   @Test
   public void testAddProjectUsersGroupsRoleNotAllowedRole() {
      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_CLONE;

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
   }

   @Test
   public void testAddProjectUsersGroupsRoleCallTwoTimes() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addProjectUserRole(projectCode, USER_X, role);
      securityFacade.addProjectUserRole(projectCode, USER_Y, role);
      securityFacade.addProjectGroupRole(projectCode, GROUP_A, role);
      securityFacade.addProjectGroupRole(projectCode, GROUP_B, role);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isTrue();
      assertThat(groups.contains(GROUP_A)).isTrue();
      assertThat(groups.contains(GROUP_B)).isTrue();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
   }

   @Test
   public void testGetProjectRolesNoQueryParam() {
      final List<Role> before = securityFacade.getProjectRoles(projectCode);

      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_MANAGE;

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      final List<Role> after = securityFacade.getProjectRoles(projectCode);

      assertThat(before.size()).isEqualTo(after.size());
      for (Role r : before) {
         assertThat(after.contains(r));
      }
   }

   @Test
   public void testGetProjectRolesBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addProjectUserRole(projectCode, USER_Y, role);
      securityFacade.addProjectGroupRole(projectCode, GROUP_B, role);
      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.ROLES_KEY))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(List.class);
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testGetProjectRolesNothingAdded() {
      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.ROLES_KEY))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(List.class);
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testRemoveProjectUsersGroupsRoleBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addProjectUserRole(projectCode, USER_X, role);
      securityFacade.addProjectUserRole(projectCode, USER_Y, role);
      securityFacade.addProjectGroupRole(projectCode, GROUP_A, role);
      securityFacade.addProjectGroupRole(projectCode, GROUP_B, role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isFalse();
      assertThat(groups.contains(GROUP_A)).isFalse();
      assertThat(groups.contains(GROUP_B)).isFalse();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      response.close();
   }

   @Test
   public void testRemoveProjectUsersGroupsRoleNotExistingUsers() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addProjectUserRole(projectCode, USER_X, role);
      securityFacade.addProjectGroupRole(projectCode, GROUP_A, role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isFalse();
      assertThat(groups.contains(GROUP_A)).isFalse();
      assertThat(groups.contains(GROUP_B)).isFalse();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      response.close();
   }

   @Test
   public void testAddCollectionUsersGroupsRoleBasic() {
      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_MANAGE;
      // PUT
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.COLLECTION_RESOURCE,
                  COLLECTION_NAME,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      List<Role> fromFacade = securityFacade.getCollectionRoles(projectCode, COLLECTION_NAME);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isTrue();
      assertThat(groups.contains(GROUP_A)).isTrue();
      assertThat(groups.contains(GROUP_B)).isTrue();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
   }

   @Test
   public void testAddCollectionUsersGroupsRoleNotAllowedRole() {
      final Client client = ClientBuilder.newBuilder().build();
      final String role = "thisIsNotAllowedRole";

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.COLLECTION_RESOURCE,
                  COLLECTION_NAME,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
   }

   @Test
   public void testAddCollectionUsersGroupsRoleCallTwoTimes() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addCollectionUserRole(projectCode, COLLECTION_NAME, USER_X, role);
      securityFacade.addCollectionUserRole(projectCode, COLLECTION_NAME, USER_Y, role);
      securityFacade.addCollectionGroupRole(projectCode, COLLECTION_NAME, GROUP_A, role);
      securityFacade.addCollectionGroupRole(projectCode, COLLECTION_NAME, GROUP_B, role);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.COLLECTION_RESOURCE,
                  COLLECTION_NAME,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      List<Role> fromFacade = securityFacade.getCollectionRoles(projectCode, COLLECTION_NAME);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isTrue();
      assertThat(groups.contains(GROUP_A)).isTrue();
      assertThat(groups.contains(GROUP_B)).isTrue();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
   }

   @Test
   public void testGetCollectionRolesNoQueryParam() {
      final List<Role> before = securityFacade.getCollectionRoles(projectCode, COLLECTION_NAME);

      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_MANAGE;

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.COLLECTION_RESOURCE,
                  COLLECTION_NAME,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      final List<Role> after = securityFacade.getCollectionRoles(projectCode, COLLECTION_NAME);

      assertThat(before.size()).isEqualTo(after.size());
      for (Role r : before) {
         assertThat(after.contains(r));
      }
   }

   @Test
   public void testGetCollectionRolesBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addCollectionUserRole(projectCode, COLLECTION_NAME, USER_Y, role);
      securityFacade.addCollectionGroupRole(projectCode, COLLECTION_NAME, GROUP_B, role);
      List<Role> fromFacade = securityFacade.getCollectionRoles(projectCode, COLLECTION_NAME);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.COLLECTION_RESOURCE,
                  COLLECTION_NAME,
                  LumeerConst.Security.ROLES_KEY))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(List.class);
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testGetCollectionRolesNothingAdded() {
      List<Role> fromFacade = securityFacade.getCollectionRoles(projectCode, COLLECTION_NAME);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.COLLECTION_RESOURCE,
                  COLLECTION_NAME,
                  LumeerConst.Security.ROLES_KEY))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(List.class);
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testRemoveCollectionUsersGroupsRoleBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addCollectionUserRole(projectCode, COLLECTION_NAME, USER_X, role);
      securityFacade.addCollectionUserRole(projectCode, COLLECTION_NAME, USER_Y, role);
      securityFacade.addCollectionGroupRole(projectCode, COLLECTION_NAME, GROUP_A, role);
      securityFacade.addCollectionGroupRole(projectCode, COLLECTION_NAME, GROUP_B, role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.COLLECTION_RESOURCE,
                  COLLECTION_NAME,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getCollectionRoles(projectCode, COLLECTION_NAME);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isFalse();
      assertThat(groups.contains(GROUP_A)).isFalse();
      assertThat(groups.contains(GROUP_B)).isFalse();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      response.close();
   }

   @Test
   public void testRemoveCollectionUsersGroupsRoleNotExistingUsers() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addCollectionUserRole(projectCode, COLLECTION_NAME, USER_X, role);
      securityFacade.addCollectionGroupRole(projectCode, COLLECTION_NAME, GROUP_A, role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.COLLECTION_RESOURCE,
                  COLLECTION_NAME,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getCollectionRoles(projectCode, COLLECTION_NAME);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isFalse();
      assertThat(groups.contains(GROUP_A)).isFalse();
      assertThat(groups.contains(GROUP_B)).isFalse();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      response.close();
   }

   @Test
   public void testAddViewUsersGroupsRoleBasic() {
      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_MANAGE;
      // PUT
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.VIEW_RESOURCE,
                  "" + VIEW_ID,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      List<Role> fromFacade = securityFacade.getViewRoles(projectCode, VIEW_ID);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isTrue();
      assertThat(groups.contains(GROUP_A)).isTrue();
      assertThat(groups.contains(GROUP_B)).isTrue();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
   }

   @Test
   public void testAddViewUsersGroupsRoleNotAllowedRole() {
      final Client client = ClientBuilder.newBuilder().build();
      final String role = "thisIsNotAllowedRole";

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.VIEW_RESOURCE,
                  "" + VIEW_ID,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
   }

   @Test
   public void testAddViewUsersGroupsRoleCallTwoTimes() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addViewUserRole(projectCode, VIEW_ID, USER_X, role);
      securityFacade.addViewUserRole(projectCode, VIEW_ID, USER_Y, role);
      securityFacade.addViewGroupRole(projectCode, VIEW_ID, GROUP_A, role);
      securityFacade.addViewGroupRole(projectCode, VIEW_ID, GROUP_B, role);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.VIEW_RESOURCE,
                  "" + VIEW_ID,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_X, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      List<Role> fromFacade = securityFacade.getViewRoles(projectCode, VIEW_ID);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isTrue();
      assertThat(groups.contains(GROUP_A)).isTrue();
      assertThat(groups.contains(GROUP_B)).isTrue();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
   }

   @Test
   public void testGetViewRolesNoQueryParam() {
      final List<Role> before = securityFacade.getViewRoles(projectCode, VIEW_ID);

      final Client client = ClientBuilder.newBuilder().build();
      final String role = LumeerConst.Security.ROLE_MANAGE;

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.VIEW_RESOURCE,
                  "" + VIEW_ID,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .request().build(PUT_METHOD_NAME)
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      final List<Role> after = securityFacade.getViewRoles(projectCode, VIEW_ID);

      assertThat(before.size()).isEqualTo(after.size());
      for (Role r : before) {
         assertThat(after.contains(r));
      }
   }

   @Test
   public void testGetViewRolesBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addViewUserRole(projectCode, VIEW_ID, USER_Y, role);
      securityFacade.addViewGroupRole(projectCode, VIEW_ID, GROUP_B, role);
      List<Role> fromFacade = securityFacade.getViewRoles(projectCode, VIEW_ID);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.VIEW_RESOURCE,
                  "" + VIEW_ID,
                  LumeerConst.Security.ROLES_KEY))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(List.class);
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testGetViewRolesNothingAdded() {
      List<Role> fromFacade = securityFacade.getViewRoles(projectCode, VIEW_ID);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.VIEW_RESOURCE,
                  "" + VIEW_ID,
                  LumeerConst.Security.ROLES_KEY))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(List.class);
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testRemoveViewUsersGroupsRoleBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addViewUserRole(projectCode, VIEW_ID, USER_X, role);
      securityFacade.addViewUserRole(projectCode, VIEW_ID, USER_Y, role);
      securityFacade.addViewGroupRole(projectCode, VIEW_ID, GROUP_A, role);
      securityFacade.addViewGroupRole(projectCode, VIEW_ID, GROUP_B, role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.VIEW_RESOURCE,
                  "" + VIEW_ID,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getViewRoles(projectCode, VIEW_ID);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isFalse();
      assertThat(groups.contains(GROUP_A)).isFalse();
      assertThat(groups.contains(GROUP_B)).isFalse();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      response.close();
   }

   @Test
   public void testRemoveViewUsersGroupsRoleNotExistingUsers() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addViewUserRole(projectCode, VIEW_ID, USER_X, role);
      securityFacade.addViewGroupRole(projectCode, VIEW_ID, GROUP_A, role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  LumeerConst.Security.VIEW_RESOURCE,
                  "" + VIEW_ID,
                  LumeerConst.Security.ROLES_KEY,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getViewRoles(projectCode, VIEW_ID);
      List<String> users = null;
      List<String> groups = null;
      for (Role item: fromFacade) {
         if (item.getName().equals(role)) {
            users = item.getUsers();
            groups = item.getGroups();
         }
      }

      assertThat(users.contains(USER_X)).isTrue();
      assertThat(users.contains(USER_Y)).isFalse();
      assertThat(groups.contains(GROUP_A)).isFalse();
      assertThat(groups.contains(GROUP_B)).isFalse();
      final Map<String, Integer> userCount = new LinkedHashMap<>();
      final Map<String, Integer> groupCount = new LinkedHashMap<>();
      users.forEach((s)-> {
         if (userCount.containsKey(s)) userCount.put(s, userCount.get(s) + 1);
         else userCount.put(s, 1);
      });
      groups.forEach((s)-> {
         if (groupCount.containsKey(s)) groupCount.put(s, groupCount.get(s) + 1);
         else groupCount.put(s, 1);
      });
      userCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      groupCount.forEach((k, v) -> {
         assertThat(v == 1).isTrue();
      });
      response.close();
   }

   /**
    * Create an url path for string arguments separated by slash.
    *
    * @param args
    *       the strings to be separated
    * @return the string containing the url path
    */
   private String createUrl(String... args) {
      StringJoiner stringJoiner = new StringJoiner("/");
      stringJoiner.add(PATH_CONTEXT);
      stringJoiner.add("rest");
      stringJoiner.add("roles");
      for (String arg : args) {
         stringJoiner.add(arg);
      }
      return stringJoiner.toString();
   }
}
