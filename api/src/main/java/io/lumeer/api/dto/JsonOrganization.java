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

import io.lumeer.api.model.Organization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonOrganization implements Organization {

   private final String code;
   private final String name;
   private final String icon;
   private final String color;
   private final JsonPermissions permissions;

   @JsonCreator
   public JsonOrganization(
         @JsonProperty("code") final String code,
         @JsonProperty("name") final String name,
         @JsonProperty("icon") final String icon,
         @JsonProperty("color") final String color) {
      this.code = code;
      this.name = name;
      this.icon = icon;
      this.color = color;
      this.permissions = new JsonPermissions();
   }

   public JsonOrganization(Organization organization) {
      this.code = organization.getCode();
      this.name = organization.getName();
      this.icon = organization.getIcon();
      this.color = organization.getColor();
      this.permissions = new JsonPermissions(organization.getPermissions());
   }

   @Override
   public String getId() {
      return null;
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
   public JsonPermissions getPermissions() {
      return permissions;
   }

   @Override
   public String toString() {
      return "JsonOrganization{" +
            "code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            '}';
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Organization)) {
         return false;
      }

      final Organization that = (Organization) o;

      return getCode().equals(that.getCode());
   }

   @Override
   public int hashCode() {
      return getCode().hashCode();
   }
}
