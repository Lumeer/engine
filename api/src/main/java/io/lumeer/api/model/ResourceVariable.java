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

import io.lumeer.api.exception.InsaneObjectException;
import io.lumeer.api.model.common.WithId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceVariable implements WithId, HealthChecking {

   private String id;

   private String resourceId;
   private ResourceType resourceType;

   private String key;
   private Object value;
   private ResourceVariableType type;
   private Boolean secure;

   private String organizationId;
   private String projectId;

   @JsonCreator
   public ResourceVariable(@JsonProperty("id") final String id,
         @JsonProperty("resourceId") final String resourceId,
         @JsonProperty("resourceType") final ResourceType resourceType,
         @JsonProperty("key") final String key,
         @JsonProperty("value") final Object value,
         @JsonProperty("type") final ResourceVariableType type,
         @JsonProperty("secure") final Boolean secure,
         @JsonProperty("organizationId") final String organizationId,
         @JsonProperty("projectId") final String projectId) {
      this.id = id;
      this.resourceId = resourceId;
      this.resourceType = resourceType;
      this.key = key;
      this.value = value;
      this.type = type;
      this.secure = secure;
      this.organizationId = organizationId;
      this.projectId = projectId;
   }

   @Override
   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getResourceId() {
      return resourceId;
   }

   public ResourceType getResourceType() {
      return resourceType;
   }

   public String getKey() {
      return key;
   }

   public void setKey(final String key) {
      this.key = key;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(final Object value) {
      this.value = value;
   }

   public ResourceVariableType getType() {
      return type;
   }

   public void setType(final ResourceVariableType type) {
      this.type = type;
   }

   public Boolean getSecure() {
      return secure;
   }

   public String getOrganizationId() {
      if (resourceType == ResourceType.ORGANIZATION) {
         return resourceId;
      }
      return organizationId;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }

   public String getProjectId() {
      if (resourceType == ResourceType.PROJECT) {
         return resourceId;
      }
      return projectId;
   }

   @JsonIgnore
   public String getStringValue() {
      if (type == ResourceVariableType.String) {
         return value != null ? value.toString() : null;
      }
      return null;
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength("key", key, MAX_LONG_STRING_LENGTH);
   }

   public void patch(ResourceVariable variable) {
      setKey(variable.getKey());
      if (!variable.getSecure() || variable.getValue() != null) {
         setValue(variable.getValue());
      }
      setType(variable.getType());
   }
}
