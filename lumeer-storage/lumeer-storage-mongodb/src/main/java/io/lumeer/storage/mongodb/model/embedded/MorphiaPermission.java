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

   public static final String ID = "id";
   public static final String ROLES = "roles";

   @Property(ID)
   private String id;

   @Property(ROLES)
   private Set<String> roles;

   public MorphiaPermission() {
   }

   public MorphiaPermission(String id, Set<String> roles) {
      this.id = id;
      this.roles = new HashSet<>(roles);
   }

   public MorphiaPermission(final Permission entity) {
      this.id = entity.getId();
      this.roles = Role.toStringRoles(entity.getRoles());
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public Set<Role> getRoles() {
      return Role.fromStringRoles(roles);
   }

   public void setId(final String id) {
      this.id = id;
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

      return getId() != null ? getId().equals(that.getId()) : that.getId() == null;
   }

   @Override
   public int hashCode() {
      return getId() != null ? getId().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "MongoPermission{" +
            "id='" + id + '\'' +
            ", roles=" + roles +
            '}';
   }
}
