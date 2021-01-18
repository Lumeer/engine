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
package io.lumeer.core.constraint.converter;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.core.constraint.manager.ConstraintManager;

import java.util.Map;

public abstract class AbstractDurationConverter extends AbstractConstraintConverter {

   protected Map<String, Integer> conversions;
   protected Map<String, String> translations;

   @Override
   @SuppressWarnings("unchecked")
   public void init(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      super.init(cm, userLocale, fromAttribute, toAttribute);

      translations = "cs".equals(userLocale) ?
              Map.of("w", "t", "d", "d", "h", "h", "m", "m", "s", "s") :
              Map.of("w", "w", "d", "d", "h", "h", "m", "m", "s", "s");

      if (isConstraintWithConfig(toAttribute) || isConstraintWithConfig(fromAttribute)) {
         var attr = isConstraintWithConfig(toAttribute) && toAttribute.getConstraint().getType() == ConstraintType.Duration ? toAttribute : fromAttribute;

         var config = (Map<String, Object>) attr.getConstraint().getConfig();
         var durationType = config.get("type").toString(); // Work, Classic, Custom
         int week = 0, day = 0, hour = 0, minute = 0, second = 0;

         if ("Custom".equalsIgnoreCase(durationType)) {
            var conversions = (Map<String, Integer>) config.get("conversions");
            second = conversions.get("s");
            minute = conversions.get("m") * second;
            hour = conversions.get("h") * minute;
            day = conversions.get("d") * hour;
            week = conversions.get("w") * day;
         } else if ("Classic".equalsIgnoreCase(durationType)) {
            second = 1000;
            minute = 60 * second;
            hour = 60 * minute;
            day = 24 * hour;
            week = 7 * day;
         } else if ("Work".equalsIgnoreCase(durationType)) {
            second = 1000;
            minute = 60 * second;
            hour = 60 * minute;
            day = 8 * hour;
            week = 5 * day;
         }

         conversions = Map.of(translations.get("w"), week, translations.get("d"), day, translations.get("h"), hour, translations.get("m"), minute, translations.get("s"), second);
      }
   }
}
