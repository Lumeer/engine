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
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class UserNotification {

   public static final String USER_ID = "userId";
   public static final String CREATED_AT = "createdAt";
   public static final String FIRST_READ_AT = "firstReadAt";
   public static final String READ = "read";
   public static final String DATA = "data";
   public static final String TYPE = "type";

   private String id;

   private String userId;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime createdAt;

   private boolean read = false;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime firstReadAt;

   private NotificationType type;

   private DataDocument data;

   public UserNotification() {
   }

   @JsonCreator
   public UserNotification(
         @JsonProperty(USER_ID) final String userId,
         @JsonProperty(CREATED_AT) final ZonedDateTime createdAt,
         @JsonProperty(READ) final boolean read,
         @JsonProperty(FIRST_READ_AT) final ZonedDateTime firstReadAt,
         @JsonProperty(TYPE) final NotificationType type,
         @JsonProperty(DATA) final DataDocument data) {
      this.userId = userId;
      this.createdAt = createdAt;
      this.read = read;
      this.firstReadAt = firstReadAt;
      this.type = type;
      this.data = data;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getUserId() {
      return userId;
   }

   public void setUserId(final String userId) {
      this.userId = userId;
   }

   public ZonedDateTime getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(final ZonedDateTime createdAt) {
      this.createdAt = createdAt;
   }

   public boolean isRead() {
      return read;
   }

   public void setRead(final boolean read) {
      this.read = read;
   }

   public ZonedDateTime getFirstReadAt() {
      return firstReadAt;
   }

   public void setFirstReadAt(final ZonedDateTime firstReadAt) {
      this.firstReadAt = firstReadAt;
   }

   public NotificationType getType() {
      return type;
   }

   public void setType(final NotificationType type) {
      this.type = type;
   }

   public DataDocument getData() {
      return data;
   }

   public void setData(final DataDocument data) {
      this.data = data;
   }

   @Override
   public String toString() {
      return "UserNotification{" +
            "id='" + id + '\'' +
            ", userId='" + userId + '\'' +
            ", createdAt=" + createdAt +
            ", read=" + read +
            ", firstReadAt=" + firstReadAt +
            ", type=" + type +
            ", data=" + data +
            '}';
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final UserNotification that = (UserNotification) o;
      return read == that.read &&
            Objects.equals(id, that.id) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(firstReadAt, that.firstReadAt) &&
            type == that.type &&
            Objects.equals(data, that.data);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, userId, createdAt, read, firstReadAt, type, data);
   }

   public interface OrganizationShared {
      String ORGANIZATION_ID = "organizationId";
      String ORGANIZATION_CODE = "organizationCode";
      String ORGANIZATION_ICON = "organizationIcon";
      String ORGANIZATION_COLOR = "organizationColor";
      String ORGANIZATION_NAME = "organizationName";
   }

   public interface ProjectShared extends OrganizationShared {
      String PROJECT_ID = "projectId";
      String PROJECT_ICON = "projectIcon";
      String PROJECT_CODE = "projectCode";
      String PROJECT_NAME = "projectName";
      String PROJECT_COLOR = "projectColor";
   }

   public interface CollectionShared extends ProjectShared {
      String COLLECTION_ID = "collectionId";
      String COLLECTION_ICON = "collectionIcon";
      String COLLECTION_NAME = "collectionName";
      String COLLECTION_COLOR = "collectionColor";
      String COLLECTION_QUERY = "collectionQuery";
   }

   public interface ViewShared extends ProjectShared {
      String VIEW_ICON = "viewIcon";
      String VIEW_COLOR = "viewColor";
      String VIEW_CODE = "viewCode";
      String VIEW_NAME = "viewName";
      String VIEW_PERSPECTIVE = "viewPerspective";
   }

   public interface TaskNotification extends CollectionShared {
      String DOCUMENT_ID = "documentId";
      String DOCUMENT_CURSOR = "documentCursor";
   }

   public interface TaskAssigned extends TaskNotification {}

   public interface TaskUpdated extends TaskNotification {}

   public interface DueDateSoon extends TaskNotification {}

   public interface PastDueDate extends TaskNotification {}

   public interface TaskUnassigned extends TaskNotification {}
}
