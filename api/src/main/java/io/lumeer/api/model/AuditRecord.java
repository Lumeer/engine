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
import io.lumeer.api.adapter.ZonedDateTimeDeserializer;
import io.lumeer.api.model.common.WithId;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditRecord implements WithId {

   public static final String PARENT_ID = "parentId";
   public static final String RESOURCE_TYPE = "resourceType";
   public static final String RESOURCE_ID = "resourceId";
   public static final String CHANGE_DATE = "changeDate";
   public static final String USER = "user";
   public static final String VIEW = "view";
   public static final String USER_NAME = "userName";
   public static final String USER_EMAIL = "userEmail";
   public static final String OLD_STATE = "oldState";
   public static final String NEW_STATE = "newState";
   public static final String AUTOMATION = "automation";
   public static final String TYPE = "type";

   private String id;

   private String parentId;

   private ResourceType resourceType;
   private String resourceId;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
   private ZonedDateTime changeDate;

   private String user;
   private String viewId;
   private String userName;
   private String userEmail;
   private String automation;
   private Object title;

   private DataDocument oldState;
   private DataDocument newState;

   private AuditType type;

   public AuditRecord() {
   }

   @JsonCreator
   public AuditRecord(@JsonProperty(PARENT_ID) final String parentId, @JsonProperty(RESOURCE_TYPE) final ResourceType resourceType,
         @JsonProperty(RESOURCE_ID) final String resourceId, @JsonProperty(CHANGE_DATE) final ZonedDateTime changeDate,
         @JsonProperty(USER) final String user, @JsonProperty(USER_NAME) final String userName, @JsonProperty(USER_EMAIL) final String userEmail,
         @JsonProperty(VIEW) final String viewId, @JsonProperty(AUTOMATION) final String automation, @JsonProperty(OLD_STATE) final DataDocument oldState,
         @JsonProperty(NEW_STATE) final DataDocument newState) {
      this.parentId = parentId;
      this.resourceType = resourceType;
      this.resourceId = resourceId;
      this.changeDate = changeDate;
      this.user = user;
      this.userName = userName;
      this.userEmail = userEmail;
      this.viewId = viewId;
      this.automation = automation;
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

   public String getAutomation() {
      return automation;
   }

   public void setAutomation(final String automation) {
      this.automation = automation;
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

   @JsonIgnore
   public Set<String> getAddedKeys() {
      if (getOldState() != null) {
         var addedKeys = new HashSet<>(getOldState().keySet());
         addedKeys.removeAll(getNewState().keySet());
         return addedKeys;
      }
      return Collections.emptySet();
   }

   @JsonIgnore
   public Set<String> getRemovedKeys() {
      if (getOldState() != null && getNewState() != null) {
         var removedKeys = new HashSet<>(getNewState().keySet());
         removedKeys.removeAll(getOldState().keySet());
         return removedKeys;
      }
      return Collections.emptySet();
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

   public String getUserName() {
      return userName;
   }

   public void setUserName(final String userName) {
      this.userName = userName;
   }

   public String getUserEmail() {
      return userEmail;
   }

   public void setUserEmail(final String userEmail) {
      this.userEmail = userEmail;
   }

   public AuditType getType() {
      return type;
   }

   public void setType(final AuditType type) {
      this.type = type;
   }

   public String getViewId() {
      return viewId;
   }

   public void setViewId(final String viewId) {
      this.viewId = viewId;
   }

   public Object getTitle() {
      return title;
   }

   public void setTitle(final Object title) {
      this.title = title;
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
            ", userName='" + userName + '\'' +
            ", userEmail='" + userEmail + '\'' +
            ", view='" + viewId + '\'' +
            ", automation='" + automation + '\'' +
            ", oldState=" + oldState +
            ", newState=" + newState +
            ", type=" + type +
            '}';
   }
}
