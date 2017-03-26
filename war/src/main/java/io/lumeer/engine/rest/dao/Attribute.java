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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class Attribute {

   private String name;
   private int count;
   private List<String> constraints;
   private Map<String, Attribute> childAttributes = new HashMap<>();

   public Attribute(final DataDocument metadata) {
      name = metadata.getString(LumeerConst.Collection.ATTRIBUTE_NAME_KEY);
      count = metadata.getInteger(LumeerConst.Collection.ATTRIBUTE_COUNT_KEY);
      constraints = metadata.getArrayList(LumeerConst.Collection.ATTRIBUTE_CONSTRAINTS_KEY, String.class);

      DataDocument childAttributesDocument = metadata.getDataDocument(LumeerConst.Collection.ATTRIBUTE_CHILDREN_KEY);
      for (String attributeName : childAttributesDocument.keySet()) {
         String nameWithParent = name + "." + attributeName;
         childAttributes.put(
               attributeName,
               new Attribute(
                     childAttributesDocument
                           .getDataDocument(attributeName)
                           .append(
                                 LumeerConst.Collection.ATTRIBUTE_NAME_KEY,
                                 nameWithParent)));
      }
   }

   public String getName() {
      return name;
   }

   public String getNameWithoutParent() {
      return name.substring(name.lastIndexOf(".") + 1);
   }

   public int getCount() {
      return count;
   }

   public List<String> getConstraints() {
      return constraints;
   }

   public Map<String, Attribute> getChildAttributes() {
      return childAttributes;
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

      return name != null ? name.equals(that.name) : that.name == null;

   }

   @Override
   public int hashCode() {
      return name != null ? name.hashCode() : 0;
   }

   @Override
   public String toString() {
      return "Attribute{"
            + ", name='" + name + '\''
            + ", count=" + count
            + ", constraints=" + constraints
            + ", childAttributes=" + childAttributes
            + '}';
   }
}
