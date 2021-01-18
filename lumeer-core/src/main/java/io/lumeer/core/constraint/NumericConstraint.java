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
package io.lumeer.core.constraint;

import io.lumeer.api.model.ConditionType;
import io.lumeer.api.model.constraint.Constraint;
import io.lumeer.core.constraint.data.NumericDataValue;
import io.lumeer.core.util.ArrayUtils;

import java.util.function.BiFunction;

public abstract class NumericConstraint<T extends NumericDataValue> extends Constraint {

   @Override
   public BiFunction<T, T[], Boolean> getDataValueEvaluator(final ConditionType condition) {
      switch (condition) {
         case EQUALS:
            return (dataValue, otherDataValues) -> dataValue.isEqual(ArrayUtils.get(otherDataValues, 0));
         case NOT_EQUALS:
            return (dataValue, otherDataValues) -> dataValue.isNotEqual(ArrayUtils.get(otherDataValues, 0));
         case GREATER_THAN:
            return (dataValue, otherDataValues) -> dataValue.greaterThan(ArrayUtils.get(otherDataValues, 0));
         case GREATER_THAN_EQUALS:
            return (dataValue, otherDataValues) -> dataValue.greaterThanEquals(ArrayUtils.get(otherDataValues, 0));
         case LOWER_THAN:
            return (dataValue, otherDataValues) -> dataValue.lowerThan(ArrayUtils.get(otherDataValues, 0));
         case LOWER_THAN_EQUALS:
            return (dataValue, otherDataValues) -> dataValue.lowerThanEquals(ArrayUtils.get(otherDataValues, 0));
      }
      return (x, y) -> false;
   }

}
