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
import io.lumeer.api.model.constraint.DataValue;
import io.lumeer.core.constraint.config.NumberConstraintConfig;
import io.lumeer.core.util.NumbroJsParser;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class NumberDataValue extends NumericDataValue {

   private final BigDecimal number;
   private final Object value;
   private final NumberConstraintConfig config;

   public NumberDataValue(Object value, NumberConstraintConfig config) {
      Number number = encodeNumber(Locale.getDefault(), value);
      if (number != null) {
         BigDecimal bigDecimal = new BigDecimal(number.toString());
         if (config.decimals != null && config.decimals >= 0) {
            this.number = bigDecimal.setScale(config.decimals, RoundingMode.HALF_DOWN);
         } else {
            this.number = bigDecimal;
         }
      } else {
         this.number = null;
      }
      this.value = value;
      this.config = config;
   }

   @Override
   public BigDecimal getNumber() {
      return number;
   }

   @Override
   public ConstraintType getType() {
      return ConstraintType.Number;
   }

   @Override
   public Object decodeValue() {
      return null;
   }

   @Override
   public Object encodeValue(final Locale locale) {
      return encodeNumber(locale, value);
   }

   @Override
   @Nonnull
   public String format() {
      if (this.number != null) {
         String formatted;
         if (StringUtils.isNotEmpty(this.config.currency)) {
            formatted = NumbroJsParser.formatCurrency(this.number, this.config);
         } else {
            formatted = NumbroJsParser.formatNumber(this.number, this.config);
         }
         if (formatted != null) {
            return formatted;
         }
      }
      return this.value != null ? this.value.toString().trim() : "";
   }

   @Override
   public Boolean intersects(final DataValue dataValue) {
      return false;
   }

}
