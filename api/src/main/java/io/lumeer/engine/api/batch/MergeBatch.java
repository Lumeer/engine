/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.api.batch;

import java.util.List;
import java.util.Set;

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

   public MergeBatch(final String collectionName, final List<String> attributes, final String resultAttribute, final String join, final MergeType mergeType) {
      this.collectionName = collectionName;
      this.attributes = attributes;
      this.resultAttribute = resultAttribute;
      this.join = join;
      this.mergeType = mergeType;
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
      return result;
   }

   @Override
   public String toString() {
      return "MergeBatch{"
            + "collectionName='" + collectionName + '\''
            + ", attributes=" + attributes
            + ", resultAttribute='" + resultAttribute + '\''
            + ", join='" + join + '\''
            + ", mergeType='" + mergeType + '\''
            + '}';
   }
}
