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
package io.lumeer.engine.api.batch;

import java.util.List;

public class SplitBatch extends AbstractCollectionBatch {

   /**
    * Splitting string.
    */
   private String delimiter = ",";

   /**
    * Attribute to split.
    */
   private String attribute;

   /**
    * When true, the individual parts are also trimmed.
    */
   private boolean trim = true;

   /**
    * List of resulting attributes names. If the original attribute splits into more parts,
    * all the extra parts are added to the last attribute.
    */
   private List<String> splitAttributes;

   //https://docs.mongodb.com/manual/reference/operator/aggregation/arrayElemAt/#exp._S_arrayElemAt
   //https://docs.mongodb.com/manual/reference/operator/aggregation/split/#exp._S_split
   //https://docs.mongodb.com/manual/reference/operator/aggregation/addFields/

   public SplitBatch() {
   }

   public SplitBatch(final String collectionCode, final String attribute, final String delimiter, final boolean trim, final List<String> splitAttributes, final boolean keepOriginal) {
      this.collectionCode = collectionCode;
      this.attribute = attribute;
      this.delimiter = delimiter;
      this.trim = trim;
      this.splitAttributes = splitAttributes;
      this.keepOriginal = keepOriginal;
   }

   public String getAttribute() {
      return attribute;
   }

   public void setAttribute(final String attribute) {
      this.attribute = attribute;
   }

   public String getDelimiter() {
      return delimiter;
   }

   public void setDelimiter(final String delimiter) {
      this.delimiter = delimiter;
   }

   public boolean isTrim() {
      return trim;
   }

   public void setTrim(final boolean trim) {
      this.trim = trim;
   }

   public List<String> getSplitAttributes() {
      return splitAttributes;
   }

   public void setSplitAttributes(final List<String> splitAttributes) {
      this.splitAttributes = splitAttributes;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final SplitBatch that = (SplitBatch) o;

      if (keepOriginal != that.keepOriginal) {
         return false;
      }
      if (trim != that.trim) {
         return false;
      }
      if (attribute != null ? !attribute.equals(that.attribute) : that.attribute != null) {
         return false;
      }
      if (collectionCode != null ? !collectionCode.equals(that.collectionCode) : that.collectionCode != null) {
         return false;
      }
      if (delimiter != null ? !delimiter.equals(that.delimiter) : that.delimiter != null) {
         return false;
      }
      return splitAttributes != null ? splitAttributes.equals(that.splitAttributes) : that.splitAttributes == null;
   }

   @Override
   public int hashCode() {
      int result = collectionCode != null ? collectionCode.hashCode() : 0;
      result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
      result = 31 * result + (delimiter != null ? delimiter.hashCode() : 0);
      result = 31 * result + (splitAttributes != null ? splitAttributes.hashCode() : 0);
      result = 31 * result + (keepOriginal ? 1 : 0);
      result = 31 * result + (trim ? 1 : 0);
      return result;
   }

   @Override
   public String toString() {
      return "SplitBatch{"
            + "collectionCode='" + collectionCode + '\''
            + ", attribute='" + attribute + '\''
            + ", delimiter='" + delimiter + '\''
            + ", trim='" + trim + '\''
            + ", splitAttributes=" + splitAttributes
            + ", keepOriginal='" + keepOriginal + '\''
            + '}';
   }
}
