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

import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.View;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonView implements View {

   private final String code;
   private final String name;
   private final String icon;
   private final String color;
   private final JsonPermissions permissions;
   private final JsonQuery query;
   private final String perspective;

   @JsonCreator
   public JsonView(@JsonProperty("code") final String code,
         @JsonProperty("name") final String name,
         @JsonProperty("icon") final String icon,
         @JsonProperty("color") final String color,
         @JsonProperty("query") final JsonQuery query,
         @JsonProperty("perspective") final String perspective) {
      this.code = code;
      this.name = name;
      this.icon = icon;
      this.color = color;
      this.permissions = new JsonPermissions();
      this.query = query;
      this.perspective = perspective;
   }

   public JsonView(View view) {
      this.code = view.getCode();
      this.name = view.getName();
      this.icon = view.getIcon();
      this.color = view.getColor();
      this.permissions = new JsonPermissions(view.getPermissions());
      this.query = new JsonQuery(view.getQuery());
      this.perspective = view.getPerspective().toString();
   }

   @Override
   public String getId() {
      return null;
   }

   @Override
   public String getCode() {
      return code;
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
   public Query getQuery() {
      return query;
   }

   @Override
   public Perspective getPerspective() {
      return Perspective.fromString(perspective);
   }

   @Override
   public String toString() {
      return "JsonView{" +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            ", query=" + query +
            ", perspective='" + perspective + '\'' +
            '}';
   }
}
