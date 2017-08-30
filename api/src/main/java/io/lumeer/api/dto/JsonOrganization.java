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
import io.lumeer.api.model.Organization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class JsonOrganization extends JsonResource implements Organization {

   public JsonOrganization() {
   }

   @JsonCreator
   public JsonOrganization(
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(PERMISSIONS) final JsonPermissions permissions) {
      super(code, name, icon, color, permissions);
   }

   public JsonOrganization(Organization organization) {
      super(organization);
   }

   @Override
   public String toString() {
      return "JsonOrganization{" +
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            '}';
   }

   public static JsonOrganization convert(Organization organization) {
      return organization instanceof JsonOrganization ? (JsonOrganization) organization : new JsonOrganization(organization);
   }

   public static List<JsonOrganization> convert(List<Organization> organizations) {
      return organizations.stream()
                          .map(JsonOrganization::convert)
                          .collect(Collectors.toList());
   }
}
