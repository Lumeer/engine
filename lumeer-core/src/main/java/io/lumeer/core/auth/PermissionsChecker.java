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
package io.lumeer.core.auth;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.core.facade.FreshdeskFacade;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentFacade;
import io.lumeer.core.util.FunctionRuleJsParser;
import io.lumeer.core.util.QueryUtils;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private UserDao userDao;

   @Inject
   private FreshdeskFacade freshdeskFacade;

   private String viewId = null;
   private List<LinkType> linkTypes;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   private Map<String, Boolean> hasRoleCache = new HashMap<>();
   private Map<String, View> viewCache = new HashMap<>();

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
    * @param resource any resource with defined permissions.
    * @param role     role to be checked.
    * @throws NoPermissionException when the user does not have the permission.
    */
   public void checkRole(Resource resource, Role role) {
      if (isManager()) {
         return;
      }

      checkOrganizationAndProject(resource, Role.READ);

      if (!hasRoleInResource(resource, role)) {
         throw new NoPermissionException(resource);
      }
   }

   /**
    * Checks if user is manager in organization or project
    *
    * @return whether the current user is a manager.
    */
   public boolean isManager() {
      return isManager(authenticatedUser.getCurrentUserId());
   }

   public boolean isManager(String userId) {
      if (workspaceKeeper.getOrganization().isPresent()) {
         Set<Role> organizationRoles = getActualRolesInResource(workspaceKeeper.getOrganization().get(), userId);
         if (organizationRoles.contains(Role.MANAGE)) {
            return true;
         }
         if (workspaceKeeper.getProject().isPresent()) {
            Set<Role> projectRoles = getActualRolesInResource(workspaceKeeper.getProject().get(), userId);
            return projectRoles.contains(Role.MANAGE) && organizationRoles.contains(Role.READ);
         }
      }
      return false;
   }

   public boolean isPublic() {
      if (workspaceKeeper.getProject().isPresent()) {
         final Project project = workspaceKeeper.getProject().get();
         return project.isPublic();
      }

      return false;
   }

   /**
    * Checks if the user has the given role on the given resource or the user has access to a view whose author has the given role.
    *
    * @param collection collection resource
    * @param role       role to be checked.
    * @param viewRole   role needed at the view.
    * @throws NoPermissionException when the user does not have the permission.
    */
   public void checkRoleWithView(final Collection collection, final Role role, final Role viewRole) {
      if (isManager()) {
         return;
      }

      checkOrganizationAndProject(collection, Role.READ);

      if (!hasRoleWithView(collection, role, viewRole)) {
         throw new NoPermissionException(collection);
      }
   }

   private void checkOrganizationAndProject(final Resource resource, final Role role) {
      if (!(resource instanceof Organization) && workspaceKeeper.getOrganization().isPresent()) {
         if (!hasRoleInResource(workspaceKeeper.getOrganization().get(), role)) {
            throw new NoPermissionException(resource);
         }
         if (!(resource instanceof Project) && workspaceKeeper.getProject().isPresent()) {
            if (!hasRoleInResource(workspaceKeeper.getProject().get(), role)) {
               throw new NoPermissionException(resource);
            }
         }
      }
   }

   /**
    * Checks whether the current user has the given role on the resource.
    *
    * @param resource The resource to be checked.
    * @param role     The required role.
    * @return True if and only if the user has the given role ont he resource.
    */
   public boolean hasRole(Resource resource, Role role) {
      return hasRole(resource, role, authenticatedUser.getCurrentUserId());
   }

   public boolean hasRole(Resource resource, Role role, String userId) {
      return isManager(userId) || hasRoleInResource(resource, role, userId);
   }

   private boolean hasRoleInResource(Resource resource, Role role) {
      return hasRoleCache.computeIfAbsent(resource.getId() + ":" + role.toString(), id -> getActualRoles(resource).contains(role));
   }

   public boolean hasAnyRoleInResource(Resource resource, Set<Role> roles) {
      return roles.stream().anyMatch(role -> hasRoleInResource(resource, role));
   }

   private boolean hasRoleInResource(Resource resource, Role role, String userId) {
      return getActualRolesInResource(resource, userId).contains(role);
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
    * Checks whether the current user has the given role on the given resource or the user has access to a view whose author has the given role.
    *
    * @param collection collection resource
    * @param role       role to be checked.
    * @param viewRole   role needed at the view.
    * @return true if and only if the user has the given role ont he resource.
    */
   public boolean hasRoleWithView(final Collection collection, final Role role, final Role viewRole) {
      return isManager() || hasRoleWithView(collection, role, viewRole, viewId);
   }

   private boolean hasRoleWithView(final Collection collection, final Role role, final Role viewRole, final String viewId) {
      return hasRoleInResource(collection, role) || getResourceRoleViaView(collection, role, viewRole, viewId);
   }

   private boolean getResourceRoleViaView(final Collection collection, final Role role, final Role viewRole, final String viewId) {
      if (StringUtils.isNotEmpty(viewId)) { // we might have the access through a view
         final View view = viewCache.computeIfAbsent(viewId, id -> viewDao.getViewById(viewId));

         if (view != null) {
            if (hasRoleInResource(view, viewRole)) { // do we have access to the view?
               final String authorId = view.getAuthorId();

               Set<String> collectionIds = QueryUtils.getQueryCollectionIds(view.getQuery(), getLinkTypes());
               if (collectionIds.contains(collection.getId())) { // does the view contain the resource?
                  if (StringUtils.isNotEmpty(authorId)) {
                     if (hasRoleInResource(collection, role, authorId)) { // has the view author access to the resource?
                        return true; // grant access
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private List<LinkType> getLinkTypes() {
      if (this.linkTypes == null) {
         this.linkTypes = this.linkTypeDao.getAllLinkTypes();
      }
      return this.linkTypes;
   }

   /**
    * Gets the active view provided in X-Lumeer-View-Id HTTP header through REST endpoint.
    *
    * @return The active View when exists, null otherwise.
    */
   public View getActiveView() {
      if (StringUtils.isNotEmpty(viewId)) {
         return viewCache.computeIfAbsent(viewId, id -> viewDao.getViewById(viewId));
      }

      return null;
   }

   /**
    * Returns all roles assigned to the authenticated user (whether direct or gained through group membership).
    *
    * @param resource any resource with defined permissions
    * @return set of actual roles
    */
   public Set<Role> getActualRoles(Resource resource) {
      String userId = authenticatedUser.getCurrentUserId();
      return getActualRoles(resource, userId);
   }

   /**
    * Returns all roles assigned to the specified user (whether direct or gained through group membership).
    *
    * @param resource any resource with defined permissions
    * @param userId   user ID to get the roles of.
    * @return set of actual roles
    */
   public Set<Role> getActualRoles(final Resource resource, final String userId) {
      if (isManager(userId)) {
         return getAllRoles(resource);
      }
      return getActualRolesInResource(resource, userId);
   }

   private Set<Role> getAllRoles(final Resource resource) {
      if (resource instanceof Organization) {
         return Organization.ROLES;
      } else if (resource instanceof Project) {
         return Project.ROLES;
      } else if (resource instanceof Collection) {
         return Collection.ROLES;
      } else if (resource instanceof View) {
         return View.ROLES;
      }
      return Collections.emptySet();
   }

   private Set<Role> getActualRolesInResource(final Resource resource, final String userId) {
      final Set<String> groups = authenticatedUser.getCurrentUserId().equals(userId) ? getUserGroups(resource) : getUserGroups(resource, userId);

      final Set<Role> actualRoles = getActualUserRoles(resource.getPermissions().getUserPermissions(), userId);
      actualRoles.addAll(getActualGroupRoles(resource.getPermissions().getGroupPermissions(), groups));
      return Role.withTransitionRoles(actualRoles);
   }

   private Set<String> getUserGroups(Resource resource) {
      if (resource instanceof Organization) {
         return Collections.emptySet();
      }
      return authenticatedUserGroups.getCurrentUserGroups();
   }

   private Set<String> getUserGroups(final Resource resource, final String userId) {
      if (resource instanceof Organization || userId == null || "".equals(userId)) {
         return Collections.emptySet();
      }

      final Optional<Organization> organizationOptional = workspaceKeeper.getOrganization();
      if (!organizationOptional.isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      final Organization organization = organizationOptional.get();
      final User user = userDao.getUserById(userId);

      return user != null ? user.getGroups().get(organization.getId()) : Collections.emptySet();
   }

   private Set<Role> getActualUserRoles(Set<Permission> userRoles, String userId) {
      return userRoles.stream()
                      .filter(entity -> entity.getId() != null && entity.getId().equals(userId))
                      .flatMap(entity -> entity.getRoles().stream())
                      .collect(toSet());
   }

   private Set<Role> getActualGroupRoles(Set<Permission> groupRoles, Set<String> groupIds) {
      return groupRoles.stream()
                       .filter(entity -> groupIds.contains(entity.getId()))
                       .flatMap(entity -> entity.getRoles().stream())
                       .collect(toSet());
   }

   public Map<String, AllowedPermissions> getCollectionsPermissions(final List<Collection> collection) {
      return collection.stream().collect(Collectors.toMap(Resource::getId, this::getCollectionPermissions));
   }

   public AllowedPermissions getCollectionPermissions(final Collection collection) {
      return new AllowedPermissions(
            hasRoleInResource(collection, Role.READ),
            hasRoleInResource(collection, Role.WRITE),
            hasRoleInResource(collection, Role.MANAGE),
            hasRoleWithView(collection, Role.READ, Role.READ),
            hasRoleWithView(collection, Role.WRITE, Role.WRITE),
            hasRoleWithView(collection, Role.MANAGE, Role.MANAGE)
      );
   }

   public Map<String, AllowedPermissions> getLinkTypesPermissions(final List<LinkType> linkTypes, final Map<String, AllowedPermissions> collectionsPermissions) {
      return linkTypes.stream().collect(Collectors.toMap(LinkType::getId, linkType ->
            mergePermissions(collectionsPermissions.get(linkType.getFirstCollectionId()), collectionsPermissions.get(linkType.getSecondCollectionId()))));
   }

   private AllowedPermissions mergePermissions(AllowedPermissions a1, AllowedPermissions a2) {
      if (a1 == null || a2 == null) {
         return a1 != null ? a1 : a2;
      }
      return new AllowedPermissions(
            a1.getRead() && a2.getRead(),
            a1.getWrite() && a2.getWrite(),
            a1.getManage() && a2.getManage(),
            a1.getReadWithView() && a2.getReadWithView(),
            a1.getWriteWithView() && a2.getWriteWithView(),
            a1.getManageWithView() && a2.getManageWithView()
      );
   }

   public void checkFunctionRuleAccess(final Collection collection, final String js, final Role role) {
      final Map<String, Collection> collections = collectionDao.getAllCollections().stream().collect(toMap(Resource::getId, Function.identity()));
      final Set<String> collectionIds = collections.keySet();
      final Map<String, LinkType> linkTypes = linkTypeDao.getAllLinkTypes().stream().collect(toMap(LinkType::getId, Function.identity()));
      final Set<String> linkTypeIds = linkTypes.keySet();

      final List<FunctionRuleJsParser.ResourceReference> references = FunctionRuleJsParser.parseRuleFunctionJs(js, collectionIds, linkTypeIds);

      references.forEach(reference -> {
         if (reference.getResourceType() == ResourceType.COLLECTION) {
            checkRole(collections.get(reference.getId()), role);
         } else if (reference.getResourceType() == ResourceType.LINK) {
            final LinkType linkType = linkTypes.get(reference.getId());
            linkType.getCollectionIds().forEach(c -> checkRole(collections.get(c), role));
         } else {
            throw new NoPermissionException(collection);
         }
      });
   }

   /**
    * Checks whether it is possible to create more resources of the given type.
    *
    * @param resource     Resource to be created.
    * @param currentCount Current no of resources of the given type.
    */
   public void checkCreationLimits(final Resource resource, final long currentCount) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = getServiceLimits();

      if (resource.getType().equals(ResourceType.PROJECT)) {
         if (limits.getProjects() > 0 && limits.getProjects() <= currentCount) {
            final Optional<Organization> organization = workspaceKeeper.getOrganization();
            freshdeskFacade.logLimitsExceeded(authenticatedUser.getCurrentUser(), "PROJECT", organization.isPresent() ? organization.get().getId() : "<empty>");
            throw new ServiceLimitsExceededException(limits.getProjects(), resource);
         }
      }

      if (resource.getType().equals(ResourceType.COLLECTION)) {
         if (limits.getFiles() > 0 && limits.getFiles() <= currentCount) {
            final Optional<Organization> organization = workspaceKeeper.getOrganization();
            freshdeskFacade.logLimitsExceeded(authenticatedUser.getCurrentUser(), "COLLECTION", organization.isPresent() ? organization.get().getId() : "<empty>");
            throw new ServiceLimitsExceededException(limits.getFiles(), resource);
         }
      }
   }

   /**
    * Checks whether it is possible to create more documents.
    *
    * @param document The document that is about to be created.
    */
   public void checkDocumentLimits(final Document document) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = getServiceLimits();
      final long documentsCount = countDocuments();

      if (limits.getDocuments() > 0 && documentsCount >= limits.getDocuments()) {
         final Optional<Organization> organization = workspaceKeeper.getOrganization();
         freshdeskFacade.logLimitsExceeded(authenticatedUser.getCurrentUser(), "DOCUMENT", organization.isPresent() ? organization.get().getId() : "<empty>");
         throw new ServiceLimitsExceededException(limits.getDocuments(), documentsCount, document);
      }
   }

   /**
    * Checks whether it is possible to create more documents.
    *
    * @param documents The list of documents that are about to be created.
    */
   public void checkDocumentLimits(final List<Document> documents) {
      checkDocumentLimits(documents.size());
   }

   public void checkDocumentLimits(final Integer number) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = getServiceLimits();
      final long documentsCount = countDocuments();

      if (limits.getDocuments() > 0 && documentsCount + number > limits.getDocuments()) {
         final Optional<Organization> organization = workspaceKeeper.getOrganization();
         freshdeskFacade.logLimitsExceeded(authenticatedUser.getCurrentUser(), "DOCUMENT", organization.isPresent() ? organization.get().getId() : "<empty>");
         throw new ServiceLimitsExceededException(limits.getDocuments(), documentsCount, null);
      }
   }

   public int getDocumentLimits() {
      if (skipLimits()) {
         return Integer.MAX_VALUE;
      }

      final ServiceLimits limits = getServiceLimits();
      return limits.getDocuments();
   }

   public void checkRulesLimit(final Collection collection) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = getServiceLimits();
      if (limits.getRulesPerCollection() >= 0 && collection.getRules().size() > limits.getRulesPerCollection()) {
         throw new ServiceLimitsExceededException(collection.getRules(), limits.getRulesPerCollection());
      }

   }

   public void checkRulesLimit(final LinkType linkType) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = getServiceLimits();
      if (limits.getRulesPerCollection() >= 0 && (linkType.getRules() != null && linkType.getRules().size() > limits.getRulesPerCollection())) {
         throw new ServiceLimitsExceededException(linkType.getRules(), limits.getRulesPerCollection());
      }
   }

   public void checkFunctionsLimit(final Collection collection) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = getServiceLimits();
      if (limits.getFunctionsPerCollection() >= 0) {
         long functions = collection.getAttributes().stream().filter(attribute -> attribute.getFunction() != null && !Utils.isEmpty(attribute.getFunction().getJs())).count();
         if (functions > limits.getFunctionsPerCollection()) {
            throw new ServiceLimitsExceededException(collection.getAttributes(), limits.getFunctionsPerCollection());
         }
      }
   }

   public void checkFunctionsLimit(final LinkType linkType) {
      if (skipLimits()) {
         return;
      }

      final ServiceLimits limits = getServiceLimits();
      if (limits.getFunctionsPerCollection() >= 0) {
         long functions = linkType.getAttributes().stream().filter(attribute -> attribute.getFunction() != null && !Utils.isEmpty(attribute.getFunction().getJs())).count();
         if (functions > limits.getFunctionsPerCollection()) {
            throw new ServiceLimitsExceededException(linkType.getAttributes(), limits.getFunctionsPerCollection());
         }
      }
   }

   private ServiceLimits getServiceLimits() {
      return paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());
   }

   private long countDocuments() {
      return collectionFacade.getDocumentsCountInAllCollections();
   }

   /**
    * Checks whether it is possible to create more users in the current organization.
    *
    * @param organizationId Organization ID where the user is being added.
    * @param newCount       New no of users.
    */
   public void checkUserCreationLimits(final String organizationId, final long newCount) {
      if (skipLimits()) {
         return;
      }

      final Organization organization = organizationFacade.getOrganizationById(organizationId);
      final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(organization);

      if (limits.getUsers() > 0 && limits.getUsers() < newCount) {
         freshdeskFacade.logLimitsExceeded(authenticatedUser.getCurrentUser(), "USER", organization.getId());
         throw new ServiceLimitsExceededException(limits.getUsers());
      }
   }

   private boolean skipLimits() {
      return System.getenv("SKIP_LIMITS") != null;
   }

   /**
    * Sets the view id that is being worked with. This allows us to execute queries under a different user supposing we have access to the view and the owner of the view can still execute it. For security reasons, the view id cannot be changed along the way.
    *
    * @param viewId id of the view
    */
   void setViewId(final String viewId) {
      if (this.viewId == null) {
         this.viewId = viewId;
      }
   }

   public String getViewId() {
      return viewId;
   }

   // For testing purposes to allow viewId manipulation during test run.
   void testSetViewId(final String viewId) {
      this.viewId = viewId;
   }

   String testGetViewId() {
      return viewId;
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

   /**
    * Get managers of current organization.
    *
    * @return Set of user IDs of all managers of the current organization.
    */
   public Set<String> getOrganizationManagers() {
      if (workspaceKeeper.getOrganization().isPresent()) {
         Organization organization = workspaceKeeper.getOrganization().get();
         return ResourceUtils.getManagers(organization);
      }
      return Collections.emptySet();
   }

   /**
    * Get managers in current workspace (both organization and project).
    *
    * @return Set of IDs of all managers of current organization and project.
    */
   public Set<String> getWorkspaceManagers() {
      Set<String> userIds = new HashSet<>(getOrganizationManagers());
      if (workspaceKeeper.getProject().isPresent()) {
         Project project = workspaceKeeper.getProject().get();
         userIds.addAll(ResourceUtils.getManagers(project));
      }
      return userIds;
   }
}
