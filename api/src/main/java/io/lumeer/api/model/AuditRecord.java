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

import io.lumeer.api.adapter.ZonedDateTimeAdapter;
import io.lumeer.api.model.common.WithId;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditRecord implements WithId {

   public static final String PARENT_ID = "parentId";
   public static final String RESOURCE_TYPE = "resourceType";
   public static final String RESOURCE_ID = "resourceId";
   public static final String CHANGE_DATE = "changeDate";
   public static final String USER = "user";
   public static final String OLD_STATE = "oldState";
   public static final String NEW_STATE = "newState";

   private String id;

   private String parentId;

   private ResourceType resourceType;
   private String resourceId;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime changeDate;

   private String user;

   private DataDocument oldState;
   private DataDocument newState;

   public AuditRecord() {}

   @JsonCreator
   public AuditRecord(@JsonProperty(PARENT_ID) final String parentId, @JsonProperty(RESOURCE_TYPE) final ResourceType resourceType,
         @JsonProperty(RESOURCE_ID) final String resourceId, @JsonProperty(CHANGE_DATE) final ZonedDateTime changeDate,
         @JsonProperty(USER) final String user, @JsonProperty(OLD_STATE) final DataDocument oldState,
         @JsonProperty(NEW_STATE) final DataDocument newState) {
      this.parentId = parentId;
      this.resourceType = resourceType;
      this.resourceId = resourceId;
      this.changeDate = changeDate;
      this.user = user;
      this.oldState = oldState;
      this.newState = newState;
   }

   @Override
   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getParentId() {
      return parentId;
   }

   public void setParentId(final String parentId) {
      this.parentId = parentId;
   }

   public ResourceType getResourceType() {
      return resourceType;
   }

   public void setResourceType(final ResourceType resourceType) {
      this.resourceType = resourceType;
   }

   public String getResourceId() {
      return resourceId;
   }

   public void setResourceId(final String resourceId) {
      this.resourceId = resourceId;
   }

   public ZonedDateTime getChangeDate() {
      return changeDate;
   }

   public void setChangeDate(final ZonedDateTime changeDate) {
      this.changeDate = changeDate;
   }

   public String getUser() {
      return user;
   }

   public void setUser(final String user) {
      this.user = user;
   }

   public DataDocument getOldState() {
      return oldState;
   }

   public void setOldState(final DataDocument oldState) {
      this.oldState = oldState;
   }

   public DataDocument getNewState() {
      return newState;
   }

   public void setNewState(final DataDocument newState) {
      this.newState = newState;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final AuditRecord that = (AuditRecord) o;
      return Objects.equals(id, that.id);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return "AuditRecord{" +
            "id='" + id + '\'' +
            ", parentId='" + parentId + '\'' +
            ", resourceType=" + resourceType +
            ", resourceId='" + resourceId + '\'' +
            ", changeDate=" + changeDate +
            ", user='" + user + '\'' +
            ", oldState=" + oldState +
            ", newState=" + newState +
            '}';
   }
}
