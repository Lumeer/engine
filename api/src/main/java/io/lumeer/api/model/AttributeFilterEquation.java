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
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class AttributeFilterEquation {

   private final List<AttributeFilterEquation> equations;
   private final AttributeFilter filter;
   private final String operator;

   @JsonCreator
   public AttributeFilterEquation(@JsonProperty("equations") final List<AttributeFilterEquation> equations,
         @JsonProperty("filter") final AttributeFilter filter,
         @JsonProperty("operator") final String operator) {
      this.equations = equations;
      this.filter = filter;
      this.operator = operator;
   }

   public List<AttributeFilterEquation> getEquations() {
      return equations;
   }

   public AttributeFilter getFilter() {
      return filter;
   }

   public String getOperator() {
      return operator;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AttributeFilterEquation)) {
         return false;
      }
      final AttributeFilterEquation that = (AttributeFilterEquation) o;
      return Objects.equals(equations, that.equations) && Objects.equals(filter, that.filter) && Objects.equals(operator, that.operator);
   }

   @Override
   public int hashCode() {
      return Objects.hash(equations, filter, operator);
   }

   @Override
   public String toString() {
      return "AttributeFilterEquation{" +
            "equations=" + equations +
            ", filter=" + filter +
            ", operator='" + operator + '\'' +
            '}';
   }
}
