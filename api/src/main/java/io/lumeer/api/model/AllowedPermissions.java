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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AllowedPermissions {

   private final Map<RoleType, Boolean> roles;
   private final Map<RoleType, Boolean> rolesWithView;

   public AllowedPermissions(final Map<RoleType, Boolean> roles, final Map<RoleType, Boolean> rolesWithView) {
      this.roles = roles;
      this.rolesWithView = rolesWithView;
   }

   public AllowedPermissions(final Map<RoleType, Boolean> roles) {
      this.roles = roles;
      this.rolesWithView = roles;
   }

   public AllowedPermissions(final Set<RoleType> roles) {
      this.roles = roles.stream().collect(Collectors.toMap(role -> role, role -> true));
      this.rolesWithView = this.roles;
   }

   public AllowedPermissions(final Set<RoleType> roles, final Set<RoleType> rolesWithView) {
      this.roles = roles.stream().collect(Collectors.toMap(role -> role, role -> true));
      this.rolesWithView = rolesWithView.stream().collect(Collectors.toMap(role -> role, role -> true));
   }

   public Map<RoleType, Boolean> getRoles() {
      return roles != null ? roles : Collections.emptyMap();
   }

   public Map<RoleType, Boolean> getRolesWithView() {
      return rolesWithView != null ? rolesWithView : Collections.emptyMap();
   }

   public static AllowedPermissions allAllowed() {
      Set<RoleType> roles = Arrays.stream(RoleType.values()).collect(Collectors.toSet());
      return new AllowedPermissions(roles);
   }

   public static AllowedPermissions merge(AllowedPermissions a1, AllowedPermissions a2) {
      if (a1 == null || a2 == null) {
         return a1 != null ? a1 : a2;
      }

      Set<RoleType> roles = a1.getRoles().entrySet().stream()
                              .filter(entry -> entry.getValue() && a2.getRoles().get(entry.getKey()))
                              .map(Map.Entry::getKey)
                              .collect(Collectors.toSet());
      Set<RoleType> rolesWithView = a1.getRolesWithView().entrySet().stream()
                                      .filter(entry -> entry.getValue() && a2.getRolesWithView().get(entry.getKey()))
                                      .map(Map.Entry::getKey)
                                      .collect(Collectors.toSet());
      return new AllowedPermissions(roles, rolesWithView);
   }
}
