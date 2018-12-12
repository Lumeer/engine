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
package io.lumeer.api.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum Role {

   MANAGE,
   WRITE,
   SHARE,
   READ,
   CLONE;

   @Override
   public String toString() {
      return name().toLowerCase();
   }

   public static Role fromString(String role) {
      return Role.valueOf(role.toUpperCase());
   }

   public static Set<String> toStringRoles(Set<Role> roles) {
      return roles.stream()
                  .map(Role::toString)
                  .collect(Collectors.toSet());
   }

   public static Set<Role> withTransitionRoles(Set<Role> roles) {
      return roles.stream()
                  .map(Role::withTransitionRoles)
                  .flatMap(Collection::stream)
                  .collect(Collectors.toSet());
   }

   public static Set<Role> withTransitionRoles(Role role) {
      if (role == Role.MANAGE) {
         return new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE, Role.SHARE, Role.CLONE));
      }
      return Collections.singleton(role);
   }

}
