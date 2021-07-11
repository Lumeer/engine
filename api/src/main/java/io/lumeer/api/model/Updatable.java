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

import io.lumeer.api.model.common.Resource;

import java.util.Set;

public interface Updatable<T> {

   void patch(T resource, final Set<RoleType> roles);

   default void patchResource(final Resource updatingResource, final Resource resource, final Set<RoleType> roles) {
      if (roles.contains(RoleType.Manage)) {
         updatingResource.setIcon(resource.getIcon());
         updatingResource.setColor(resource.getColor());
         updatingResource.setCode(resource.getCode());
         updatingResource.setName(resource.getName());
         updatingResource.setDescription(resource.getDescription());
         updatingResource.setPriority(resource.getPriority());
      }
      // if (roles.contains(RoleType.UserConfig)) {
      //   updatingResource.setPermissions(resource.getPermissions());
      // }
   }
}
