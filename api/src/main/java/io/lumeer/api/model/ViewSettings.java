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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ViewSettings {

   public static final String ATTRIBUTES = "attributes";
   public static final String DATA = "data";
   public static final String MODALS = "modals";
   public static final String PERMISSIONS = "permissions";

   private Object attributes;
   private Object data;
   private Object modals;

   private ResourcesPermissions permissions;

   @JsonCreator
   public ViewSettings(@JsonProperty(ATTRIBUTES) final Object attributes,
         @JsonProperty(DATA) final Object data,
         @JsonProperty(MODALS) final Object modals,
         @JsonProperty(PERMISSIONS) final ResourcesPermissions permissions) {
      this.attributes = attributes;
      this.data = data;
      this.modals = modals;
      this.permissions = permissions;
   }

   public ViewSettings() {
      this.attributes = null;
      this.data = null;
      this.modals = null;
      this.permissions = new ResourcesPermissions();
   }

   public Object getAttributes() {
      return attributes;
   }

   public Object getData() {
      return data;
   }

   public Object getModals() {
      return modals;
   }

   public ResourcesPermissions getPermissions() {
      return permissions;
   }

   @Override
   public String toString() {
      return "ViewSettings{" +
            "attributes=" + attributes +
            ", data=" + data +
            ", modals=" + modals +
            ", resourcesPermissions=" + permissions +
            '}';
   }
}
