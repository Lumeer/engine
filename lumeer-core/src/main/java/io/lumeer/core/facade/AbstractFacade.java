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

import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.PermissionsChecker;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.cache.UserCache;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.query.SearchQuery;

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
   protected WorkspaceKeeper workspaceKeeper;

   protected <T extends Resource> T keepOnlyActualUserRoles(final T resource) {
      Set<Role> roles = permissionsChecker.getActualRoles(resource);
      Permission permission = new SimplePermission(authenticatedUser.getUserEmail(), roles);

      resource.getPermissions().clear();
      resource.getPermissions().updateUserPermissions(permission);

      return resource;
   }

   protected void keepStoredPermissions(final Resource resource, final Permissions storedPermissions) {
      Set<Permission> userPermissions = storedPermissions.getUserPermissions();
      resource.getPermissions().updateUserPermissions(userPermissions.toArray(new Permission[0]));

      Set<Permission> groupPermissions = storedPermissions.getGroupPermissions();
      resource.getPermissions().updateGroupPermissions(groupPermissions.toArray(new Permission[0]));
   }

   protected SearchQuery createSearchQuery(Pagination pagination) {
      String user = authenticatedUser.getCurrentUsername();
      Set<String> groups = userCache.getUser(user).getGroups();

      return SearchQuery.createBuilder(user)
                        .groups(groups)
                        .build();
   }
}
