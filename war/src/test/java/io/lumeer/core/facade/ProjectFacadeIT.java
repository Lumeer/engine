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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
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
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
public class ProjectFacadeIT extends IntegrationTestBase {

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private PermissionsChecker permissionsChecker;

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String STRANGER_USER = "stranger@nowhere.com";
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TPROJ1";
   private static final String CODE2 = "TPROJ2";
   private static final String CODE3 = "TPROJ3";

   private static final String NAME = "Testing project";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private Permission userPermissions;
   private Permission userReadonlyPermissions;
   private Permission userStrangerPermissions;
   private Permission groupPermissions;

   private User user;
   private User stranger;
   private Group group;
   private Organization organization;

   private static final String ORGANIZATION_CODE = "TORG";

   private Project createProject(String code) {
      Project project = new Project(code, NAME, ICON, COLOR, null, null, null, false, null);
      project.getPermissions().updateUserPermissions(userPermissions);
      project.getPermissions().updateGroupPermissions(groupPermissions);
      return projectDao.createProject(project);
   }

   private Project createProjectWithoutPermissions(String code) {
      Project project = new Project(code, NAME, ICON, COLOR, null, null, null, false, null);
      return projectDao.createProject(project);
   }

   private Project createProjectWithReadOnlyPermissions(final String code) {
      Project project = new Project(code, NAME, ICON, COLOR, null, null, null, false, null);
      project.getPermissions().updateUserPermissions(Set.of(userReadonlyPermissions, userStrangerPermissions));
      project.getPermissions().updateGroupPermissions(groupPermissions);
      return projectDao.createProject(project);
   }

   private Project createProjectWithStrangerPermissions(final String code) {
      Project project = new Project(code, NAME, ICON, COLOR, null, null, null, false, null);
      project.getPermissions().updateUserPermissions(Set.of(userPermissions, userStrangerPermissions));
      project.getPermissions().updateGroupPermissions(groupPermissions);
      return projectDao.createProject(project);
   }

   private Project createProjectWithGroupRoles(final String code, final Set<Role> roles) {
      Project project = new Project(code, NAME, ICON, COLOR, null, null, null, false, null);
      project.getPermissions().updateGroupPermissions(new Permission(group.getId(), roles));
      return projectDao.createProject(project);
   }

   @BeforeEach
   public void configureProject() {
      this.user = userDao.createUser(new User(USER));
      this.stranger = userDao.createUser(new User(STRANGER_USER));

      userPermissions = Permission.buildWithRoles(this.user.getId(), Project.ROLES);
      userReadonlyPermissions = Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read)));
      userStrangerPermissions = Permission.buildWithRoles(this.stranger.getId(), Collections.singleton(new Role(RoleType.Read)));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      organization.getPermissions().updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read))));
      this.organization = organizationDao.createOrganization(organization);

      groupDao.setOrganization(this.organization);
      group = groupDao.createGroup(new Group(GROUP, Collections.singletonList(user.getId())));
      user.setOrganizations(Collections.singleton(this.organization.getId()));
      user = userDao.updateUser(user.getId(), user);
      groupPermissions = Permission.buildWithRoles(group.getId(), Collections.singleton(new Role(RoleType.Read)));

      projectDao.setOrganization(this.organization);

      workspaceKeeper.setOrganizationId(this.organization.getId());

      PermissionCheckerUtil.allowGroups();
      permissionsChecker.getPermissionAdapter().invalidateUserCache();
   }

   @Test
   public void testGetProjects() {
      createProject(CODE1);
      createProject(CODE2);

      assertThat(projectFacade.getProjects())
            .extracting(Resource::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testGetProjectsByGroupTransitive() {
      createProjectWithoutPermissions(CODE1);
      createProjectWithoutPermissions(CODE2);

      assertThat(projectFacade.getProjects()).isEmpty();

      setOrganizationGroupRoles(Set.of(new Role(RoleType.Read, true)));

      assertThat(projectFacade.getProjects()).hasSize(2);

      setOrganizationGroupRoles(Set.of(new Role(RoleType.Read)));

      assertThat(projectFacade.getProjects()).isEmpty();
   }

   private void setOrganizationGroupRoles(final Set<Role> roles) {
      Permissions organizationPermissions = new Permissions();
      organizationPermissions.updateGroupPermissions(Permission.buildWithRoles(this.group.getId(), roles));
      organization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.clear();
   }

   @Test
   public void testGetProjectById() {
      final Project project = createProject(CODE1);

      Project storedProject = projectFacade.getProjectById(project.getId());
      assertThat(storedProject).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertAll();

      assertPermissions(storedProject.getPermissions().getUserPermissions(), userPermissions);
      assertPermissions(storedProject.getPermissions().getGroupPermissions(), groupPermissions);
   }

   @Test
   public void testDeleteProject() {
      final Project project = createProject(CODE1);

      projectFacade.deleteProject(project.getId());

      assertThatThrownBy(() -> projectDao.getProjectByCode(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testDeleteProjectByGroup() {
      final Project project = createProjectWithGroupRoles(CODE1, Set.of(new Role(RoleType.Manage)));

      projectFacade.deleteProject(project.getId());

      assertThatThrownBy(() -> projectDao.getProjectByCode(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);

      final Project project2 = createProjectWithGroupRoles(CODE1, Set.of(new Role(RoleType.Read)));

      assertThatThrownBy(() -> projectFacade.deleteProject(project2.getId()))
            .isInstanceOf(NoResourcePermissionException.class);
   }

   @Test
   public void testCreateProject() {
      setOrganizationUserRoles(Set.of(new Role(RoleType.Read)));
      Project project = new Project(CODE1, NAME, ICON, COLOR, null, null, null, false, null);

      assertThatThrownBy(() -> projectFacade.createProject(project))
            .isInstanceOf(NoResourcePermissionException.class);

      setOrganizationUserRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.ProjectContribute)));
      Project returnedProject = projectFacade.createProject(project);
      assertThat(returnedProject).isNotNull();
      assertThat(returnedProject.getId()).isNotNull();

      Project storedProject = projectFacade.getProjectById(returnedProject.getId());
      assertThat(storedProject).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getPermissions().getUserPermissions()).containsOnly(userPermissions);
      assertions.assertThat(storedProject.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   private void setOrganizationUserRoles(final Set<Role> roles) {
      Permissions organizationPermissions = new Permissions();
      organizationPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), roles));
      organization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.clear();
   }

   @Test
   public void testUpdateProject() {
      String id = createProject(CODE1).getId();

      Project updatedProject = new Project(CODE2, NAME, ICON, COLOR, null, null, new Permissions(Set.of(userPermissions), Set.of(groupPermissions)), false, null);

      projectFacade.updateProject(id, updatedProject);

      Project storedProject = projectDao.getProjectByCode(CODE2);
      assertThat(storedProject).isNotNull();
      assertThat(storedProject.getId()).isEqualTo(id);
      assertThat(storedProject.getName()).isEqualTo(NAME);
      assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertThat(storedProject.getPermissions().getUserPermissions()).containsOnly(userPermissions);
   }

   @Test
   public void testGetProjectPermissions() {
      final Project project = createProject(CODE1);
      final Project project2 = createProjectWithReadOnlyPermissions(CODE2);
      final Project project3 = createProjectWithStrangerPermissions(CODE3);

      Permissions permissions = projectFacade.getProjectPermissions(project.getId());
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermissions);
      assertPermissions(permissions.getGroupPermissions(), groupPermissions);

      assertThatThrownBy(() -> projectFacade.getProjectPermissions(project2.getId()))
            .isInstanceOf(NoResourcePermissionException.class);

      permissions = projectFacade.getProjectPermissions(project3.getId());
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).hasSize(2).contains(userPermissions, userStrangerPermissions);
   }

   @Test
   public void testUpdateUserPermissions() {
      final Project project = createProject(CODE1);

      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.ViewContribute), new Role(RoleType.DataWrite, true)));
      projectFacade.updateUserPermissions(project.getId(), Set.of(userPermission));

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermissions);
   }

   @Test
   public void testRemoveUserPermission() {
      final Project project = createProject(CODE1);

      projectFacade.removeUserPermission(project.getId(), this.user.getId());

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      Assertions.assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermissions);
   }

   @Test
   public void testUpdateGroupPermissions() {
      final Project project = createProject(CODE1);

      Permission groupPermission = Permission.buildWithRoles(group.getId(), Set.of(new Role(RoleType.CollectionContribute, true), new Role(RoleType.DataWrite)));
      projectFacade.updateGroupPermissions(project.getId(), Set.of(groupPermission));

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermissions);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      final Project project = createProject(CODE1);

      projectFacade.removeGroupPermission(project.getId(), group.getId() );

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermissions);
      Assertions.assertThat(permissions.getGroupPermissions()).isEmpty();
   }
}
