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
