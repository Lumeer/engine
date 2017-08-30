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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.model.MorphiaOrganization;
import io.lumeer.storage.mongodb.model.MorphiaUser;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

import org.assertj.core.api.Assertions;
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
public class ProjectFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private OrganizationDao organizationDao;

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TPROJ1";
   private static final String CODE2 = "TPROJ2";

   private static final String NAME = "Testing project";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Permission USER_PERMISSION;
   private static final Permission GROUP_PERMISSION;

   private static final String ORGANIZATION_CODE = "TORG";

   static {
      USER_PERMISSION = new SimplePermission(USER, Project.ROLES);
      GROUP_PERMISSION = new SimplePermission(GROUP, Collections.singleton(Role.READ));
   }

   private Project createProject(String code) {
      Project project = new JsonProject(code, NAME, ICON, COLOR, null);
      project.getPermissions().updateUserPermissions(USER_PERMISSION);
      project.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      return projectDao.createProject(project);
   }

   @Before
   public void configureProject() {
      MorphiaOrganization organization = new MorphiaOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new MorphiaPermissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      userDao.setOrganization(storedOrganization);
      projectDao.setOrganization(storedOrganization);

      MorphiaUser user = new MorphiaUser();
      user.setUsername(USER);
      userDao.createUser(user);

      workspaceKeeper.setOrganization(ORGANIZATION_CODE);
   }

   @Test
   public void testGetProjects() {
      createProject(CODE1);
      createProject(CODE2);

      assertThat(projectFacade.getProjects())
            .extracting(Resource::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testGetProjectByCode() {
      createProject(CODE1);

      Project storedProject = projectFacade.getProject(CODE1);
      assertThat(storedProject).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      assertPermissions(storedProject.getPermissions().getUserPermissions(), USER_PERMISSION);
   }

   @Test
   public void testDeleteProject() {
      createProject(CODE1);

      projectFacade.deleteProject(CODE1);

      assertThatThrownBy(() -> projectDao.getProjectByCode(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateProject() {
      Project project = new JsonProject(CODE1, NAME, ICON, COLOR, null);

      Project returnedProject = projectFacade.createProject(project);
      assertThat(returnedProject).isNotNull();
      assertThat(returnedProject.getId()).isNotNull();

      Project storedProject = projectDao.getProjectByCode(CODE1);
      assertThat(storedProject).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedProject.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateProject() {
      String id = createProject(CODE1).getId();

      Project updatedProject = new JsonProject(CODE2, NAME, ICON, COLOR, null);
      updatedProject.getPermissions().removeUserPermission(USER);

      projectFacade.updateProject(CODE1, updatedProject);

      Project storedProject = projectDao.getProjectByCode(CODE2);
      assertThat(storedProject).isNotNull();
      assertThat(storedProject.getId()).isEqualTo(id);
      assertThat(storedProject.getName()).isEqualTo(NAME);
      assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertThat(storedProject.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
   }

   @Test
   public void testGetProjectPermissions() {
      createProject(CODE1);

      Permissions permissions = projectFacade.getProjectPermissions(CODE1);
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateUserPermissions() {
      createProject(CODE1);

      SimplePermission userPermission = new SimplePermission(USER, new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      projectFacade.updateUserPermissions(CODE1, userPermission);

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testRemoveUserPermission() {
      createProject(CODE1);

      projectFacade.removeUserPermission(CODE1, USER);

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      Assertions.assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createProject(CODE1);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      projectFacade.updateGroupPermissions(CODE1, groupPermission);

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createProject(CODE1);

      projectFacade.removeGroupPermission(CODE1, GROUP);

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      Assertions.assertThat(permissions.getGroupPermissions()).isEmpty();
   }
}
