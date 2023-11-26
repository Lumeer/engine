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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
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
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
public class OrganizationFacadeIT extends IntegrationTestBase {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private AuthenticatedUser authenticatedUser;

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String STRANGER_USER = "stranger@nowhere.com";
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TORG";
   private static final String CODE2 = "TORG2";
   private static final String CODE3 = "TORG3";
   private static final String NOT_EXISTING_CODE = "NOT_EXISTING_CODE";
   private static final String NAME = "Testing organization";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private Permission userPermission;
   private Permission userReadonlyPermission;
   private Permission userStrangerPermission;
   private Permission groupPermission;

   private User user;
   private User strangerUser;
   private Group group;

   @BeforeEach
   public void configure() {
      this.user = userDao.createUser(new User(USER));
      this.strangerUser = userDao.createUser(new User(STRANGER_USER));

      userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      userReadonlyPermission = Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read)));
      userStrangerPermission = Permission.buildWithRoles(this.strangerUser.getId(), Collections.singleton(new Role(RoleType.Read)));
      groupPermission = Permission.buildWithRoles(GROUP, Collections.singleton(new Role(RoleType.Read)));

      PermissionCheckerUtil.allowGroups();
   }

   @Test
   public void testGetOrganizations() {
      var id1 = createOrganization(CODE1);
      var id2 = createOrganization(CODE2);

      assertThat(organizationFacade.getOrganizations()).hasSize(0); // without adding the organizations to the user, they are not found

      // user and authenticatedUser are two copies of the same user,
      // however user object does not sync changes to authenticatedUser for OrganizationFacade to find them,
      // so we need to update authenticatedUser directly
      authenticatedUser.getCurrentUser().getOrganizations().add(id1);
      authenticatedUser.getCurrentUser().getOrganizations().add(id2);

      final List<Organization> organizations = organizationFacade.getOrganizations();

      // if the assert failed, we already reverted the authenticated user to its original state
      authenticatedUser.getCurrentUser().getOrganizations().remove(id1);
      authenticatedUser.getCurrentUser().getOrganizations().remove(id2);

      assertThat(organizations).extracting(Resource::getCode).containsOnly(CODE1, CODE2);
   }

   private String createOrganization(final String code) {
      Organization organization = new Organization(code, NAME, ICON, COLOR, null, null, null);
      organization.getPermissions().updateUserPermissions(userPermission);
      organization.getPermissions().updateGroupPermissions(groupPermission);
      return organizationDao.createOrganization(organization).getId();
   }

   private String createOrganizationWithGroupPermissions(final String code, final RoleType roleType) {
      Organization organization = new Organization(code, NAME, ICON, COLOR, null, null, null);
      Organization storedOrganization = organizationDao.createOrganization(organization);

      groupDao.setOrganization(storedOrganization);
      group = groupDao.createGroup(new Group(GROUP, Collections.singletonList(user.getId())));
      user.setOrganizations(Collections.singleton(storedOrganization.getId()));
      user = userDao.updateUser(user.getId(), user);

      permissionsChecker.getPermissionAdapter().invalidateUserCache();

      groupPermission = Permission.buildWithRoles(this.group.getId(), Collections.singleton(new Role(roleType)));
      storedOrganization.getPermissions().updateGroupPermissions(groupPermission);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      return storedOrganization.getId();
   }

   private Organization createOrganizationWithReadOnlyPermissions(final String code) {
      Organization organization = new Organization(code, NAME, ICON, COLOR, null, null, null);
      organization.getPermissions().updateUserPermissions(Set.of(
            userReadonlyPermission,
            userStrangerPermission));
      organization.getPermissions().updateGroupPermissions(groupPermission);
      return organizationDao.createOrganization(organization);
   }

   private Organization createOrganizationWithStrangerPermissions(final String code) {
      Organization organization = new Organization(code, NAME, ICON, COLOR, null, null, null);
      organization.getPermissions().updateUserPermissions(Set.of(
            userPermission,
            userStrangerPermission));
      organization.getPermissions().updateGroupPermissions(groupPermission);
      return organizationDao.createOrganization(organization);
   }

   @Test
   public void testGetOrganization() {
      final String organizationId = createOrganization(CODE1);

      Organization storedOrganization = organizationFacade.getOrganizationById(organizationId);
      assertThat(storedOrganization).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertAll();

      assertPermissions(storedOrganization.getPermissions().getUserPermissions(), userPermission);
      assertPermissions(storedOrganization.getPermissions().getGroupPermissions(), groupPermission);
   }

   @Test
   public void testGetOrganizationByGroup() {
      final String organizationId = createOrganizationWithGroupPermissions(CODE1, RoleType.Read);

      Organization storedOrganization = organizationFacade.getOrganizationById(organizationId);
      assertThat(storedOrganization).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getPermissions().getUserPermissions()).isEmpty();
      assertions.assertAll();

      assertPermissions(storedOrganization.getPermissions().getGroupPermissions(), groupPermission);

      groupDao.deleteGroup(group.getId());
      permissionsChecker.getPermissionAdapter().invalidateUserCache();

      assertThatThrownBy(() -> organizationFacade.getOrganizationById(organizationId))
            .isInstanceOf(NoResourcePermissionException.class);
   }

   @Test
   public void testGetOrganizationNotExisting() {
      assertThatThrownBy(() -> organizationDao.getOrganizationByCode(NOT_EXISTING_CODE))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testDeleteOrganization() {
      final String organizationId = createOrganization(CODE1);
      organizationFacade.deleteOrganization(organizationId);

      assertThatThrownBy(() -> organizationDao.getOrganizationByCode(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testDeleteOrganizationByGroup() {
      final String organizationId = createOrganizationWithGroupPermissions(CODE1, RoleType.Manage);
      organizationFacade.deleteOrganization(organizationId);

      assertThatThrownBy(() -> organizationDao.getOrganizationByCode(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateOrganization() {
      Organization organization = new Organization(CODE1, NAME, ICON, COLOR, null, null, null);

      Organization createdOrganization = organizationFacade.createOrganization(organization);
      assertThat(createdOrganization).isNotNull();
      assertThat(createdOrganization.getId()).isNotNull();

      Organization storedOrganization = organizationDao.getOrganizationByCode(CODE1);
      assertThat(storedOrganization).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedOrganization.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertions.assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
      assertions.assertThat(storedOrganization.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateOrganization() {
      String id = createOrganization(CODE1);

      Organization updatedOrganization = new Organization(CODE2, NAME, ICON, COLOR, null, null, new Permissions(Set.of(userPermission), Set.of(groupPermission)));

      organizationFacade.updateOrganization(id, updatedOrganization);

      Organization storedOrganization = organizationDao.getOrganizationByCode(CODE2);

      assertThat(storedOrganization).isNotNull();
      assertThat(storedOrganization.getId()).isEqualTo(id);
      assertThat(storedOrganization.getName()).isEqualTo(NAME);
      assertThat(storedOrganization.getIcon()).isEqualTo(ICON);
      assertThat(storedOrganization.getColor()).isEqualTo(COLOR);
      assertThat(storedOrganization.getPermissions().getUserPermissions()).containsOnly(userPermission);
   }

   @Test
   public void testGetOrganizationPermissions() {
      final String organizationId = createOrganization(CODE1);
      final Organization organization2 = createOrganizationWithReadOnlyPermissions(CODE2);
      final Organization organization3 = createOrganizationWithStrangerPermissions(CODE3);

      Permissions permissions = organizationFacade.getOrganizationPermissions(organizationId);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);

      assertThatThrownBy(() -> organizationFacade.getOrganizationPermissions(organization2.getId()))
            .isInstanceOf(NoResourcePermissionException.class);

      permissions = organizationFacade.getOrganizationPermissions(organization3.getId());
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).hasSize(2).contains(userPermission, userStrangerPermission);
   }

   @Test
   public void testUpdateUserPermissions() {
      final String organizationId = createOrganization(CODE1);

      Permission userPermission = Permission.buildWithRoles(user.getId(), Set.of(new Role(RoleType.Manage), new Role(RoleType.TechConfig, true)));
      organizationFacade.updateUserPermissions(organizationId, Set.of(userPermission));

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveUserPermission() {
      final String organizationId = createOrganization(CODE1);
      workspaceKeeper.setOrganizationId(organizationId);

      organizationFacade.removeUserPermission(organizationId, user.getId());

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testUpdateGroupPermissions() {
      final String organizationId = createOrganization(CODE1);

      Permission groupPermission = Permission.buildWithRoles(GROUP, Set.of(new Role(RoleType.DataDelete, true), new Role(RoleType.Manage)));
      organizationFacade.updateGroupPermissions(organizationId, Set.of(groupPermission));

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      final String organizationId = createOrganization(CODE1);
      workspaceKeeper.setOrganizationId(organizationId);

      organizationFacade.removeGroupPermission(organizationId, GROUP);

      Permissions permissions = organizationDao.getOrganizationByCode(CODE1).getPermissions();
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertThat(permissions.getGroupPermissions()).isEmpty();
   }

}
