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

import javax.annotation.Nullable;

public enum RoleType {

   // read resource
   Read,

   // change name, description, icon, color, folders in view
   Manage,

   // read child resources data (i.e. collection -> documents)
   DataRead,

   // edit child resources data (i.e. collection -> documents)
   DataWrite,

   // delete child resources (i.e. collection -> documents)
   DataDelete,

   // create and remove child resources (i.e. collection -> documents)
   DataContribute,

   // create new views and delete them
   ViewContribute,

   // create new collections and delete them
   CollectionContribute,

   // create new links and delete them
   LinkContribute,

   // create new projects and delete them
   ProjectContribute,

   // create new comments and delete them
   CommentContribute,

   // create, edit and delete attribute type, name, description, default attribute
   AttributeEdit,

   // set user and group permissions
   UserConfig,

   // sequences, properties, workflow, automation
   TechConfig,

   // edit perspective config in view
   PerspectiveConfig,

   // edit query in view
   QueryConfig;

   public static @Nullable
   RoleType fromString(String role) {
      if (role == null) {
         return null;
      }
      try {
         return RoleType.valueOf(role);
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }

}
