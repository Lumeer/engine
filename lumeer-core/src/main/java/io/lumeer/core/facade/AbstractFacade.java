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

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.adapter.FacadeAdapter;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.cache.UserCache;

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

   protected FacadeAdapter facadeAdapter;

   public FacadeAdapter getFacadeAdapter() {
      if (facadeAdapter == null) {
         facadeAdapter = new FacadeAdapter(permissionsChecker.getPermissionAdapter());
      }
      return facadeAdapter;
   }

   protected String getCurrentUserId() {
      return authenticatedUser.getCurrentUserId();
   }

   protected <T extends Resource> T mapResource(final T resource, final String userId) {
      if (authenticatedUser.getCurrentUserId().equals(userId)) {
         return getFacadeAdapter().mapResource(getOrganization(), getProject(), resource, authenticatedUser.getCurrentUser());
      } else {
         return getFacadeAdapter().mapResource(getOrganization(), getProject(), resource, userCache.getUserById(userId));
      }
   }

   protected <T extends Resource> T mapResource(final T resource) {
      return getFacadeAdapter().mapResource(getOrganization(), getProject(), resource, authenticatedUser.getCurrentUser());
   }

   protected LinkType mapLinkType(final LinkType linkType) {
      return getFacadeAdapter().mapLinkType(getOrganization(), getProject(), linkType, authenticatedUser.getCurrentUser());
   }

   protected void keepUnmodifiableFields(final Resource destinationResource, final Resource originalResource) {
      getFacadeAdapter().keepUnmodifiableFields(destinationResource, originalResource);
   }

   protected Organization getOrganization() {
      return workspaceKeeper.getOrganization().orElse(null);
   }

   protected Project getProject() {
      return workspaceKeeper.getProject().orElse(null);
   }

   protected <T extends Resource> T setupPublicPermissions(final T resource) {
      return getFacadeAdapter().setupPublicPermissions(resource, authenticatedUser.getCurrentUserId());
   }
}
