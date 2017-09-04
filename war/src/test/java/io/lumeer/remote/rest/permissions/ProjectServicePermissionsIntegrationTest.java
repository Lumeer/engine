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

package io.lumeer.remote.rest.permissions;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.ProjectFacade;
import io.lumeer.engine.IntegrationTestBase;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class ProjectServicePermissionsIntegrationTest extends IntegrationTestBase {

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   private Client client;
   private String organizationCode = "OrganizationServicePermissionsIntegrationTest_id";
   private String organizationName = "OrganizationServicePermissionsIntegrationTest";
   private final String TARGET_URI = "http://localhost:8080";
   private String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/" + organizationCode + "/projects/";
   private String userEmail = AuthenticatedUser.DEFAULT_EMAIL;

   @Before
   public void createOrganization() {
      organizationFacade.createOrganization(new JsonOrganization(organizationCode, organizationName, "icon", "colour", null));
   }

   @Before
   public void createClient() {
      client = ClientBuilder.newBuilder().build();
   }

   @After
   public void closeClient() {
      client.close();
   }

   @Test
   public void testGetProjectNoRole() {
      String projectCode = "testGetProjectNoRole_code";
      String projectName = "testGetProjectNoRole";
      Project project = new JsonProject(projectCode, projectName, "a", "b", null);
      projectFacade.createProject(project);
      projectFacade.removeUserPermission(projectCode, userEmail);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
      client.close();
      createClient();
   }

   @Test
   public void testGetProjectReadRole() {
      String projectCode = "testGetProjectReadRole_code";
      String projectName = "testGetProjectReadRole";
      Project project = new JsonProject(projectCode, projectName, "a", "b", null);
      projectFacade.createProject(project);
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
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.removeUserPermission(projectCode, userEmail);
      String newProjectName = "NewName2";
      Project newProject = new JsonProject(projectCode, newProjectName, "a", "b", null);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newProject)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testUpdateProjectManageRole() {
      String projectCode = "testUpdateProjectManageRole_code";
      String projectName = "testUpdateProjectManageRole";
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));
      String newProjectName = "NewName";
      Project newProject = new JsonProject(projectCode, newProjectName, "a", "b", null);

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
         projectFacade.createProject(new JsonProject(projectCodes.get(i), projectNames.get(i), "a", "b", null));

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
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.removeUserPermission(projectCode, userEmail);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testDeleteProjectInOrganizationManageRole() {
      String projectCode = "ProjectServiceTestProject_code1";
      String projectName = "ProjectServiceTestProject1";
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testCreateCollectionInProjectNoRole() {
      String projectCode = "testCreateCollectionInProjectNoRole_code1";
      String projectName = "estCreateCollectionInProjectNoRole";
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
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
      String projectCode1 = "testCreateCollectionInProjectWriteRole_code1";
      String projectName1 = "testCreateCollectionInProjectWriteRole";
      projectFacade.createProject(new JsonProject(projectCode1, projectName1, "a", "b", null));
      String collectionName = "CollectionName";
      String collectionCode = "ColCode2";
      Collection collection = new JsonCollection(collectionCode, collectionName, "a", "b", null);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode1 + "/collections").
            request(MediaType.APPLICATION_JSON).buildPost(Entity.json(collection)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);
   }

   @Test
   public void testGetProjectPermissionsNoRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.removeUserPermission(projectCode, userEmail);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testGetProjectPermissionsManageRole() {
      String projectCode = "testGetProjectPermissionsManageRole_code1";
      String projectName = "testGetProjectPermissionsManageRole";
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

   }

   @Test
   public void testUpdateUserPermissionsNoRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
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
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
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
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.removeUserPermission(projectCode, userEmail);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/users/" + userEmail).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);

   }

   @Test
   public void tesRemoveUserPermissionsManageRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/users/" + userEmail).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testUpdateGroupPermissionsNoRole() {
      String projectCode = "testGetProjectPermissionsNoRole_code1";
      String projectName = "testGetProjectPermissionsNoRole";
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
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
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
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
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
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
      projectFacade.createProject(new JsonProject(projectCode, projectName, "a", "b", null));
      projectFacade.updateUserPermissions(projectCode, new JsonPermission(userEmail, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.READ, Role.MANAGE)))));
      String group = "testGroup7";

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/permissions/groups/" + group).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }
}