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

import io.lumeer.api.dto.JsonAttribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class LinkType {

   public static final String ID = "_id";
   public static final String NAME = "name";
   public static final String COLLECTION_IDS = "collectionIds";
   public static final String ATTRIBUTES = "attributes";

   private String id;
   private String name;
   private List<String> collectionIds;
   private List<JsonAttribute> attributes;

   @JsonCreator
   public LinkType(@JsonProperty(NAME) final String name,
         @JsonProperty(COLLECTION_IDS) final List<String> collectionIds,
         @JsonProperty(ATTRIBUTES) final List<JsonAttribute> attributes) {
      this.name = name;
      this.collectionIds = collectionIds;
      this.attributes = attributes;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<String> getCollectionIds() {
      return Collections.unmodifiableList(collectionIds);
   }

   public void setCollectionIds(List<String> collectionIds) {
      this.collectionIds = collectionIds;
   }

   public List<JsonAttribute> getAttributes() {
      return Collections.unmodifiableList(attributes);
   }

   public void setAttributes(List<JsonAttribute> attributes) {
      this.attributes = attributes;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof LinkType)) {
         return false;
      }

      final LinkType linkType = (LinkType) o;

      return id != null ? id.equals(linkType.id) : linkType.id == null;
   }

   @Override
   public int hashCode() {
      return id != null ? id.hashCode() : 0;
   }

   @Override
   public String toString() {
      return "LinkType{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", collectionIds=" + collectionIds +
            ", attributes=" + attributes +
            '}';
   }

}
