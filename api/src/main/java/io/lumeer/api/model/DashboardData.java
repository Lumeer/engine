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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DashboardData  {

   public static final String TYPE = "type";
   public static final String TYPE_ID = "typeId";
   public static final String DATA = "data";

   private String type;
   private String typeId;
   private String userId;
   private Object data;

   @JsonCreator
   public DashboardData(@JsonProperty(TYPE) final String type,
         @JsonProperty(TYPE_ID) final String typeId,
         @JsonProperty(DATA) final Object data) {
      this.type = type;
      this.typeId = typeId;
      this.data = data;
   }

   public DashboardData(final String type, final String typeId, final String userId, final Object data) {
      this.type = type;
      this.typeId = typeId;
      this.userId = userId;
      this.data = data;
   }

   public String getType() {
      return type;
   }

   public void setType(final String type) {
      this.type = type;
   }

   public String getTypeId() {
      return typeId;
   }

   public void setTypeId(final String typeId) {
      this.typeId = typeId;
   }

   public Object getData() {
      return data;
   }

   public void setData(final Object data) {
      this.data = data;
   }

   @JsonIgnore
   public String getUserId() {
      return userId;
   }

   public void setUserId(final String userId) {
      this.userId = userId;
   }

}
