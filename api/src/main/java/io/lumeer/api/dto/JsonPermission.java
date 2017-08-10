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
import io.lumeer.api.model.Role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

@Immutable
public class JsonPermission implements Permission {

   private final String name;
   private final Set<String> roles;

   @JsonCreator
   public JsonPermission(@JsonProperty("name") final String name,
         @JsonProperty("roles") final Set<String> roles) {
      this.name = name;
      this.roles = roles;
   }

   public JsonPermission(final Permission permission) {
      this.name = permission.getName();
      this.roles = permission.getRoles().stream()
                             .map(Role::toString)
                             .collect(Collectors.toSet());
   }

   @Override
   public String getName() {
      return name;
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

      return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
   }

   @Override
   public int hashCode() {
      return getName() != null ? getName().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "JsonPermission{" +
            "name='" + name + '\'' +
            ", roles=" + roles +
            '}';
   }
}
