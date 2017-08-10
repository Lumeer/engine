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
package io.lumeer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.storage.api.dao.UserDao;

import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Set;

public class PermissionsCheckerTest {

   private static final String USER = "testUser";
   private static final String GROUP = "testGroup";

   private PermissionsChecker permissionsChecker;

   @Before
   public void preparePermissionsChecker() {
      User user = Mockito.mock(User.class);
      Mockito.when(user.getGroups()).thenReturn(Collections.singleton(GROUP));

      UserDao userDao = Mockito.mock(UserDao.class);
      Mockito.when(userDao.getUserByUsername(USER)).thenReturn(user);

      AuthenticatedUser authenticatedUser = Mockito.mock(AuthenticatedUser.class);
      Mockito.when(authenticatedUser.getUserEmail()).thenReturn(USER);

      permissionsChecker = new PermissionsChecker(userDao, authenticatedUser);
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
      Mockito.when(resource.getType()).thenReturn(ResourceType.ORGANIZATION);
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
