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
package io.lumeer.core.task.executor.operation;

import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.View;

import java.util.Set;

public class ViewPermissionsOperation extends ResourceOperation<View> {

   public View originalView;

   public ViewPermissionsOperation(final View entity, final String userId, final Set<RoleType> value) {
      super(entity, userId, value);
      originalView = entity.copy();
   }

   public String getUserId() {
      return getAttrId();
   }

   @SuppressWarnings("unchecked")
   public Set<RoleType> getRoles() {
      return (Set<RoleType>) getValue();
   }

   public View getOriginalView() {
      return originalView;
   }
}
