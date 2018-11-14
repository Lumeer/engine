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
package io.lumeer.core.auth;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.cache.UserCache;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class AuthenticatedUserGroups {

   @Inject
   private UserCache userCache;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private AuthenticatedUser authenticatedUser;

   public Set<String> getCurrentUserGroups(){
      Optional<Organization> organizationOptional = workspaceKeeper.getOrganization();
      if (!organizationOptional.isPresent()){
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      Organization organization = organizationOptional.get();
      Map<String,Set<String>> userGroups = userCache.getUser(authenticatedUser.getUserEmail()).getGroups();
      return userGroups != null && userGroups.containsKey(organization.getId()) ? userGroups.get(organization.getId()) : Collections.emptySet();
   }

}
