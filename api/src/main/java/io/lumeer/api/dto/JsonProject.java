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
package io.lumeer.api.dto;

import io.lumeer.api.dto.common.JsonResource;
import io.lumeer.api.model.Project;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class JsonProject extends JsonResource implements Project {

   private int collectionsCount;

   public JsonProject() {
   }

   @JsonCreator
   public JsonProject(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PERMISSIONS) final JsonPermissions permissions,
         @JsonProperty(COLLECTIONS_COUNT) final int collectionsCount) {
      super(code, name, icon, color, description, permissions);
      this.collectionsCount = collectionsCount;
   }

   public JsonProject(final String code, final String name, final String icon, final String color,
         final String description, final JsonPermissions permissions) {
      super(code, name, icon, color, description, permissions);
      this.collectionsCount = 0;
   }

   public JsonProject(Project project) {
      super(project);
      this.collectionsCount = project.getCollectionsCount();
   }

   @Override
   public String toString() {
      return "JsonProject{" +
            "code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            ", collectionsCount=" + collectionsCount +
            '}';
   }

   @Override
   public int getCollectionsCount() {
      return collectionsCount;
   }

   public void setCollectionsCount(final int collectionsCount) {
      this.collectionsCount = collectionsCount;
   }

   public static JsonProject convert(Project project) {
      return project instanceof JsonProject ? (JsonProject) project : new JsonProject(project);
   }

   public static List<JsonProject> convert(List<Project> projects) {
      return projects.stream()
                     .map(JsonProject::convert)
                     .collect(Collectors.toList());
   }
}
