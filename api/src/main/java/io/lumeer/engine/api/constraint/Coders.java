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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Encoders and decoders used by constraints.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public final class Coders {

   private Coders() {
      // no instance is allowed
   }

   /**
    * Gets to string encode function.
    *
    * @return A function that can encode only to String and tries to convert everything to String.
    */
   static BiFunction<Object, Class, Object> getToStringEncodeFunction() {
      return (o, t) -> {
         if (t != null && t != String.class) {
            return null;
         }

         if (o instanceof String) {
            return o;
         }

         return o.toString();
      };
   }

   /**
    * Gets a function to decode to string.
    *
    * @return A function that decodes everything to String.
    */
   static Function<Object, Object> getAsStringDecodeFunction() {
      return o -> {
         if (o instanceof String) {
            return o;
         }

         return o.toString();
      };
   }

   /**
    * Gets a function that encodes strings of given format to Date and keeps Date as is.
    *
    * @param format
    *       A format to parse string to Date.
    * @return A function that encodes strings of given format to Date and keeps Date as is.
    */
   static BiFunction<Object, Class, Object> getDateEncodeFunction(final DateFormat format) {
      return (o, t) -> {
         if (t != null && t != Date.class) {
            return null;
         }

         if (o instanceof Date) {
            return o;
         }

         try {
            return format.parse(o.toString().trim());
         } catch (ParseException e) {
            return null;
         }
      };
   }

   /**
    * Gets a function that decodes a String to Date based on the provided DateFormat.
    *
    * @param format
    *       A format to decode to Date.
    * @return A function that decodes a String to Date based on the provided DateFormat.
    */
   static Function<Object, Object> getDateDecodeFunction(final DateFormat format) {
      return o -> {
         if (o instanceof String) {
            return o;
         }

         if (o instanceof Date) {
            return format.format((Date) o);
         }

         return o.toString();
      };
   }

   /**
    * Gets a function that encodes enum values to string and tags to an array.
    *
    * @param oneOf
    *       Decodes to an array iff false, decodes to String iff true.
    * @return A function that encodes enum values to string and tags to an array.
    */
   static BiFunction<Object, Class, Object> getTagsEncodeFunction(final boolean oneOf) {
      return (o, t) -> {
         if (oneOf) {
            if (t == null || t == String.class) {
               return o.toString();
            }
         } else {
            if (t == null || String[].class.isAssignableFrom(t)) {
               return Arrays.asList(o.toString().split(",")).stream()
                            .map(String::trim)
                            .collect(Collectors.toList());
            }
         }

         return null;
      };
   }

   /**
    * Gets a function that decodes an array or set of String to a list.
    *
    * @return A function that decodes an array or set of String to a list.
    */
   static Function<Object, Object> getTagsDecodeFunction() {
      return o -> {
         if (o instanceof String[]) {
            return Arrays.asList((String[]) o);
         } else if (o instanceof List) {
            return o;
         } else if (o instanceof Set) {
            return new ArrayList<String>((Set) o);
         } else if (o instanceof String) {
            return o;
         }

         return o.toString();
      };
   }

   /**
    * Tries to convert the parameter to a number (either integer, double or big decimal) and return it.
    *
    * @param value
    *       The value to try to convert to number.
    * @return The value converted to a number data type or null when the conversion was not possible.
    */
   static Number encodeNumber(final Locale locale, final Object value) {
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
   static Number encodeNumber(final NumberFormat numberFormat, final NumberFormat bigNumberFormat, Object value) {
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
    * Gets a function that decodes String to a Number (integer, double or big decimal as needed).
    *
    * @param nf
    *       Number format to encode to integer or double.
    * @param big
    *       Number format to encode to big decimal.
    * @return A function that decodes String to a Number (integer, double or big decimal as needed).
    */
   static BiFunction<Object, Class, Object> getNumberEncodeFunction(final NumberFormat nf, final NumberFormat big) {
      return (o, t) -> {
         if (t != null && t != Number.class) {
            return null;
         }

         if (o instanceof Number) {
            return o;
         }

         final String trim = o.toString().replaceAll(" ", "");

         return encodeNumber(nf, big, trim);
      };
   }

   /**
    * Gets an identity decode function.
    *
    * @return An identity decode function.
    */
   static Function<Object, Object> getIdentityDecodeFunction() {
      return o -> o;
   }
}
