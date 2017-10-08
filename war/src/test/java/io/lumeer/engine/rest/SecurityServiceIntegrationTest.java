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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.dto.Role;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DatabaseInitializer;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
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
   private SecurityFacade securityFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private DatabaseInitializer databaseInitializer;

   @Inject
   private UserFacade userFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   private final String ROLES_PATH = "roles";

   private final String USER_X = "user X";
   private final String USER_Y = "userY";
   private final String GROUP_A = "groupA";
   private final String GROUP_B = "groupB";
   private final String organizationCode = "LMR";
   private final String projectCode = "PR";
   private int viewId = 42;

   private final String USERS_QP = "users";
   private final String GROUPS_QP = "groups";

   @Before
   public void init() throws Exception {
      DataFilter filter = dataStorageDialect.documentFilter("{}");
      sysDataStorage.dropManyDocuments(LumeerConst.Security.ORGANIZATION_ROLES_COLLECTION_NAME, filter);
      dataStorage.dropManyDocuments(LumeerConst.Security.ROLES_COLLECTION_NAME, filter);
      sysDataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, filter);
      sysDataStorage.dropManyDocuments(LumeerConst.Project.COLLECTION_NAME, filter);

      organizationFacade.createOrganization(new Organization(organizationCode, "Lumeer"));
      organizationFacade.setOrganizationCode(organizationCode);

      projectFacade.createProject(new Project(projectCode, "project"));
      projectFacade.setCurrentProjectCode(projectCode);
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
                  ROLES_PATH,
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
      for (Role item : fromFacade) {
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
      users.forEach((s) -> {
         if (userCount.containsKey(s)) {
            userCount.put(s, userCount.get(s) + 1);
         } else {
            userCount.put(s, 1);
         }
      });
      groups.forEach((s) -> {
         if (groupCount.containsKey(s)) {
            groupCount.put(s, groupCount.get(s) + 1);
         } else {
            groupCount.put(s, 1);
         }
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
                                      ROLES_PATH,
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

      securityFacade.addOrganizationUsersRole(organizationCode, Arrays.asList(USER_X, USER_Y), role);
      securityFacade.addOrganizationGroupsRole(organizationCode, Arrays.asList(GROUP_A, GROUP_B), role);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  ROLES_PATH,
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
      for (Role item : fromFacade) {
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
      users.forEach((s) -> {
         if (userCount.containsKey(s)) {
            userCount.put(s, userCount.get(s) + 1);
         } else {
            userCount.put(s, 1);
         }
      });
      groups.forEach((s) -> {
         if (groupCount.containsKey(s)) {
            groupCount.put(s, groupCount.get(s) + 1);
         } else {
            groupCount.put(s, 1);
         }
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
                  ROLES_PATH,
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

      securityFacade.addOrganizationUsersRole(organizationCode, Collections.singletonList(USER_Y), role);
      securityFacade.addOrganizationGroupsRole(organizationCode, Collections.singletonList(GROUP_B), role);
      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  ROLES_PATH))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(new GenericType<List<Role>>(List.class) {
      });
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
                  ROLES_PATH))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(new GenericType<List<Role>>(List.class) {
      });
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testRemoveOrganizationUsersGroupsRoleBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;

      securityFacade.addOrganizationUsersRole(organizationCode, Arrays.asList(USER_X, USER_Y), role);
      securityFacade.addOrganizationGroupsRole(organizationCode, Arrays.asList(GROUP_A, GROUP_B), role);
      securityFacade.addOrganizationUsersRole(organizationCode, Collections.singletonList(USER_X), role);
      securityFacade.addOrganizationUsersRole(organizationCode, Collections.singletonList(USER_Y), role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  ROLES_PATH,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item : fromFacade) {
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
      users.forEach((s) -> {
         if (userCount.containsKey(s)) {
            userCount.put(s, userCount.get(s) + 1);
         } else {
            userCount.put(s, 1);
         }
      });
      groups.forEach((s) -> {
         if (groupCount.containsKey(s)) {
            groupCount.put(s, groupCount.get(s) + 1);
         } else {
            groupCount.put(s, 1);
         }
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
      securityFacade.addOrganizationUsersRole(organizationCode, Collections.singletonList(USER_X), role);
      securityFacade.addOrganizationGroupsRole(organizationCode, Collections.singletonList(GROUP_A), role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  ROLES_PATH,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getOrganizationRoles(organizationCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item : fromFacade) {
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
      users.forEach((s) -> {
         if (userCount.containsKey(s)) {
            userCount.put(s, userCount.get(s) + 1);
         } else {
            userCount.put(s, 1);
         }
      });
      groups.forEach((s) -> {
         if (groupCount.containsKey(s)) {
            groupCount.put(s, groupCount.get(s) + 1);
         } else {
            groupCount.put(s, 1);
         }
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
                  ROLES_PATH,
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
      for (Role item : fromFacade) {
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
      users.forEach((s) -> {
         if (userCount.containsKey(s)) {
            userCount.put(s, userCount.get(s) + 1);
         } else {
            userCount.put(s, 1);
         }
      });
      groups.forEach((s) -> {
         if (groupCount.containsKey(s)) {
            groupCount.put(s, groupCount.get(s) + 1);
         } else {
            groupCount.put(s, 1);
         }
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
                  ROLES_PATH,
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

      securityFacade.addProjectUsersRole(projectCode, Arrays.asList(USER_X, USER_Y), role);
      securityFacade.addProjectGroupsRole(projectCode, Arrays.asList(GROUP_A, GROUP_B), role);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  ROLES_PATH,
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
      for (Role item : fromFacade) {
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
      users.forEach((s) -> {
         if (userCount.containsKey(s)) {
            userCount.put(s, userCount.get(s) + 1);
         } else {
            userCount.put(s, 1);
         }
      });
      groups.forEach((s) -> {
         if (groupCount.containsKey(s)) {
            groupCount.put(s, groupCount.get(s) + 1);
         } else {
            groupCount.put(s, 1);
         }
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
                  ROLES_PATH,
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

      securityFacade.addProjectUsersRole(projectCode, Collections.singletonList(USER_Y), role);
      securityFacade.addProjectGroupsRole(projectCode, Collections.singletonList(GROUP_B), role);
      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  ROLES_PATH))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(new GenericType<List<Role>>(List.class) {
      });
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
                  ROLES_PATH))
            .request().buildGet()
            .invoke();

      List<Role> fromService = response.readEntity(new GenericType<List<Role>>(List.class) {
      });
      assertThat(fromService).hasSize(fromFacade.size());
      for (Role r : fromFacade) {
         assertThat(fromService.contains(r));
      }

      response.close();
   }

   @Test
   public void testRemoveProjectUsersGroupsRoleBasic() {
      final String role = LumeerConst.Security.ROLE_MANAGE;
      securityFacade.addProjectUsersRole(projectCode, Arrays.asList(USER_X, USER_Y), role);
      securityFacade.addProjectGroupsRole(projectCode, Arrays.asList(GROUP_A, GROUP_B), role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  ROLES_PATH,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item : fromFacade) {
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
      users.forEach((s) -> {
         if (userCount.containsKey(s)) {
            userCount.put(s, userCount.get(s) + 1);
         } else {
            userCount.put(s, 1);
         }
      });
      groups.forEach((s) -> {
         if (groupCount.containsKey(s)) {
            groupCount.put(s, groupCount.get(s) + 1);
         } else {
            groupCount.put(s, 1);
         }
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
      securityFacade.addProjectUsersRole(projectCode, Collections.singletonList(USER_X), role);
      securityFacade.addProjectGroupsRole(projectCode, Collections.singletonList(GROUP_A), role);

      final Client client = ClientBuilder.newBuilder().build();

      Response response = client
            .target(TARGET_URI)
            .path(createUrl(
                  LumeerConst.Security.ORGANIZATION_RESOURCE,
                  organizationCode,
                  LumeerConst.Security.PROJECT_RESOURCE,
                  projectCode,
                  ROLES_PATH,
                  role))
            .queryParam(USERS_QP, USER_Y)
            .queryParam(GROUPS_QP, GROUP_A, GROUP_B)
            .request().buildDelete()
            .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<Role> fromFacade = securityFacade.getProjectRoles(projectCode);
      List<String> users = null;
      List<String> groups = null;
      for (Role item : fromFacade) {
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
      users.forEach((s) -> {
         if (userCount.containsKey(s)) {
            userCount.put(s, userCount.get(s) + 1);
         } else {
            userCount.put(s, 1);
         }
      });
      groups.forEach((s) -> {
         if (groupCount.containsKey(s)) {
            groupCount.put(s, groupCount.get(s) + 1);
         } else {
            groupCount.put(s, 1);
         }
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
