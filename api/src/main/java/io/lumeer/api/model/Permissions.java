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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Permissions {

   public static final String USER_PERMISSIONS = "users";
   public static final String GROUP_PERMISSIONS = "groups";

   @JsonProperty(USER_PERMISSIONS)
   private final Set<Permission> userPermissions;

   @JsonProperty(GROUP_PERMISSIONS)
   private final Set<Permission> groupPermissions;

   public Permissions() {
      this(new HashSet<>(), new HashSet<>());
   }

   public Permissions(Permissions permissions) {
      this(permissions.getUserPermissions(), permissions.getGroupPermissions());
   }

   @JsonCreator
   public Permissions(@JsonProperty(USER_PERMISSIONS) final Set<Permission> userPermissions,
         @JsonProperty(GROUP_PERMISSIONS) final Set<Permission> groupPermissions) {
      this.userPermissions = userPermissions != null ? new HashSet<>(userPermissions) : new HashSet<>();
      this.groupPermissions = groupPermissions != null ? new HashSet<>(groupPermissions) : new HashSet<>();
   }

   public Set<Permission> getUserPermissions() {
      return Collections.unmodifiableSet(userPermissions);
   }

   public void updateUserPermissions(final Permission newUserPermission) {
      updateUserPermissions(Set.of(newUserPermission));
   }

   public void updateUserPermissions(final Set<Permission> newUserPermissions) {
      updatePermissions(userPermissions, newUserPermissions);
   }

   public void addUserPermissions(final Set<Permission> newUserPermissions) {
      addPermissions(userPermissions, newUserPermissions);
   }

   public void removeUserPermission(final String userId) {
      userPermissions.removeIf(userRoles -> userRoles.getId().equals(userId));
   }

   public Set<Permission> getGroupPermissions() {
      return Collections.unmodifiableSet(groupPermissions);
   }

   public void updateGroupPermissions(final Permission newGroupPermission) {
      updateGroupPermissions(Set.of(newGroupPermission));
   }

   public void updateGroupPermissions(final Set<Permission> newGroupPermissions) {
      updatePermissions(groupPermissions, newGroupPermissions);
   }

   private void updatePermissions(Set<Permission> permissions, final Set<Permission> newPermissions) {
      newPermissions.stream()
                    .map(Permission::new)
                    .forEach(permission -> {
                       permissions.remove(permission);
                       if (!permission.getRoles().isEmpty()) {
                          permissions.add(permission);
                       }
                    });
   }

   private void addPermissions(Set<Permission> permissions, final Set<Permission> newPermissions) {
      newPermissions.stream()
                    .map(Permission::new)
                    .filter(permission -> !permissions.contains(permission))
                    .forEach(permissions::add);
   }

   public void removeGroupPermission(final String groupId) {
      groupPermissions.removeIf(groupRoles -> groupRoles.getId().equals(groupId));
   }

   public void clearUserPermissions() {
      userPermissions.clear();
   }

   public void clearGroupPermissions() {
      groupPermissions.clear();
   }

   public void clear() {
      clearUserPermissions();
      clearGroupPermissions();
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Permissions)) {
         return false;
      }

      final Permissions that = (Permissions) o;

      if (userPermissions != null ? !userPermissions.equals(that.getUserPermissions()) : that.getUserPermissions() != null) {
         return false;
      }
      return groupPermissions != null ? groupPermissions.equals(that.getGroupPermissions()) : that.getGroupPermissions() == null;
   }

   @Override
   public int hashCode() {
      int result = userPermissions != null ? userPermissions.hashCode() : 0;
      result = 31 * result + (groupPermissions != null ? groupPermissions.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "Permissions{" +
            "users=" + userPermissions +
            ", groups=" + groupPermissions +
            '}';
   }

}
