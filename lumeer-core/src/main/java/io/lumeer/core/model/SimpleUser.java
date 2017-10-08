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

import io.lumeer.api.model.User;

import java.util.Collections;
import java.util.Set;

public class SimpleUser implements User {

   private final String id;
   private final String username;
   private final Set<String> groups;

   public SimpleUser(final String username) {
      this.id = null;
      this.username = username;
      this.groups = Collections.emptySet();
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public String getUsername() {
      return username;
   }

   @Override
   public Set<String> getGroups() {
      return groups;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof User)) {
         return false;
      }

      final User that = (User) o;

      return getUsername() != null ? getUsername().equals(that.getUsername()) : that.getUsername() == null;
   }

   @Override
   public int hashCode() {
      return getUsername() != null ? getUsername().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "SimpleUser{" +
            "id='" + id + '\'' +
            ", username='" + username + '\'' +
            ", groups=" + groups +
            '}';
   }
}
