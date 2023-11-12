/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.facade.ProjectFacade;
import io.lumeer.remote.rest.ServiceIntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class  ProjectServicePermissionsIT extends ServiceIntegrationTestBase {

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

   private User user;

   private static final String ORGANIZATION_CODE = "OrganizationServicePermissionsIntegrationTest_id";
   private static final String ORGANIZATION_NAME = "OrganizationServicePermissionsIT";

   private String projectsUrl;

   @Before
   public void configureProject() {
      User user = new User(AuthenticatedUser.DEFAULT_EMAIL);
      this.user = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setName(ORGANIZATION_NAME);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read)));
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      projectDao.setOrganization(storedOrganization);
      workspaceKeeper.setOrganizationId(storedOrganization.getId());

      projectsUrl = organizationPath(storedOrganization) + "projects/";

      PermissionCheckerUtil.allowGroups();
   }

   private Project createProject(String code, String name) {
      Project project = new Project(code, name, "a", "b", null, null, null, false, null);
      project.getPermissions().updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Project.ROLES));
      return projectDao.createProject(project);
   }

   @Test
   public void testGetProjectNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());

      Response response = client.target(projectsUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void testGetProjectReadRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read)))));

      Response response = client.target(projectsUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      Project fetchedProject = response.readEntity(Project.class);
      assertThat(fetchedProject.getCode()).isEqualTo(projectCode);
      assertThat(fetchedProject.getName()).isEqualTo(projectName);
   }

   @Test
   public void testUpdateProjectNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());
      String newProjectName = "NewName2";
      Project newProject = new Project(projectCode, newProjectName, "a", "b", null, null, null, false, null);

      Response response = client.target(projectsUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newProject)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void testUpdateProjectManageRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.Manage)))));
      String newProjectName = "NewName";
      Project newProject = new Project(projectCode, newProjectName, "a", "b", null, null, null, false, null);

      Response response = client.target(projectsUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newProject)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      Project fetchedProject = response.readEntity(Project.class);
      assertThat(fetchedProject.getCode()).isEqualTo(projectCode);
      assertThat(fetchedProject.getName()).isEqualTo(newProjectName);
   }

   @Test
   public void testGetProjectsSomeReadRoles() {
      String projectCode1 = "CODE1";
      String projectName1 = "name1";
      String projectCode2 = "CODE2";
      String projectName2 = "name2";
      String projectCode3 = "CODE3";
      String projectName3 = "name3";
      String projectCode4 = "CODE4";
      String projectName4 = "name4";
      List<String> projectCodes = Arrays.asList(projectCode1, projectCode2, projectCode3, projectCode4);
      List<String> projectNames = Arrays.asList(projectName1, projectName2, projectName3, projectName4);

      for (int i = 0; i < projectCodes.size(); i++) {
         final Project project = createProject(projectCodes.get(i), projectNames.get(i));
         if (i % 2 == 0) {
            projectFacade.removeUserPermission(project.getId(), this.user.getId());
         } else {
            projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read)))));
         }
      }

      Response response = client.target(projectsUrl)
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();

      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      List<Project> projects = response.readEntity(new GenericType<List<Project>>(List.class) {
      });
      assertThat(projects).extracting("code").containsOnly(projectCode2, projectCode4);
      assertThat(projects).extracting("name").containsOnly(projectName2, projectName4);
   }

   @Test
   public void testDeleteProjectInOrganizationNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());

      Response response = client.target(projectsUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void testDeleteProjectInOrganizationManageRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.Manage)))));

      Response response = client.target(projectsUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testCreateCollectionInProjectNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());
      String collectionName = "CollectionName";
      String collectionCode = "ColCode";
      Collection collection = new Collection(collectionCode, collectionName, "a", "b", null);

      Response response = client.target(projectsUrl).path(project.getId()).path("collections")
                                .request(MediaType.APPLICATION_JSON).buildPost(Entity.json(collection)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void testCreateCollectionInProjectWriteRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      String collectionName = "CollectionName";
      String collectionCode = "ColCode2";
      Collection collection = new Collection(collectionCode, collectionName, "a", "b", null);

      Response response = client.target(projectsUrl).path(project.getId()).path("collections")
                                .request(MediaType.APPLICATION_JSON).buildPost(Entity.json(collection)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testGetProjectPermissionsNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());

      Response response = client.target(projectsUrl).path(project.getId()).path("permissions")
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void testGetProjectPermissionsManageRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)))));

      Response response = client.target(projectsUrl).path(project.getId()).path("permissions")
                                .request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

   }

   @Test
   public void testUpdateUserPermissionsNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());

      Permission[] newPermission = { Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.DataWrite))) };
      Response response = client.target(projectsUrl).path(project.getId()).path("permissions").path("users")
                                .request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newPermission)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void testUpdateUserPermissionsManageRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)))));

      Permission[] newPermission = { Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.DataWrite))) };
      Response response = client.target(projectsUrl).path(project.getId()).path("permissions").path("users")
                                .request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newPermission)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void tesRemoveUserPermissionsNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());

      Response response = client.target(projectsUrl).path(project.getId()).path("permissions").path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void tesRemoveUserPermissionsManageRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(),Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)))));

      Response response = client.target(projectsUrl).path(project.getId()).path("permissions").path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testUpdateGroupPermissionsNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());
      String group = "testGroup4";
      Permission[] newPermission = { new Permission(group, Set.of(new Role(RoleType.DataWrite))) };

      Response response = client.target(projectsUrl).path(project.getId()).path("permissions").path("groups")
                                .request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newPermission)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void testUpdateGroupPermissionsManageRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)))));
      String group = "testGroup5";
      Permission[] newPermission = { new Permission(group, Set.of(new Role(RoleType.DataWrite))) };

      Response response = client.target(projectsUrl).path(project.getId()).path("permissions").path("groups")
                                .request(MediaType.APPLICATION_JSON).buildPut(Entity.json(newPermission)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testRemoveGroupPermissionsNoRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.removeUserPermission(project.getId(), this.user.getId());
      String group = "testGroup6";

      Response response = client.target(projectsUrl).path(project.getId()).path("permissions").path("groups").path(group)
                                .request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.FORBIDDEN);
   }

   @Test
   public void testRemoveGroupPermissionsManageRole() {
      String projectCode = "CODE";
      String projectName = "name";
      final Project project = createProject(projectCode, projectName);
      projectFacade.updateUserPermissions(project.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)))));
      String group = "testGroup7";

      Response response = client.target(projectsUrl).path(project.getId()).path("permissions").path("groups").path(group)
                                .request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }
}
