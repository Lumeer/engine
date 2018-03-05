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
import io.lumeer.api.model.Query;
import io.lumeer.api.model.View;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class JsonView extends JsonResource implements View {

   public static final String QUERY = "query";
   public static final String PERSPECTIVE = "perspective";
   public static final String CONFIG = "config";

   private JsonQuery query;
   private String perspective;
   private Object config;

   public JsonView() {
   }

   @JsonCreator
   public JsonView(@JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PERMISSIONS) final JsonPermissions permissions,
         @JsonProperty(QUERY) final JsonQuery query,
         @JsonProperty(PERSPECTIVE) final String perspective,
         @JsonProperty(CONFIG) final Object config) {
      super(code, name, icon, color, description, permissions);

      this.query = query;
      this.perspective = perspective;
      this.config = config;
   }

   public JsonView(View view) {
      super(view);

      this.query = new JsonQuery(view.getQuery());
      this.perspective = view.getPerspective();
      this.config = view.getConfig();
   }

   @Override
   public Query getQuery() {
      return query;
   }

   @Override
   public String getPerspective() {
      return perspective;
   }

   @Override
   public Object getConfig() {
      return config;
   }

   public void setQuery(final JsonQuery query) {
      this.query = query;
   }

   public void setPerspective(final String perspective) {
      this.perspective = perspective;
   }

   public void setConfig(final Object config) {
      this.config = config;
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
