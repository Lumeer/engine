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
package io.lumeer.storage.mongodb.model.common;

import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Resource;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;

public abstract class MorphiaResource extends MorphiaEntity implements Resource {

   public static final String CODE = "code";
   public static final String NAME = "name";
   public static final String ICON = "icon";
   public static final String COLOR = "color";
   public static final String PERMISSIONS = "permissions";

   @Property(CODE)
   protected String code;

   @Property(NAME)
   protected String name;

   @Property(ICON)
   protected String icon;

   @Property(COLOR)
   protected String color;

   @Embedded(PERMISSIONS)
   protected MorphiaPermissions permissions;

   protected MorphiaResource() {
   }

   protected MorphiaResource(final Resource resource) {
      super(resource.getId());

      this.code = resource.getCode();
      this.name = resource.getName();
      this.icon = resource.getIcon();
      this.color = resource.getColor();
      this.permissions = new MorphiaPermissions(resource.getPermissions());
   }

   public String getCode() {
      return code;
   }

   public void setCode(final String code) {
      this.code = code;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getIcon() {
      return icon;
   }

   public void setIcon(final String icon) {
      this.icon = icon;
   }

   public String getColor() {
      return color;
   }

   public void setColor(final String color) {
      this.color = color;
   }

   public Permissions getPermissions() {
      return permissions;
   }

   public void setPermissions(final Permissions permissions) {
      this.permissions = new MorphiaPermissions(permissions);
   }
}
