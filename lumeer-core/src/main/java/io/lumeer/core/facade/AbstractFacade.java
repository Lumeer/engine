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
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.adapter.FacadeAdapter;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.auth.PermissionsChecker;
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
   protected AuthenticatedUserGroups authenticatedUserGroups;

   @Inject
   protected WorkspaceKeeper workspaceKeeper;

   protected final FacadeAdapter facadeAdapter = new FacadeAdapter();

   protected String getCurrentUserId() {
      return authenticatedUser.getCurrentUserId();
   }

   protected Set<String> getCurrentUserGroups() {
      return authenticatedUserGroups.getCurrentUserGroups();
   }

   protected boolean isWorkspaceManager() {
      return permissionsChecker.isManager();
   }

   protected <T extends Resource> T mapResource(final T resource, final String userId) {
      if (authenticatedUser.getCurrentUserId().equals(userId)) {
         return facadeAdapter.mapResource(getOrganization(), getProject(), resource, authenticatedUser.getCurrentUser());
      } else {
         return facadeAdapter.mapResource(getOrganization(), getProject(), resource, userCache.getUserById(userId));
      }
   }

   protected <T extends Resource> T mapResource(final T resource) {
      return facadeAdapter.mapResource(getOrganization(), getProject(), resource, authenticatedUser.getCurrentUser());
   }

   protected void keepStoredPermissions(final Resource resource, final Permissions storedPermissions) {
      facadeAdapter.keepStoredPermissions(resource, storedPermissions);
   }

   protected void keepUnmodifiableFields(final Resource destinationResource, final Resource originalResource) {
      facadeAdapter.keepUnmodifiableFields(destinationResource, originalResource);
   }

   protected DatabaseQuery createSimpleQuery() {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return DatabaseQuery.createBuilder(user)
                          .groups(groups)
                          .build();
   }

   private Organization getOrganization() {
      return workspaceKeeper.getOrganization().orElse(null);
   }

   private Project getProject() {
      return workspaceKeeper.getProject().orElse(null);
   }

   protected <T extends Resource> T setupPublicPermissions(final T resource) {
      return facadeAdapter.setupPublicPermissions(resource, authenticatedUser.getCurrentUserId());
   }
}
