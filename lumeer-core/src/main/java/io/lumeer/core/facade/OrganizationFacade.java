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
package io.lumeer.core.facade;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.core.exception.NoSystemPermissionException;
import io.lumeer.core.util.Utils;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class OrganizationFacade extends AbstractFacade {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private UserDao userDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private PaymentDao paymentDao;

   @Inject
   private WorkspaceCache workspaceCache;

   public Organization createOrganization(final Organization organization) {
      Utils.checkCodeSafe(organization.getCode());
      checkSystemPermission();

      Permission defaultUserPermission = Permission.buildWithRoles(authenticatedUser.getCurrentUserId(), Organization.ROLES);
      organization.getPermissions().updateUserPermissions(defaultUserPermission);

      Organization storedOrganization = organizationDao.createOrganization(organization);

      createOrganizationInUser(storedOrganization.getId());
      createOrganizationScopedRepositories(storedOrganization);

      return storedOrganization;
   }

   public Organization updateOrganization(final String organizationId, final Organization organization) {
      Utils.checkCodeSafe(organization.getCode());
      Organization storedOrganization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(storedOrganization, Role.MANAGE);

      keepStoredPermissions(organization, storedOrganization.getPermissions());
      keepUnmodifiableFields(organization, storedOrganization);
      Organization updatedOrganization = organizationDao.updateOrganization(storedOrganization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(updatedOrganization.getId(), updatedOrganization);

      return mapResource(updatedOrganization);
   }

   public void deleteOrganization(final String organizationId) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, Role.MANAGE);
      permissionsChecker.checkCanDelete(organization);

      deleteOrganizationScopedRepositories(organization);

      organizationDao.deleteOrganization(organization.getId());
      workspaceCache.removeOrganization(organizationId);
   }

   public Organization getOrganizationByCode(final String code) {
      final Organization organization = organizationDao.getOrganizationByCode(code);
      permissionsChecker.checkRole(organization, Role.READ);

      return mapResource(organization);
   }

   public Organization getOrganizationById(final String id) {
      final Organization organization = organizationDao.getOrganizationById(id);
      permissionsChecker.checkRole(organization, Role.READ);

      return mapResource(organization);
   }

   public List<Organization> getOrganizations() {
      String userEmail = authenticatedUser.getCurrentUserId();
      DatabaseQuery query = DatabaseQuery.createBuilder(userEmail)
                                         .build();

      return organizationDao.getOrganizations(query).stream()
                            .map(this::mapResource)
                            .collect(Collectors.toList());
   }

   public Set<String> getOrganizationsCodes() {
      return organizationDao.getOrganizationsCodes();
   }

   private Organization checkRoleAndGetOrganization(final String organizationId, final Role role) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   public Permissions getOrganizationPermissions(final String organizationId) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, Role.READ);

      return mapResource(organization).getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String organizationId, final Set<Permission> userPermissions) {
      Organization organization = checkRoleAndGetOrganization(organizationId, Role.MANAGE);

      final Organization originalOrganization = organization.copy();
      organization.getPermissions().updateUserPermissions(userPermissions);
      organizationDao.updateOrganization(organization.getId(), organization, originalOrganization);
      workspaceCache.updateOrganization(organizationId, organization);

      return organization.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String organizationId, final String userId) {
      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, Role.MANAGE);
      final Organization organization = storedOrganization.copy();

      organization.getPermissions().removeUserPermission(userId);
      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);
   }

   public Set<Permission> updateGroupPermissions(final String organizationId, final Set<Permission> groupPermissions) {
      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, Role.MANAGE);
      final Organization organization = storedOrganization.copy();

      organization.getPermissions().updateGroupPermissions(groupPermissions);
      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);

      return organization.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String organizationId, final String groupId) {
      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, Role.MANAGE);
      final Organization organization = storedOrganization.copy();

      organization.getPermissions().removeGroupPermission(groupId);
      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);
   }

   private void createOrganizationInUser(final String organizationId) {
      User currentUser = authenticatedUser.getCurrentUser();

      Map<String, Set<String>> groups = currentUser.getGroups() != null ? new HashMap<>(currentUser.getGroups()) : new HashMap<>();
      groups.put(organizationId, new HashSet<>());
      currentUser.setGroups(groups);

      userDao.updateUser(currentUser.getId(), currentUser);
   }

   private void createOrganizationScopedRepositories(Organization organization) {
      projectDao.setOrganization(organization);
      projectDao.createProjectsRepository(organization);
      groupDao.createGroupsRepository(organization);
      paymentDao.createPaymentRepository(organization);
      favoriteItemDao.createRepositories(organization);
   }

   private void deleteOrganizationScopedRepositories(Organization organization) {
      projectDao.setOrganization(organization);
      projectDao.deleteProjectsRepository(organization);
      groupDao.deleteGroupsRepository(organization);
      paymentDao.deletePaymentRepository(organization);
      favoriteItemDao.deleteRepositories(organization);

      userDao.deleteUsersGroups(organization.getId());
      userCache.clear();
   }

   private void checkSystemPermission() {
      String currentUserEmail = authenticatedUser.getUserEmail();
      List<String> allowedEmails = Arrays.asList("support@lumeer.io", "martin@vecerovi.com", "kubedo8@gmail.com", "livoratom@gmail.com", "aturing@lumeer.io");
      if (!allowedEmails.contains(currentUserEmail)) {
         throw new NoSystemPermissionException();
      }
   }
}
