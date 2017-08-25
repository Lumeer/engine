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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.core.cache.UserCache;
import io.lumeer.core.exception.NoPermissionException;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class PermissionsChecker {

   @Inject
   private UserCache userCache;

   @Inject
   private AuthenticatedUser authenticatedUser;

   public PermissionsChecker() {
   }

   PermissionsChecker(UserCache userCache, AuthenticatedUser authenticatedUser) {
      this.userCache = userCache;
      this.authenticatedUser = authenticatedUser;
   }

   /**
    * Checks if the user has the given role on the given resource (either directly or through group membership).
    *
    * @param resource
    *       any resource with defined permissions
    * @param role
    *       role to be checked
    * @throws NoPermissionException
    */
   public void checkRole(Resource resource, Role role) {
      if (!hasRole(resource, role)) {
         throw new NoPermissionException(resource);
      }
   }

   private boolean hasRole(Resource resource, Role role) {
      return getActualRoles(resource).contains(role);
   }

   /**
    * Returns all roles assigned to the authenticated user (whether direct or gained through group membership).
    *
    * @param resource
    *       any resource with defined permissions
    * @return set of actual roles
    */
   public Set<Role> getActualRoles(Resource resource) {
      String user = authenticatedUser.getUserEmail();
      Set<String> groups = getUserGroups(user, resource);

      Set<Role> actualRoles = getActualUserRoles(resource.getPermissions().getUserPermissions(), user);
      actualRoles.addAll(getActualGroupRoles(resource.getPermissions().getGroupPermissions(), groups));
      return actualRoles;
   }

   private Set<String> getUserGroups(String user, Resource resource) {
      if (resource instanceof Organization) {
         return Collections.emptySet();
      }
      return userCache.getUser(user).getGroups();
   }

   private Set<Role> getActualUserRoles(Set<Permission> userRoles, String user) {
      return userRoles.stream()
                      .filter(entity -> entity.getName().equals(user))
                      .flatMap(entity -> entity.getRoles().stream())
                      .collect(Collectors.toSet());
   }

   private Set<Role> getActualGroupRoles(Set<Permission> groupRoles, Set<String> groups) {
      return groupRoles.stream()
                       .filter(entity -> groups.contains(entity.getName()))
                       .flatMap(entity -> entity.getRoles().stream())
                       .collect(Collectors.toSet());
   }

}
