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
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.List;
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

   public Organization createOrganization(final Organization organization) {
      // TODO check system role for creating organizations

      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getUserEmail(), Organization.ROLES);
      organization.getPermissions().updateUserPermissions(defaultUserPermission);

      Organization storedOrganization = organizationDao.createOrganization(organization);

      createOrganizationScopedRepositories(storedOrganization);

      return storedOrganization;
   }

   public Organization updateOrganization(final String organizationCode, final Organization organization) {
      Organization storedOrganization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(storedOrganization, Role.MANAGE);

      keepStoredPermissions(organization, storedOrganization.getPermissions());
      Organization updatedOrganization = organizationDao.updateOrganization(storedOrganization.getId(), organization);

      return keepOnlyActualUserRoles(updatedOrganization);
   }

   public void deleteOrganization(final String organizationCode) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, Role.MANAGE);

      deleteOrganizationScopedRepositories(organization);

      organizationDao.deleteOrganization(organization.getId());
   }

   public Organization getOrganization(final String organizationCode) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, Role.READ);

      return keepOnlyActualUserRoles(organization);
   }

   public List<Organization> getOrganizations() {
      String userEmail = authenticatedUser.getUserEmail();
      DatabaseQuery query = DatabaseQuery.createBuilder(userEmail)
                                         .build();

      return organizationDao.getOrganizations(query).stream()
                            .map(this::keepOnlyActualUserRoles)
                            .collect(Collectors.toList());
   }

   public Set<String> getOrganizationsCodes(){
      return organizationDao.getOrganizationsCodes();
   }

   private Organization checkRoleAndGetOrganization(final String organizationCode, final Role role) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, role);

      return organization;
   }

   public Permissions getOrganizationPermissions(final String organizationCode) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      return organization.getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String organizationCode, final Permission... userPermissions) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      organization.getPermissions().updateUserPermissions(userPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);

      return organization.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String organizationCode, final String user) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      organization.getPermissions().removeUserPermission(user);
      organizationDao.updateOrganization(organization.getId(), organization);
   }

   public Set<Permission> updateGroupPermissions(final String organizationCode, final Permission... groupPermissions) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      organization.getPermissions().updateGroupPermissions(groupPermissions);
      organizationDao.updateOrganization(organization.getId(), organization);

      return organization.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String organizationCode, final String group) {
      Organization organization = checkRoleAndGetOrganization(organizationCode, Role.MANAGE);

      organization.getPermissions().removeGroupPermission(group);
      organizationDao.updateOrganization(organization.getId(), organization);
   }

   private void createOrganizationScopedRepositories(Organization organization) {
      projectDao.setOrganization(organization);
      projectDao.createProjectsRepository(organization);
      groupDao.createGroupsRepository(organization);
   }

   private void deleteOrganizationScopedRepositories(Organization organization) {
      projectDao.setOrganization(organization);
      projectDao.deleteProjectsRepository(organization);
      groupDao.deleteGroupsRepository(organization);
   }
}
