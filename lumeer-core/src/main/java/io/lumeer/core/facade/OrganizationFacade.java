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
package io.lumeer.core.facade;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.PermissionsChecker;
import io.lumeer.core.cache.UserCache;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.DatabaseQuery;
import io.lumeer.storage.api.dao.OrganizationDao;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class OrganizationFacade extends AbstractFacade {

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private UserCache userCache;

   @Inject
   private OrganizationDao organizationDao;

   public OrganizationFacade() {
   }

   OrganizationFacade(AuthenticatedUser authenticatedUser, PermissionsChecker permissionsChecker, OrganizationDao organizationDao) {
      this.authenticatedUser = authenticatedUser;
      this.permissionsChecker = permissionsChecker;
      this.organizationDao = organizationDao;
   }

   public List<Organization> getOrganizations() {
      String userEmail = authenticatedUser.getUserEmail();
      DatabaseQuery query = new DatabaseQuery.Builder(userEmail)
            .groups(userCache.getUser(userEmail).getGroups())
            .build();

      return organizationDao.getOrganizations(query).stream()
            .map(resource -> keepOnlyActualUserRoles(resource))
            .collect(Collectors.toList());
   }

   public Organization createOrganization(final Organization organization) {
      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getUserEmail(), Organization.ROLES);
      organization.getPermissions().updateUserPermissions(defaultUserPermission);

      return organizationDao.createOrganization(organization);
   }

   public Organization getOrganization(final String organizationCode) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, Role.READ);

      return keepOnlyActualUserRoles(organization);
   }

   public void deleteOrganization(final String organizationCode) {
      Organization organization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(organization, Role.MANAGE);

      organizationDao.deleteOrganization(organization.getId());
   }

   public Organization updateOrganization(final String organizationCode, final Organization organization) {
      Organization storedOrganization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(storedOrganization, Role.MANAGE);

      keepStoredPermissions(organization, storedOrganization.getPermissions());
      Organization updatedOrganization = organizationDao.updateOrganization(storedOrganization.getId(), organization);

      return keepOnlyActualUserRoles(updatedOrganization);
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
}
