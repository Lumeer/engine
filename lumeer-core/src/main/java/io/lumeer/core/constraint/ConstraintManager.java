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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.coordinates.CoordinatesParser;
import io.lumeer.core.util.coordinates.LatLng;
import io.lumeer.engine.api.data.DataDocument;

import com.mongodb.client.model.geojson.GeoJsonObjectType;
import com.mongodb.client.model.geojson.NamedCoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Holds a list of constraints that can be obtained from a list of string configurations.
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

   private Set<DateTimeFormatter> formatters;

   private DateTimeFormatter dateDecoder;

   private static final ZoneId utcZone = ZoneId.ofOffset("UTC", ZoneOffset.UTC);

   private static final DecimalFormat dfFullFraction = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

   static {
      dfFullFraction.setMaximumFractionDigits(340); // 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
   }

   /**
    * Obtains a default instance of ConstraintManager configured according to system properties.
    *
    * @param configurationProducer
    *       A provider of configuration.
    * @return The default ConstraintManager
    */
   public static ConstraintManager getInstance(final DefaultConfigurationProducer configurationProducer) {
      final ConstraintManager constraintManager = new ConstraintManager();
      final String locale = configurationProducer.get(DefaultConfigurationProducer.LOCALE);

      if (StringUtils.isNotEmpty(locale)) {
         constraintManager.setLocale(Locale.forLanguageTag(locale));
      } else {
         constraintManager.setLocale(Locale.getDefault());
      }

      return constraintManager;
   }

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
         final String trimmed = ((String) value).trim();

         if (trimmed.matches("^0[^\\.].*")) { // we need to keep leading and trailing zeros, so no conversion to number
            return null;
         }

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
         final Number n = encodeNumber(locale, ((String) value).replaceFirst(",", ".").replace("e", "E"));
         return n == null ? value : n;
      }

      return value;
   }

   public Object encode(final Object value, final Constraint constraint) {
      return encode(value, constraint, false);
   }

   public Object encodeForFce(final Object value, final Constraint constraint) {
      return encode(value, constraint, true);
   }

   private Object encode(final Object value, final Constraint constraint, final boolean tryHard) {
      if (locale == null) {
         throw new IllegalStateException("No locale was set in ConstraintManager. Please use function setLocale() so it can encode correctly.");
      }

      if (value == null) {
         return null;
      }

      if (!tryHard && (constraint == null || constraint.getType() == ConstraintType.Number)) {
         return encode(value);
      }

      if (tryHard || (constraint != null && (constraint.getType() == ConstraintType.Percentage || constraint.getType() == ConstraintType.Duration))) {
         if (value instanceof BigDecimal || value instanceof Long || value instanceof Double) {
            return value;
         }

         if (value instanceof String) {
            if (constraint != null && constraint.getType() == ConstraintType.Duration) {
               return encode(value);
            } else {

               final String strValue = ((String) value).replace(" ", "").trim();

               if (strValue.endsWith("%") && strValue.indexOf("%") == strValue.length() - 1) {
                  final Object result = encode(strValue.substring(0, strValue.length() - 1));

                  if (result instanceof BigDecimal) {
                     return ((BigDecimal) result).movePointLeft(2);
                  } else if (result instanceof Long) {
                     return BigDecimal.valueOf((Long) result).movePointLeft(2);
                  }
               }
            }
         }

         if (!tryHard) {
            return encode(value);
         }
      }

      if (tryHard || (constraint != null && constraint.getType() == ConstraintType.Boolean)) {
         if (value instanceof Boolean) {
            return value;
         } else if (value.toString().trim().equalsIgnoreCase("true")) {
            return Boolean.TRUE;
         } else if (value.toString().trim().equalsIgnoreCase("false")) {
            return Boolean.FALSE;
         }
      }

      if (tryHard || (constraint != null && constraint.getType() == ConstraintType.DateTime)) {
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

      if (constraint != null && constraint.getType() == ConstraintType.Select) {
         var numericValue = encodeNumber(NumberFormat.getIntegerInstance(), NumberFormat.getIntegerInstance(), value);
         return numericValue != null && numericValue.toString().equals(String.valueOf(value)) ? numericValue : value;
      }

      if (tryHard || (constraint != null && constraint.getType() == ConstraintType.Coordinates)) {
         if (value instanceof Point) {
            return value;
         }

         if (value instanceof String) {
            final Optional<LatLng> latLng = CoordinatesParser.parseVerbatimCoordinates((String) value);

            if (latLng.isPresent()) {
               return new Point(NamedCoordinateReferenceSystem.EPSG_4326, new Position(latLng.get().getLat(), latLng.get().getLng()));
            }
         }
      }

      return tryHard ? encode(value) : value;
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

         if (value instanceof DataDocument) { // Point
            final String type = ((DataDocument) value).getString("type");
            if (type != null && type.equals("Point") && ((DataDocument) value).containsKey("coordinates")) {
               List<Double> values = ((DataDocument) value).getArrayList("coordinates", Double.class);
               return values.get(0) + ", " + values.get(1);
            }
         } else if (value instanceof Point) {
            final Point p = (Point) value;
            if (p.getType() == GeoJsonObjectType.POINT) {
               List<Double> values = p.getCoordinates().getValues();
               return doubleToString(values.get(0)) + ", " + doubleToString(values.get(1));
            }
         }
      }

      return value;
   }

   private Map<String, Constraint> getConstraints(final Collection collection) {
      if (collection == null) {
         return Collections.emptyMap();
      }
      return collection.getAttributes()
                       .stream()
                       .filter(attr -> attr.getId() != null && attr.getConstraint() != null)
                       .collect(Collectors.toMap(Attribute::getId, Attribute::getConstraint));
   }

   public Query encodeQuery(final Query query, final List<Collection> collections, final List<LinkType> linkTypes) {
      var queryCopy = new Query(new ArrayList<>(query.getStems()), query.getFulltexts(), query.getPage(), query.getPageSize());
      this.processQuery(queryCopy, collections, linkTypes, this::encode);
      return queryCopy;
   }

   public Query decodeQuery(final Query query, final List<Collection> collections, final List<LinkType> linkTypes) {
      var queryCopy = new Query(new ArrayList<>(query.getStems()), query.getFulltexts(), query.getPage(), query.getPageSize());
      this.processQuery(queryCopy, collections, linkTypes, this::decode);
      return queryCopy;
   }

   private void processQuery(final Query query, final List<Collection> collections, final List<LinkType> linkTypes, final BiFunction<Object, Constraint, Object> processor) {
      Map<String, Collection> collectionsMap = collections.stream().collect(Collectors.toMap(Resource::getId, c -> c));
      Map<String, LinkType> linkTypesMap = linkTypes.stream().collect(Collectors.toMap(LinkType::getId, c -> c));

      query.getAttributeFilters().forEach(filter -> {
         var collection = collectionsMap.get(filter.getCollectionId());
         if (collection != null) {
            var constraint = ResourceUtils.findConstraint(collection.getAttributes(), filter.getAttributeId());
            filter.setValue(processor.apply(filter.getValue(), constraint));
         }
      });

      query.getLinkAttributeFilters().forEach(filter -> {
         var linkType = linkTypesMap.get(filter.getLinkTypeId());
         if (linkType != null) {
            var constraint = ResourceUtils.findConstraint(linkType.getAttributes(), filter.getAttributeId());
            filter.setValue(processor.apply(filter.getValue(), constraint));
         }
      });
   }

   public DataDocument encodeDataTypes(final Collection collection, final DataDocument data) {
      return processData(data, getConstraints(collection), this::encode);
   }

   public DataDocument encodeDataTypesForFce(final Collection collection, final DataDocument data) {
      return processData(data, getConstraints(collection), this::encodeForFce);
   }

   public DataDocument decodeDataTypes(final Collection collection, final DataDocument data) {
      return processData(data, getConstraints(collection), this::decode);
   }

   private Map<String, Constraint> getConstraints(final LinkType linkType) {
      if (linkType == null) {
         return Collections.emptyMap();
      }
      return linkType.getAttributes()
                     .stream()
                     .filter(attr -> attr.getId() != null && attr.getConstraint() != null)
                     .collect(Collectors.toMap(Attribute::getId, Attribute::getConstraint));
   }

   public DataDocument encodeDataTypes(final LinkType linkType, final DataDocument data) {
      return processData(data, getConstraints(linkType), this::encode);
   }

   public DataDocument encodeDataTypesForFce(final LinkType linkType, final DataDocument data) {
      return processData(data, getConstraints(linkType), this::encodeForFce);
   }

   public DataDocument decodeDataTypes(final LinkType linkType, final DataDocument data) {
      return processData(data, getConstraints(linkType), this::decode);
   }

   private DataDocument processData(final DataDocument data, final Map<String, Constraint> constraints, final BiFunction<Object, Constraint, Object> processor) {
      if (data == null) {
         return null;
      }

      final DataDocument newData = new DataDocument();

      data.keySet().forEach(key -> {
         if (!DataDocument.ID.equals(key)) {
            newData.put(key, processor.apply(data.get(key), constraints.get(key)));
         } else {
            newData.put(key, data.get(key));
         }
      });

      return newData;
   }

   public DateTimeFormatter getDateDecoder() {
      return dateDecoder;
   }

   public boolean isNumber(final String value) {
      return numberMatch.matcher(value).matches();
   }

   private String doubleToString(final Double d) {
      return d != null ? dfFullFraction.format(d) : null;
   }
}
