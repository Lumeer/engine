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
package io.lumeer.storage.mongodb.model.embedded;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Role;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;

import java.util.HashSet;
import java.util.Set;

@Embedded
public class MorphiaPermission implements Permission {

   public static final String NAME = "name";
   public static final String ROLES = "roles";

   @Property(NAME)
   private String name;

   @Property(ROLES)
   private Set<String> roles;

   public MorphiaPermission() {
   }

   public MorphiaPermission(String name, Set<String> roles) {
      this.name = name;
      this.roles = new HashSet<>(roles);
   }

   public MorphiaPermission(final Permission entity) {
      this.name = entity.getName();
      this.roles = Role.toStringRoles(entity.getRoles());
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Set<Role> getRoles() {
      return Role.fromStringRoles(roles);
   }

   public void setName(final String name) {
      this.name = name;
   }

   public void setRoles(final Set<String> roles) {
      this.roles = roles;
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
      return "MongoPermission{" +
            "name='" + name + '\'' +
            ", roles=" + roles +
            '}';
   }
}
