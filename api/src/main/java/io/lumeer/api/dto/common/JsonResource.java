/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
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
