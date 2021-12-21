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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AttributeLockExceptionGroup {

   private final AttributeFilterEquation equation;
   private final List<String> typeValue;
   private final String type;

   @JsonCreator
   public AttributeLockExceptionGroup(@JsonProperty("equation") final AttributeFilterEquation equation,
         @JsonProperty("typeValue") final List<String> typeValue,
         @JsonProperty("type") final String type) {
      this.equation = equation;
      this.typeValue = typeValue;
      this.type = type;
   }

   public AttributeFilterEquation getEquation() {
      return equation;
   }

   public List<String> getTypeValue() {
      return typeValue;
   }

   public String getType() {
      return type;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AttributeLockExceptionGroup)) {
         return false;
      }
      final AttributeLockExceptionGroup that = (AttributeLockExceptionGroup) o;
      return Objects.equals(equation, that.equation) && Objects.equals(typeValue, that.typeValue) && Objects.equals(type, that.type);
   }

   @Override
   public int hashCode() {
      return Objects.hash(equation, typeValue, type);
   }

   @Override
   public String toString() {
      return "AttributeLockExceptionGroup{" +
            "equation=" + equation +
            ", typeValue=" + typeValue +
            ", type='" + type + '\'' +
            '}';
   }
}
