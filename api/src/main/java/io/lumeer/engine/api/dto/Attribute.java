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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst.Collection;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@Immutable
public class Attribute {

   private final String name;
   private final String fullName;
   private final int count;
   private final List<String> constraints;

   public Attribute(final DataDocument metadata) {
      name = metadata.getString(Collection.ATTRIBUTE_NAME_KEY);
      fullName = metadata.getString(Collection.ATTRIBUTE_FULL_NAME_KEY);
      count = metadata.getInteger(Collection.ATTRIBUTE_COUNT_KEY);
      constraints = metadata.getArrayList(Collection.ATTRIBUTE_CONSTRAINTS_KEY, String.class);
   }

   @JsonCreator
   public Attribute(final @JsonProperty(Collection.ATTRIBUTE_NAME_KEY) String name,
         final @JsonProperty(Collection.ATTRIBUTE_FULL_NAME_KEY) String fullName,
         final @JsonProperty(Collection.ATTRIBUTE_COUNT_KEY) int count,
         final @JsonProperty(Collection.ATTRIBUTE_CONSTRAINTS_KEY) List<String> constraints) {
      this.name = name;
      this.fullName = fullName;
      this.count = count;
      this.constraints = constraints;
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
   }

   public int getCount() {
      return count;
   }

   public List<String> getConstraints() {
      return Collections.unmodifiableList(constraints);
   }

   public DataDocument toDataDocument() {
      return new DataDocument(Collection.ATTRIBUTE_NAME_KEY, name)
            .append(Collection.ATTRIBUTE_FULL_NAME_KEY, fullName)
            .append(Collection.ATTRIBUTE_COUNT_KEY, count)
            .append(Collection.ATTRIBUTE_CONSTRAINTS_KEY, constraints);
   }
}
