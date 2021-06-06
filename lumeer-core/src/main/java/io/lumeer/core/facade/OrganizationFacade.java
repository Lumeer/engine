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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectDescription;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.core.exception.NoSystemPermissionException;
import io.lumeer.core.util.Utils;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

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
   private PaymentFacade paymentFacade;

   @Inject
   private WorkspaceCache workspaceCache;

   @Inject
   private DelayedActionDao delayedActionDao;

   public Organization createOrganization(final Organization organization) {
      Utils.checkCodeSafe(organization.getCode());

      List<Organization> organizations = getOrganizations();
      if (organizations.stream().anyMatch(o -> permissionsChecker.hasRole(o, RoleType.Read))) {
         checkCreateOrganization(organizations);
      }

      Permission defaultUserPermission = Permission.buildWithRoles(getCurrentUserId(), Organization.ROLES);
      organization.getPermissions().updateUserPermissions(defaultUserPermission);

      Organization storedOrganization = organizationDao.createOrganization(organization);

      createOrganizationInUser(storedOrganization.getId());
      createOrganizationScopedRepositories(storedOrganization);

      return storedOrganization;
   }

   private void checkCreateOrganization(List<Organization> organizations) {
      var hasManagedOrganization = organizations.stream().anyMatch(organization -> permissionsChecker.hasRole(organization, RoleType.Config));
      if (hasManagedOrganization) {
         this.checkSystemPermission();
      }
   }

   public Organization updateOrganization(final String organizationId, final Organization organization) {
      Utils.checkCodeSafe(organization.getCode());
      Organization storedOrganization = checkRoleAndGetOrganization(organizationId, RoleType.Config);

      keepStoredPermissions(organization, storedOrganization.getPermissions());
      keepUnmodifiableFields(organization, storedOrganization);
      Organization updatedOrganization = organizationDao.updateOrganization(storedOrganization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(updatedOrganization.getId(), updatedOrganization);

      return mapResource(updatedOrganization);
   }

   public void deleteOrganization(final String organizationId) {
      Organization organization = checkRoleAndGetOrganization(organizationId, RoleType.Config);
      permissionsChecker.checkCanDelete(organization);

      deleteOrganizationScopedRepositories(organization);

      organizationDao.deleteOrganization(organization.getId());
      workspaceCache.removeOrganization(organizationId);
   }

   public Organization getOrganizationByCode(final String code) {
      final Organization organization = organizationDao.getOrganizationByCode(code);
      permissionsChecker.checkRole(organization, RoleType.Read);

      return mapResource(organization);
   }

   public Organization getOrganizationById(final String id) {
      return mapResource(checkRoleAndGetOrganization(id, RoleType.Read));
   }

   public List<Organization> getOrganizations() {
      return organizationDao.getAllOrganizations().stream()
                            .filter(organization -> permissionsChecker.hasRole(organization, RoleType.Read))
                            .map(this::mapResource)
                            .collect(Collectors.toList());
   }

   public Set<String> getOrganizationsCodes() {
      return organizationDao.getOrganizationsCodes();
   }

   private Organization checkRoleAndGetOrganization(final String organizationId, final RoleType role) {
      Organization organization = organizationDao.getOrganizationById(organizationId);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   public Permissions getOrganizationPermissions(final String organizationId) {
      return mapResource(checkRoleAndGetOrganization(organizationId, RoleType.UserConfig)).getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String organizationId, final Set<Permission> userPermissions) {
      return updateUserPermissions(organizationId, userPermissions, true);
   }

   public Set<Permission> addUserPermissions(final String organizationId, final Set<Permission> userPermissions) {
      return updateUserPermissions(organizationId, userPermissions, false);
   }

   public Set<Permission> updateUserPermissions(final String organizationId, final Set<Permission> userPermissions, boolean update) {
      Organization organization = checkRoleAndGetOrganization(organizationId, RoleType.UserConfig);

      final Organization originalOrganization = organization.copy();
      if (update) {
         organization.getPermissions().updateUserPermissions(userPermissions);
      } else {
         organization.getPermissions().addUserPermissions(userPermissions);
      }
      organizationDao.updateOrganization(organization.getId(), organization, originalOrganization);
      workspaceCache.updateOrganization(organizationId, organization);

      return organization.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String organizationId, final String userId) {
      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, RoleType.UserConfig);
      final Organization organization = storedOrganization.copy();

      projectDao.getAllProjects().forEach(project -> {
         final Project originalProject = project.copy();
         project.getPermissions().removeUserPermission(userId);
         final Project updatedProject = projectDao.updateProject(project.getId(), project, originalProject);
         workspaceCache.updateProject(project.getId(), updatedProject);
      });

      organization.getPermissions().removeUserPermission(userId);
      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);
   }

   public Set<Permission> updateGroupPermissions(final String organizationId, final Set<Permission> groupPermissions) {
      permissionsChecker.checkGroupsHandle();

      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, RoleType.UserConfig);
      final Organization organization = storedOrganization.copy();

      organization.getPermissions().updateGroupPermissions(groupPermissions);
      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);

      return organization.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String organizationId, final String groupId) {
      permissionsChecker.checkGroupsHandle();

      final Organization storedOrganization = checkRoleAndGetOrganization(organizationId, RoleType.UserConfig);
      final Organization organization = storedOrganization.copy();

      projectDao.getAllProjects().forEach(project -> {
         final Project originalProject = project.copy();
         project.getPermissions().removeGroupPermission(groupId);
         final Project updatedProject = projectDao.updateProject(project.getId(), project, originalProject);
         workspaceCache.updateProject(project.getId(), updatedProject);
      });

      organization.getPermissions().removeGroupPermission(groupId);
      organizationDao.updateOrganization(organization.getId(), organization, storedOrganization);
      workspaceCache.updateOrganization(organizationId, organization);
   }

   public List<Organization> getOrganizationsCapableForProject(final ProjectDescription projectDescription) {
      return getOrganizations().stream().filter(org -> permissionsChecker.hasRole(org, RoleType.Contribute)
      ).filter(org -> {
         final ServiceLimits serviceLimits = paymentFacade.getCurrentServiceLimits(org);
         final long projects = projectDao.getProjectsCount(org);

         if (projects >= serviceLimits.getProjects()) {
            return false;
         }

         if (projectDescription == null) {
            return true;
         }

         return serviceLimits.fitsLimits(projectDescription);
      }).collect(Collectors.toList());
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
      projectDao.createRepository(organization);
      groupDao.createRepository(organization);
      paymentDao.createRepository(organization);
      favoriteItemDao.createRepository(organization);
   }

   private void deleteOrganizationScopedRepositories(Organization organization) {
      projectDao.setOrganization(organization);
      projectDao.deleteRepository(organization);
      groupDao.deleteRepository(organization);
      paymentDao.deleteRepository(organization);
      favoriteItemDao.deleteRepository(organization);

      userDao.deleteUsersGroups(organization.getId());
      userCache.clear();

      delayedActionDao.deleteAllScheduledActions(organization.getId());
   }

   private void checkSystemPermission() {
      String currentUserEmail = authenticatedUser.getUserEmail();
      List<String> allowedEmails = Arrays.asList("support@lumeer.io", "mvecera@lumeer.io", "kubedo8@gmail.com", "aturing@lumeer.io");
      if (!allowedEmails.contains(currentUserEmail)) {
         throw new NoSystemPermissionException();
      }
   }
}
