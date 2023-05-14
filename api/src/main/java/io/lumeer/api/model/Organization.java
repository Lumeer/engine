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

import io.lumeer.api.exception.InsaneObjectException;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.RoleUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Organization extends Resource implements Updatable<Organization> {

   public static Set<Role> ROLES = RoleUtils.organizationResourceRoles();

   public Organization() {
   }

   @JsonCreator
   public Organization(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PRIORITY) final Long order,
         @JsonProperty(PERMISSIONS) final Permissions permissions) {
      super(code, name, icon, color, description, order, permissions);
   }

   public Organization(final Resource resource) {
      super(resource);
   }

   @Override
   public Organization copy() {
      final Organization o = new Organization();

      o.id = this.id;
      o.code = this.code;
      o.name = this.name;
      o.icon = this.icon;
      o.color = this.color;
      o.description = this.description;
      o.nonRemovable = this.nonRemovable;
      o.permissions = new Permissions(this.getPermissions());
      o.priority = this.priority;
      o.version = this.version;

      return o;
   }

   @Override
   public ResourceType getType() {
      return ResourceType.ORGANIZATION;
   }

   @Override
   public String toString() {
      return "Organization{" +
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", order='" + priority + '\'' +
            ", permissions=" + permissions +
            '}';
   }

   @Override
   public void patch(final Organization resource, final Set<RoleType> roles) {
      patchResource(this, resource, roles);
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      super.checkHealth();

      checkStringLength("code", code, 6);
   }
}
