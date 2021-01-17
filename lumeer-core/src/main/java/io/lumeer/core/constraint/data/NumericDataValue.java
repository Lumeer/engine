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

import java.math.BigDecimal;

public abstract class NumericDataValue extends DataValue {

   public abstract BigDecimal getNumber();

   public Boolean isEqual(final NumericDataValue dataValue) {
      if (isEmpty() || dataValue.isEmpty()) {
         return isEmpty() && dataValue.isEmpty();
      }
      return getNumber().compareTo(dataValue.getNumber()) == 0;
   }

   public Boolean isNotEqual(final NumericDataValue dataValue) {
      return !isEqual(dataValue);
   }

   public Boolean greaterThan(final NumericDataValue dataValue) {
      if (isEmpty() || dataValue.isEmpty()) {
         return false;
      }
      return getNumber().compareTo(dataValue.getNumber()) > 0;
   }

   public Boolean greaterThanEquals(final NumericDataValue dataValue) {
      if (isEmpty() || dataValue.isEmpty()) {
         return false;
      }
      return getNumber().compareTo(dataValue.getNumber()) >= 0;
   }

   public Boolean lowerThan(final NumericDataValue dataValue) {
      if (isEmpty() || dataValue.isEmpty()) {
         return false;
      }
      return getNumber().compareTo(dataValue.getNumber()) < 0;
   }

   public Boolean lowerThanEquals(final NumericDataValue dataValue) {
      if (isEmpty() || dataValue.isEmpty()) {
         return false;
      }
      return getNumber().compareTo(dataValue.getNumber()) <= 0;
   }

   public Boolean isEmpty() {
      return getNumber() == null;
   }

   public Boolean isNotEmpty() {
      return !isEmpty();
   }

}
