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
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class View extends Resource {

   public static Set<Role> ROLES = new HashSet<>(Arrays.asList(Role.MANAGE, Role.CLONE, Role.READ, Role.SHARE, Role.WRITE));

   public static final String QUERY = "query";
   public static final String PERSPECTIVE = "perspective";
   public static final String CONFIG = "config";
   public static final String AUTHOR_ID = "authorId";

   private Query query;
   private String perspective;
   private Object config;
   private String authorId;
   private Map<String, Set<Role>> authorRights;

   public View() {
   }

   @JsonCreator
   public View(@JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PERMISSIONS) final Permissions permissions,
         @JsonProperty(QUERY) final Query query,
         @JsonProperty(PERSPECTIVE) final String perspective,
         @JsonProperty(CONFIG) final Object config,
         @JsonProperty(AUTHOR_ID) final String authorId) {
      super(code, name, icon, color, description, permissions);

      this.query = query;
      this.perspective = perspective;
      this.config = config;
      this.authorId = authorId;
   }

   @Override
   public View copy() {
      final View o = new View();

      o.id = this.id;
      o.code = this.code;
      o.name = this.name;
      o.icon = this.icon;
      o.color = this.color;
      o.description = this.description;
      o.nonRemovable = this.nonRemovable;
      o.permissions = new Permissions(this.getPermissions());
      o.query = this.query;
      o.perspective = this.perspective;
      o.config = this.config;
      o.authorId = this.authorId;
      o.authorRights = this.authorRights;
      o.version = this.version;

      return o;
   }

   public ResourceType getType() {
      return ResourceType.VIEW;
   }

   public Query getQuery() {
      return query;
   }

   public String getPerspective() {
      return perspective;
   }

   public Object getConfig() {
      return config;
   }

   public String getAuthorId() {
      return authorId;
   }

   public void setQuery(final Query query) {
      this.query = query;
   }

   public void setPerspective(final String perspective) {
      this.perspective = perspective;
   }

   public void setConfig(final Object config) {
      this.config = config;
   }

   public void setAuthorId(final String authorId) {
      this.authorId = authorId;
   }

   public Map<String, Set<Role>> getAuthorRights() {
      return authorRights;
   }

   public void setAuthorRights(final Map<String, Set<Role>> authorRights) {
      this.authorRights = authorRights;
   }

   @Override
   public String toString() {
      return "View{" +
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            ", query=" + query +
            ", perspective='" + perspective + '\'' +
            ", authorId='" + authorId + '\'' +
            ", authorRights='" + authorRights + '\'' +
            '}';
   }

}
