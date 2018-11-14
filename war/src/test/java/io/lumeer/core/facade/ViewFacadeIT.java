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
package io.lumeer.core.facade;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.*;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.engine.IntegrationTestBase;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class ViewFacadeIT extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String PROJECT_CODE = "TPROJ";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ff00";
   private static final Query QUERY;
   private static final String PERSPECTIVE = "postit";
   private static final Object CONFIG = "configuration object";

   private Permission userPermission;
   private Permission groupPermission;
   private User user;

   private static final String CODE2 = "TVIEW2";

   private static final String ORGANIZATION_CODE = "TORG";

   static {
      QUERY = new Query(Collections.singleton("testAttribute=42"), new HashSet<>(), null, null, "test", 0, Integer.MAX_VALUE);
   }

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Before
   public void configureProject() {
      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);
      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      User user = new User(USER);
      this.user = userDao.createUser(user);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      this.userPermission = Permission.buildWithRoles(this.user.getId(), View.ROLES);
      this.groupPermission = Permission.buildWithRoles(GROUP, Collections.singleton(Role.READ));

      Project project = new Project();
      project.setCode(PROJECT_CODE);
      project.setPermissions(new Permissions());
      Project storedProject = projectDao.createProject(project);

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(this.user.getId(), Project.ROLES);
      projectPermissions.updateUserPermissions(userProjectPermission);
      storedProject.setPermissions(projectPermissions);
      storedProject = projectDao.updateProject(storedProject.getId(), storedProject);

      viewDao.setProject(storedProject);

      Collection collection = collectionFacade.createCollection(
            new Collection("abc", "abc random", ICON, COLOR, projectPermissions));
      collectionFacade.updateUserPermissions(collection.getId(), Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ)));
      QUERY.getCollectionIds().clear();
      QUERY.getCollectionIds().add(collection.getId());
   }

   private View prepareView(String code) {
      return new View(code, NAME, ICON, COLOR, null, null, QUERY, PERSPECTIVE.toString(), CONFIG, this.user.getId());
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
      assertions.assertThat(storedView.getQuery()).isEqualTo(QUERY);
      assertions.assertThat(storedView.getPerspective()).isEqualTo(PERSPECTIVE);
      assertions.assertThat(storedView.getConfig()).isEqualTo(CONFIG);
      assertions.assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateView() {
      createView(CODE);

      View updatedView = prepareView(CODE2);
      updatedView.getPermissions().removeUserPermission(this.user.getId());

      viewFacade.updateView(CODE, updatedView);

      View storedView = viewDao.getViewByCode(CODE2);
      assertThat(storedView).isNotNull();
      assertThat(storedView.getName()).isEqualTo(NAME);
      assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(userPermission);
   }

   @Test
   public void testDeleteView() {
      createView(CODE);

      viewFacade.deleteView(CODE);

      assertThatThrownBy(() -> viewDao.getViewByCode(CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testGetViewByCode() {
      createView(CODE);

      View storedView = viewFacade.getViewByCode(CODE);
      assertThat(storedView).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedView.getCode()).isEqualTo(CODE);
      assertions.assertThat(storedView.getName()).isEqualTo(NAME);
      assertions.assertThat(storedView.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedView.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedView.getQuery()).isEqualTo(QUERY);
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

      assertThat(viewFacade.getViews(new Pagination(null, null)))
            .extracting(Resource::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewPermissions() {
      createView(CODE);

      Permissions permissions = viewFacade.getViewPermissions(CODE);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      createView(CODE);

      Permission userPermission = Permission.buildWithRoles(this.user.getId(), new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      viewFacade.updateUserPermissions(CODE, userPermission);

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      createView(CODE);

      viewFacade.removeUserPermission(CODE, this.user.getId());

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createView(CODE);

      Permission groupPermission = Permission.buildWithRoles(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      viewFacade.updateGroupPermissions(CODE, groupPermission);

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createView(CODE);

      viewFacade.removeGroupPermission(CODE, GROUP);

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

   @Test
   public void testCollectionAccessViaView() {
      final String NON_EXISTING_USER = "aaaaa4444400000000111111"; // non-existing user
      final String COLLECTION_NAME = "kolekce1";
      final String COLLECTION_ICON = "fa-eye";
      final String COLLECTION_COLOR = "#abcdea";

      // create collection under a different user
      Permissions collectionPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(NON_EXISTING_USER, Collection.ROLES);
      collectionPermissions.updateUserPermissions(userPermission);

      Collection collection = collectionFacade.createCollection(
            new Collection("", COLLECTION_NAME, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions));
      collectionFacade.updateUserPermissions(collection.getId(), Permission.buildWithRoles(this.user.getId(), Collections.emptySet()));

      // we are not able to read the collection now
      try {
         collectionFacade.getCollection(collection.getId());
         fail("Still able to access collection where I have no access rights");
      } catch (Exception e) {
         assertThat(e).isInstanceOf(NoPermissionException.class);
      }

      // create a view under a different user
      View view = createView(CODE2);
      view.setAuthorId(NON_EXISTING_USER);
      view.setQuery(new Query(Collections.singleton(collection.getId()), Collections.emptySet(), Collections.emptySet()));
      viewDao.updateView(view.getId(), view);

      viewFacade.updateUserPermissions(view.getCode(), Permission.buildWithRoles(NON_EXISTING_USER, View.ROLES), Permission.buildWithRoles(this.user.getId(), Collections.emptySet()));

      try {
         viewFacade.getViewByCode(CODE2);
         fail("Still able to access view where I have no access rights");
      } catch (Exception e) {
         assertThat(e).isInstanceOf(NoPermissionException.class);
      }

      try {
         viewFacade.updateUserPermissions(view.getCode(), Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ)));
         fail("Can manage view without manage rights");
      } catch (Exception e) {
         assertThat(e).isInstanceOf(NoPermissionException.class);
      }

      // share the view and make sure we can see it now
      Permissions viewPermissions = new Permissions();
      viewPermissions.updateUserPermissions(Permission.buildWithRoles(NON_EXISTING_USER, View.ROLES));
      viewPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Collections.singleton(Role.READ)));
      view.setPermissions(viewPermissions);
      viewDao.updateView(view.getId(), view); // since we lost manage rights, we can only do it directly
      permissionsChecker.invalidateCache(view);

      // now this should be all possible
      viewFacade.getViewByCode(CODE2);

      // access the collection via the view with the current user
      PermissionCheckerUtil.setViewCode(permissionsChecker, view.getCode());
      collectionFacade.getCollection(collection.getId());
   }
}
