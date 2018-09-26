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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.View;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentFacade;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class PermissionsChecker {

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
   private CollectionFacade collectionFacade;

   @Inject
   private ViewDao viewDao;

   @Inject
   private UserDao userDao;

   private String viewCode = null;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   private Map<String, Boolean> hasRoleCache = new HashMap<>();

   public PermissionsChecker() {
   }

   PermissionsChecker(AuthenticatedUser authenticatedUser, AuthenticatedUserGroups authenticatedUserGroups, WorkspaceKeeper workspaceKeeper) {
      this.authenticatedUser = authenticatedUser;
      this.authenticatedUserGroups = authenticatedUserGroups;
      this.workspaceKeeper = workspaceKeeper;
   }

   /**
    * Checks if the user has the given role on the given resource (either directly or through group membership).
    *
    * @param resource
    *       any resource with defined permissions.
    * @param role
    *       role to be checked.
    * @throws NoPermissionException when the user does not have the permission.
    */
   public void checkRole(Resource resource, Role role) {
      checkOrganizationAndProject(resource, role);

      if (!hasRole(resource, role)) {
         throw new NoPermissionException(resource);
      }
   }

   /**
    * Checks if the user has the given role on the given resource or the user has access to a view whose author has the given role.
    * @param resource
    *       any resource with the defined permissions.
    * @param role
    *       role to be checked.
    * @param viewRole
    *       role needed at the view.
    * @throws NoPermissionException when the user does not have the permission.
    */
   public void checkRoleWithView(final Resource resource, final Role role, final Role viewRole) {
      checkOrganizationAndProject(resource, role);

      if (!hasRoleWithView(resource, role, viewRole)) {
         throw new NoPermissionException(resource);
      }
   }

   private void checkOrganizationAndProject(final Resource resource, final Role role) {
      if (!(resource instanceof Organization) && workspaceKeeper.getOrganization().isPresent()) {
         if (!hasRole(workspaceKeeper.getOrganization().get(), Role.READ)) {
            throw new NoPermissionException(resource);
         }
         if (!(resource instanceof Project) && workspaceKeeper.getProject().isPresent()) {
            if (!hasRole(workspaceKeeper.getProject().get(), Role.READ)) {
               throw new NoPermissionException(resource);
            }
         }
      }
   }

   /**
    * Checks whether the current user has the given role on the resource.
    *
    * @param resource
    *       The resource to be checked.
    * @param role
    *       The required role.
    * @return True if and only if the user has the given role ont he resource.
    */
   public boolean hasRole(Resource resource, Role role) {
      return hasRoleCache.computeIfAbsent(resource.getId() + ":" + role.toString(), id -> getActualRoles(resource).contains(role));
   }

   private boolean hasRole(Resource resource, Role role, String userId) {
      return getActualRoles(resource, userId).contains(role);
   }

   /**
    * Invalidates a bit of cache when the information changes.
    *
    * @param resource Resource being updated.
    */
   public void invalidateCache(final Resource resource) {
      for (final Role role : Role.values()) {
         hasRoleCache.remove(resource.getId() + ":" + role.toString());
      }
   }

   /**
    * Checks whether the current user has the given role on the given resource
    * or the user has access to a view whose author has the given role.
    * @param resource
    *       any resource with the defined permissions.
    * @param role
    *       role to be checked.
    * @param viewRole
    *       role needed at the view.
    * @return true if and only if the user has the given role ont he resource.
    */
   public boolean hasRoleWithView(final Resource resource, final Role role, final Role viewRole) {
      return hasRoleWithView(resource, role, viewRole, viewCode);
   }

   public boolean hasRoleWithView(final Resource resource, final Role role, final Role viewRole, final String viewCode) {
      if (!hasRole(resource, role)) { // we do not have direct access
         return getResourceRoleViaView(resource, role, viewRole, viewCode);
      } else { // we have direct access
         return true;
      }
   }



   private boolean getResourceRoleViaView(final Resource resource, final Role role, final Role viewRole, final String viewCode) {
      if (viewCode != null && !"".equals(viewCode)) { // we might have the access through a view
         final View view = viewDao.getViewByCode(viewCode);

         if (view != null) {
            if (hasRole(view, viewRole)) { // do we have access to the view?
               final String authorId = view.getAuthorId();

               if (resource instanceof Collection) {
                  if (view.getQuery().getCollectionIds().contains(resource.getId())) { // does the view contain the resource?
                     if (authorId != null && !"".equals(authorId)) {
                        if (hasRole(resource, role, authorId)) { // has the view author access to the resource?
                           return true; // grant access
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   /**
    * Gets the active view provided in view_code HTTP header through REST endpoint.
    * @return The active View when exists, null otherwise.
    */
   public View getActiveView() {
      if (viewCode != null && !"".equals(viewCode)) {
         return viewDao.getViewByCode(viewCode);
      }

      return null;
   }


   public boolean hasRoleWithView(final Resource resource, final Role role, final Role viewRole, final Query query) {
      return hasRoleWithView(resource, role, viewRole, viewCode, query);
   }

   public boolean hasRoleWithView(final Resource resource, final Role role, final Role viewRole, final String viewCode, final Query query) {
      if (!hasRole(resource, role)) { // we do not have direct access
         return getResourceRoleViaView(resource, role, viewRole, viewCode) &&
               (query == null || query.isMoreSpecificThan(viewDao.getViewByCode(viewCode).getQuery()));
      } else { // we have direct access
         return true;
      }
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
      return getActualRoles(resource, userId);
   }

   private Set<Role> getActualRoles(final Resource resource, final String userId) {
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
    *
    * @param resource
    *       Resource to be created.
    * @param currentCount
    *       Current no of resources of the given type.
    */
   public void checkCreationLimits(final Resource resource, final long currentCount) {
      if (skipLimits()) {
         return;
      }

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
    *
    * @param document
    *       The document that is about to be created.
    */
   public void checkDocumentLimits(final Document document) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());
      final long documentsCount = countDocuments();

      if (limits.getDocuments() > 0 && documentsCount >= limits.getDocuments()) {
         throw new ServiceLimitsExceededException(limits.getDocuments(), documentsCount, document);
      }
   }

   /**
    * Checks whether it is possible to create more documents.
    *
    * @param documents
    *       The list of documents that are about to be created.
    */
   public void checkDocumentLimits(final List<Document> documents) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());
      final long documentsCount = countDocuments();

      if (limits.getDocuments() > 0 && documentsCount + documents.size() > limits.getDocuments()) {
         throw new ServiceLimitsExceededException(limits.getDocuments(), documentsCount, null);
      }
   }

   private long countDocuments() {
      return collectionFacade.getDocumentsCountInAllCollections();
   }

   /**
    * Checks whether it is possible to create more users in the current organization.
    *
    * @param organizationId
    *       Organization ID where the user is being added.
    * @param currentCount
    *       Current no of users.
    */
   public void checkUserCreationLimits(final String organizationId, final long currentCount) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(organizationFacade.getOrganizationById(organizationId));

      if (limits.getUsers() > 0 && limits.getUsers() <= currentCount) {
         throw new ServiceLimitsExceededException(limits.getUsers());
      }
   }

   private boolean skipLimits() {
      return System.getenv("SKIP_LIMITS") != null;
   }

   /**
    * Sets the view code that is being worked with. This allows us to execute queries under a different user supposing
    * we have access to the view and the owner of the view can still execute it. For security reasons, the view code
    * cannot be changed along the way.
    *
    * @param viewCode code of the view
    */
   void setViewCode(final String viewCode) {
      if (this.viewCode == null) {
         this.viewCode = viewCode;
      }
   }

   public String getViewCode() {
      return viewCode;
   }

   // For testing purposes to allow viewCode manipulation during test run.
   void testSetViewCode(final String viewCode) {
      this.viewCode = viewCode;
   }

   String testGetViewCode() {
      return viewCode;
   }

   /**
    * Checks whether it is possible to delete the given resource.
    *
    * @param resource Resource to check.
    * @throws NoPermissionException When it is not possible to delete the resource.
    */
   public void checkCanDelete(Resource resource) {
      if (resource.isNonRemovable()) {
         throw new NoPermissionException(resource);
      }
   }
}
