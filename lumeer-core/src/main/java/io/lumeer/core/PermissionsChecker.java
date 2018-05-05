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

import io.lumeer.api.model.Document;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.core.cache.UserCache;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentFacade;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageStats;

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

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   public PermissionsChecker() {
   }

   PermissionsChecker(UserCache userCache, AuthenticatedUser authenticatedUser, AuthenticatedUserGroups authenticatedUserGroups) {
      this.userCache = userCache;
      this.authenticatedUser = authenticatedUser;
      this.authenticatedUserGroups = authenticatedUserGroups;
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

   /**
    * Checks whether the current user has the given role on the resource.
    * @param resource The resource to be checked.
    * @param role The required role.
    * @return True if and only if the user has the given role ont he resource.
    */
   public boolean hasRole(Resource resource, Role role) {
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
      String userId = authenticatedUser.getCurrentUserId();
      Set<String> groups = getUserGroups(resource);

      Set<Role> actualRoles = getActualUserRoles(resource.getPermissions().getUserPermissions(), userId);
      actualRoles.addAll(getActualGroupRoles(resource.getPermissions().getGroupPermissions(), groups));
      return actualRoles;
   }

   private Set<String> getUserGroups(Resource resource) {
      if (resource instanceof Organization) {
         return Collections.emptySet();
      }
      return authenticatedUserGroups.getCurrentUserGroups();
   }

   private Set<Role> getActualUserRoles(Set<Permission> userRoles, String userId) {
      return userRoles.stream()
                      .filter(entity -> entity.getId().equals(userId))
                      .flatMap(entity -> entity.getRoles().stream())
                      .collect(Collectors.toSet());
   }

   private Set<Role> getActualGroupRoles(Set<Permission> groupRoles, Set<String> groupIds) {
      return groupRoles.stream()
                       .filter(entity -> groupIds.contains(entity.getId()))
                       .flatMap(entity -> entity.getRoles().stream())
                       .collect(Collectors.toSet());
   }

   /**
    * Checks whether it is possible to create more resources of the given type.
    * @param resource Resource to be created.
    * @param currentCount Current no of resources of the given type.
    */
   public void checkCreationLimits(final Resource resource, final long currentCount) {
      final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());

      if (resource.getType().equals(ResourceType.PROJECT)) {
         if (limits.getProjects() > 0 && limits.getProjects() <= currentCount) {
            throw new ServiceLimitsExceededException(limits.getProjects(), resource);
         }
      }

      if (resource.getType().equals(ResourceType.COLLECTION)) {
         if (limits.getFiles() > 0 && limits.getFiles() <= currentCount) {
            throw new ServiceLimitsExceededException(limits.getFiles(), resource);
         }
      }
   }

   /**
    * Checks whether it is possible to create more documents.
    * @param document The document that is about to be created.
    */
   public void checkDocumentLimits(final Document document) {
      final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());
      final DataStorageStats storageStats = dataStorage.getDbStats();
      long dbSizeMb = storageStats.getDataSize() / (1024 * 1024L);

      if (limits.getDocuments() > 0 && storageStats.getDocuments() >= limits.getDocuments()) {
         throw new ServiceLimitsExceededException(limits.getDocuments(), storageStats.getDocuments(), document);
      }

      if (limits.getDbSizeMb() > 0 && limits.getDbSizeMb() < dbSizeMb) {
         throw new ServiceLimitsExceededException(limits.getDbSizeMb(), dbSizeMb);
      }
   }

   /**
    * Checks whether it is possible to create more users in the current organization.
    * @param organizationId Organization ID where the user is being added.
    * @param currentCount Current no of users.
    */
   public void checkUserCreationLimits(final String organizationId, final long currentCount) {
      final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(organizationFacade.getOrganizationById(organizationId));

      if (limits.getUsers() > 0 && limits.getUsers() <= currentCount) {
         throw new ServiceLimitsExceededException(limits.getUsers());
      }
   }
}
