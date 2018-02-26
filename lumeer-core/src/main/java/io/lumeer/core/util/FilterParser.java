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
package io.lumeer.core.util;

import io.lumeer.api.model.ConditionType;
import io.lumeer.storage.api.filter.AttributeFilter;

public class FilterParser {

   public static AttributeFilter parse(String filter) {
      String[] parts = filter.split(":", 3);
      if (parts.length < 3) {
         return null;
      }

      String collectionId = parts[0].trim();
      String attributeName = parts[1].trim();
      String condition = parts[2].trim();

      String[] conditionParts = condition.split(" +", 2); // one or more spaces
      if (conditionParts.length < 2) {
         return null;
      }

      ConditionType conditionType = ConditionType.fromString(conditionParts[0].trim().toLowerCase());
      if (conditionType == null) {
         return null;
      }

      String value = conditionParts[1].trim();

      return new AttributeFilter(collectionId, conditionType, attributeName, value);
   }

}
