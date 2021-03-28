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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.cache.UserCache;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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

   protected <T extends Resource> T keepOnlyActualUserRoles(final T resource, String userId) {
      Set<Role> roles = permissionsChecker.getActualRoles(resource, userId);
      Permission permission = Permission.buildWithRoles(userId, roles);

      Set<String> managers;
      if(resource instanceof Organization) {
         managers = permissionsChecker.getOrganizationManagers();
      } else if(resource instanceof Project) {
         managers = permissionsChecker.getWorkspaceManagers();
      } else {
         managers = ResourceUtils.getResourceManagers(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), resource);
      }

      Set<Permission> managersUserPermission = resource.getPermissions().getUserPermissions().stream()
                                                       .filter(perm -> managers.contains(perm.getId()))
                                                       .collect(Collectors.toSet());

      resource.getPermissions().clear();
      resource.getPermissions().updateUserPermissions(managersUserPermission);
      resource.getPermissions().updateUserPermissions(permission);

      return resource;
   }

   protected <T extends Resource> T mapResource(final T resource, final String userId) {
      if (permissionsChecker.hasRole(resource, Role.MANAGE, userId)) {
         return resource;
      }
      return keepOnlyActualUserRoles(resource, userId);
   }

   protected <T extends Resource> T mapResource(final T resource) {
      return mapResource(resource, authenticatedUser.getCurrentUserId());
   }

   protected void keepStoredPermissions(final Resource resource, final Permissions storedPermissions) {
      Set<Permission> userPermissions = storedPermissions.getUserPermissions();
      resource.getPermissions().updateUserPermissions(userPermissions);

      Set<Permission> groupPermissions = storedPermissions.getGroupPermissions();
      resource.getPermissions().updateGroupPermissions(groupPermissions);
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

   protected <T extends Resource> T setupPublicPermissions(final T resource) {
      String user = authenticatedUser.getCurrentUserId();
      var userPermission = new Permission(user, new HashSet<>());
      var currentUserPermission = resource.getPermissions().getUserPermissions()
                                          .stream()
                                          .filter(permission -> permission.getId().equals(user))
                                          .findFirst()
                                          .orElse(userPermission);
      resource.getPermissions().clear();

      currentUserPermission.getRoles().add(Role.READ);
      resource.getPermissions().addUserPermissions(Collections.singleton(currentUserPermission));

      return resource;
   }
}
