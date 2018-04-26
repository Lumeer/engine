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
package io.lumeer.remote.rest.permissions;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.ProjectFacade;
import io.lumeer.remote.rest.ServiceIntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.test.util.LumeerAssertions;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.*;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class OrganizationServicePermissionsIT extends ServiceIntegrationTestBase {

   private final String TARGET_URI = "http://localhost:8080";
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/";

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private UserDao userDao;

   private String userId;

   @Before
   public void prepare() {
      User user = new User(AuthenticatedUser.DEFAULT_EMAIL);
      userId = userDao.createUser(user).getId();
   }

   @Test
   public void testGetOrganizationNoRole() {
      String name = "TestGetOrganizationNoRole";
      String code = "TestGetOrganizationNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.removeUserPermission(code, userId);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testGetOrganizationReadRole() {
      String name = "TestGetOrganizationReadRole";
      String code = "TestGetOrganizationReadRole_id";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      Permission newPermission = new JsonPermission(userId, Role.toStringRoles(Collections.singleton(Role.READ)));
      organizationFacade.updateUserPermissions(code, newPermission);
      Set<Permission> perm = organizationDao.getOrganizationByCode(code).getPermissions().getUserPermissions();
      LumeerAssertions.assertPermissions(perm, newPermission);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testGetOrganizationsSomeReadRoles() {
      String name1 = "testGetOrganizationsSomeReadRoles1";
      String name2 = "testGetOrganizationsSomeReadRoles2";
      String code1 = "testGetOrganizationsSomeReadRoles1_code";
      String code2 = "testGetOrganizationsSomeReadRoles2_code";
      String name3 = "testGetOrganizationsSomeReadRoles3";
      String name4 = "testGetOrganizationsSomeReadRoles4";
      String code3 = "testGetOrganizationsSomeReadRoles3_code";
      String code4 = "testGetOrganizationsSomeReadRoles4_code";

      List<String> names = Arrays.asList(name1, name2, name3, name4);
      List<String> codes = Arrays.asList(code1, code2, code3, code4);

      for (int i = 0; i < codes.size(); i++) {
         organizationFacade.createOrganization(new JsonOrganization(codes.get(i), names.get(i), "a", "b", null, null));
         if (i % 2 == 0) {
            organizationFacade.removeUserPermission(codes.get(i), userId);
         } else {
            organizationFacade.updateUserPermissions(codes.get(i), new JsonPermission(userId, Role.toStringRoles(Collections.singleton(Role.READ))));
         }
      }

      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<Organization> organizations = response.readEntity(new GenericType<List<Organization>>(List.class) {
      });
      assertThat(organizations).extracting("code").containsOnly(code2, code4);
      assertThat(organizations).extracting("name").containsOnly(name2, name4);
   }

   @Test
   public void testUpdateOrganizationNoRole() {
      String name = "TestUpdateOrganizationNoRole";
      String code = "TestUpdateOrganizationNoRole_id";
      String newName = "NewTestUpdateOrganizationNoRole";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.removeUserPermission(code, userId);
      Organization newOrganization = new JsonOrganization(code, newName, "c", "d", null, null);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.json(newOrganization))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);

   }

   @Test
   public void testUpdateOrganizationManageRole() {
      String name = "TestUpdateOrganizationManageRole";
      String code = "TestUpdateOrganizationManageRole_code";
      String newName = "NewTestUpdateOrganizationManageRole";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.updateUserPermissions(code, new JsonPermission(userId, Role.toStringRoles(new HashSet<Role>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.json(new JsonOrganization(code, newName, "c", "d", null, null)))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      Organization org = response.readEntity(JsonOrganization.class);
      assertThat(org.getName()).isEqualTo(newName);
      assertThat(org.getCode()).isEqualTo(code);

   }

   @Test
   public void testGetOrganizationPermissionsNoRole() {
      String name = "testGetOrganizationPermissionsNoRole";
      String code = "testGetOrganizationPermissionsNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.removeUserPermission(code, userId);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet()
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testGetOrganizationPermissionsManageRole() {
      String name = "testGetOrganizationPermissionsManageRole";
      String code = "testGetOrganizationPermissionsManageRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.updateUserPermissions(code, new JsonPermission(userId, Role.toStringRoles(new HashSet<Role>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet()
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testUpdateUserPermissionNoRole() {
      String name = "testUpdateUserPermissionNoRole";
      String code = "testUpdateUserPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.removeUserPermission(code, userId);
      Permission newPermission = new JsonPermission(userId, Role.toStringRoles(new HashSet<>(Collections.singletonList(Role.WRITE))));

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions/users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.json(newPermission))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testUpdateUserPermissionManageRole() {
      String name = "testUpdateUserPermissionNoRole";
      String code = "testUpdateUserPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.updateUserPermissions(code, new JsonPermission(userId, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));
      Permission newPermission = new JsonPermission(userId, Role.toStringRoles(new HashSet<>(Collections.singletonList(Role.WRITE))));

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions/users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.json(newPermission))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testRemoveUserPermissionNoRole() {
      String name = "testRemoveUserPermissionNoRole";
      String code = "testRemoveUserPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.removeUserPermission(code, userId);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions/users/" + userId)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete()
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testRemoveUserPermissionManageRole() {
      String name = "testRemoveUserPermissionNoRole";
      String code = "testRemoveUserPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.updateUserPermissions(code, new JsonPermission(userId, Role.toStringRoles(new HashSet<Role>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions/users/" + userId)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete()
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testUpdateGroupPermissionNoRole() {
      String name = "testUpdateGroupPermissionNoRole";
      String code = "testUpdateGroupPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.removeUserPermission(code, userId);
      String group = "testGroup1";
      Permission newPermission = new JsonPermission(group, Role.toStringRoles(new HashSet<>(Collections.singletonList(Role.WRITE))));

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions/groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.json(newPermission))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testUpdateGroupPermissionManageRole() {
      String name = "testUpdateGroupPermissionNoRole";
      String code = "testUpdateGroupPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.updateUserPermissions(code, new JsonPermission(userId, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));
      String group = "testGroup2";
      Permission newPermission = new JsonPermission(group, Role.toStringRoles(new HashSet<>(Collections.singletonList(Role.WRITE))));

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions/groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(Entity.json(newPermission))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testRemoveGroupPermissionNoRole() {
      String name = "testRemoveGroupPermissionNoRole";
      String code = "testRemoveGroupPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.removeUserPermission(code, userId);
      String group = "testGroup3";

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions/groups/" + group)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete()
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testRemoveGroupPermissionManageRole() {
      String name = "testRemoveGroupPermissionNoRole";
      String code = "testRemoveGroupPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.updateUserPermissions(code, new JsonPermission(userId, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));
      String group = "testGroup3";

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/permissions/groups/" + group)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete()
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testCreateProjectInOrganizationNoRole() {
      String name = "testRemoveGroupPermissionNoRole";
      String code = "testRemoveGroupPermissionNoRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      organizationFacade.removeUserPermission(code, userId);
      String projectCode = "proj1";
      String projectName = "proj1_code";
      Project project = new JsonProject(projectCode, projectName, "a", "b", null, null);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/projects")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(Entity.json(project))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testCreateProjectInOrganizationManageRole() {
      String name = "testCreateProjectInOrganizationManageRole";
      String code = "testCreateProjectInOrganizationManageRole_code";
      Organization organization = new JsonOrganization(code, name, "a", "b", null, null);
      organizationFacade.createOrganization(organization);
      String projectCode = "proj2";
      String projectName = "proj2_code";
      Project project = new JsonProject(projectCode, projectName, "a", "b", null, null);

      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + code + "/projects")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(Entity.json(project))
                                .invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }
}
