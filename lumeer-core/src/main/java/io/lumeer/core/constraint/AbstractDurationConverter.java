package io.lumeer.core.constraint;/*
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

import io.lumeer.api.model.Attribute;

import java.util.Map;

public abstract class AbstractDurationConverter extends AbstractConstraintConverter {

   protected Map<String, Long> conversions;

   @Override
   @SuppressWarnings("unchecked")
   public void init(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      if (isConstraintWithConfig(toAttribute)) {
         var config = (Map<String, Object>) toAttribute.getConstraint().getConfig();
         var durationType = config.get("type").toString(); // Work, Classic, Custom
         long week = 0, day = 0, hour = 0, minute = 0, second = 0;

         if ("Custom".equalsIgnoreCase(durationType)) {
            var conversions = (Map<String, Long>) config.get("conversions");
            second = conversions.get("s");
            minute = conversions.get("m") * second;
            hour = conversions.get("h") * minute;
            day = conversions.get("d") * hour;
            week = conversions.get("w") * day;
         } else if ("Classic".equalsIgnoreCase(durationType)) {
            second = 1000L;
            minute = 60 * second;
            hour = 60 * minute;
            day = 24 * hour;
            week = 7 * day;
         } else if ("Work".equalsIgnoreCase(durationType)) {
            second = 1000L;
            minute = 60 * second;
            hour = 60 * minute;
            day = 8 * hour;
            week = 5 * day;
         }

         conversions = "cs".equals(userLocale) ?
               Map.of("t", week, "d", day, "h", hour, "m", minute, "s", second) :
               Map.of("w", week, "d", day, "h", hour, "m", minute, "s", second);
      }
   }
}
