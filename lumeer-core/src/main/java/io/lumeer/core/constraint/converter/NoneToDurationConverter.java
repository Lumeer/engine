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

import io.lumeer.api.model.ConstraintType;
import io.lumeer.engine.api.data.DataDocument;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoneToDurationConverter extends AbstractDurationConverter {

   @Override
   public Set<ConstraintType> getFromTypes() {
      return Set.of(ConstraintType.None);
   }

   @Override
   public Set<ConstraintType> getToTypes() {
      return Set.of(ConstraintType.Duration);
   }

   @Override
   public DataDocument getPatchDocument(DataDocument document) {
      if (document.containsKey(toAttribute.getId())) {
         var originalValue = document.get(fromAttribute.getId());

         if (originalValue != null) {

            if (originalValue instanceof Number) {
               return null;
            }

            var originalValueStr = originalValue.toString();

            if (constraintManager.isNumber(originalValueStr)) {
               return null;
            }

            var accum = new AtomicLong(0);

            conversions.forEach((k, v) -> {
               final Pattern p = Pattern.compile("[0-9]+" + k);
               final Matcher m = p.matcher(originalValueStr);

               int lastEnd = 0;
               while (m.find()) {
                  var num = originalValueStr.substring(m.start(), m.end() - 1);

                  try {
                     accum.addAndGet(Long.parseLong(num) * v);
                  } catch (NumberFormatException nfe) {
                     // nps
                  }
               }
            });

            if (!originalValue.equals(Long.valueOf(accum.get()).toString())) {
               return new DataDocument(toAttribute.getId(), accum.get());
            }
         }
      }
      return null;
   }
}
