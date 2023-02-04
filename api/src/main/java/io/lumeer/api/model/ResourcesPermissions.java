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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public class ResourcesPermissions {

   public static final String COLLECTIONS = "collections";
   public static final String LINK_TYPES = "linkTypes";

   private Map<String, Permissions> collections;

   private Map<String, Permissions> linkTypes;

   @JsonCreator
   public ResourcesPermissions(@JsonProperty(COLLECTIONS) final Map<String, Permissions> collections,
         @JsonProperty(LINK_TYPES) final Map<String, Permissions> linkTypes) {
      this.collections = collections;
      this.linkTypes = linkTypes;
   }

   public ResourcesPermissions() {
      this.collections = Collections.emptyMap();
      this.linkTypes = Collections.emptyMap();
   }

   public Map<String, Permissions> getCollections() {
      return collections;
   }

   public Map<String, Permissions> getLinkTypes() {
      return linkTypes;
   }

   @Override
   public String toString() {
      return "ResourcesPermissions{" +
            "collections=" + collections +
            ", linkTypes=" + linkTypes +
            '}';
   }
}
