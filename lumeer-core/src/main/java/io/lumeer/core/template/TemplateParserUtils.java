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
package io.lumeer.core.template;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TemplateParserUtils {

   public static List<Attribute> getAttributes(final JSONArray a) {
      final var attrs = new ArrayList<Attribute>();

      a.forEach(o -> {
         attrs.add(getAttribute((JSONObject) o));
      });

      return attrs;
   }

   private static Attribute getAttribute(final JSONObject o) {
      final var attr = new Attribute((String) o.get(Attribute.NAME));
      attr.setId((String) o.get(Attribute.ID));
      attr.setDescription((String) o.get(Attribute.DESCRIPTION));

      if (o.get("constraint") != null) {
         attr.setConstraint(getAttributeConstraint((JSONObject) o.get("constraint")));
      }

      return attr;
   }

   private static Constraint getAttributeConstraint(final JSONObject o) {
      return new Constraint(ConstraintType.valueOf((String) o.get("type")), o.get("config"));
   }

   public static String getId(final JSONObject o) {
      return (String) o.get("_id");
   }

   public static String getDate(final DayOfWeek preferredDay, final String relative) {
      LocalDateTime now = LocalDateTime.now();

      if (StringUtils.isNotEmpty(relative)) {
         int offset = Integer.parseInt(relative.substring(1, relative.length() - 1));
         TemporalUnit temporal = getTemporalUnit(relative.substring(relative.length() - 1));

         if (relative.startsWith("-")) {
            now = now.minus(offset, temporal);
         } else if (relative.startsWith("+")) {
            now = now.plus(offset, temporal);
         }
      }

      if (preferredDay != null) {
         while (!now.getDayOfWeek().equals(preferredDay)) {
            now = now.plusDays(1);
         }
      }

      return now.toString();
   }

   private static TemporalUnit getTemporalUnit(final String unit) {
      switch (unit) {
         case "y":
            return ChronoUnit.YEARS;
         case "m":
            return ChronoUnit.MONTHS;
         case "w":
            return ChronoUnit.WEEKS;
         case "d":
            return ChronoUnit.DAYS;
         default:
            return ChronoUnit.HOURS;
      }
   }

   public static String replacer(final String text, final String matchPrefix, final String matchSuffix, final java.util.function.Function<String, String> replacer) {
      var pattern = Pattern.compile("(" + matchPrefix + ")([0-9a-f]{24})(" + matchSuffix + ")");
      var matcher = pattern.matcher(text);
      var sb = new StringBuilder();

      int lastEnd = 0;
      while (matcher.find()) {
         sb.append(text.substring(lastEnd, matcher.start()));
         sb.append(text.substring(matcher.start(1), matcher.end(1)));
         sb.append(replacer.apply(text.substring(matcher.start(2), matcher.end(2))));
         sb.append(text.substring(matcher.start(3), matcher.end(3)));
         lastEnd = matcher.end();
      }
      sb.append(text.substring(lastEnd, text.length()));

      return sb.toString();
   }
}
