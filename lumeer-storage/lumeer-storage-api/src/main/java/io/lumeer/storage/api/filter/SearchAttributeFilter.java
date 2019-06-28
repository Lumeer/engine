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
package io.lumeer.storage.api.filter;

import io.lumeer.api.model.ConditionType;

import java.util.Objects;

public class SearchAttributeFilter {

   private final ConditionType conditionType;
   private final String attributeId;
   private final Object value;

   public SearchAttributeFilter(final ConditionType conditionType, final String attributeId, final Object value) {
      this.conditionType = conditionType;
      this.attributeId = attributeId;
      this.value = value;
   }

   public ConditionType getConditionType() {
      return conditionType;
   }

   public Object getValue() {
      return value;
   }

   public String getAttributeId() {
      return attributeId;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof SearchAttributeFilter)) {
         return false;
      }
      final SearchAttributeFilter that = (SearchAttributeFilter) o;
      return getConditionType() == that.getConditionType() &&
            Objects.equals(getValue(), that.getValue()) &&
            Objects.equals(getAttributeId(), that.getAttributeId());
   }

   @Override
   public int hashCode() {

      return Objects.hash(getConditionType(), getValue(), getAttributeId());
   }

   @Override
   public String toString() {
      return "CollectionSearchAttributeFilter{" +
            ", conditionType=" + conditionType +
            ", value='" + value + '\'' +
            ", attributeId='" + attributeId + '\'' +
            '}';
   }
}
