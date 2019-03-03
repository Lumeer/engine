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
package io.lumeer.core.constraint;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.LinkType;
import io.lumeer.engine.api.data.DataDocument;

import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

   private Set<DateTimeFormatter> formatters;

   private DateTimeFormatter dateDecoder;

   private static final ZoneId utcZone = ZoneId.ofOffset("UTC", ZoneOffset.UTC);

   /**
    * Gets the currently used locale.
    *
    * @param locale
    *       The currently used locale.
    */
   public void setLocale(final Locale locale) {
      this.locale = locale;
      initNumberMatchPatten(locale);
      initDateTimeFormatters(locale);
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

      this.numberMatch = Pattern.compile("^[-+]?\\d+([\\.,]\\d+)?([Ee][+-]?\\d+)?$");
   }

   private void initDateTimeFormatters(final Locale locale) {
      dateDecoder = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", locale);
      formatters = Set.of(
            dateDecoder,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSO", locale),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSx", locale),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX", locale)
      );
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
         final Number n = encodeNumber(locale, ((String) value).replaceFirst(",", "."));
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

         DateTimeFormatter dtf;
         for (final Iterator<DateTimeFormatter> i = formatters.iterator(); i.hasNext(); ) {
            dtf = i.next();
            try {
               return Date.from(ZonedDateTime.from(dtf.parse(value.toString().trim())).toInstant());
            } catch (DateTimeParseException e) {
               // no problem, we will try another
            }
         }
      }

      return value;
   }

   public Object decode(final Object value, final Constraint constraint) {
      if (value != null) {
         if (value instanceof Date) {
            final ZonedDateTime dt = ZonedDateTime.from(((Date) value).toInstant().atZone(utcZone));
            try {
               return dateDecoder.format(dt);
            } catch (DateTimeException dte) {
               return value;
            }
         }

         if (value instanceof BigDecimal) {
            return value.toString();
         }
      }

      return value;
   }

   private Map<String, Constraint> getConstraints(final Collection collection) {
      return collection.getAttributes()
                 .stream()
                 .filter(attr -> attr.getId() != null && attr.getConstraint() != null)
                 .collect(Collectors.toMap(Attribute::getId, Attribute::getConstraint));
   }

   public void encodeDataTypes(final Collection collection, final DataDocument data) {
      processData(data, getConstraints(collection), this::encode);
   }

   public void decodeDataTypes(final Collection collection, final DataDocument data) {
      processData(data, getConstraints(collection), this::decode);
   }

   private Map<String, Constraint> getConstraints(final LinkType linkType) {
      return linkType.getAttributes()
                       .stream()
                       .filter(attr -> attr.getId() != null && attr.getConstraint() != null)
                       .collect(Collectors.toMap(Attribute::getId, Attribute::getConstraint));
   }

   public void encodeDataTypes(final LinkType linkType, final DataDocument data) {
      processData(data, getConstraints(linkType), this::encode);
   }

   public void decodeDataTypes(final LinkType linkType, final DataDocument data) {
      processData(data, getConstraints(linkType), this::decode);
   }

   private void processData(final DataDocument data, final Map<String, Constraint> constraints, final BiFunction<Object, Constraint, Object> processor) {
      data.keySet().forEach(key -> {
         if (!DataDocument.ID.equals(key)) {
            data.put(key, processor.apply(data.get(key), constraints.get(key)));
         }
      });
   }
}
