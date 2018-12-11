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

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.cache.UserCache;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.Set;
import javax.inject.Inject;

abstract class AbstractFacade {

   @Inject
   protected AuthenticatedUser authenticatedUser;

   @Inject
   protected PermissionsChecker permissionsChecker;

   @Inject
   protected UserCache userCache;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   @Inject
   protected WorkspaceKeeper workspaceKeeper;

   protected boolean isManager() {
      return permissionsChecker.isManager();
   }

   protected <T extends Resource> T keepOnlyActualUserRoles(final T resource) {
      Set<Role> roles = permissionsChecker.getActualRoles(resource);
      Permission permission = Permission.buildWithRoles(authenticatedUser.getCurrentUserId(), roles);

      resource.getPermissions().clear();
      resource.getPermissions().updateUserPermissions(permission);

      return resource;
   }

   protected <T extends Resource> T mapResource(final T resource) {
      if (permissionsChecker.hasRole(resource, Role.MANAGE)) {
         return resource;
      }
      return keepOnlyActualUserRoles(resource);
   }

   protected void keepStoredPermissions(final Resource resource, final Permissions storedPermissions) {
      Set<Permission> userPermissions = storedPermissions.getUserPermissions();
      resource.getPermissions().updateUserPermissions(userPermissions.toArray(new Permission[0]));

      Set<Permission> groupPermissions = storedPermissions.getGroupPermissions();
      resource.getPermissions().updateGroupPermissions(groupPermissions.toArray(new Permission[0]));
   }

   protected void keepUnmodifiableFields(final Resource destinationResource, final Resource originalResource) {
      destinationResource.setNonRemovable(originalResource.isNonRemovable());
   }

   protected DatabaseQuery createSimpleQuery() {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return DatabaseQuery.createBuilder(user)
                          .groups(groups)
                          .build();
   }
}
