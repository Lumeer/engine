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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AttributeFilter {

   private final String attributeId;
   private final ConditionType condition;
   private List<ConditionValue> conditionValues;

   public AttributeFilter(final String attributeId, final ConditionType condition, final List<ConditionValue> value) {
      this.attributeId = attributeId;
      this.condition = condition;
      this.conditionValues = value;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public ConditionType getCondition() {
      return condition;
   }

   public List<ConditionValue> getConditionValues() {
      return conditionValues != null ? conditionValues : Collections.emptyList();
   }

   public void setConditionValues(final List<ConditionValue> conditionValues) {
      this.conditionValues = conditionValues;
   }

   public Object getValue() {
      return conditionValues != null && !conditionValues.isEmpty() ? conditionValues.get(0).getValue() : null;
   }

   @JsonIgnore
   public void setValue(final Object value) {
      this.conditionValues = Collections.singletonList(new ConditionValue(value));
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
            Objects.equals(condition, that.condition) &&
            Objects.equals(conditionValues, that.conditionValues);
   }

   @Override
   public int hashCode() {
      return Objects.hash(attributeId, condition, conditionValues);
   }

   @Override
   public String toString() {
      return "AttributeFilter{" +
            "attributeId='" + attributeId + '\'' +
            ", condition='" + condition + '\'' +
            ", value=" + conditionValues +
            '}';
   }
}
