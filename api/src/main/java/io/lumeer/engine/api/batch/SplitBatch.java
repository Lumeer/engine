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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class SplitBatch extends AbstractCollectionBatch {

   private String delimiter = ",";

   private List<String> splitAttributes;

   //https://docs.mongodb.com/manual/reference/operator/aggregation/arrayElemAt/#exp._S_arrayElemAt
   //https://docs.mongodb.com/manual/reference/operator/aggregation/split/#exp._S_split
   //https://docs.mongodb.com/manual/reference/operator/aggregation/addFields/

   public SplitBatch() {
   }

   public SplitBatch(final String collectionName, final String delimiter, final List<String> splitAttributes) {
      this.collectionName = collectionName;
      this.delimiter = delimiter;
      this.splitAttributes = splitAttributes;
   }

   public String getDelimiter() {
      return delimiter;
   }

   public void setDelimiter(final String delimiter) {
      this.delimiter = delimiter;
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

      if (collectionName != null ? !collectionName.equals(that.collectionName) : that.collectionName != null) {
         return false;
      }
      if (delimiter != null ? !delimiter.equals(that.delimiter) : that.delimiter != null) {
         return false;
      }
      return splitAttributes != null ? splitAttributes.equals(that.splitAttributes) : that.splitAttributes == null;
   }

   @Override
   public int hashCode() {
      int result = collectionName != null ? collectionName.hashCode() : 0;
      result = 31 * result + (delimiter != null ? delimiter.hashCode() : 0);
      result = 31 * result + (splitAttributes != null ? splitAttributes.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "SplitBatch{"
            + "collectionName='" + collectionName + '\''
            + "delimiter='" + delimiter + '\''
            + ", splitAttributes=" + splitAttributes
            + '}';
   }
}
