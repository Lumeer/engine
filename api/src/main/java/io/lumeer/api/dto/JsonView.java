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
import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.View;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class JsonView extends JsonResource implements View {

   public static final String QUERY = "query";
   public static final String PERSPECTIVE = "perspective";

   private JsonQuery query;
   private String perspective;

   @JsonCreator
   public JsonView(@JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(PERMISSIONS) final JsonPermissions permissions,
         @JsonProperty(QUERY) final JsonQuery query,
         @JsonProperty(PERSPECTIVE) final String perspective) {
      super(code, name, icon, color, permissions);

      this.query = query;
      this.perspective = perspective;
   }

   public JsonView(View view) {
      super(view);

      this.query = new JsonQuery(view.getQuery());
      this.perspective = view.getPerspective().toString();
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
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            ", query=" + query +
            ", perspective='" + perspective + '\'' +
            '}';
   }

   public static JsonView convert(View view) {
      return view instanceof JsonView ? (JsonView) view : new JsonView(view);
   }

   public static List<JsonView> convert(List<View> views) {
      return views.stream()
                  .map(JsonView::convert)
                  .collect(Collectors.toList());
   }
}
