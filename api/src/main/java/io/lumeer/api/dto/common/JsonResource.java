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
package io.lumeer.api.dto.common;

import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Resource;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class JsonResource implements Resource {

   public static final String CODE = "code";
   public static final String NAME = "name";
   public static final String ICON = "icon";
   public static final String COLOR = "color";
   public static final String PERMISSIONS = "permissions";

   @JsonIgnore
   protected String id;

   protected String code;
   protected String name;
   protected String icon;
   protected String color;

   protected JsonPermissions permissions;

   protected JsonResource() {
   }

   protected JsonResource(final String code, final String name, final String icon, final String color, final JsonPermissions permissions) {
      this.code = code;
      this.name = name;
      this.icon = icon;
      this.color = color;
      this.permissions = permissions != null ? permissions : new JsonPermissions();
   }

   protected JsonResource(final Resource resource) {
      this.id = resource.getId();
      this.code = resource.getCode();
      this.name = resource.getName();
      this.icon = resource.getIcon();
      this.color = resource.getColor();
      this.permissions = new JsonPermissions(resource.getPermissions());
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public void setId(final String id) {
      this.id = id;
   }

   @Override
   public String getCode() {
      return code;
   }

   @Override
   public void setCode(final String code) {
      this.code = code;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public String getIcon() {
      return icon;
   }

   @Override
   public String getColor() {
      return color;
   }

   @Override
   public Permissions getPermissions() {
      return permissions;
   }

   @Override
   public void setPermissions(final Permissions permissions) {
      this.permissions = JsonPermissions.convert(permissions);
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Resource)) {
         return false;
      }

      final Resource that = (Resource) o;

      return getCode() != null ? getCode().equals(that.getCode()) : that.getCode() == null;
   }

   @Override
   public int hashCode() {
      return getCode() != null ? getCode().hashCode() : 0;
   }
}
