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
package io.lumeer.api.dto;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JsonPermissions implements Permissions {

   public static final String USER_PERMISSIONS = "users";
   public static final String GROUP_PERMISSIONS = "groups";

   @JsonProperty(USER_PERMISSIONS)
   private final Set<JsonPermission> userPermissions;

   @JsonProperty(GROUP_PERMISSIONS)
   private final Set<JsonPermission> groupPermissions;

   public JsonPermissions() {
      this(new HashSet<>(), new HashSet<>());
   }

   public JsonPermissions(Permissions permissions) {
      this.userPermissions = JsonPermission.convert(permissions.getUserPermissions());
      this.groupPermissions = JsonPermission.convert(permissions.getGroupPermissions());
   }

   @JsonCreator
   public JsonPermissions(@JsonProperty(USER_PERMISSIONS) final Set<JsonPermission> userPermissions,
         @JsonProperty(GROUP_PERMISSIONS) final Set<JsonPermission> groupPermissions) {
      this.userPermissions = userPermissions;
      this.groupPermissions = groupPermissions;
   }

   @Override
   public Set<Permission> getUserPermissions() {
      return Collections.unmodifiableSet(userPermissions);
   }

   @Override
   public void updateUserPermissions(final Permission... newUserPermissions) {
      Arrays.stream(newUserPermissions)
            .map(JsonPermission::new)
            .forEach(userPermission -> {
               userPermissions.remove(userPermission);
               userPermissions.add(userPermission);
            });
   }

   @Override
   public void removeUserPermission(final String userId) {
      userPermissions.removeIf(userRoles -> userRoles.getId().equals(userId));
   }

   @Override
   public Set<Permission> getGroupPermissions() {
      return Collections.unmodifiableSet(groupPermissions);
   }

   @Override
   public void updateGroupPermissions(final Permission... newGroupPermissions) {
      Arrays.stream(newGroupPermissions)
            .map(JsonPermission::new)
            .forEach(groupPermission -> {
               groupPermissions.remove(groupPermission);
               groupPermissions.add(groupPermission);
            });
   }

   @Override
   public void removeGroupPermission(final String groupId) {
      groupPermissions.removeIf(groupRoles -> groupRoles.getId().equals(groupId));
   }

   @Override
   public void clear() {
      userPermissions.clear();
      groupPermissions.clear();
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
      return "JsonPermissions{" +
            "users=" + userPermissions +
            ", groups=" + groupPermissions +
            '}';
   }

   public static JsonPermissions convert(Permissions permissions) {
      return permissions instanceof JsonPermissions ? (JsonPermissions) permissions : new JsonPermissions(permissions);
   }
}
