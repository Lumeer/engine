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

import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.constraint.DataValue;
import io.lumeer.core.constraint.config.PercentageConstraintConfig;
import io.lumeer.core.constraint.data.NumericDataValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import javax.annotation.Nonnull;

public class PercentageDataValue extends NumericDataValue {

   private final Integer SHIFT_BY_DECIMALS = 2;
   private final BigDecimal number;
   private final Object value;

   public PercentageDataValue(Object value, PercentageConstraintConfig config) {
      Number number = encodePercentage(Locale.getDefault(), value);
      if (number != null) {
         BigDecimal bigDecimal = new BigDecimal(number.toString());
         if (config.decimals != null && config.decimals >= 0) {
            this.number = bigDecimal.setScale(config.decimals + SHIFT_BY_DECIMALS, RoundingMode.HALF_DOWN);
         } else {
            this.number = bigDecimal;
         }
      } else {
         this.number = null;
      }
      this.value = value;
   }

   private Number encodePercentage(Locale locale, Object value) {
      if (value instanceof String) {
         final String strValue = ((String) value).replace(" ", "").trim();

         if (strValue.endsWith("%") && strValue.indexOf("%") == strValue.length() - 1) {
            final Number result = encodeNumber(locale, strValue.substring(0, strValue.length() - 1));

            if (result instanceof BigDecimal) {
               return ((BigDecimal) result).movePointLeft(SHIFT_BY_DECIMALS);
            } else if (result instanceof Long) {
               return BigDecimal.valueOf((Long) result).movePointLeft(SHIFT_BY_DECIMALS);
            }
         }
      }
      return encodeNumber(locale, value);
   }

   @Override
   public BigDecimal getNumber() {
      return number;
   }

   @Override
   public ConstraintType getType() {
      return ConstraintType.Percentage;
   }

   @Override
   public Object decodeValue() {
      return null;
   }

   @Override
   public Object encodeValue(final Locale locale) {
      return encodePercentage(locale, this.value);
   }

   @Override
   @Nonnull
   public String format() {
      if (this.number != null) {
         return this.number.movePointRight(SHIFT_BY_DECIMALS).toString() + "%";
      }
      return this.value != null ? this.value.toString().trim() : "";
   }

   @Override
   public Boolean intersects(final DataValue dataValue) {
      return false;
   }

}
