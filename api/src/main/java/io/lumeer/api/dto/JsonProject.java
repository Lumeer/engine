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
package io.lumeer.api.dto;

import io.lumeer.api.dto.common.JsonResource;
import io.lumeer.api.model.Project;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class JsonProject extends JsonResource implements Project {

   public JsonProject() {
   }

   @JsonCreator
   public JsonProject(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(PERMISSIONS) final JsonPermissions permissions) {
      super(code, name, icon, color, permissions);
   }

   public JsonProject(Project project) {
      super(project);
   }

   @Override
   public String toString() {
      return "JsonProject{" +
            "code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            '}';
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
