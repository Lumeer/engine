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
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.exception.NoSystemPermissionException;
import io.lumeer.core.model.SimplePermission;
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
      checkSystemPermission();

      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getCurrentUserId(), Organization.ROLES);
      organization.getPermissions().updateUserPermissions(defaultUserPermission);

      Organization storedOrganization = organizationDao.createOrganization(organization);

      createOrganizationInUser(storedOrganization.getId());
      createOrganizationScopedRepositories(storedOrganization);

      return storedOrganization;
   }

   public Organization updateOrganization(final String organizationCode, final Organization organization) {
      Organization storedOrganization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(storedOrganization, Role.MANAGE);

      keepStoredPermissions(organization, storedOrganization.getPermissions());
      Organization updatedOrganization = organizationDao.updateOrganization(storedOrganization.getId(), organization);
      workspaceCache.updateOrganization(updatedOrganization.getCode(), updatedOrganization);

      return mapResource(updatedOrganization);
   }

   public void deleteOrganization(final String organizationCode) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, Role.MANAGE);

      deleteOrganizationScopedRepositories(organization);

      organizationDao.deleteOrganization(organization.getId());
      workspaceCache.removeOrganization(organizationCode);
   }

   public Organization getOrganization(final String organizationCode) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
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

   private Organization checkRoleAndGetOrganization(final String organizationCode, final Role role) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   public Permissions getOrganizationPermissions(final String organizationCode) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);

      if (permissionsChecker.hasRole(organization, Role.MANAGE)) {
         return organization.getPermissions();
      } else if (permissionsChecker.hasRole(organization, Role.READ)) { // return only user's own permissions
         return keepOnlyActualUserRoles(organization).getPermissions();
      }

      throw new NoPermissionException(organization);
   }

   public Set<Permission> updateUserPermissions(final String organizationCode, final Permission... userPermissions) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      organization.getPermissions().updateUserPermissions(userPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.updateOrganization(organizationCode, organization);

      return organization.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String organizationCode, final String userId) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      organization.getPermissions().removeUserPermission(userId);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.updateOrganization(organizationCode, organization);
   }

   public Set<Permission> updateGroupPermissions(final String organizationCode, final Permission... groupPermissions) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      organization.getPermissions().updateGroupPermissions(groupPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.updateOrganization(organizationCode, organization);

      return organization.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String organizationCode, final String groupId) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      organization.getPermissions().removeGroupPermission(groupId);
      organizationDao.updateOrganization(organization.getId(), organization);
      workspaceCache.updateOrganization(organizationCode, organization);
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
