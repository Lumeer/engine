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

import io.lumeer.api.exception.InsaneObjectException;
import io.lumeer.api.model.HealthChecking;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.ResourceType;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Resource implements WithId, HealthChecking {

   public static final String CODE = "code";
   public static final String NAME = "name";
   public static final String ICON = "icon";
   public static final String COLOR = "color";
   public static final String PRIORITY = "priority";
   public static final String DESCRIPTION = "description";
   public static final String PERMISSIONS = "permissions";

   protected String id;

   protected String code;
   protected String name;
   protected String icon;
   protected String color;
   protected Long priority;
   protected long version;
   protected String description;
   protected boolean nonRemovable;

   protected Permissions permissions;

   protected Resource() {
   }

   public Resource(final String code, final String name, final String icon, final String color, String description, Long priority, final Permissions permissions) {
      this.code = code;
      this.name = name;
      this.icon = icon;
      this.color = color;
      this.description = description;
      this.priority = priority;
      this.permissions = permissions != null ? permissions : new Permissions();
   }

   protected Resource(final Resource resource) {
      this.id = resource.getId();
      this.code = resource.getCode();
      this.name = resource.getName();
      this.icon = resource.getIcon();
      this.color = resource.getColor();
      this.version = resource.getVersion();
      this.description = resource.getDescription();
      this.nonRemovable = resource.isNonRemovable();
      this.priority = resource.getPriority();
      this.permissions = new Permissions(resource.getPermissions());
      this.version = resource.getVersion();
   }

   public abstract <T extends Resource> T copy();

   @JsonIgnore
   public abstract ResourceType getType();

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
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

   public String getIcon() {
      return icon;
   }

   public String getDescription() {
      return description;
   }

   public String getColor() {
      return color;
   }

   public boolean isNonRemovable() {
      return nonRemovable;
   }

   public void setNonRemovable(final boolean nonRemovable) {
      this.nonRemovable = nonRemovable;
   }

   public Permissions getPermissions() {
      return permissions;
   }

   public void setPermissions(final Permissions permissions) {
      this.permissions = new Permissions(permissions);
   }

   public void setName(final String name) {
      this.name = name;
   }

   public void setIcon(final String icon) {
      this.icon = icon;
   }

   public void setColor(final String color) {
      this.color = color;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public long getVersion() {
      return version;
   }

   public void setVersion(final long version) {
      this.version = version;
   }

   public Long getPriority() {
      return priority;
   }

   public void setPriority(final Long priority) {
      this.priority = priority;
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

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength("icon", code, MAX_STRING_LENGTH);
      checkStringLength("color", code, MAX_STRING_LENGTH);
      checkStringLength("code", code, MAX_STRING_LENGTH);
      checkStringLength("name", name, MAX_STRING_LENGTH);
      checkStringLength("description", description, MAX_LONG_STRING_LENGTH);
   }
}
