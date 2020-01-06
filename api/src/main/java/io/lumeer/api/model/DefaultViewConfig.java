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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DefaultViewConfig {

   private String userId;
   private String collectionId;
   private String perspective;
   private Object config;

   public DefaultViewConfig() {
   }

   @JsonCreator
   public DefaultViewConfig(@JsonProperty("collectionId") final String collectionId,
         @JsonProperty("perspective") final String perspective,
         @JsonProperty("config") final Object config) {
      this.collectionId = collectionId;
      this.perspective = perspective;
      this.config = config;
   }

   public String getUserId() {
      return userId;
   }

   public void setUserId(final String userId) {
      this.userId = userId;
   }

   public String getCollectionId() {
      return collectionId;
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
            ", collectionId='" + collectionId + '\'' +
            ", perspective='" + perspective + '\'' +
            ", config=" + config +
            '}';
   }
}
