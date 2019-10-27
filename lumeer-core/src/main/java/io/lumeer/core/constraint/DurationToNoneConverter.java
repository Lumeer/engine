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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationToNoneConverter extends AbstractDurationConverter {

   private static class DurationUnit {
      int value;
      String name;

      public DurationUnit(int value, String name) {
         this.value = value;
         this.name = name;
      }
   }

   private static class DurationUnitComparator implements java.util.Comparator<DurationUnit> {
      @Override
      public int compare(DurationUnit o1, DurationUnit o2) {
         return o2.value - o1.value;
      }
   }

   @Override
   public Set<ConstraintType> getFromTypes() {
      return Set.of(ConstraintType.Duration);
   }

   @Override
   public Set<ConstraintType> getToTypes() {
      return Set.of(ConstraintType.None);
   }

   @Override
   public DataDocument getPatchDocument(DataDocument document) {
      if (document.containsKey(toAttribute.getId())) {
         var originalValue = document.get(fromAttribute.getId());

         if (originalValue != null) {

            if (!(originalValue instanceof Number)) {
               return null;
            }

            long originalValueLong = ((Number) originalValue).longValue();

            var units = new ArrayList<DurationUnit>(conversions.size());

            conversions.forEach((k, v) -> units.add(new DurationUnit(v, k)));
            Collections.sort(units, new DurationUnitComparator());

            final StringBuilder sb = new StringBuilder();
            var rem = originalValueLong;
            for (int i = 0; i < units.size(); i++) {
               var m = rem / units.get(i).value;
               rem = rem % units.get(i).value;

               if (m > 0) {
                  sb.append(m).append(translations.get(units.get(i).name));
               }
            }

            return new DataDocument(toAttribute.getId(), sb.toString());
         }
      }
      return null;
   }
}
