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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Project extends Resource {

   public static Set<Role> ROLES = new HashSet<>(Arrays.asList(Role.MANAGE, Role.WRITE, Role.READ));

   private TemplateMetadata templateMetadata;
   private int collectionsCount;
   private boolean isPublic;

   public Project() {
   }

   @JsonCreator
   public Project(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PRIORITY) final Long order,
         @JsonProperty(PERMISSIONS) final Permissions permissions,
         @JsonProperty("public") final boolean isPublic,
         @JsonProperty("templateMetadata") final TemplateMetadata templateMetadata) {
      super(code, name, icon, color, description, order, permissions);
      this.templateMetadata = templateMetadata;
      this.isPublic = isPublic;
   }

   @Override
   public String toString() {
      return "Project{" +
            "code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            '}';
   }

   @Override
   public Project copy() {
      final Project o = new Project();

      o.id = this.id;
      o.code = this.code;
      o.name = this.name;
      o.icon = this.icon;
      o.color = this.color;
      o.description = this.description;
      o.nonRemovable = this.nonRemovable;
      o.permissions = new Permissions(this.getPermissions());
      o.collectionsCount = this.collectionsCount;
      o.version = this.version;
      o.isPublic = this.isPublic;
      o.priority = this.priority;
      o.templateMetadata = this.templateMetadata != null ? new TemplateMetadata(this.templateMetadata) : null;

      return o;
   }

   public TemplateMetadata getTemplateMetadata() {
      return templateMetadata;
   }

   public void setTemplateMetadata(final TemplateMetadata templateMetadata) {
      this.templateMetadata = templateMetadata;
   }

   public ResourceType getType() {
      return ResourceType.PROJECT;
   }

   public int getCollectionsCount() {
      return collectionsCount;
   }

   public boolean isPublic() {
      return isPublic;
   }

   public void setCollectionsCount(final int collectionsCount) {
      this.collectionsCount = collectionsCount;
   }
}
