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
package io.lumeer.remote.rest;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonQuery;
import io.lumeer.api.dto.JsonView;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.model.MongoOrganization;
import io.lumeer.storage.mongodb.model.MongoProject;
import io.lumeer.storage.mongodb.model.MongoUser;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@RunWith(Arquillian.class)
public class ViewServiceIntegrationTest extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ff00";
   private static final JsonQuery QUERY;
   private static final Perspective PERSPECTIVE = Perspective.COLLECTION_POSTIT;

   private static final Set<Role> USER_ROLES = View.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(Role.READ);
   private static final Permission USER_PERMISSION = new SimplePermission(USER, USER_ROLES);
   private static final Permission GROUP_PERMISSION = new SimplePermission(GROUP, GROUP_ROLES);

   private static final String CODE2 = "TVIEW2";

   private static final String SERVER_URL = "http://localhost:8080";
   private static final String VIEWS_PATH = "/" + PATH_CONTEXT + "/rest/" + "organizations/" + ORGANIZATION_CODE + "/projects/" + PROJECT_CODE + "/views";
   private static final String VIEWS_URL = SERVER_URL + VIEWS_PATH;
   private static final String PERMISSIONS_URL = VIEWS_URL + "/" + CODE + "/permissions";

   static {
      QUERY = new JsonQuery(Collections.singleton("testCollection"), Collections.singleton("testAttribute=42"), "test", 0, Integer.MAX_VALUE);
   }

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private ViewDao viewDao;

   private Client client;

   @Before
   public void createClient() {
      client = ClientBuilder.newBuilder().build();
   }

   @After
   public void closeClient() {
      if (client != null) {
         client.close();
      }
   }

   @Before
   public void configureProject() {
      MongoUser user = new MongoUser();
      user.setUsername(USER);
      userDao.createUser(user);

      MongoOrganization organization = new MongoOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new MongoPermissions());
      organizationDao.createOrganization(organization);

      MongoProject project = new MongoProject();
      project.setCode(PROJECT_CODE);
      project.setPermissions(new MongoPermissions());
      Project returnedProject = projectDao.createProject(project);

      viewDao.setProject(returnedProject);
   }

   private View prepareView(String code) {
      return new JsonView(code, NAME, ICON, COLOR, QUERY, PERSPECTIVE.toString());
   }

   private View createView(String code) {
      View view = prepareView(code);
      view.getPermissions().updateUserPermissions(USER_PERMISSION);
      view.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      return viewDao.createView(view);
   }

   @Test
   public void testCreateView() {
      View view = prepareView(CODE);
      Entity entity = Entity.json(view);

      Response response = client.target(VIEWS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);
      assertThat(response.getLocation().getPath()).isEqualTo(VIEWS_PATH + "/" + CODE);

      View storedView = viewDao.getViewByCode(CODE);
      assertThat(storedView).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedView.getName()).isEqualTo(NAME);
      assertions.assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedView.getQuery()).isEqualTo(QUERY);
      assertions.assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateView() {
      createView(CODE);

      View updatedView = prepareView(CODE2);
      Entity entity = Entity.json(updatedView);

      Response response = client.target(VIEWS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(JsonView.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedView.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedView.getQuery()).isEqualTo(QUERY);
      assertions.assertThat(returnedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(returnedView.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(returnedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      View storedView = viewDao.getViewByCode(CODE2);
      assertThat(storedView).isNotNull();

      assertions = new SoftAssertions();
      assertions.assertThat(storedView.getCode()).isEqualTo(CODE2);
      assertions.assertThat(storedView.getName()).isEqualTo(NAME);
      assertions.assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedView.getQuery()).isEqualTo(QUERY);
      assertions.assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedView.getPermissions().getGroupPermissions()).containsOnly(GROUP_PERMISSION);
      assertions.assertAll();
   }

   @Test
   public void testDeleteView() {
      createView(CODE);

      Response response = client.target(VIEWS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(VIEWS_URL).build());

      assertThatThrownBy(() -> viewDao.getViewByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetViewByCode() {
      createView(CODE);

      Response response = client.target(VIEWS_URL).path(CODE)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(JsonView.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(returnedView.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedView.getQuery()).isEqualTo(QUERY);
      assertions.assertThat(returnedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(returnedView.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(returnedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testGetAllViews() {
      createView(CODE);
      createView(CODE2);

      Response response = client.target(VIEWS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<JsonView> views = response.readEntity(new GenericType<List<JsonView>>() {
      });
      assertThat(views).extracting(Resource::getCode).containsOnly(CODE, CODE2);

      Permissions permissions1 = views.get(0).getPermissions();
      assertThat(permissions1).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(USER_PERMISSION));
      assertThat(permissions1).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions1).extracting(Permissions::getGroupPermissions).containsOnly(Collections.emptySet());

      Permissions permissions2 = views.get(1).getPermissions();
      assertThat(permissions2).extracting(Permissions::getUserPermissions).containsOnly(Collections.singleton(USER_PERMISSION));
      assertThat(permissions2).extracting(p -> p.getUserPermissions().iterator().next().getRoles()).containsOnly(USER_ROLES);
      assertThat(permissions2).extracting(Permissions::getGroupPermissions).containsOnly(Collections.emptySet());
   }

   @Test
   public void testGetViewPermissions() {
      createView(CODE);

      Response response = client.target(PERMISSIONS_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Permissions permissions = response.readEntity(JsonPermissions.class);
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateUserPermissions() {
      createView(CODE);

      SimplePermission userPermission = new SimplePermission(USER, new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      Entity entity = Entity.json(userPermission);

      Response response = client.target(PERMISSIONS_URL).path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<JsonPermission> returnedPermissions = response.readEntity(new GenericType<Set<JsonPermission>>() {});
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission);

      Permissions storedPermissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testRemoveUserPermission() {
      createView(CODE);

      Response response = client.target(PERMISSIONS_URL).path("users").path(USER)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createView(CODE);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      Entity entity = Entity.json(groupPermission);

      Response response = client.target(PERMISSIONS_URL).path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<JsonPermission> returnedPermissions = response.readEntity(new GenericType<Set<JsonPermission>>() {});
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission);

      Permissions storedPermissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createView(CODE);

      Response response = client.target(PERMISSIONS_URL).path("groups").path(GROUP)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(PERMISSIONS_URL).build());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

}
