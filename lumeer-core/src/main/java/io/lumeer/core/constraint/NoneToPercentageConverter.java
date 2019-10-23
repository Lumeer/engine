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

import io.lumeer.api.model.ConstraintType;
import io.lumeer.engine.api.data.DataDocument;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoneToPercentageConverter extends AbstractConstraintConverter {

   @Override
   public Set<ConstraintType> getFromTypes() {
      return Set.of(ConstraintType.None);
   }

   @Override
   public Set<ConstraintType> getToTypes() {
      return Set.of(ConstraintType.Percentage);
   }

   @Override
   public DataDocument getPatchDocument(DataDocument document) {
      if (document.containsKey(toAttribute.getId())) {
         var originalValue = document.get(fromAttribute.getId());

         if (originalValue instanceof String) {

            var originalValueStr = ((String) originalValue).trim();

            if (originalValueStr.indexOf("%") == originalValueStr.length() - 1) { // ends with %
               var num = originalValueStr.substring(0, originalValueStr.length() - 1).trim();
               if (constraintManager.isNumber(num)) {
                  var res = constraintManager.encode(num);

                  if (res instanceof BigDecimal) {
                     res = ((BigDecimal) res).movePointLeft(2);
                  } else if (res instanceof Double) {
                     res = (double) res / 100d;
                  } else if (res instanceof Long) {
                     res = (long) res / 100d;
                  } else {
                     return null;
                  }

                  return new DataDocument(toAttribute.getId(), res);
               }
            }
         }
      }
      return null;
   }
}
