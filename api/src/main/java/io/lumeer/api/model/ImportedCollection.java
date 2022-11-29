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

public class ImportedCollection {

   public static final String COLLECTION = "collection";
   public static final String DATA = "data";
   public static final String TYPE = "type";
   public static final String ATTRIBUTE_ID = "mergeAttributeId";

   private Collection collection;
   private String data;
   private String mergeAttributeId;
   private ImportType type;

   @JsonCreator
   public ImportedCollection(@JsonProperty(COLLECTION) final Collection collection,
         @JsonProperty(DATA) final String data,
         @JsonProperty(TYPE) final ImportType type,
         @JsonProperty(ATTRIBUTE_ID) final String mergeAttributeId) {
      this.collection = collection;
      this.data = data;
      this.type = type;
      this.mergeAttributeId = mergeAttributeId;
   }

   public Collection getCollection() {
      return collection;
   }

   public String getData() {
      return data;
   }

   public ImportType getType() {
      return type;
   }

   public String getMergeAttributeId() {
      return mergeAttributeId;
   }

   @Override
   public String toString() {
      return "ImportedCollection{" +
            "collection=" + collection +
            ", data='" + data + '\'' +
            ", type=" + type +
            ", mergeAttributeId=" + mergeAttributeId +
            '}';
   }
}
