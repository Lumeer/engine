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
package io.lumeer.remote.rest;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.model.MorphiaOrganization;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

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

   private static final String NAME = "Testing project";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Set<Role> USER_ROLES = Project.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(Role.READ);
   private Permission userPermission;
   private Permission groupPermission;

   private User user;

   private static final String SERVER_URL = "http://localhost:8080";
   private static final String PROJECT_PATH = "/" + PATH_CONTEXT + "/rest/" + "organizations/" + ORGANIZATION_CODE + "/projects";
   private static final String PROJECT_URL = SERVER_URL + PROJECT_PATH;
   private static final String PERMISSIONS_URL = PROJECT_URL + "/" + CODE1 + "/permissions";

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

      userPermission = new SimplePermission(this.user.getId(), USER_ROLES);
      groupPermission = new SimplePermission(GROUP, GROUP_ROLES);

      MorphiaOrganization organization = new MorphiaOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new MorphiaPermissions());
      organization.getPermissions().updateUserPermissions(new MorphiaPermission(this.user.getId(), Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

   }

   private Project createProject(String code) {
      Project project = new JsonProject(code, NAME, ICON, COLOR, null, null);
      project.getPermissions().updateUserPermissions(userPermission);
      project.getPermissions().updateGroupPermissions(groupPermission);
      return projectDao.createProject(project);
   }

   @Test
   public void testGetProjects() {
      createProject(CODE1);
      createProject(CODE2);

      Response response = client.target(PROJECT_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<JsonProject> projects = response.readEntity(new GenericType<List<JsonProject>>() {
      });
      assertThat(projects).extracting(Resource::getCode).containsOnly(CODE1, CODE2);

      Project project1 = projects.get(0);
      assertThat(project1.getName()).isEqualTo(NAME);
      assertThat(project1.getIcon()).isEqualTo(ICON);
      assertThat(project1.getColor()).isEqualTo(COLOR);
      Permissions permissions1 = project1.getPermissions();
      assertThat(permissions1).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(userPermission));
      assertThat(permissions1).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions1).extracting(Permissions::getGroupPermissions).containsOnly(Collections.singleton(groupPermission));

      Project project2 = projects.get(1);
      assertThat(project2.getName()).isEqualTo(NAME);
      assertThat(project2.getIcon()).isEqualTo(ICON);
      assertThat(project2.getColor()).isEqualTo(COLOR);
      Permissions permissions2 = project2.getPermissions();
      assertThat(permissions2).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(userPermission));
      assertThat(permissions2).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions2).extracting(Permissions::getGroupPermissions).containsOnly(Collections.singleton(groupPermission));
   }

   @Test
   public void testGetProject() {
      createProject(CODE1);

      Response response = client.target(PROJECT_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Project returnedProject = response.readEntity(JsonProject.class);
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
      createProject(CODE1);

      Response response = client.target(PROJECT_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PROJECT_URL).build());

      assertThatThrownBy(() -> projectDao.getProjectByCode(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateProject() {

      Project project = new JsonProject(CODE1, NAME, ICON, COLOR, null, null);
      Entity entity = Entity.json(project);

      Response response = client.target(PROJECT_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Project returnedProject = response.readEntity(JsonProject.class);

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
      createProject(CODE1);

      Project updatedProject = new JsonProject(CODE2, NAME, ICON, COLOR, null, null);
      Entity entity = Entity.json(updatedProject);

      Response response = client.target(PROJECT_URL).path(CODE1)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Project returnedProject = response.readEntity(JsonProject.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedProject.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedProject.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedProject.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();

      Project storedProject = projectDao.getProjectByCode(CODE2);
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
      createProject(CODE1);

      Response response = client.target(PERMISSIONS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = response.readEntity(JsonPermissions.class);
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      createProject(CODE1);

      SimplePermission userPermission = new SimplePermission(this.user.getId(), new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      Entity entity = Entity.json(userPermission);

      Response response = client.target(PERMISSIONS_URL).path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<JsonPermission> returnedPermissions = response.readEntity(new GenericType<Set<JsonPermission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission);

      Permissions storedPermissions = projectDao.getProjectByCode(CODE1).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      createProject(CODE1);

      Response response = client.target(PERMISSIONS_URL).path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createProject(CODE1);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      Entity entity = Entity.json(groupPermission);

      Response response = client.target(PERMISSIONS_URL).path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<JsonPermission> returnedPermissions = response.readEntity(new GenericType<Set<JsonPermission>>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission);

      Permissions storedPermissions = projectDao.getProjectByCode(CODE1).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createProject(CODE1);

      Response response = client.target(PERMISSIONS_URL).path("groups").path(GROUP)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   private Response createProjectViaRest(final String code) {
      Project project = new JsonProject(code, NAME, ICON, COLOR, null, null);
      Entity entity = Entity.json(project);
      return client.target(PROJECT_URL).request(MediaType.APPLICATION_JSON).buildPost(entity).invoke();
   }

   @Test
   public void testTooManyProjects() {
      Response response = createProjectViaRest(CODE1);
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      response = createProjectViaRest(CODE2);
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.PAYMENT_REQUIRED);
   }

}
