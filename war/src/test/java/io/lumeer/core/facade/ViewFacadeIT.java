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
package io.lumeer.core.facade;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.*;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
public class ViewFacadeIT extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String PROJECT_CODE = "TPROJ";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ff00";
   private Query query = new Query();
   private static final Perspective PERSPECTIVE = Perspective.Kanban;
   private static final Object CONFIG = "configuration object";

   private Permission userPermission;
   private Permission groupPermission;
   private User user;
   private Group group;
   private Organization organization;
   private Project project;

   private static final String CODE2 = "TVIEW2";

   private static final String ORGANIZATION_CODE = "TORG";

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private PermissionsChecker permissionsChecker;

   @BeforeEach
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);
      groupDao.setOrganization(storedOrganization);
      group = groupDao.createGroup(new Group(GROUP, Collections.singletonList(user.getId())));
      user.setOrganizations(Collections.singleton(storedOrganization.getId()));
      this.user = userDao.updateUser(user.getId(), user);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read)));
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      this.organization = organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      this.userPermission = Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read)));
      this.groupPermission = Permission.buildWithRoles(this.group.getId(), Set.of(new Role(RoleType.Read)));

      Project project = new Project();
      project.setCode(PROJECT_CODE);
      project.setPermissions(new Permissions());
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(storedOrganization.getId(), storedProject.getId());

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.ViewContribute), new Role(RoleType.CollectionContribute)));
      projectPermissions.updateUserPermissions(userProjectPermission);
      storedProject.setPermissions(projectPermissions);
      this.project = projectDao.updateProject(storedProject.getId(), storedProject);

      viewDao.setProject(this.project);

      Collection collection = collectionFacade.createCollection(
            new Collection("abc", "abc random", ICON, COLOR, projectPermissions));
      collectionFacade.updateUserPermissions(collection.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read)))));
      query = new Query(new QueryStem(collection.getId()));

      PermissionCheckerUtil.allowGroups();
      permissionsChecker.getPermissionAdapter().invalidateUserCache();
   }

   private View prepareView(String code) {
      return new View(code, NAME, ICON, COLOR, null, null, null, query, Collections.emptyList(), PERSPECTIVE, CONFIG, null, this.user.getId(), Collections.emptyList());
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

      View returnedView = viewFacade.createView(view);
      assertThat(returnedView).isNotNull();
      assertThat(returnedView.getId()).isNotNull();

      View storedView = viewDao.getViewByCode(CODE);
      assertThat(storedView).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedView.getName()).isEqualTo(NAME);
      assertions.assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedView.getQuery()).isEqualTo(query);
      assertions.assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(storedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateView() {
      final View view = createView(CODE);

      View updatedView = prepareView(CODE);
      updatedView.setName("Some other name");

      viewFacade.updateView(view.getId(), updatedView);

      View storedView = viewDao.getViewById(view.getId());
      assertThat(storedView).isNotNull();
      assertThat(storedView.getName()).isEqualTo(NAME);

      setViewGroupRoles(view, Set.of(new Role(RoleType.Read), new Role(RoleType.Manage)));
      updatedView.setPermissions(view.getPermissions());

      viewFacade.updateView(view.getId(), updatedView);

      storedView = viewDao.getViewById(view.getId());
      assertThat(storedView).isNotNull();
      assertThat(storedView.getName()).isEqualTo("Some other name");
      assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
   }

   @Test
   public void testDeleteView() {
      final View view = createView(CODE);

      setViewGroupRoles(view, Set.of(new Role(RoleType.Read), new Role(RoleType.Manage)));

      viewFacade.deleteView(view.getId());

      assertThatThrownBy(() -> viewDao.getViewByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetViewByCode() {
      final View view = createView(CODE);

      View storedView = viewFacade.getViewById(view.getId());
      assertThat(storedView).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedView.getName()).isEqualTo(NAME);
      assertions.assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedView.getQuery()).isEqualTo(query);
      assertions.assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(storedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertAll();

      assertPermissions(storedView.getPermissions().getUserPermissions(), userPermission);
      assertPermissions(storedView.getPermissions().getGroupPermissions(), groupPermission);
   }

   @Test
   public void testGetAllViews() {
      createView(CODE);
      createView(CODE2);

      assertThat(viewFacade.getViews())
            .extracting(Resource::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewPermissions() {
      final View view = createView(CODE);

      View viewWithPermissions = setViewUserRoles(view, Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)));

      Permissions permissions = viewFacade.getViewPermissions(view.getId());
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), viewWithPermissions.getPermissions().getUserPermissions().toArray(new Permission[0]));
      assertPermissions(permissions.getGroupPermissions(), viewWithPermissions.getPermissions().getGroupPermissions().toArray(new Permission[0]));
   }

   @Test
   public void testUpdateUserPermissions() {
      final View view = createView(CODE);

      View viewWithPermissions = setViewGroupRoles(view, Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)));

      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.QueryConfig), new Role(RoleType.PerspectiveConfig)));
      viewFacade.updateUserPermissions(view.getId(), Set.of(userPermission));

      Permissions permissions = viewDao.getViewById(view.getId()).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), viewWithPermissions.getPermissions().getGroupPermissions().toArray(new Permission[0]));
   }

   @Test
   public void testRemoveUserPermission() {
      final View view = createView(CODE);

      View viewWithPermissions = setViewGroupRoles(view, Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)));

      viewFacade.removeUserPermission(view.getId(), this.user.getId());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), viewWithPermissions.getPermissions().getGroupPermissions().toArray(new Permission[0]));
   }

   @Test
   public void testUpdateGroupPermissions() {
      final View view = createView(CODE);

      View viewWithPermissions = setViewGroupRoles(view, Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)));

      Permission groupPermission = Permission.buildWithRoles(group.getId(), Set.of(new Role(RoleType.UserConfig)));
      viewFacade.updateGroupPermissions(view.getId(), Set.of(groupPermission));

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), viewWithPermissions.getPermissions().getUserPermissions().toArray(new Permission[0]));
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      final View view = createView(CODE);

      View viewWithPermissions = setViewUserRoles(view, Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)));

      viewFacade.removeGroupPermission(view.getId(), group.getId());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), viewWithPermissions.getPermissions().getUserPermissions().toArray(new Permission[0]));
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   @Test
   public void testCollectionAccessViaView() {
      final String NON_EXISTING_USER = "aaaaa4444400000000111111"; // non-existing user
      final String COLLECTION_NAME = "kolekce1";
      final String COLLECTION_ICON = "fa-eye";
      final String COLLECTION_COLOR = "#abcdea";

      setOrganizationUserRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.UserConfig)));
      setProjectUserRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.ViewContribute), new Role(RoleType.CollectionContribute), new Role(RoleType.UserConfig)));

      Permission workspacePermission = Permission.buildWithRoles(NON_EXISTING_USER, Set.of(new Role(RoleType.Read)));
      organizationFacade.updateUserPermissions(organization.getId(), Set.of(workspacePermission));
      projectFacade.updateUserPermissions(project.getId(), Set.of(workspacePermission));

      // create collection under a different user
      Permissions collectionPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(NON_EXISTING_USER, Collection.ROLES);
      collectionPermissions.updateUserPermissions(userPermission);

      Collection collection = collectionFacade.createCollection(
            new Collection("", COLLECTION_NAME, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions));
      collectionFacade.updateUserPermissions(collection.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Collections.emptySet())));

      removeOrganizationManagePermission();
      removeProjectManagePermission();

      // we are not able to read the collection now
      try {
         collectionFacade.getCollection(collection.getId());
         fail("Still able to access collection where I have no access rights");
      } catch (Exception e) {
         assertThat(e).isInstanceOf(NoResourcePermissionException.class);
      }

      // create a view under a different user
      View view = createView(CODE2);
      view.setAuthorId(NON_EXISTING_USER);
      view.setQuery(new Query(new QueryStem(collection.getId())));

      Permissions permissions = new Permissions();
      permissions.updateUserPermissions(Set.of(Permission.buildWithRoles(NON_EXISTING_USER, View.ROLES), Permission.buildWithRoles(this.user.getId(), Collections.emptySet())));
      view.setPermissions(permissions);
      viewDao.updateView(view.getId(), view);

      try {
         viewFacade.getViewById(view.getId());
         fail("Still able to access view where I have no access rights");
      } catch (Exception e) {
         assertThat(e).isInstanceOf(NoResourcePermissionException.class);
      }

      try {
         viewFacade.updateUserPermissions(view.getId(), Set.of(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read)))));
         fail("Can manage view without manage rights");
      } catch (Exception e) {
         assertThat(e).isInstanceOf(NoResourcePermissionException.class);
      }

      // share the view and make sure we can see it now
      Permissions viewPermissions = new Permissions();
      viewPermissions.updateUserPermissions(Permission.buildWithRoles(NON_EXISTING_USER, View.ROLES));
      viewPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read))));
      view.setPermissions(viewPermissions);
      viewDao.updateView(view.getId(), view); // since we lost manage rights, we can only do it directly

      // now this should be all possible
      viewFacade.getViewById(view.getId());

      // access the collection via the view with the current user
      PermissionCheckerUtil.setViewId(permissionsChecker, view.getId());
      collectionFacade.getCollection(collection.getId());
   }

   @Test
   public void testGetAllViewsProjectManager() {
      viewDao.createView(prepareView("CD1"));
      viewDao.createView(prepareView("CD2"));

      setOrganizationUserRoles(Set.of(new Role(RoleType.Read, true)));
      setProjectUserRoles(Set.of(new Role(RoleType.Read, true)));

      assertThat(viewFacade.getViews()).hasSize(2);

      setProjectUserRoles(Set.of(new Role(RoleType.Read)));

      assertThat(viewFacade.getViews()).hasSize(2);

      setOrganizationUserRoles(Set.of(new Role(RoleType.Read)));

      assertThat(viewFacade.getViews()).isEmpty();
   }

   private void removeOrganizationManagePermission() {
      Permissions organizationPermissions = organizationDao.getOrganizationById(organization.getId()).getPermissions();
      organizationPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read))));
      organization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.clear();
   }

   private void removeProjectManagePermission() {
      Permissions projectPermissions = projectDao.getProjectById(project.getId()).getPermissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read))));
      project.setPermissions(projectPermissions);
      projectDao.updateProject(project.getId(), project);
      workspaceCache.clear();
   }

   private void setOrganizationUserRoles(final Set<Role> roles) {
      Permissions organizationPermissions = new Permissions();
      organizationPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), roles));
      organization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.clear();
   }

   private void setProjectUserRoles(final Set<Role> roles) {
      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), roles));
      project.setPermissions(projectPermissions);
      projectDao.updateProject(project.getId(), project);
      workspaceCache.clear();
   }

   private View setViewUserRoles(View view, final Set<Role> roles) {
      Permissions permissions = view.getPermissions();
      permissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), roles));
      view.setPermissions(permissions);
      return viewDao.updateView(view.getId(), view, null);
   }

   private View setViewGroupRoles(View view, final Set<Role> roles) {
      Permissions permissions = view.getPermissions();
      permissions.updateGroupPermissions(Permission.buildWithRoles(this.group.getId(), roles));
      view.setPermissions(permissions);
      return viewDao.updateView(view.getId(), view, null);
   }
}
