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
package io.lumeer.api.model.common;

import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.ResourceType;

public class SimpleResource extends Resource {

   public SimpleResource(final String code, final String name, final String icon, final String color, String description, Long order, final Permissions permissions) {
      super(code, name, icon, color, description, order, permissions);
   }

   @Override
   public SimpleResource copy() {
      final SimpleResource o = new SimpleResource(this.code, this.name, this.icon, this.color, this.description, this.priority, new Permissions(this.getPermissions()));

      o.id = this.id;
      o.nonRemovable = this.nonRemovable;
      o.version = this.version;
      o.createdBy = this.createdBy;
      o.creationDate = this.creationDate;
      o.updatedBy = this.updatedBy;
      o.updateDate = this.updateDate;

      return o;
   }

   @Override
   public ResourceType getType() {
      return null;
   }

}
