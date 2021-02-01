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
package io.lumeer.core.util;

import io.lumeer.engine.api.data.DataDocument;

import com.mongodb.client.model.geojson.GeoJsonObjectType;
import com.mongodb.client.model.geojson.Point;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataUtils {

   private DataUtils() {
   }

   private static final DecimalFormat dfFullFraction = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

   static {
      dfFullFraction.setMaximumFractionDigits(340); // 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
   }

   public static String convertPointToString(Object value) {
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
            return convertDoubleToString(values.get(0)) + ", " + convertDoubleToString(values.get(1));
         }
      }

      return null;
   }

   public static String convertDoubleToString(final Double d) {
      return d != null ? dfFullFraction.format(d) : null;
   }

}
