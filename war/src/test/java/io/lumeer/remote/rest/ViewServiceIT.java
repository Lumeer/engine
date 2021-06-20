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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.ProjectFacade;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
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
public class ViewServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ff00";
   private Query query;
   private static final String PERSPECTIVE = "postit";
   private static final Object CONFIG = "configuration object";

   private static final Set<Role> USER_ROLES = View.ROLES;
   private static final Set<Role> GROUP_ROLES = Collections.singleton(new Role(RoleType.Read));
   private Permission userPermission;
   private Permission groupPermission;

   private User user;
   private Organization organization;
   private Project project;

   private static final String CODE2 = "TVIEW2";

   private String viewsUrl;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      userPermission = Permission.buildWithRoles(this.user.getId(), USER_ROLES);
      groupPermission = Permission.buildWithRoles(GROUP, GROUP_ROLES);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);
      this.organization = storedOrganization;

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      projectDao.setOrganization(storedOrganization);

      Project project = new Project();
      project.setCode(PROJECT_CODE);
      project.setPermissions(new Permissions());
      Project storedProject = projectDao.createProject(project);
      this.project = storedProject;

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(this.user.getId(), Project.ROLES);
      projectPermissions.updateUserPermissions(userProjectPermission);
      storedProject.setPermissions(projectPermissions);
      storedProject = projectDao.updateProject(storedProject.getId(), storedProject);

      workspaceKeeper.setWorkspaceIds(storedOrganization.getId(), storedProject.getId());

      viewDao.setProject(storedProject);

      Collection collection = collectionFacade.createCollection(
            new Collection("abc", "abc random", ICON, COLOR, projectPermissions));
      collectionFacade.updateUserPermissions(collection.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read)))));
      query = new Query(new QueryStem(collection.getId()));

      viewsUrl = projectPath(storedOrganization, storedProject) + "views";

      PermissionCheckerUtil.allowGroups();
   }

   private View prepareView(String code) {
      return new View(code, NAME, ICON, COLOR, null, null, null, query, PERSPECTIVE, CONFIG, null, this.user.getId(), Collections.emptyList());
   }

   private View createView(String code) {
      View view = prepareView(code);
      view.getPermissions().updateUserPermissions(userPermission);
      view.getPermissions().updateGroupPermissions(groupPermission);
      return viewDao.createView(view);
   }

   @Test
   public void testCreateView() {
      View view = prepareView(CODE);
      Entity<View> entity = Entity.json(view);

      Response response = client.target(viewsUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(View.class);
      assertThat(returnedView).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(returnedView.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedView.getQuery()).isEqualTo(query);
      assertions.assertThat(returnedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(returnedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(returnedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateView() {
      final View view = createView(CODE);

      View updatedView = prepareView(CODE2);
      updatedView.setPermissions(new Permissions(Set.of(userPermission), Set.of(groupPermission)));
      Entity<View> entity = Entity.json(updatedView);

      Response response = client.target(viewsUrl).path(view.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(View.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getCode()).isEqualTo(CODE2);
      assertions.assertThat(returnedView.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedView.getQuery()).isEqualTo(query);
      assertions.assertThat(returnedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(returnedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(returnedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedView.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();

      View storedView = viewDao.getViewByCode(CODE2);
      assertThat(storedView).isNotNull();

      assertions = new SoftAssertions();
      assertions.assertThat(storedView.getCode()).isEqualTo(CODE2);
      assertions.assertThat(storedView.getName()).isEqualTo(NAME);
      assertions.assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedView.getQuery()).isEqualTo(query);
      assertions.assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(storedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedView.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();
   }

   @Test
   public void testDeleteView() {
      final View view = createView(CODE);

      Response response = client.target(viewsUrl).path(view.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(viewsUrl).build());

      assertThatThrownBy(() -> viewDao.getViewByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetViewByCode() {
      final View view = createView(CODE);

      Response response = client.target(viewsUrl).path(view.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(View.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(returnedView.getName()).isEqualTo(NAME);
      assertions.assertThat(returnedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(returnedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(returnedView.getQuery()).isEqualTo(query);
      assertions.assertThat(returnedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(returnedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(returnedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(returnedView.getPermissions().getGroupPermissions()).containsOnly(groupPermission);
      assertions.assertAll();
   }

   @Test
   @SuppressWarnings("unchecked")
   public void testGetViewWithAuthorRights() {
      final String USER = "aaaaa4444400000000111112"; // non-existing author

      Permission workspacePermission = new Permission(USER, Collections.singleton(new Role(RoleType.Read)));
      organizationFacade.updateUserPermissions(organization.getId(), Set.of(workspacePermission));
      projectFacade.updateUserPermissions(project.getId(), Set.of(workspacePermission));

      Permission permission = new Permission(USER, Collections.singleton(new Role(RoleType.DataWrite)));
      Collection collection = collectionFacade.createCollection(
            new Collection("cdefg", "abcefg random", ICON, COLOR, new Permissions(new HashSet<>(Collections.singletonList(permission)), Collections.emptySet())));
      collectionFacade.updateUserPermissions(collection.getId(), Set.of(Permission.buildWithRoles(USER, Set.of(new Role(RoleType.DataWrite)))));

      View view = prepareView(CODE + "3");
      view.setQuery(new Query(new QueryStem(collection.getId())));
      view.getPermissions().updateUserPermissions(userPermission);
      view.getPermissions().updateGroupPermissions(groupPermission);
      view.setAuthorId(USER);
      view.getPermissions().updateUserPermissions(permission);
      viewDao.createView(view);

      Response response = client.target(viewsUrl).path(view.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      View returnedView = response.readEntity(View.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(returnedView.getAuthorCollectionsRights()).containsOnly(new HashMap.SimpleEntry<>(collection.getId(), Set.of(RoleType.DataWrite)));
      assertions.assertAll();
   }

   @Test
   public void testGetAllViews() {
      createView(CODE);
      createView(CODE2);

      Response response = client.target(viewsUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<View> views = response.readEntity(new GenericType<>() {
      });
      assertThat(views).extracting(View::getCode).containsOnly(CODE, CODE2);

      Permissions permissions1 = views.get(0).getPermissions();
      assertThat(permissions1.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions1.getUserPermissions().stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(USER_ROLES);
      assertThat(permissions1.getGroupPermissions().iterator()).toIterable().containsOnly(groupPermission);

      Permissions permissions2 = views.get(1).getPermissions();
      assertThat(permissions2.getUserPermissions()).containsOnly(userPermission);
      assertThat(permissions2.getUserPermissions().stream().map(Permission::getRoles).collect(Collectors.toSet()).iterator()).toIterable().containsOnly(USER_ROLES);
      assertThat(permissions2.getGroupPermissions().iterator()).toIterable().containsOnly(groupPermission);
   }

   @Test
   public void testGetViewPermissions() {
      final View view = createView(CODE);

      Response response = client.target(viewsUrl).path(view.getId()).path("permissions")
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
      final View view = createView(CODE);

      Permission[] userPermission = { Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.PerspectiveConfig), new Role(RoleType.QueryConfig, true))) };
      Entity<Permission[]> entity = Entity.json(userPermission);

      Response response = client.target(viewsUrl).path(view.getId()).path("permissions").path("users")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), userPermission[0]);

      Permissions storedPermissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission[0]);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      final View view = createView(CODE);

      Response response = client.target(viewsUrl).path(view.getId()).path("permissions").path("users").path(this.user.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(viewsUrl + "/" + view.getId() + "/permissions").build());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      final View view = createView(CODE);

      Permission[] groupPermission = { Permission.buildWithRoles(GROUP, Set.of(new Role(RoleType.PerspectiveConfig, true), new Role(RoleType.QueryConfig, true))) };
      Entity<Permission[]> entity = Entity.json(groupPermission);

      Response response = client.target(viewsUrl).path(view.getId()).path("permissions").path("groups")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      Set<Permission> returnedPermissions = response.readEntity(new GenericType<>() {
      });
      assertThat(returnedPermissions).isNotNull().hasSize(1);
      assertPermissions(Collections.unmodifiableSet(returnedPermissions), groupPermission[0]);

      Permissions storedPermissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(storedPermissions).isNotNull();
      assertPermissions(storedPermissions.getUserPermissions(), userPermission);
      assertPermissions(storedPermissions.getGroupPermissions(), groupPermission[0]);
   }

   @Test
   public void testRemoveGroupPermission() {
      final View view = createView(CODE);

      Response response = client.target(viewsUrl).path(view.getId()).path("permissions").path("groups").path(GROUP)
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(viewsUrl + "/" + view.getId() + "/permissions").build());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

}
