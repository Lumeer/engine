/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.rest.dao;

import io.lumeer.engine.api.LumeerConst.Collection;
import io.lumeer.engine.api.data.DataDocument;

import java.util.List;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class Attribute {

   private String name;
   private String fullName;
   private int count;
   private int level;
   private List<String> constraints;

   public Attribute(final DataDocument metadata) {
      name = metadata.getString(Collection.ATTRIBUTE_NAME_KEY);
      fullName = metadata.getString(Collection.ATTRIBUTE_FULL_NAME_KEY, name);
      count = metadata.getInteger(Collection.ATTRIBUTE_COUNT_KEY);
      level = metadata.getInteger(Collection.ATTRIBUTE_LEVEL_KEY);
      constraints = metadata.getArrayList(Collection.ATTRIBUTE_CONSTRAINTS_KEY, String.class);
   }

   public String getName() {
      return name;
   }

   public int getCount() {
      return count;
   }

   public List<String> getConstraints() {
      return constraints;
   }

   public String getFullName() {
      return fullName;
   }

   public int getLevel() {
      return level;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final Attribute that = (Attribute) o;

      return fullName != null ? fullName.equals(that.fullName) : that.fullName == null;

   }

   @Override
   public int hashCode() {
      return fullName != null ? fullName.hashCode() : 0;
   }

   //   @Override
   //   public String toString() {
   //      return "Attribute{"
   //            + ", name='" + name + '\''
   //            + ", count=" + count
   //            + ", constraints=" + constraints
   //            + ", childAttributes=" + childAttributes
   //            + '}';
   //   }
}
