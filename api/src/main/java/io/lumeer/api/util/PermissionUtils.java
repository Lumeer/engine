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
package io.lumeer.api.util;

import static java.util.stream.Collectors.toSet;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class PermissionUtils {

   private PermissionUtils() {
   }

   public static Set<String> getOrganizationUsersByRole(Organization organization, List<User> users, RoleType roleType) {
      return getResourceUsersByRole(organization, null, organization, users, roleType);
   }

   public static Set<String> getProjectUsersByRole(Organization organization, Project project, List<User> users, RoleType roleType) {
      return getResourceUsersByRole(organization, project, project, users, roleType);
   }

   public static Set<String> getResourceUsersByRole(Organization organization, @Nullable Project project, Resource resource, List<User> users, RoleType roleType) {
      return users.stream().filter(user -> getUserRolesInResource(organization, project, resource, user).contains(roleType)).map(User::getId).collect(Collectors.toSet());
   }

   public static Set<RoleType> getUserRolesInResource(Organization organization, @Nullable Project project, Resource resource, User user) {
      final Set<String> groups = getUserGroups(organization, user);

      final Set<Role> actualRoles = getUserRolesInResource(resource, user, groups);
      final Set<Role> organizationRoles = resource instanceof Organization ? actualRoles : getUserRolesInResource(organization, user, groups);

      if (organization != null && !(resource instanceof Organization)) {
         // It's necessary to have read permission in organization in order to process resource (project, collection, view, link)
         if (organizationRoles.stream().noneMatch(role -> role.getRoleType() == RoleType.Read)) {
            return Collections.emptySet();
         }

         final Set<Role> organizationTransitiveRoles = organizationRoles.stream().filter(Role::isTransitive).collect(Collectors.toSet());
         actualRoles.addAll(organizationTransitiveRoles);
      }

      if (project != null && !(resource instanceof Project)) {
         final Set<Role> projectRoles = getUserRolesInResource(project, user, groups);

         // It's necessary to have read permission in project in order to process resource (collection, view, link)
         if (organizationRoles.stream().noneMatch(role -> role.getRoleType() == RoleType.Read && role.isTransitive())
               && projectRoles.stream().noneMatch(role -> role.getRoleType() == RoleType.Read)) {
            return Collections.emptySet();
         }

         final Set<Role> projectTransitiveRoles = projectRoles.stream().filter(Role::isTransitive).collect(Collectors.toSet());
         actualRoles.addAll(projectTransitiveRoles);
      }

      return actualRoles.stream().map(Role::getRoleType).collect(Collectors.toSet());
   }

   public static Set<Role> getUserRolesInResource(Resource resource, User user, Set<String> groups) {
      Permissions permissions = resource != null ? resource.getPermissions() : new Permissions();
      Set<Permission> userPermissions = Objects.requireNonNullElse(permissions.getUserPermissions(), Collections.emptySet());
      Set<Permission> groupPermissions = Objects.requireNonNullElse(permissions.getGroupPermissions(), Collections.emptySet());

      final Set<Role> actualRoles = getRolesByUser(userPermissions, user.getId());
      actualRoles.addAll(getRolesByGroups(groupPermissions, groups));
      return actualRoles;
   }

   public static Set<Role> getRolesByUser(Set<Permission> userRoles, String userId) {
      return userRoles.stream()
                      .filter(entity -> entity.getId() != null && entity.getId().equals(userId))
                      .flatMap(entity -> entity.getRoles().stream())
                      .collect(toSet());
   }

   public static Set<Role> getRolesByGroups(Set<Permission> groupRoles, Set<String> groupIds) {
      return groupRoles.stream()
                       .filter(entity -> groupIds.contains(entity.getId()))
                       .flatMap(entity -> entity.getRoles().stream())
                       .collect(toSet());
   }

   public static Set<String> getUserGroups(final Organization organization, final User user) {
      if (organization == null || user == null || "".equals(user.getId())) {
         return Collections.emptySet();
      }

      return Objects.requireNonNullElse(user.getGroups().get(organization.getId()), Collections.emptySet());
   }
}
