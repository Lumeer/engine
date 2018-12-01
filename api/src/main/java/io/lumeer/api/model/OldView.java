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
package io.lumeer.api.model;

import io.lumeer.api.model.common.Resource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OldView extends Resource {

   public static Set<Role> ROLES = new HashSet<>(Arrays.asList(Role.MANAGE, Role.CLONE, Role.READ, Role.SHARE, Role.WRITE));

   private OldQuery query;
   private String perspective;
   private Object config;
   private String authorId;
   private Map<String, Set<Role>> authorRights;

   public OldView() {
   }

   public OldView(final String code,
         final String name,
         final String icon,
         final String color,
         final String description,
         final Permissions permissions,
         final OldQuery query,
         final String perspective,
         final Object config,
         final String authorId) {
      super(code, name, icon, color, description, permissions);

      this.query = query;
      this.perspective = perspective;
      this.config = config;
      this.authorId = authorId;
   }

   @Override
   public OldView copy() {
      final OldView o = new OldView();

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

      return o;
   }

   public ResourceType getType() {
      return ResourceType.VIEW;
   }

   public OldQuery getQuery() {
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

   public void setQuery(final OldQuery query) {
      this.query = query;
   }

   public void setConfig(final Object config) {
      this.config = config;
   }

   public Map<String, Set<Role>> getAuthorRights() {
      return authorRights;
   }

}
