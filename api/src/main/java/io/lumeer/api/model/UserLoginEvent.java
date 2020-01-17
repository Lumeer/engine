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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Objects;

public class UserLoginEvent {

   public static final String ID = "id";
   public static final String USER_ID = "userId";
   public static final String DATE = "date";

   private String id;
   private String userId;
   private ZonedDateTime date;

   public UserLoginEvent(@JsonProperty(ID) final String id, @JsonProperty(USER_ID) final String userId, @JsonProperty(DATE) final ZonedDateTime date) {
      this.id = id;
      this.userId = userId;
      this.date = date;
   }

   public UserLoginEvent(final String userId) {
      this.userId = userId;
      this.date = ZonedDateTime.now();
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

   public ZonedDateTime getDate() {
      return date;
   }

   public void setDate(final ZonedDateTime date) {
      this.date = date;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final UserLoginEvent that = (UserLoginEvent) o;
      return Objects.equals(id, that.id) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(date, that.date);
   }

   @Override
   public int hashCode() {

      return Objects.hash(id, userId, date);
   }

   @Override
   public String toString() {
      return "UserLoginEvent{" +
            "id='" + id + '\'' +
            ", userId='" + userId + '\'' +
            ", date=" + date +
            '}';
   }
}
