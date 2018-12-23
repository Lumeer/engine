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
package io.lumeer.core.util;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.common.Resource;

import java.util.Set;
import java.util.stream.Collectors;

public class ResourceUtils {

   private ResourceUtils() {
   }

   public static Set<String> getManagers(Resource resource) {
      return resource.getPermissions().getUserPermissions()
                     .stream()
                     .filter(permission -> permission.getRoles().contains(Role.MANAGE))
                     .map(Permission::getId)
                     .collect(Collectors.toSet());
   }

   public static Set<String> usersAllowedRead(Resource resource) {
      return resource.getPermissions().getUserPermissions().stream()
                     .filter(ResourceUtils::canReadByPermission)
                     .map(Permission::getId)
                     .collect(Collectors.toSet());
   }

   public static boolean canReadByPermission(Permission permission) {
      return permission.getRoles().contains(Role.READ) || permission.getRoles().contains(Role.MANAGE);
   }
}
