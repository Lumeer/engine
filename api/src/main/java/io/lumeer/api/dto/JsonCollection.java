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

import static io.lumeer.engine.api.LumeerConst.Collection.ATTRIBUTES;

import io.lumeer.api.dto.common.JsonResource;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonCollection extends JsonResource implements Collection {

   private Set<JsonAttribute> attributes;
   private Integer documentsCount;
   private LocalDateTime lastTimeUsed;

   public JsonCollection(final String code, final String name, final String icon, final String color, final JsonPermissions permissions) {
      this(code, name, icon, color, permissions, new LinkedHashSet<>());
   }

   @JsonCreator
   public JsonCollection(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(PERMISSIONS) final JsonPermissions permissions,
         @JsonProperty(ATTRIBUTES) final Set<JsonAttribute> attributes) {
      super(code, name, icon, color, permissions);

      this.attributes = attributes != null ? attributes : new LinkedHashSet<>();
      this.documentsCount = 0;
   }

   public JsonCollection(Collection collection) {
      super(collection);

      this.attributes = JsonAttribute.convert(collection.getAttributes());
      this.documentsCount = collection.getDocumentsCount();
      this.lastTimeUsed = collection.getLastTimeUsed();
   }

   @Override
   public Set<Attribute> getAttributes() {
      return Collections.unmodifiableSet(attributes);
   }

   @Override
   public void setAttributes(final Set<Attribute> attributes) {
      this.attributes = JsonAttribute.convert(attributes);
   }

   @Override
   public void updateAttribute(final String attributeFullName, final Attribute attribute) {
      attributes.removeIf(a -> a.getFullName().equals(attributeFullName));
      attributes.add(JsonAttribute.convert(attribute));
   }

   @Override
   public void deleteAttribute(final String attributeFullName) {
      attributes.removeIf(a -> a.getFullName().startsWith(attributeFullName));
   }

   @Override
   public Integer getDocumentsCount() {
      return documentsCount;
   }

   @Override
   public void setDocumentsCount(final Integer documentsCount) {
      this.documentsCount = documentsCount;
   }

   @Override
   public LocalDateTime getLastTimeUsed() {
      return lastTimeUsed;
   }

   @Override
   public void setLastTimeUsed(final LocalDateTime lastTimeUsed) {
      this.lastTimeUsed = lastTimeUsed;
   }

   @Override
   public String toString() {
      return "JsonCollection{" +
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            ", attributes=" + attributes +
            ", documentsCount=" + documentsCount +
            ", lastTimeUsed=" + lastTimeUsed +
            '}';
   }

   public static JsonCollection convert(Collection collection) {
      return collection instanceof JsonCollection ? (JsonCollection) collection : new JsonCollection(collection);
   }

   public static List<JsonCollection> convert(List<Collection> collections) {
      return collections.stream()
                        .map(JsonCollection::convert)
                        .collect(Collectors.toList());
   }
}
