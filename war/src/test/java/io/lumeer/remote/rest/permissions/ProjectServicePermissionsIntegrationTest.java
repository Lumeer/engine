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

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.facade.ProjectFacade;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.remote.rest.ServiceIntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.mongodb.model.MorphiaOrganization;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class ProjectServicePermissionsIntegrationTest extends ServiceIntegrationTestBase {

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private UserDao userDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   private String organizationCode = "OrganizationServicePermissionsIntegrationTest_id";
   private String organizationName = "OrganizationServicePermissionsIntegrationTest";
   private final String TARGET_URI = "http://localhost:8080";
   private String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/" + organizationCode + "/projects/";
   private String userEmail = AuthenticatedUser.DEFAULT_EMAIL;

   @Before
   public void configureProject() {
      MorphiaOrganization organization = new MorphiaOrganization();
      organization.setCode(organizationCode);
      organization.setName(organizationName);
      organization.setPermissions(new MorphiaPermissions());
      organization.getPermissions().updateUserPermissions(new MorphiaPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);
      workspaceKeeper.setOrganization(organizationCode);

      User user = new User(userEmail);
      userDao.createUser(user);
   }

   private Project createProject(String code, String name) {
      Project project = new JsonProject(code, name, "a", "b", null, null);
      project.getPermissions().updateUserPermissions(new SimplePermission(userEmail, Project.ROLES));
      return projectDao.createProject(project);
   }

   @Test
   public void testGetProjectNoRole() {
      String projectCode = "testGetProjectNoRole_code";
      String projectName = "testGetProjectNoRole";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testGetProjectReadRole() {
      String projectCode = "testGetProjectReadRole_code";
      String projectName = "testGetProjectReadRole";
      createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ)))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      JsonOrganization fetchedProject = response.readEntity(JsonOrganization.class);
      assertThat(fetchedProject.getCode()).isEqualTo(projectCode);
      assertThat(fetchedProject.getName()).isEqualTo(projectName);
   }

   @Test
   public void testUpdateProjectNoRole() {
      String projectCode = "testUpdateProjectNoRole_id";
      String projectName = "testUpdateProjectNoRole";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);
      String newProjectName = "NewName2";
      Project newProject = new JsonProject(projectCode, newProjectName, "a", "b", null, null);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newProject)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testUpdateProjectManageRole() {
      String projectCode = "testUpdateProjectManageRole_code";
      String projectName = "testUpdateProjectManageRole";
      createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));
      String newProjectName = "NewName";
      Project newProject = new JsonProject(projectCode, newProjectName, "a", "b", null, null);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newProject)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      Project fetchedProject = response.readEntity(JsonProject.class);
      assertThat(fetchedProject.getCode()).isEqualTo(projectCode);
      assertThat(fetchedProject.getName()).isEqualTo(newProjectName);
   }

   @Test
   public void testGetProjectsSomeReadRoles() {
      String projectCode1 = "ProjectServiceTestProject_code1";
      String projectName1 = "ProjectServiceTestProject1";
      String projectCode2 = "ProjectServiceTestProject_code2";
      String projectName2 = "ProjectServiceTestProject2";
      String projectCode3 = "ProjectServiceTestProject_code3";
      String projectName3 = "ProjectServiceTestProject3";
      String projectCode4 = "ProjectServiceTestProject_code4";
      String projectName4 = "ProjectServiceTestProject4";
      List<String> projectCodes = Arrays.asList(projectCode1, projectCode2, projectCode3, projectCode4);
      List<String> projectNames = Arrays.asList(projectName1, projectName2, projectName3, projectName4);

      for (int i = 0; i < projectCodes.size(); i++) {
         createProject(projectCodes.get(i), projectNames.get(i));
         if (i % 2 == 0) {
            projectFacade.removeUserPermission(projectCodes.get(i), userEmail);
         } else {
            projectFacade.updateUserPermissions(projectCodes.get(i), new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ)))));
         }
      }

      Response response = client.target(TARGET_URI).path(PATH_PREFIX).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();

      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      List<Project> projects = response.readEntity(new GenericType<List<Project>>(List.class) {
      });
      assertThat(projects).extracting("code").containsOnly(projectCode2, projectCode4);
      assertThat(projects).extracting("name").containsOnly(projectName2, projectName4);
   }

   @Test
   public void testDeleteProjectInOrganizationNoRole() {
      String projectCode = "ProjectServiceTestProject_code1";
      String projectName = "ProjectServiceTestProject1";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testDeleteProjectInOrganizationManageRole() {
      String projectCode = "ProjectServiceTestProject_code1";
      String projectName = "ProjectServiceTestProject1";
      createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testCreateCollectionInProjectNoRole() {
      String projectCode = "testCreateCollectionInProjectNoRole_code1";
      String projectName = "estCreateCollectionInProjectNoRole";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);
      String collectionName = "CollectionName";
      String collectionCode = "ColCode";
      Collection collection = new JsonCollection(collectionCode, collectionName, "a", "b", null);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/collections").
            request(MediaType.APPLICATION_JSON).buildPost(Entity.json(collection)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testCreateCollectionInProjectWriteRole() {
      String projectCode = "testCreateCollectionInProjectWriteRole_code1";
      String projectName = "testCreateCollectionInProjectWriteRole";
      createProject(projectCode, projectName);
      String collectionName = "CollectionName";
      String collectionCode = "ColCode2";
      Collection collection = new JsonCollection(collectionCode, collectionName, "a", "b", null);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/collections").
            request(MediaType.APPLICATION_JSON).buildPost(Entity.json(collection)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testGetProjectPermissionsNoRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testGetProjectPermissionsManageRole() {
      String projectCode = "testGetProjectPermissionsManageRole_code1";
      String projectName = "testGetProjectPermissionsManageRole";
      createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

   }

   @Test
   public void testUpdateUserPermissionsNoRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);

      Permission newPermission = new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE))));
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/users").
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newPermission)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testUpdateUserPermissionsManageRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Permission newPermission = new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE))));
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/users").
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newPermission)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void tesRemoveUserPermissionsNoRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/users/" + userEmail).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void tesRemoveUserPermissionsManageRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/users/" + userEmail).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testUpdateGroupPermissionsNoRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);
      String group = "testGroup4";
      Permission newPermission = new JsonPermission(group, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/groups").
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newPermission)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testUpdateGroupPermissionsManageRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));
      String group = "testGroup5";
      Permission newPermission = new JsonPermission(group, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/groups").
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newPermission)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testRemoveGroupPermissionsNoRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.removeUserPermission(projectCode, userEmail);
      String group = "testGroup6";

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/groups/" + group).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testRemoveGroupPermissionsManageRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));
      String group = "testGroup7";

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/groups/" + group).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }
}