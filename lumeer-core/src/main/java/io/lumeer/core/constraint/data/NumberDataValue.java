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
package io.lumeer.core.constraint.data;

import io.lumeer.api.model.ConstraintType;
import io.lumeer.core.constraint.config.NumberConstraintConfig;

import java.math.BigDecimal;

public class NumberDataValue extends NumericDataValue {

   private final Object decodedValue;

   public NumberDataValue(Object value, NumberConstraintConfig config) {
      this.decodedValue = value;
   }

   @Override
   public BigDecimal getNumber() {
      return null;
   }

   @Override
   public ConstraintType getType() {
      return ConstraintType.Number;
   }

   @Override
   public Object decodedValue() {
      return null;
   }

   @Override
   public Boolean intersects(final DataValue dataValue) {
      return false;
   }

}
