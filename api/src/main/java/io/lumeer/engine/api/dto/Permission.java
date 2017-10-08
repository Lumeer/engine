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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Permission {

   private final String name;
   private final Set<String> roles;

   public Permission(DataDocument document) {
      this.name = document.getString(LumeerConst.Security.USERGROUP_NAME_KEY);
      this.roles = new HashSet<>(document.getArrayList(LumeerConst.Security.USERGROUP_ROLES_KEY, String.class));
   }

   @JsonCreator
   public Permission(final @JsonProperty("name") String name,
         final @JsonProperty("users") Set<String> roles) {
      this.name = name;
      this.roles = roles;
   }

   public String getName() {
      return name;
   }

   public Set<String> getRoles() {
      return roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final Permission that = (Permission) o;

      return name.equals(that.name);
   }

}
