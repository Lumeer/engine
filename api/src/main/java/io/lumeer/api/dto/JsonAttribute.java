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

import io.lumeer.api.model.Attribute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonAttribute implements Attribute {

   public static final String NAME = "name";
   public static final String FULLNAME = "fullName";
   public static final String CONSTRAINTS = "constraints";
   public static final String USAGE_COUNT = "usageCount";

   private String name;
   private String fullName;
   private Set<String> constraints;
   private Integer usageCount;

   public JsonAttribute(Attribute attribute) {
      this.name = attribute.getName();
      this.fullName = attribute.getFullName();
      this.constraints = new HashSet<>(attribute.getConstraints());
      this.usageCount = attribute.getUsageCount();
   }

   @JsonCreator
   public JsonAttribute(
         @JsonProperty(NAME) final String name,
         @JsonProperty(FULLNAME) final String fullName,
         @JsonProperty(CONSTRAINTS) final Set<String> constraints,
         @JsonProperty(USAGE_COUNT) final Integer usageCount) {
      this.name = name;
      this.fullName = fullName;
      this.constraints = constraints;
      this.usageCount = usageCount;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public void setName(final String name) {
      this.name = name;
   }

   @Override
   public String getFullName() {
      return fullName;
   }

   @Override
   public Set<String> getConstraints() {
      return constraints;
   }

   @Override
   public Integer getUsageCount() {
      return usageCount;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Attribute)) {
         return false;
      }

      final Attribute that = (Attribute) o;

      return getFullName() != null ? getFullName().equals(that.getFullName()) : that.getFullName() == null;
   }

   @Override
   public int hashCode() {
      return getFullName() != null ? getFullName().hashCode() : 0;
   }

   @Override
   public String toString() {
      return "JsonAttribute{" +
            "name='" + name + '\'' +
            ", fullName='" + fullName + '\'' +
            ", constraints=" + constraints +
            ", usageCount=" + usageCount +
            '}';
   }

   public static JsonAttribute convert(Attribute attribute) {
      return attribute instanceof JsonAttribute ? (JsonAttribute) attribute : new JsonAttribute(attribute);
   }

   public static List<JsonAttribute> convert(List<Attribute> attributes) {
      return attributes.stream()
                       .map(JsonAttribute::new)
                       .collect(Collectors.toList());
   }
}
