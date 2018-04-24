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
import io.lumeer.api.model.Role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

@Immutable
public class JsonPermission implements Permission {

   private final String id;
   private final Set<String> roles;

   @JsonCreator
   public JsonPermission(@JsonProperty("id") final String id,
         @JsonProperty("roles") final Set<String> roles) {
      this.id = id;
      this.roles = roles;
   }

   public JsonPermission(final Permission permission) {
      this.id = permission.getId();
      this.roles = permission.getRoles().stream()
                             .map(Role::toString)
                             .collect(Collectors.toSet());
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public Set<Role> getRoles() {
      return roles.stream()
                  .map(Role::fromString)
                  .collect(Collectors.toSet());
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Permission)) {
         return false;
      }

      final Permission that = (Permission) o;

      return getId() != null ? getId().equals(that.getId()) : that.getId() == null;
   }

   @Override
   public int hashCode() {
      return getId() != null ? getId().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "JsonPermission{" +
            "id='" + id + '\'' +
            ", roles=" + roles +
            '}';
   }

   public static JsonPermission convert(Permission permission) {
      return permission instanceof JsonPermission ? (JsonPermission) permission : new JsonPermission(permission);
   }

   public static Set<JsonPermission> convert(Set<Permission> permissions) {
      return permissions.stream()
                        .map(JsonPermission::convert)
                        .collect(Collectors.toSet());
   }
}
