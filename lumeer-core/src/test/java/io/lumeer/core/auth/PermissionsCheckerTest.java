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
package io.lumeer.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleOld;
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class PermissionsCheckerTest {

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private PermissionsChecker permissionsChecker;

   @Before
   public void preparePermissionsChecker() {
      User user = Mockito.mock(User.class);
      Mockito.when(user.getId()).thenReturn(USER);
      Mockito.when(user.getGroups()).thenReturn(Collections.singletonMap("LMR", Collections.singleton(GROUP)));

      AuthenticatedUser authenticatedUser = Mockito.mock(AuthenticatedUser.class);
      Mockito.when(authenticatedUser.getCurrentUserId()).thenReturn(USER);
      Mockito.when(authenticatedUser.getUserEmail()).thenReturn(USER);

      Organization organization = Mockito.mock(Organization.class);
      preparePermissions(organization, Collections.singleton(RoleOld.READ), Collections.emptySet());
      Mockito.when(organization.getId()).thenReturn("LMR");

      Project project = Mockito.mock(Project.class);
      preparePermissions(project, Collections.singleton(RoleOld.READ), Collections.emptySet());
      Mockito.when(project.getId()).thenReturn("LMR");

      WorkspaceKeeper workspaceKeeper = Mockito.mock(WorkspaceKeeper.class);
      Mockito.when(workspaceKeeper.getOrganization()).thenReturn(Optional.of(organization));
      Mockito.when(workspaceKeeper.getProject()).thenReturn(Optional.of(project));

      CollectionDao collectionDao = Mockito.mock(CollectionDao.class);
      LinkTypeDao linkTypeDao = Mockito.mock(LinkTypeDao.class);
      ViewDao viewDao = Mockito.mock(ViewDao.class);
      UserDao userDao = Mockito.mock(UserDao.class);
      Mockito.when(userDao.getUserById(USER)).thenReturn(user);
      FavoriteItemDao favoriteItemDao = Mockito.mock(FavoriteItemDao.class);
      DocumentDao documentDao = Mockito.mock(DocumentDao.class);

      permissionsChecker = new PermissionsChecker(authenticatedUser, workspaceKeeper, userDao, collectionDao, viewDao, linkTypeDao, favoriteItemDao, documentDao);
      permissionsChecker.init();
   }

   private Resource prepareResource(Set<RoleOld> userRoles, Set<RoleOld> groupRoles) {
      Resource resource = Mockito.mock(Resource.class);
      preparePermissions(resource, userRoles, groupRoles);
      Mockito.when(resource.getType()).thenReturn(ResourceType.PROJECT);
      return resource;
   }

   private <T extends Resource> void preparePermissions(T resource, Set<RoleOld> userRoles, Set<RoleOld> groupRoles) {
      Permission userPermission = Mockito.mock(Permission.class);
      Mockito.when(userPermission.getId()).thenReturn(USER);
      Mockito.when(userPermission.getRoles()).thenReturn(userRoles);

      Permission groupPermission = Mockito.mock(Permission.class);
      Mockito.when(groupPermission.getId()).thenReturn(GROUP);
      Mockito.when(groupPermission.getRoles()).thenReturn(groupRoles);

      Permissions permissions = Mockito.mock(Permissions.class);
      Mockito.when(permissions.getUserPermissions()).thenReturn(Collections.singleton(userPermission));
      Mockito.when(permissions.getGroupPermissions()).thenReturn(Collections.singleton(groupPermission));
      Mockito.when(resource.getPermissions()).thenReturn(permissions);
   }

   @Test
   public void testCheckUserRole() {
      Resource resource = prepareResource(Collections.singleton(RoleOld.READ), Collections.emptySet());
      permissionsChecker.checkRole(resource, RoleOld.READ);
   }

   @Test
   public void testCheckGroupRole() {
      Resource resource = prepareResource(Collections.emptySet(), Collections.singleton(RoleOld.READ));
      permissionsChecker.checkRole(resource, RoleOld.READ);
   }

   @Test
   public void testCheckNoRole() {
      Resource resource = prepareResource(Collections.emptySet(), Collections.emptySet());
      assertThatThrownBy(() -> permissionsChecker.checkRole(resource, RoleOld.READ))
            .isInstanceOf(NoResourcePermissionException.class)
            .hasFieldOrPropertyWithValue("resource", resource);
   }

   @Test
   public void testCheckDifferentRole() {
      Resource resource = prepareResource(Collections.singleton(RoleOld.WRITE), Collections.singleton(RoleOld.READ));
      assertThatThrownBy(() -> permissionsChecker.checkRole(resource, RoleOld.MANAGE))
            .isInstanceOf(NoResourcePermissionException.class)
            .hasFieldOrPropertyWithValue("resource", resource);
   }

   @Test
   public void testGetActualRolesUserOnly() {
      Resource resource = prepareResource(Sets.newLinkedHashSet(RoleOld.READ, RoleOld.WRITE), Collections.emptySet());
      Set<RoleOld> roles = permissionsChecker.getActualRoles(resource);
      assertThat(roles).containsOnly(RoleOld.READ, RoleOld.WRITE);
   }

   @Test
   public void testGetActualRolesGroupOnly() {
      Resource resource = prepareResource(Collections.emptySet(), Sets.newLinkedHashSet(RoleOld.READ, RoleOld.SHARE));
      Set<RoleOld> roles = permissionsChecker.getActualRoles(resource);
      assertThat(roles).containsOnly(RoleOld.READ, RoleOld.SHARE);
   }

   @Test
   public void testGetActualRolesIntersection() {
      Resource resource = prepareResource(Sets.newLinkedHashSet(RoleOld.READ, RoleOld.WRITE), Sets.newLinkedHashSet(RoleOld.READ, RoleOld.SHARE));
      Set<RoleOld> roles = permissionsChecker.getActualRoles(resource);
      assertThat(roles).containsOnly(RoleOld.READ, RoleOld.WRITE, RoleOld.SHARE);
   }

   @Test
   public void testGetActualRolesEmpty() {
      Resource resource = prepareResource(Collections.emptySet(), Collections.emptySet());
      Set<RoleOld> roles = permissionsChecker.getActualRoles(resource);
      assertThat(roles).isEmpty();

   }

}
