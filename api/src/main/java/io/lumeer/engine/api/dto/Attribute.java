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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst.Collection;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Attribute {

   private final String name;
   private final String fullName;
   private final int count;
   private final List<String> constraints;

   public Attribute(final DataDocument metadata) {
      name = metadata.getString(Collection.ATTRIBUTE_NAME);
      fullName = metadata.getString(Collection.ATTRIBUTE_FULL_NAME);
      count = metadata.getInteger(Collection.ATTRIBUTE_COUNT);
      constraints = metadata.getArrayList(Collection.ATTRIBUTE_CONSTRAINTS, String.class);
   }

   @JsonCreator
   public Attribute(final @JsonProperty("name") String name,
         final @JsonProperty("fullName") String fullName,
         final @JsonProperty("count") int count,
         final @JsonProperty("constraints") List<String> constraints) {
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
      return constraints != null ? Collections.unmodifiableList(constraints) : Collections.emptyList();
   }

   public DataDocument toDataDocument() {
      return new DataDocument(Collection.ATTRIBUTE_NAME, name)
            .append(Collection.ATTRIBUTE_FULL_NAME, fullName)
            .append(Collection.ATTRIBUTE_COUNT, count)
            .append(Collection.ATTRIBUTE_CONSTRAINTS, constraints);
   }
}
