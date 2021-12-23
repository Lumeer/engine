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

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.adapter.CollectionAdapter;
import io.lumeer.core.adapter.PermissionAdapter;
import io.lumeer.core.exception.FeatureNotAllowedException;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.core.exception.ServiceLimitsExceededException;
import io.lumeer.core.facade.FreshdeskFacade;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.PaymentFacade;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class PermissionsChecker {

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private SelectedWorkspace selectedWorkspace;

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
   private GroupDao groupDao;

   @Inject
   private FreshdeskFacade freshdeskFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   private CollectionAdapter collectionAdapter;
   private PermissionAdapter permissionAdapter;

   private static boolean allowGroups = false;

   @PostConstruct
   public void init() {
      collectionAdapter = new CollectionAdapter(collectionDao, favoriteItemDao, documentDao);
      permissionAdapter = new PermissionAdapter(userDao, groupDao, viewDao, linkTypeDao, collectionDao);
   }

   public PermissionsChecker() {
   }

   PermissionsChecker(AuthenticatedUser authenticatedUser, SelectedWorkspace selectedWorkspace, UserDao userDao, GroupDao groupDao, CollectionDao collectionDao, ViewDao viewDao, LinkTypeDao linkTypeDao, FavoriteItemDao favoriteItemDao, DocumentDao documentDao) {
      this.authenticatedUser = authenticatedUser;
      this.selectedWorkspace = selectedWorkspace;
      this.userDao = userDao;
      this.groupDao = groupDao;
      this.collectionDao = collectionDao;
      this.viewDao = viewDao;
      this.linkTypeDao = linkTypeDao;
      this.favoriteItemDao = favoriteItemDao;
      this.documentDao = documentDao;
   }

   public static PermissionsChecker getPermissionsChecker(final AuthenticatedUser authenticatedUser, final DaoContextSnapshot daoContextSnapshot) {
      var pc = new PermissionsChecker(authenticatedUser, daoContextSnapshot.getSelectedWorkspace(), daoContextSnapshot.getUserDao(), daoContextSnapshot.getGroupDao(), daoContextSnapshot.getCollectionDao(), daoContextSnapshot.getViewDao(), daoContextSnapshot.getLinkTypeDao(), daoContextSnapshot.getFavoriteItemDao(), daoContextSnapshot.getDocumentDao());
      pc.init();

      return pc;
   }

   public PermissionAdapter getPermissionAdapter() {
      return permissionAdapter;
   }

   /**
    * Checks if user can read all collections, link types and views in organization or project
    *
    * @return whether the current user is a manager.
    */
   public boolean canReadAllInWorkspace() {
      return canReadAllInWorkspace(authenticatedUser.getCurrentUserId());
   }

   public boolean canReadAllInWorkspace(String userId) {
      return permissionAdapter.canReadAllInWorkspace(getOrganization(), getProject(), userId);
   }

   public boolean isPublic() {
      return permissionAdapter.isPublic(getOrganization(), getProject());
   }

   /**
    * Checks if the user has the given role on the given resource (either directly or through group membership).
    *
    * @param resource any resource with defined permissions.
    * @param role     role to be checked.
    * @throws NoResourcePermissionException when the user does not have the permission.
    */
   public void checkRole(Resource resource, RoleType role) {
      permissionAdapter.checkRole(getOrganization(), getProject(), resource, role, authenticatedUser.getCurrentUserId());
   }

   /**
    * Checks whether the current user has the given role on the resource.
    *
    * @param resource The resource to be checked.
    * @param role     The required role.
    * @return True if and only if the user has the given role on the resource.
    */
   public boolean hasRole(Resource resource, RoleType role) {
      return hasRole(resource, role, authenticatedUser.getCurrentUserId());
   }

   public boolean hasRole(Resource resource, RoleType role, String userId) {
      return permissionAdapter.hasRole(getOrganization(), getProject(), resource, role, userId);
   }

   public boolean hasRole(Resource resource, RoleType role, Group group) {
      return permissionAdapter.hasRole(getOrganization(), getProject(), resource, role, group);
   }

   public boolean hasAnyRole(Resource resource, Set<RoleType> roles) {
      return permissionAdapter.hasAnyRole(getOrganization(), getProject(), resource, roles, authenticatedUser.getCurrentUserId());
   }

   public boolean hasAllRoles(Resource resource, Set<RoleType> roles) {
      return permissionAdapter.hasAllRoles(getOrganization(), getProject(), resource, roles, authenticatedUser.getCurrentUserId());
   }

   public void checkAllRoles(Resource resource, Set<RoleType> roles) {
      permissionAdapter.checkAllRoles(getOrganization(), getProject(), resource, roles, authenticatedUser.getCurrentUserId());
   }

   public void checkAnyRole(Resource resource, Set<RoleType> roles) {
      permissionAdapter.checkAnyRole(getOrganization(), getProject(), resource, roles, authenticatedUser.getCurrentUserId());
   }

   /**
    * Checks whether it is possible to delete the given resource.
    *
    * @param resource Resource to check.
    * @throws NoResourcePermissionException When it is not possible to delete the resource.
    */
   public void checkCanDelete(Resource resource) {
      permissionAdapter.checkCanDelete(getOrganization(), getProject(), resource, authenticatedUser.getCurrentUserId());
   }

   public void checkCanDelete(LinkType linkType) {
      permissionAdapter.checkCanDelete(getOrganization(), getProject(), linkType, authenticatedUser.getCurrentUserId());
   }

   /**
    * Checks whether the current user has the given role on the given resource or the user has access to a view whose author has the given role.
    *
    * @param collection collection resource
    * @param role       role to be checked.
    * @return true if and only if the user has the given role ont he resource.
    */
   public boolean hasRoleInCollectionWithView(final Collection collection, final RoleType role) {
      return permissionAdapter.hasRoleInCollectionWithView(getOrganization(), getProject(), collection, role, authenticatedUser.getCurrentUserId());
   }

   /**
    * Checks if the user has the given role on the given resource or the user has access to a view whose author has the given role.
    *
    * @param collection collection resource
    * @param role       role to be checked.
    * @throws NoResourcePermissionException when the user does not have the permission.
    */
   public void checkRoleInCollectionWithView(final Collection collection, final RoleType role) {
      permissionAdapter.checkRoleInCollectionWithView(getOrganization(), getProject(), collection, role, authenticatedUser.getCurrentUserId());
   }

   public void checkRoleInLinkTypeWithView(LinkType linkType, RoleType role) {
      permissionAdapter.checkRoleInLinkTypeWithView(getOrganization(), getProject(), linkType, role, authenticatedUser.getCurrentUserId());
   }

   public boolean hasRoleInLinkTypeWithView(LinkType linkType, RoleType role) {
      return permissionAdapter.hasRoleInLinkTypeWithView(getOrganization(), getProject(), linkType, role, authenticatedUser.getCurrentUserId());
   }

   public boolean hasRoleInLinkType(LinkType linkType, RoleType role) {
      return permissionAdapter.hasRoleInLinkType(getOrganization(), getProject(), linkType, role, authenticatedUser.getCurrentUserId());
   }

   public void checkAnyRoleInLinkType(LinkType linkType, Set<RoleType> roles) {
      permissionAdapter.checkAnyRoleInLinkType(getOrganization(), getProject(), linkType, roles, authenticatedUser.getCurrentUserId());
   }

   public void checkRoleInLinkType(LinkType linkType, RoleType role) {
      checkRoleInLinkType(linkType, role, authenticatedUser.getCurrentUserId());
   }

   public void checkRoleInLinkType(LinkType linkType, RoleType role, String userId) {
      permissionAdapter.checkRoleInLinkType(getOrganization(), getProject(), linkType, role, userId);
   }

   /**
    * Checks if user can create documents in the given collection
    *
    * @param collection collection resource
    * @throws NoResourcePermissionException when the user does not have the permission.
    */

   public void checkCreateDocuments(final Collection collection) {
      permissionAdapter.checkCanCreateDocuments(getOrganization(), getProject(), collection, authenticatedUser.getCurrentUserId());
   }

   public boolean canCreateDocuments(final Collection collection) {
      return permissionAdapter.canCreateDocuments(getOrganization(), getProject(), collection, authenticatedUser.getCurrentUserId());
   }

   public void checkReadDocument(final Collection collection, final Document document) {
      permissionAdapter.checkCanReadDocument(getOrganization(), getProject(), document, collection, authenticatedUser.getCurrentUserId());
   }

   public boolean canReadDocument(final Collection collection, final Document document) {
      return permissionAdapter.canReadDocument(getOrganization(), getProject(), document, collection, authenticatedUser.getCurrentUserId());
   }

   public void checkEditDocument(final Collection collection, final Document document) {
      permissionAdapter.checkCanEditDocument(getOrganization(), getProject(), document, collection, authenticatedUser.getCurrentUserId());
   }

   public boolean canEditDocument(final Collection collection, final Document document) {
      return permissionAdapter.canEditDocument(getOrganization(), getProject(), document, collection, authenticatedUser.getCurrentUserId());
   }

   public void checkDeleteDocument(final Collection collection, final Document document) {
      permissionAdapter.checkCanDeleteDocument(getOrganization(), getProject(), document, collection, authenticatedUser.getCurrentUserId());
   }

   public boolean canDeleteDocuments(final Collection collection, final Document document) {
      return permissionAdapter.canDeleteDocument(getOrganization(), getProject(), document, collection, authenticatedUser.getCurrentUserId());
   }

   /**
    * Checks if user can create link instances in the given lnk type
    *
    * @param linkType linkType resource
    * @throws NoPermissionException when the user does not have the permission.
    */

   public void checkCreateLinkInstances(final LinkType linkType) {
      permissionAdapter.checkCanCreateLinkInstances(getOrganization(), getProject(), linkType, authenticatedUser.getCurrentUserId());
   }

   public boolean canCreateLinkInstances(final LinkType linkType) {
      return permissionAdapter.canCreateLinkInstances(getOrganization(), getProject(), linkType, authenticatedUser.getCurrentUserId());
   }

   public void checkReadLinkInstance(final LinkType linkType, final LinkInstance linkInstance) {
      permissionAdapter.checkCanReadLinkInstance(getOrganization(), getProject(), linkInstance, linkType, authenticatedUser.getCurrentUserId());
   }

   public boolean canReadLinkInstance(final LinkType linkType, final LinkInstance linkInstance) {
      return permissionAdapter.canReadLinkInstance(getOrganization(), getProject(), linkInstance, linkType, authenticatedUser.getCurrentUserId());
   }

   public void checkEditLinkInstance(final LinkType linkType, final LinkInstance linkInstance) {
      permissionAdapter.checkCanEditLinkInstance(getOrganization(), getProject(), linkInstance, linkType, authenticatedUser.getCurrentUserId());
   }

   public boolean canEditLinkInstance(final LinkType linkType, final LinkInstance linkInstance) {
      return permissionAdapter.canEditLinkInstance(getOrganization(), getProject(), linkInstance, linkType, authenticatedUser.getCurrentUserId());
   }

   public void checkDeleteLinkInstance(final LinkType linkType, final LinkInstance linkInstance) {
      permissionAdapter.checkCanDeleteLinkInstance(getOrganization(), getProject(), linkInstance, linkType, authenticatedUser.getCurrentUserId());
   }

   public boolean canDeleteLinkInstance(final LinkType linkType, final LinkInstance linkInstance) {
      return permissionAdapter.canDeleteLinkInstance(getOrganization(), getProject(), linkInstance, linkType, authenticatedUser.getCurrentUserId());
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
   public Set<RoleType> getActualRoles(Resource resource) {
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
   public Set<RoleType> getActualRoles(final Resource resource, final String userId) {
      return permissionAdapter.getUserRolesInResource(getOrganization(), getProject(), resource, userId);
   }

   public Set<RoleType> getActualRoles(final LinkType linkType) {
      return permissionAdapter.getUserRolesInLinkType(getOrganization(), getProject(), linkType, authenticatedUser.getCurrentUserId());
   }

   public Map<String, AllowedPermissions> getCollectionsPermissions(final java.util.Collection<Collection> collection) {
      return collection.stream().collect(Collectors.toMap(Resource::getId, this::getCollectionPermissions));
   }

   public AllowedPermissions getCollectionPermissions(final Collection collection) {
      User user = authenticatedUser.getCurrentUser();
      Set<RoleType> roles = permissionAdapter.getUserRolesInResource(getOrganization(), getProject(), collection, user);
      Set<RoleType> rolesWithView = new HashSet<>(permissionAdapter.getUserRolesInCollectionWithView(getOrganization(), getProject(), collection, user));
      rolesWithView.addAll(roles);
      return new AllowedPermissions(roles, rolesWithView);
   }

   public Map<String, AllowedPermissions> getLinkTypesPermissions(final java.util.Collection<LinkType> linkTypes) {
      return linkTypes.stream().collect(Collectors.toMap(LinkType::getId, this::getLinkTypePermissions));
   }

   public AllowedPermissions getLinkTypePermissions(final LinkType linkType) {
      User user = authenticatedUser.getCurrentUser();
      Set<RoleType> roles = permissionAdapter.getUserRolesInLinkType(getOrganization(), getProject(), linkType, user);
      Set<RoleType> rolesWithView = new HashSet<>(permissionAdapter.getUserRolesInLinkTypeWithView(getOrganization(), getProject(), linkType, user));
      rolesWithView.addAll(roles);
      return new AllowedPermissions(roles, rolesWithView);
   }

   public void checkRulesPermissions(Map<String, Rule> ruleMap) {
      if (ruleMap != null) {
         ruleMap.values().forEach(this::checkRulePermissions);
      }
   }

   public void checkRulePermissions(Rule rule) {
      if (rule.getType() == Rule.RuleType.BLOCKLY) {
         checkFunctionRuleAccess(new BlocklyRule(rule).getJs(), RoleType.DataWrite);
      }
   }

   public void checkAttributesFunctionAccess(final java.util.Collection<Attribute> attributes) {
      Objects.requireNonNullElse(attributes, new ArrayList<Attribute>()).stream().filter(Attribute::isFunctionDefined).forEach(attribute -> {
         checkFunctionRuleAccess(attribute.getFunction().getJs(), RoleType.Read);
      });
   }

   public void checkFunctionRuleAccess(final String js, final RoleType role) {
      if (!Utils.isEmpty(js)) {
         permissionAdapter.checkFunctionRuleAccess(getOrganization(), getProject(), js, role, authenticatedUser.getCurrentUserId());
      }
   }

   /**
    * Checks whether current plan includes groups handling.
    */
   public void checkGroupsHandle() {
      if (skipLimits() || allowGroups) {
         return;
      }

      final ServiceLimits limits = getServiceLimits();
      if (!limits.isGroups()) {
         throw new FeatureNotAllowedException("groups");
      }
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
            final Optional<Organization> organization = selectedWorkspace.getOrganization();
            freshdeskFacade.logLimitsExceeded(authenticatedUser.getCurrentUser(), "PROJECT", organization.isPresent() ? organization.get().getId() : "<empty>");
            throw new ServiceLimitsExceededException(limits.getProjects(), resource);
         }
      }

      if (resource.getType().equals(ResourceType.COLLECTION)) {
         if (limits.getFiles() > 0 && limits.getFiles() <= currentCount) {
            final Optional<Organization> organization = selectedWorkspace.getOrganization();
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
         final Optional<Organization> organization = selectedWorkspace.getOrganization();
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
         final Optional<Organization> organization = selectedWorkspace.getOrganization();
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
      return paymentFacade.getCurrentServiceLimits(getOrganization());
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

   public boolean skipPayments() {
      return System.getenv("SKIP_PAYMENTS") != null;
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

   // For testing purposes to allow manipulate with groups
   static void allowGroups() {
      PermissionsChecker.allowGroups = true;
   }

   String testGetViewId() {
      return this.permissionAdapter.getViewId();
   }

   private Organization getOrganization() {
      return selectedWorkspace.getOrganization().orElse(null);
   }

   private Project getProject() {
      return selectedWorkspace.getProject().orElse(null);
   }
}
