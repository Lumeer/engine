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

import java.util.Objects;

public class AttributeFilter {

   private final String attributeId;
   private final String operator;
   private Object value;

   public AttributeFilter(final String attributeId, final String operator, final Object value) {
      this.attributeId = attributeId;
      this.operator = operator;
      this.value = value;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public String getOperator() {
      return operator;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(final Object value) {
      this.value = value;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AttributeFilter)) {
         return false;
      }
      final AttributeFilter that = (AttributeFilter) o;
      return Objects.equals(attributeId, that.attributeId) &&
            Objects.equals(operator, that.operator) &&
            Objects.equals(value, that.value);
   }

   @Override
   public int hashCode() {
      return Objects.hash(attributeId, operator, value);
   }

   @Override
   public String toString() {
      return "AttributeFilter{" +
            "attributeId='" + attributeId + '\'' +
            ", operator='" + operator + '\'' +
            ", value=" + value +
            '}';
   }
}
