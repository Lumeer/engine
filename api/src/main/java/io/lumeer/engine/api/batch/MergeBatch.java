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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MergeBatch extends AbstractCollectionBatch {

   public enum MergeType {
      DOCUMENT, JOIN, SUM;
   }

   private List<String> attributes;

   private String resultAttribute;

   private String join;

   private MergeType mergeType = MergeType.DOCUMENT;

   public MergeBatch() {
   }

   public MergeBatch(final String collectionCode, final List<String> attributes, final String resultAttribute, final String join, final MergeType mergeType, boolean keepOriginal) {
      this.collectionCode = collectionCode;
      this.attributes = attributes;
      this.resultAttribute = resultAttribute;
      this.join = join;
      this.mergeType = mergeType;
      this.keepOriginal = keepOriginal;
   }

   public List<String> getAttributes() {
      return attributes;
   }

   public void setAttributes(final List<String> attributes) {
      this.attributes = attributes;
   }

   public String getResultAttribute() {
      return resultAttribute;
   }

   public void setResultAttribute(final String resultAttribute) {
      this.resultAttribute = resultAttribute;
   }

   public String getJoin() {
      return join;
   }

   public void setJoin(final String join) {
      this.join = join;
   }

   public MergeType getMergeType() {
      return mergeType;
   }

   public void setMergeType(final MergeType mergeType) {
      this.mergeType = mergeType;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final MergeBatch that = (MergeBatch) o;

      if (keepOriginal != that.keepOriginal) {
         return false;
      }
      if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) {
         return false;
      }
      if (resultAttribute != null ? !resultAttribute.equals(that.resultAttribute) : that.resultAttribute != null) {
         return false;
      }
      if (join != null ? !join.equals(that.join) : that.join != null) {
         return false;
      }
      return mergeType == that.mergeType;
   }

   @Override
   public int hashCode() {
      int result = attributes != null ? attributes.hashCode() : 0;
      result = 31 * result + (resultAttribute != null ? resultAttribute.hashCode() : 0);
      result = 31 * result + (join != null ? join.hashCode() : 0);
      result = 31 * result + (mergeType != null ? mergeType.hashCode() : 0);
      result = 31 * result + (keepOriginal ? 1 : 0);
      return result;
   }

   @Override
   public String toString() {
      return "MergeBatch{"
            + "collectionCode='" + collectionCode + '\''
            + ", attributes=" + attributes
            + ", resultAttribute='" + resultAttribute + '\''
            + ", join='" + join + '\''
            + ", mergeType='" + mergeType + '\''
            + ", keepOriginal='" + keepOriginal + '\''
            + '}';
   }
}
