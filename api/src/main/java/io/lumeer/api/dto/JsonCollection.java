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
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JsonCollection extends JsonResource implements Collection {

   private List<JsonAttribute> attributes;
   private Integer documentsCount;
   private LocalDateTime lastTimeUsed;

   @JsonCreator
   public JsonCollection(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(PERMISSIONS) final JsonPermissions permissions) {
      super(code, name, icon, color, permissions);

      this.attributes = Collections.emptyList();
   }

   public JsonCollection(Collection collection) {
      super(collection);

      this.attributes = JsonAttribute.convert(collection.getAttributes());
      this.documentsCount = collection.getDocumentsCount();
      this.lastTimeUsed = collection.getLastTimeUsed();
   }

   @Override
   public List<Attribute> getAttributes() {
      return Collections.unmodifiableList(attributes);
   }

   @Override
   public void setAttributes(final List<Attribute> attributes) {
      this.attributes = JsonAttribute.convert(attributes);
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
