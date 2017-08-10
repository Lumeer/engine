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
package io.lumeer.storage.mongodb.model;

import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.View;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;
import io.lumeer.storage.mongodb.model.embedded.MongoQuery;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Property;

@Entity
public class MongoView extends MorphiaEntity implements View {

   public static final String CODE = "code";
   public static final String NAME = "name";
   public static final String ICON = "icon";
   public static final String COLOR = "color";
   public static final String PERMISSIONS = "permissions";
   public static final String QUERY = "query";
   public static final String PERSPECTIVE = "perspective";

   @Property(CODE)
   @Indexed(options = @IndexOptions(unique = true))
   private String code;

   @Property(NAME)
   private String name;

   @Property(ICON)
   private String icon;

   @Property(COLOR)
   private String color;

   @Embedded(PERMISSIONS)
   private MongoPermissions permissions;

   @Embedded(QUERY)
   private MongoQuery query;

   @Property(PERSPECTIVE)
   private String perspective;

   public MongoView() {
   }

   public MongoView(final View view) {
      super(view.getId());

      this.code = view.getCode();
      this.name = view.getName();
      this.icon = view.getIcon();
      this.color = view.getColor();
      this.permissions = new MongoPermissions(view.getPermissions());
      this.query = new MongoQuery(view.getQuery());
      this.perspective = view.getPerspective().toString();
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

   public void setCode(final String code) {
      this.code = code;
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

   public void setPermissions(final MongoPermissions permissions) {
      this.permissions = permissions;
   }

   public void setQuery(final MongoQuery query) {
      this.query = query;
   }

   public void setPerspective(final String perspective) {
      this.perspective = perspective;
   }

   @Override
   public String toString() {
      return "MongoView{" +
            "id='" + getId() + '\'' +
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
