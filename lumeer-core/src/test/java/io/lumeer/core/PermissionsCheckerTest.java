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
package io.lumeer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.cache.UserCache;
import io.lumeer.core.exception.NoPermissionException;

import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Set;

public class PermissionsCheckerTest {

   public static final String USER = "testUser";
   public static final String GROUP = "testGroup";

   private PermissionsChecker permissionsChecker;

   @Before
   public void preparePermissionsChecker() {
      User user = Mockito.mock(User.class);
      Mockito.when(user.getGroups()).thenReturn(Collections.singletonMap("LMR", Collections.singleton(GROUP)));

      UserCache userCache = Mockito.mock(UserCache.class);
      Mockito.when(userCache.getUser(USER)).thenReturn(user);

      AuthenticatedUser authenticatedUser = Mockito.mock(AuthenticatedUser.class);
      AuthenticatedUserGroups authenticatedUserGroups = Mockito.mock(AuthenticatedUserGroups.class);
      Mockito.when(authenticatedUser.getCurrentUsername()).thenReturn(USER);
      Mockito.when(authenticatedUser.getUserEmail()).thenReturn(USER);
      Mockito.when(authenticatedUserGroups.getCurrentUserGroups()).thenReturn(Collections.singleton(GROUP));

      permissionsChecker = new PermissionsChecker(userCache, authenticatedUser, authenticatedUserGroups);
   }

   private Resource prepareResource(Set<Role> userRoles, Set<Role> groupRoles) {
      Permission userPermission = Mockito.mock(Permission.class);
      Mockito.when(userPermission.getName()).thenReturn(USER);
      Mockito.when(userPermission.getRoles()).thenReturn(userRoles);

      Permission groupPermission = Mockito.mock(Permission.class);
      Mockito.when(groupPermission.getName()).thenReturn(GROUP);
      Mockito.when(groupPermission.getRoles()).thenReturn(groupRoles);

      Permissions permissions = Mockito.mock(Permissions.class);
      Mockito.when(permissions.getUserPermissions()).thenReturn(Collections.singleton(userPermission));
      Mockito.when(permissions.getGroupPermissions()).thenReturn(Collections.singleton(groupPermission));

      Resource resource = Mockito.mock(Resource.class);
      Mockito.when(resource.getPermissions()).thenReturn(permissions);
      Mockito.when(resource.getType()).thenReturn(ResourceType.PROJECT);
      return resource;
   }

   @Test
   public void testCheckUserRole() {
      Resource resource = prepareResource(Collections.singleton(Role.READ), Collections.emptySet());
      permissionsChecker.checkRole(resource, Role.READ);
   }

   @Test
   public void testCheckGroupRole() {
      Resource resource = prepareResource(Collections.emptySet(), Collections.singleton(Role.READ));
      permissionsChecker.checkRole(resource, Role.READ);
   }

   @Test
   public void testCheckNoRole() {
      Resource resource = prepareResource(Collections.emptySet(), Collections.emptySet());
      assertThatThrownBy(() -> permissionsChecker.checkRole(resource, Role.READ))
            .isInstanceOf(NoPermissionException.class)
            .hasFieldOrPropertyWithValue("resource", resource);
   }

   @Test
   public void testCheckDifferentRole() {
      Resource resource = prepareResource(Collections.singleton(Role.WRITE), Collections.singleton(Role.READ));
      assertThatThrownBy(() -> permissionsChecker.checkRole(resource, Role.MANAGE))
            .isInstanceOf(NoPermissionException.class)
            .hasFieldOrPropertyWithValue("resource", resource);
   }

   @Test
   public void testGetActualRolesUserOnly() {
      Resource resource = prepareResource(Sets.newLinkedHashSet(Role.READ, Role.WRITE), Collections.emptySet());
      Set<Role> roles = permissionsChecker.getActualRoles(resource);
      assertThat(roles).containsOnly(Role.READ, Role.WRITE);
   }

   @Test
   public void testGetActualRolesGroupOnly() {
      Resource resource = prepareResource(Collections.emptySet(), Sets.newLinkedHashSet(Role.READ, Role.SHARE));
      Set<Role> roles = permissionsChecker.getActualRoles(resource);
      assertThat(roles).containsOnly(Role.READ, Role.SHARE);
   }

   @Test
   public void testGetActualRolesIntersection() {
      Resource resource = prepareResource(Sets.newLinkedHashSet(Role.READ, Role.WRITE), Sets.newLinkedHashSet(Role.READ, Role.SHARE));
      Set<Role> roles = permissionsChecker.getActualRoles(resource);
      assertThat(roles).containsOnly(Role.READ, Role.WRITE, Role.SHARE);
   }

   @Test
   public void testGetActualRolesEmpty() {
      Resource resource = prepareResource(Collections.emptySet(), Collections.emptySet());
      Set<Role> roles = permissionsChecker.getActualRoles(resource);
      assertThat(roles).isEmpty();

   }

}
