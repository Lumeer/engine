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
