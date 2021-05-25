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
package io.lumeer.remote.rest;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.RoleOld;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@RunWith(Arquillian.class)
public class ProjectServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String ORGANIZATION_CODE = "TORG";

   private static final String CODE1 = "TPROJ1";
   private static final String CODE2 = "TPROJ2";
   private static final String CODE3 = "TPROJ3";
   private static final String CODE4 = "TPROJ4";

   private static final String NAME = "Testing project";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Set<RoleOld> USER_ROLES = Project.ROLES;
   private static final Set<RoleOld> GROUP_ROLES = Collections.singleton(RoleOld.READ);
   private Permission userPermission;
   private Permission groupPermission;

   private User user;

   private String projectUrl;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      userPermission = Permission.buildWithRoles(this.user.getId(), USER_ROLES);
      groupPermission = Permission.buildWithRoles(GROUP, GROUP_ROLES);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      organization.getPermissions().updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Organization.ROLES));
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      projectUrl = organizationPath(storedOrganization) + "projects";
   }

   private Project createProject(String code) {
      Project project = new Project(code, NAME, ICON, COLOR, null, null, null, false, null);
      project.getPermissions().updateUserPermissions(userPermission);
      project.getPermissions().updateGroupPermissions(groupPermission);
      return projectDao.createProject(project);
   }

   @Test
   public void testGetProjects() {
      createProject(CODE1);
      createProject(CODE2);

      Response response = client.target(projectUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<Project> projects = response.readEntity(new GenericType<List<Project>>() {
      });
      assertThat(projects).extracting(Project::getCode).containsOnly(CODE1, CODE2);

      Project project1 = projects.get(0);
      assertThat(project1.getName()).isEqualTo(NAME);
      assertThat(project1.getIcon()).isEqualTo(ICON);
      assertThat(project1.getColor()).isEqualTo(COLOR);
      Permissions permissions1 = project1.getPermissions();
      assertThat(permissions1.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions1.getUserPermissions().stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(USER_ROLES);
      assertThat(permissions1.getGroupPermissions().iterator()).toIterable().containsOnly(groupPermission);

      Project project2 = projects.get(1);
      assertThat(project2.getName()).isEqualTo(NAME);
      assertThat(project2.getIcon()).isEqualTo(ICON);
      assertThat(project2.getColor()).isEqualTo(COLOR);
      Permissions permissions2 = project2.getPermissions();
      assertThat(permissions2.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions2.getUserPermissions().stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(USER_ROLES);
      assertThat(permissions2.getGroupPermissions().iterator()).toIterable().containsOnly(groupPermission);
   }

   @Test
   public void testGetProject() {
      final Project project = createProject(CODE1);

      Response response = client.target(projectUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Project returnedProject = response.readEntity(Project.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(returnedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedProject.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedProject.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();
   }

   @Test
   public void testDeleteProject() {
      final Project project = createProject(CODE1);

      Response response = client.target(projectUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(projectUrl).build());

      assertThatThrownBy(() -> projectDao.getProjectById(project.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateProject() {
      Project project = new Project(CODE1, NAME, ICON, COLOR, null, null, null, false, null);
      Entity entity = Entity.json(project);

      Response response = client.target(projectUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Project returnedProject = response.readEntity(Project.class);

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(returnedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedProject.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedProject.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateProject() {
      final Project project = createProject(CODE1);

      Project updatedProject = new Project(CODE2, NAME, ICON, COLOR, null, null, null, false, null);
      Entity entity = Entity.json(updatedProject);

      Response response = client.target(projectUrl).path(project.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Project returnedProject = response.readEntity(Project.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedProject.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedProject.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedProject.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();

      Project storedProject = projectDao.getProjectById(project.getId());
      assertThat(storedProject).isNotNull();

      assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE2);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedProject.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();
   }

   @Test
   public void testGetProjectPermissions() {
      final Project project = createProject(CODE1);

      Response response = client.target(projectUrl).path(project.getId()).path("permissions")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = response.readEntity(Permissions.class);
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      final Project project = createProject(CODE1);

      Permission[] userPermission = { Permission.buildWithRoles(this.user.getId(), new HashSet<>(Arrays.asList(RoleOld.MANAGE, RoleOld.READ))) };
      Entity entity = Entity.json(userPermission);

      Response response = client.target(projectUrl).path(project.getId()).path("permissions").path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<Set<Permission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission[0]);

      Permissions storedPermissions = projectDao.getProjectByCode(CODE1).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission[0]);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      final Project project = createProject(CODE1);

      Response response = client.target(projectUrl).path(project.getId()).path("permissions").path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(projectUrl + "/" + project.getId() + "/permissions").build());

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      final Project project = createProject(CODE1);

      Permission[] groupPermission = { Permission.buildWithRoles(GROUP, new HashSet<>(Arrays.asList(RoleOld.SHARE, RoleOld.READ))) };
      Entity entity = Entity.json(groupPermission);

      Response response = client.target(projectUrl).path(project.getId()).path("permissions").path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<Set<Permission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission[0]);

      Permissions storedPermissions = projectDao.getProjectByCode(CODE1).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission[0]);
   }

   @Test
   public void testRemoveGroupPermission() {
      final Project project = createProject(CODE1);

      Response response = client.target(projectUrl).path(project.getId()).path("permissions").path("groups").path(GROUP)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(projectUrl + "/" + project.getId() + "/permissions").build());

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   private Response createProjectViaRest(final String code) {
      Project project = new Project(code, NAME, ICON, COLOR, null, null, null, false, null);
      Entity entity = Entity.json(project);
      return client.target(projectUrl).request(MediaType.APPLICATION_JSON).buildPost(entity).invoke();
   }

   @Test
   public void testTooManyProjects() {
      Response response = createProjectViaRest(CODE1);
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      response = createProjectViaRest(CODE2);
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      response = createProjectViaRest(CODE3);
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      response = createProjectViaRest(CODE4);
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.PAYMENT_REQUIRED);
   }

}
