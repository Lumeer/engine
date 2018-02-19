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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Import {

   public static final String COLLECTION = "collection";
   public static final String FORMAT = "format";
   public static final String DATA = "data";

   private Collection collection;
   private String format;
   private String data;

   @JsonCreator
   public Import(@JsonProperty(COLLECTION) final Collection collection,
         @JsonProperty(FORMAT) final String format,
         @JsonProperty(DATA) final String data) {
      this.collection = collection;
      this.format = format;
      this.data = data;
   }

   public Collection getCollection() {
      return collection;
   }

   public void setCollection(final Collection collection) {
      this.collection = collection;
   }

   public String getFormat() {
      return format;
   }

   public void setFormat(final String format) {
      this.format = format;
   }

   public String getData() {
      return data;
   }

   public void setData(final String data) {
      this.data = data;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Import)) {
         return false;
      }
      final Import anImport = (Import) o;
      return Objects.equals(getCollection(), anImport.getCollection()) &&
            Objects.equals(getFormat(), anImport.getFormat()) &&
            Objects.equals(getData(), anImport.getData());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getCollection(), getFormat(), getData());
   }

   @Override
   public String toString() {
      return "Import{" +
            "collection=" + collection +
            ", format='" + format + '\'' +
            ", data='" + data + '\'' +
            '}';
   }
}
