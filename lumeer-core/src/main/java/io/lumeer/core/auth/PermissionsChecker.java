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
import io.lumeer.core.adapter.CollectionAdapter;
import io.lumeer.core.adapter.PermissionAdapter;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.core.facade.FreshdeskFacade;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentFacade;
import io.lumeer.core.util.FunctionRuleJsParser;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
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
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private DocumentDao documentDao;

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

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   private CollectionAdapter collectionAdapter;
   private PermissionAdapter permissionAdapter;

   @PostConstruct
   public void init() {
      collectionAdapter = new CollectionAdapter(collectionDao, favoriteItemDao, documentDao);
      permissionAdapter = new PermissionAdapter(userDao, viewDao, linkTypeDao, collectionDao);
   }

   public PermissionsChecker() {
   }

   PermissionsChecker(AuthenticatedUser authenticatedUser, AuthenticatedUserGroups authenticatedUserGroups, WorkspaceKeeper workspaceKeeper) {
      this.authenticatedUser = authenticatedUser;
      this.authenticatedUserGroups = authenticatedUserGroups;
      this.workspaceKeeper = workspaceKeeper;
   }

   public PermissionAdapter getPermissionAdapter() {
      return permissionAdapter;
   }

   /**
    * Checks if the user has the given role on the given resource (either directly or through group membership).
    *
    * @param resource any resource with defined permissions.
    * @param role     role to be checked.
    * @throws NoResourcePermissionException when the user does not have the permission.
    */
   public void checkRole(Resource resource, Role role) {
      permissionAdapter.checkRole(getOrganization(), getProject(), resource, role, authenticatedUser.getCurrentUserId());
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
      return permissionAdapter.isManager(getOrganization(), getProject(), userId);
   }

   public boolean isPublic() {
      return permissionAdapter.isPublic(getOrganization(), getProject());
   }

   public void checkLinkTypeRoleWithView(final java.util.Collection<String> collectionIds, final Role role, final boolean strict) {
      permissionAdapter.checkLinkTypeRoleWithView(getOrganization(), getProject(), collectionIds, role, authenticatedUser.getCurrentUserId(), strict);
   }

   public void checkLinkTypeRoleWithView(LinkType linkType, Role role, boolean strict) {
      checkLinkTypeRoleWithView(linkType.getCollectionIds(), role, strict);
   }

   public boolean hasLinkTypeRoleWithView(LinkType linkType, Role role) {
      return permissionAdapter.hasLinkTypeRoleWithView(getOrganization(), getProject(), linkType, role, authenticatedUser.getCurrentUserId());
   }

   public boolean hasLinkTypeRole(LinkType linkType, Map<String, Collection> collectionMap, Role role, String userId) {
      return permissionAdapter.hasLinkTypeRole(getOrganization(), getProject(), linkType, collectionMap, role, userId);
   }

   /**
    * Checks if the user has the given role on the given resource or the user has access to a view whose author has the given role.
    *
    * @param collection collection resource
    * @param role       role to be checked.
    * @param viewRole   role needed at the view.
    * @throws NoResourcePermissionException when the user does not have the permission.
    */
   public void checkRoleWithView(final Collection collection, final Role role, final Role viewRole) {
      permissionAdapter.checkRoleWithView(getOrganization(), getProject(), collection, role, viewRole, authenticatedUser.getCurrentUserId());
   }

   /**
    * Checks whether the current user has the given role on the document.
    *
    * @param document   The document to be checked.
    * @param collection Parent collection.
    * @param role       The required role.
    * @return True if and only if the user has the given role ont he document.
    */
   public boolean hasRole(Document document, Collection collection, Role role) {
      return permissionAdapter.hasRole(getOrganization(), getProject(), document, collection, role, authenticatedUser.getCurrentUserId());
   }

   public boolean hasRoleWithView(Document document, Collection collection, Role role, Role viewRole) {
      return permissionAdapter.hasRoleWithView(getOrganization(), getProject(), document, collection, role, viewRole, authenticatedUser.getCurrentUserId());
   }

   public void checkRole(Document document, Collection collection, Role role) {
      permissionAdapter.checkRole(getOrganization(), getProject(), document, collection, role, authenticatedUser.getCurrentUserId());
   }

   public void checkRoleWithView(Document document, Collection collection, Role role, Role viewRole) {
      permissionAdapter.checkRoleWithView(getOrganization(), getProject(), document, collection, role, viewRole, authenticatedUser.getCurrentUserId());
   }

   public boolean hasRoleInDocument(Document document, Collection collection, Role role) {
      return permissionAdapter.hasRoleInDocument(document, collection, role, authenticatedUser.getCurrentUserId());
   }

   /**
    * Checks whether the current user has the given role on the resource.
    *
    * @param resource The resource to be checked.
    * @param role     The required role.
    * @return True if and only if the user has the given role on the resource.
    */
   public boolean hasRole(Resource resource, Role role) {
      return hasRole(resource, role, authenticatedUser.getCurrentUserId());
   }

   public boolean hasRole(Resource resource, Role role, String userId) {
      return permissionAdapter.hasRole(getOrganization(), getProject(), resource, role, userId);
   }

   public static boolean hasRole(final Organization organization, final Project project, final Resource resource, final Role role, final User user) {
      return ResourceUtils.userIsManagerInWorkspace(user.getId(), organization, project) || hasRoleInResource(organization, resource, role, user);
   }

   public boolean hasAnyRoleInResource(Resource resource, Set<Role> roles) {
      return permissionAdapter.hasAnyRoleInResource(getOrganization(), getProject(), resource, roles, authenticatedUser.getCurrentUserId());
   }

   private static boolean hasRoleInResource(final Organization organization, final Resource resource, final Role role, final User user) {
      return getActualRolesInResource(organization, resource, user).contains(role);
   }

   /**
    * Invalidates a bit of cache when the information changes.
    *
    * @param resource Resource being updated.
    */
   public void invalidateCache(final Resource resource) {
      permissionAdapter.invalidateCache(resource);
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
      return permissionAdapter.hasRoleWithView(getOrganization(), getProject(), collection, role, viewRole, authenticatedUser.getCurrentUserId());
   }

   /**
    * Gets the active view provided in X-Lumeer-View-Id HTTP header through REST endpoint.
    *
    * @return The active View when exists, null otherwise.
    */
   public View getActiveView() {
      return permissionAdapter.activeView();
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

   public static Set<Role> getActualRoles(final Organization organization, final Project project, final Resource resource, final User user) {
      if (ResourceUtils.userIsManagerInWorkspace(user.getId(), organization, project)) {
         return getAllRoles(resource);
      }

      return getActualRolesInResource(organization, resource, user);
   }

   private static Set<Role> getAllRoles(final Resource resource) {
      return ResourceUtils.getAllResourceRoles(resource);
   }

   private Set<Role> getActualRolesInResource(final Resource resource, final String userId) {
      final Set<String> groups = authenticatedUser.getCurrentUserId().equals(userId) ? getUserGroups(resource) : getUserGroups(resource, userId);

      final Set<Role> actualRoles = getActualUserRoles(resource.getPermissions().getUserPermissions(), userId);
      actualRoles.addAll(getActualGroupRoles(resource.getPermissions().getGroupPermissions(), groups));
      return Role.withTransitionRoles(actualRoles);
   }

   private static Set<Role> getActualRolesInResource(final Organization organization, final Resource resource, final User user) {
      return ResourceUtils.getRolesInResource(organization, resource, user);
   }

   private Set<String> getUserGroups(Resource resource) {
      if (resource instanceof Organization) {
         return Collections.emptySet();
      }
      return authenticatedUserGroups.getCurrentUserGroups();
   }

   private Set<String> getUserGroups(final Resource resource, final String userId) {
      final User user = userDao.getUserById(userId);
      final Organization organization = workspaceKeeper.getOrganization().orElse(null);
      return getUserGroups(organization, resource, user);
   }

   private static Set<String> getUserGroups(final Organization organization, final Resource resource, final User user) {
      return ResourceUtils.getUserGroupsInResource(organization, resource, user);
   }

   private static Set<Role> getActualUserRoles(Set<Permission> userRoles, String userId) {
      return ResourceUtils.getRolesByUser(userRoles, userId);
   }

   private static Set<Role> getActualGroupRoles(Set<Permission> groupRoles, Set<String> groupIds) {
      return ResourceUtils.getRolesByGroups(groupRoles, groupIds);
   }

   public Map<String, AllowedPermissions> getCollectionsPermissions(final java.util.Collection<Collection> collection) {
      return collection.stream().collect(Collectors.toMap(Resource::getId, this::getCollectionPermissions));
   }

   public AllowedPermissions getCollectionPermissions(final Collection collection) {
      var userId = authenticatedUser.getCurrentUserId();
      return new AllowedPermissions(
            permissionAdapter.hasRoleInResource(getOrganization(), getProject(), collection, Role.READ, userId),
            permissionAdapter.hasRoleInResource(getOrganization(), getProject(), collection, Role.WRITE, userId),
            permissionAdapter.hasRoleInResource(getOrganization(), getProject(), collection, Role.MANAGE, userId),
            hasRoleWithView(collection, Role.READ, Role.READ),
            hasRoleWithView(collection, Role.WRITE, Role.WRITE),
            hasRoleWithView(collection, Role.MANAGE, Role.MANAGE)
      );
   }

   public Map<String, AllowedPermissions> getLinkTypesPermissions(final java.util.Collection<LinkType> linkTypes, final Map<String, AllowedPermissions> collectionsPermissions) {
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
            throw new NoResourcePermissionException(collection);
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

   public long countDocuments() {
      return collectionAdapter.getDocumentsCount();
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
      if (permissionAdapter.getViewId() == null) {
         this.permissionAdapter.setViewId(viewId);
      }
   }

   public String getViewId() {
      return this.permissionAdapter.getViewId();
   }

   // For testing purposes to allow viewId manipulation during test run.
   void testSetViewId(final String viewId) {
      this.permissionAdapter.setViewId(viewId);
   }

   String testGetViewId() {
      return this.permissionAdapter.getViewId();
   }

   private Organization getOrganization() {
      return workspaceKeeper.getOrganization().orElse(null);
   }

   private Project getProject() {
      return workspaceKeeper.getProject().orElse(null);
   }

   /**
    * Checks whether it is possible to delete the given resource.
    *
    * @param resource Resource to check.
    * @throws NoResourcePermissionException When it is not possible to delete the resource.
    */
   public void checkCanDelete(Resource resource) {
      if (resource.isNonRemovable()) {
         throw new NoResourcePermissionException(resource);
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
         return ResourceUtils.getOrganizationManagers(organization);
      }
      return Collections.emptySet();
   }

   /**
    * Get managers in current workspace (both organization and project).
    *
    * @return Set of IDs of all managers of current organization and project.
    */
   public Set<String> getWorkspaceManagers() {
      if (workspaceKeeper.getOrganization().isPresent() && workspaceKeeper.getProject().isPresent()) {
         Project project = workspaceKeeper.getProject().get();
         return ResourceUtils.getProjectManagers(workspaceKeeper.getOrganization().get(), project);
      }
      return Collections.emptySet();
   }
}
