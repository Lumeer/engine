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
   public void removeUserPermission(final String user) {
      userPermissions.removeIf(userRoles -> userRoles.getName().equals(user));
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
   public void removeGroupPermission(final String group) {
      groupPermissions.removeIf(groupRoles -> groupRoles.getName().equals(group));
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
