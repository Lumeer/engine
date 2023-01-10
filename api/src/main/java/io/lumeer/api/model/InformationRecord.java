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

import java.time.ZonedDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformationRecord implements WithId, HealthChecking {

   public static final String ID = "id";
   public static final String USER_ID = "userId";
   public static final String DATE = "date";
   public static final String SOURCE = "source";
   public static final String TARGET = "target";
   public static final String DATA = "data";

   private String id;
   private String userId;
   private ZonedDateTime date;
   private String source;
   private String target;
   private String data;

   @JsonCreator
   public InformationRecord(@JsonProperty(ID) final String id,
                            @JsonProperty(USER_ID) final String userId,
                            @JsonProperty(DATE) final ZonedDateTime date,
                            @JsonProperty(SOURCE) final String source,
                            @JsonProperty(TARGET) final String target,
                            @JsonProperty(DATA) final String data) {
      this.id = id;
      this.userId = userId;
      this.date = date;
      this.source = source;
      this.target = target;
      this.data = data;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getUserId() {
      return userId;
   }

   public ZonedDateTime getDate() {
      return date;
   }

   public String getSource() {
      return source;
   }

   public String getTarget() {
      return target;
   }

   public String getData() {
      return data;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      InformationRecord that = (InformationRecord) o;
      return Objects.equals(id, that.id) && Objects.equals(userId, that.userId) && Objects.equals(source, that.source) && Objects.equals(target, that.target) && Objects.equals(data, that.data) && Objects.equals(date, that.date);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, userId, source, target, data, date);
   }

   @Override
   public String toString() {
      return "InformationRecord{" +
              "id='" + id + '\'' +
              ", userId='" + userId + '\'' +
              ", date=" + date +
              ", source='" + source + '\'' +
              ", target='" + target + '\'' +
              ", data='" + data + '\'' +
              '}';
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength(DATA, data, MAX_MESSAGE_SIZE);
   }
}
