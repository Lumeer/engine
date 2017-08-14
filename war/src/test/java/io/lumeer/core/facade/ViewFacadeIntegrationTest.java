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
package io.lumeer.core.facade;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.model.MongoProject;
import io.lumeer.storage.mongodb.model.MongoUser;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;

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
public class ViewFacadeIntegrationTest extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String PROJECT_CODE = "TPROJ";

   private static final String CODE = "TVIEW";
   private static final String NAME = "Test view";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ff00";
   private static final JsonQuery QUERY;
   private static final Perspective PERSPECTIVE = Perspective.COLLECTION_POSTIT;

   private static final Permission USER_PERMISSION;
   private static final Permission GROUP_PERMISSION;

   private static final String CODE2 = "TVIEW2";

   static {
      QUERY = new JsonQuery(Collections.singleton("testCollection"), Collections.singleton("testAttribute=42"), "test", 0, Integer.MAX_VALUE);

      USER_PERMISSION = new SimplePermission(USER, View.ROLES);
      GROUP_PERMISSION = new SimplePermission(GROUP, Collections.singleton(Role.READ));
   }

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void configureProject() {
      MongoUser user = new MongoUser();
      user.setUsername(USER);
      userDao.createUser(user);

      MongoProject project = new MongoProject();
      project.setCode(PROJECT_CODE);
      project.setPermissions(new MongoPermissions());
      Project returnedProject = projectDao.createProject(project);

      workspaceKeeper.setProject(PROJECT_CODE);

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
      assertions.assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateView() {
      createView(CODE);

      View updatedView = prepareView(CODE2);
      updatedView.getPermissions().removeUserPermission(USER);

      viewFacade.updateView(CODE, updatedView);

      View storedView = viewDao.getViewByCode(CODE2);
      assertThat(storedView).isNotNull();
      assertThat(storedView.getName()).isEqualTo(NAME);
      assertThat(storedView.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
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
      assertions.assertThat(storedView.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      assertPermissions(storedView.getPermissions().getUserPermissions(), USER_PERMISSION);
   }

   @Test
   public void testGetAllViews() {
      createView(CODE);
      createView(CODE2);

      assertThat(viewFacade.getAllViews())
            .extracting(Resource::getCode).containsOnly(CODE, CODE2);
   }

   @Test
   public void testGetViewPermissions() {
      createView(CODE);

      Permissions permissions = viewFacade.getViewPermissions(CODE);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateUserPermissions() {
      createView(CODE);

      SimplePermission userPermission = new SimplePermission(USER, new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      viewFacade.updateUserPermissions(CODE, userPermission);

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testRemoveUserPermission() {
      createView(CODE);

      viewFacade.removeUserPermission(CODE, USER);

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createView(CODE);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      viewFacade.updateGroupPermissions(CODE, groupPermission);

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createView(CODE);

      viewFacade.removeGroupPermission(CODE, GROUP);

      Permissions permissions = viewDao.getViewByCode(CODE).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }
}
