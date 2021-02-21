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
import io.lumeer.api.exception.InsaneObjectException;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class Feedback implements HealthChecking {

   public static final String ID = "id";
   public static final String USER_ID = "userId";
   public static final String CREATION_TIME = "creationTime";
   public static final String MESSAGE = "message";

   private String id;
   private String userId;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime creationTime;

   private String message;

   public Feedback(final String message) {
      this.message = message;
   }

   public Feedback(final String userId, final ZonedDateTime creationTime, final String message) {
      this.userId = userId;
      this.creationTime = creationTime;
      this.message = message;
   }

   public Feedback(@JsonProperty(ID) final String id,
         @JsonProperty(USER_ID) final String userId,
         @JsonProperty(CREATION_TIME) final ZonedDateTime creationTime,
         @JsonProperty(MESSAGE) final String message) {
      this.id = id;
      this.userId = userId;
      this.creationTime = creationTime;
      this.message = message;
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

   public ZonedDateTime getCreationTime() {
      return creationTime;
   }

   public void setCreationTime(final ZonedDateTime creationTime) {
      this.creationTime = creationTime;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(final String message) {
      this.message = message;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Feedback feedback = (Feedback) o;
      return Objects.equals(id, feedback.id);
   }

   @Override
   public int hashCode() {

      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return "Feedback{" +
            "id='" + id + '\'' +
            ", userId='" + userId + '\'' +
            ", creationTime=" + creationTime +
            ", message='" + message + '\'' +
            '}';
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength("message", message, MAX_LONG_STRING_LENGTH);
   }
}
