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

      this.attributes = attributes;
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
      deleteAttribute(attributeFullName);
      attributes.add(JsonAttribute.convert(attribute));
   }

   @Override
   public void deleteAttribute(final String attributeFullName) {
      attributes.removeIf(a -> a.getFullName().equals(attributeFullName));
   }

   @Override
   public Integer getDocumentsCount() {
      return documentsCount;
   }

   @Override
   public LocalDateTime getLastTimeUsed() {
      return lastTimeUsed;
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
