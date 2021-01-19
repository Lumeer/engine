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

import io.lumeer.api.model.constraint.DataValue;

import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Pattern;

public abstract class NumericDataValue extends DataValue {

   public abstract BigDecimal getNumber();

   @Override
   public Boolean isValid() {
      return getNumber() != null;
   }

   private final Pattern numberMatch = Pattern.compile("^[-+]?\\d+([.,]\\d+)?([Ee][+-]?\\d+)?$");

   /**
    * Tries to convert the parameter to a number (either integer, double or big decimal) and return it.
    *
    * @param value The value to try to convert to number.
    * @return The value converted to a number data type or null when the conversion was not possible.
    */
   protected Number encodeNumber(final Locale locale, final Object value) {
      Object numberValue = value;
      if (value instanceof String && numberMatch.matcher((String) value).matches()) {
         numberValue = ((String) value).replaceFirst(",", ".").replace("e", "E");
      }
      final DecimalFormat df = (DecimalFormat) DecimalFormat.getNumberInstance(locale);
      df.setParseBigDecimal(true);
      final NumberFormat nf = NumberFormat.getNumberInstance(locale);

      return encodeNumber(nf, df, numberValue);
   }

   /**
    * Tries to convert the parameter to a number (either integer, double or big decimal) and return it.
    *
    * @param numberFormat    Number format to parse to double or integer.
    * @param bigNumberFormat Number format to parse to big decimal.
    * @param value           The value to try to convert to number.
    * @return The value converted to a number data type or null when the conversion was not possible.
    */
   protected Number encodeNumber(final NumberFormat numberFormat, final NumberFormat bigNumberFormat, Object value) {
      if (value instanceof Number) {
         return (Number) value;
      } else if (value instanceof String) {
         final String trimmed = ((String) value).trim();

         if (trimmed.matches("^0[^\\.].*")) { // we need to keep leading and trailing zeros, so no conversion to number
            return null;
         }

         try {
            // figure out whether we need to use BigDecimal
            final Number n2 = numberFormat.parse(trimmed);

            if (bigNumberFormat == null) {
               return n2;
            }

            final Number n1 = bigNumberFormat.parse(trimmed);

            if (n1 instanceof BigDecimal) {
               try {
                  new Decimal128((BigDecimal) n1); // are we able to fit into Decimal128?
               } catch (NumberFormatException nfe) {
                  if (n2 instanceof Double && Double.isInfinite((Double) n2)) {
                     return null;
                  }
                  return n2;
               }
            }

            return n1.toString().equals(n2.toString()) && !(n2 instanceof Double) ? n2 : n1;
         } catch (final ParseException pe) {
            return null;
         }
      }

      return null;
   }

   public Boolean isEqual(final NumericDataValue dataValue) {
      if (isValid() && dataValue.isValid()) {
         return getNumber().compareTo(dataValue.getNumber()) == 0;
      }
      return format().equals(dataValue.format());
   }

   public Boolean isNotEqual(final NumericDataValue dataValue) {
      return !isEqual(dataValue);
   }

   public Boolean greaterThan(final NumericDataValue dataValue) {
      if (isValid() && dataValue.isValid()) {
         return getNumber().compareTo(dataValue.getNumber()) > 0;
      }
      return isValid() && !dataValue.isValid();
   }

   public Boolean greaterThanEquals(final NumericDataValue dataValue) {
      if (isValid() && dataValue.isValid()) {
         return getNumber().compareTo(dataValue.getNumber()) >= 0;
      }
      return isValid() && !dataValue.isValid();
   }

   public Boolean lowerThan(final NumericDataValue dataValue) {
      if (isValid() && dataValue.isValid()) {
         return getNumber().compareTo(dataValue.getNumber()) < 0;
      }
      return !isValid() && dataValue.isValid();
   }

   public Boolean lowerThanEquals(final NumericDataValue dataValue) {
      if (isValid() && dataValue.isValid()) {
         return getNumber().compareTo(dataValue.getNumber()) <= 0;
      }
      return !isValid() && dataValue.isValid();
   }

   public Boolean isEmpty() {
      return !isValid() && format().isEmpty();
   }

   public Boolean isNotEmpty() {
      return !isEmpty();
   }

}
