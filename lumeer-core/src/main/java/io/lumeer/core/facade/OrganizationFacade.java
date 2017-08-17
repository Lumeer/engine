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
public class OrganizationFacade {

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
            .map(this::keepOnlyActualUserRoles)
            .collect(Collectors.toList());
   }

   private Organization keepOnlyActualUserRoles(final Organization organization) {
      Set<Role> roles = permissionsChecker.getActualRoles(organization);
      Permission permission = new SimplePermission(authenticatedUser.getUserEmail(), roles);

      organization.getPermissions().clear();
      organization.getPermissions().updateUserPermissions(permission);

      return organization;
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

      organizationDao.deleteOrganization(organizationCode);
   }

   public Organization editOrganization(final String organizationCode, final Organization organization) {
      Organization storedOrganization = organizationDao.getOrganizationByCode(organizationCode);
      permissionsChecker.checkRole(storedOrganization, Role.MANAGE);

      keepStoredPermissions(organization, storedOrganization.getPermissions());
      Organization updatedOrganization = organizationDao.editOrganization(organizationCode, organization);

      return keepOnlyActualUserRoles(updatedOrganization);
   }

   private void keepStoredPermissions(final Organization organization, final Permissions storedPermissions) {
      Set<Permission> userPermissions = storedPermissions.getUserPermissions();
      organization.getPermissions().updateUserPermissions(userPermissions.toArray(new Permission[0]));

      Set<Permission> groupPermissions = storedPermissions.getGroupPermissions();
      organization.getPermissions().updateGroupPermissions(groupPermissions.toArray(new Permission[0]));
   }
}
