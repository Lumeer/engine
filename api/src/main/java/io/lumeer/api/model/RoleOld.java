/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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
package io.lumeer.api.model;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@Deprecated
public enum RoleOld {

   MANAGE,
   WRITE,
   SHARE,
   READ,
   CLONE;

   @Override
   public String toString() {
      return name().toLowerCase();
   }

   public static @Nullable
   RoleOld fromString(String role) {
      try {
         return RoleOld.valueOf(role.toUpperCase());
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }

   public static Set<String> toStringRoles(Set<RoleOld> roles) {
      return roles.stream()
                  .map(RoleOld::toString)
                  .collect(Collectors.toSet());
   }

}
