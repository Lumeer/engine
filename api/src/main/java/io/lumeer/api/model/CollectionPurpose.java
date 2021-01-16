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

import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CollectionPurpose {

   public static final String TYPE = "type";
   public static final String META_DATA = "metaData";

   private final CollectionPurposeType type;
   private DataDocument metaData;

   @JsonCreator
   public CollectionPurpose(@JsonProperty("type") final CollectionPurposeType type,
         @JsonProperty("metaData") final DataDocument metaData) {
      this.type = type;
      this.metaData = metaData;
   }

   public CollectionPurposeType getType() {
      return type;
   }

   public DataDocument getMetaData() {
      return metaData;
   }

   public DataDocument createIfAbsentMetaData() {
      if (metaData == null) {
         this.metaData = new DataDocument();
      }

      return metaData;
   }

   @Override
   public String toString() {
      return "CollectionPurpose{" +
            "type=" + type +
            ", metaData=" + metaData +
            '}';
   }
}
