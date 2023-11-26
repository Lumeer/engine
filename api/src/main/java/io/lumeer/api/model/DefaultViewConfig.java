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
import io.lumeer.api.adapter.ZonedDateTimeSerializer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZonedDateTime;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class DefaultViewConfig {

   private String userId;
   private String key;
   private String perspective;
   private Object config;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   @JsonSerialize(using = ZonedDateTimeSerializer.class)
   @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
   private ZonedDateTime updatedAt;

   public DefaultViewConfig() {
   }

   @JsonCreator
   public DefaultViewConfig(@JsonProperty("key") final String key,
         @JsonProperty("perspective") final String perspective,
         @JsonProperty("config") final Object config,
         @JsonProperty("updatedAt")
         @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class) final ZonedDateTime updateAt) {
      this.key = key;
      this.perspective = perspective;
      this.config = config;
      this.updatedAt = updateAt;
   }

   public ZonedDateTime getUpdatedAt() {
      return updatedAt;
   }

   public String getUserId() {
      return userId;
   }

   public void setUserId(final String userId) {
      this.userId = userId;
   }

   public String getKey() {
      return key;
   }

   public String getPerspective() {
      return perspective;
   }

   public Object getConfig() {
      return config;
   }

   @Override
   public String toString() {
      return "DefaultViewConfig{" +
            "userId='" + userId + '\'' +
            ", key='" + key + '\'' +
            ", perspective='" + perspective + '\'' +
            ", config=" + config +
            '}';
   }
}
