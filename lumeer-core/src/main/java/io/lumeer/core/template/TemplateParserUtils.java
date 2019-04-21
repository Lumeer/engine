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
package io.lumeer.core.template;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
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
      attr.setId(getId((JSONObject) o));

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

      if (relative != null && !"".equals(relative)) {
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
}
