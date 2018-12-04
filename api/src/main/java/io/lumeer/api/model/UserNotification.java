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
package io.lumeer.api.model;

import io.lumeer.api.adapter.ZonedDateTimeAdapter;
import io.lumeer.engine.api.data.DataDocument;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
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

   public UserNotification(final String userId, final ZonedDateTime createdAt, final boolean read, final ZonedDateTime firstReadAt, final NotificationType type, final DataDocument data) {
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

   public enum NotificationType {
      ORGANIZATION_SHARED, PROJECT_SHARED, COLLECTION_SHARED, VIEW_SHARED
   }

   public interface OrganizationShared {
      String ORGANIZATION_ID = "organizationId";
   }

   public interface ProjectShared extends OrganizationShared {
      String PROJECT_ID = "projectId";
   }

   public interface CollectionShared extends ProjectShared {
      String COLLECTION_ID = "collectionId";
   }

   public interface ViewShared extends ProjectShared {
      String VIEW_ID = "viewId";
   }
}
