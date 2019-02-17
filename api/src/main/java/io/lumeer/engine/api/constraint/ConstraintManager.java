/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.api.constraint;

import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Holds a list of constraints that can be obtained from a list of string configurations.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ConstraintManager {

   /**
    * Locale that will be passed to all constraints.
    */
   private Locale locale;

   /**
    * Pattern used to determine whether the input value is a number.
    * Initialized upon setting a specific locale.
    */
   private Pattern numberMatch;

   /**
    * Sets the user's locale.
    *
    * @return The user's locale.
    */
   public Locale getLocale() {
      return locale;
   }

   /**
    * Gets the currently used locale.
    *
    * @param locale
    *       The currently used locale.
    */
   public void setLocale(final Locale locale) {
      this.locale = locale;
      initNumberMatchPatten(locale);
   }

   /**
    * Initialize a pattern to determine whether a given input value is a number.
    *
    * @param locale
    *       The currently used locale.
    */
   private void initNumberMatchPatten(final Locale locale) {
      final DecimalFormat df = (DecimalFormat) DecimalFormat.getNumberInstance(locale);
      final char separator = df.getDecimalFormatSymbols().getDecimalSeparator();
      final String escapedSeparator = separator == '.' ? "\\." : ",";

      this.numberMatch = Pattern.compile("^[-+]?\\d+(" + escapedSeparator + "\\d+)?([Ee][+-]?\\d+)?$");
   }

   /**
    * Tries to convert the parameter to a number (either integer, double or big decimal) and return it.
    *
    * @param value
    *       The value to try to convert to number.
    * @return The value converted to a number data type or null when the conversion was not possible.
    */
   private Number encodeNumber(final Locale locale, final Object value) {
      final DecimalFormat df = (DecimalFormat) DecimalFormat.getNumberInstance(locale);
      df.setParseBigDecimal(true);
      final NumberFormat nf = NumberFormat.getNumberInstance(locale);

      return encodeNumber(nf, df, value);
   }

   /**
    * Tries to convert the parameter to a number (either integer, double or big decimal) and return it.
    *
    * @param numberFormat
    *       Number format to parse to double or integer.
    * @param bigNumberFormat
    *       Number format to parse to big decimal.
    * @param value
    *       The value to try to convert to number.
    * @return The value converted to a number data type or null when the conversion was not possible.
    */
   private Number encodeNumber(final NumberFormat numberFormat, final NumberFormat bigNumberFormat, Object value) {
      if (value instanceof Number) {
         return (Number) value;
      } else if (value instanceof String) {
         try {
            // figure out whether we need to use BigDecimal
            final Number n2 = numberFormat.parse(((String) value).trim());

            if (bigNumberFormat == null) {
               return n2;
            }

            final Number n1 = bigNumberFormat.parse(((String) value).trim());

            return n1.toString().equals(n2.toString()) && !(n2 instanceof Double) ? n2 : n1;
         } catch (final ParseException pe) {
            return null;
         }
      }

      return null;
   }

   /**
    * Encodes the given value to a data type suitable for database storage based on current constraints configuration.
    *
    * @param value
    *       The value to convert.
    * @return The same value with changed data type.
    */
   public Object encode(final Object value) {
      if (locale == null) {
         throw new IllegalStateException("No locale was set in ConstraintManager. Please use function setLocale() so it can encode correctly.");
      }

      if (value instanceof String && numberMatch.matcher((String) value).matches()) {
         final Number n = encodeNumber(locale, value);
         return n == null ? value : n;
      }

      return value;
   }

   public Object encode(final Object value, final Constraint constraint) {
      if (locale == null) {
         throw new IllegalStateException("No locale was set in ConstraintManager. Please use function setLocale() so it can encode correctly.");
      }

      if (value == null) {
         return null;
      }

      if (constraint == null || constraint.getType() == ConstraintType.Number || constraint.getType() == ConstraintType.Percentage) {
         return encode(value);
      }

      if (constraint.getType() == ConstraintType.Boolean) {
         if (value instanceof Boolean) {
            return value;
         } else if (value.toString().trim().equalsIgnoreCase("true")) {
            return Boolean.TRUE;
         } else if (value.toString().trim().equalsIgnoreCase("false")) {
            return Boolean.FALSE;
         }
      }

      if (constraint.getType() == ConstraintType.DateTime) {
         if (value instanceof Date) {
            return value;
         }

         DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", locale);
         try {
            return df.parse(value.toString().trim());
         } catch (ParseException e) {
            return value;
         }
      }

      return value;
   }
}
