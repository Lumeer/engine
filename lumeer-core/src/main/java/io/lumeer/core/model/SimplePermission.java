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
package io.lumeer.core.model;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Role;

import java.util.Collections;
import java.util.Set;

public class SimplePermission implements Permission {

   private final String name;
   private final Set<Role> roles;

   public SimplePermission(final String name, final Set<Role> roles) {
      this.name = name;
      this.roles = roles;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Set<Role> getRoles() {
      return Collections.unmodifiableSet(roles);
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
      return "SimplePermission{" +
            "name='" + name + '\'' +
            ", roles=" + roles +
            '}';
   }
}
