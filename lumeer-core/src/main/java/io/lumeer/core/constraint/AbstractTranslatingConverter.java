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
import io.lumeer.engine.api.data.DataDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractTranslatingConverter extends AbstractConstraintConverter {

   protected Map<String, Object> translations = new HashMap<>();
   protected boolean ignoreMissing = false;

   protected boolean translateFromArray = false;
   protected boolean translateToArray = false;

   @Override
   public void init(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      super.init(cm, userLocale, fromAttribute, toAttribute);

      initTranslationsTable(cm, userLocale, fromAttribute, toAttribute);
   }

   abstract void initTranslationsTable(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute);

   @Override
   public DataDocument getPatchDocument(DataDocument document) {
      if (!translations.isEmpty() || translateFromArray || translateToArray) {
         if (document.containsKey(toAttribute.getId())) {
            var originalValue = document.get(fromAttribute.getId());

            if (originalValue != null) {
               var translatedArray = translateArray(originalValue);
               if (translatedArray != null) {
                  return translatedArray;
               }

               return translate(originalValue);
            }
         }
      }
      return null;
   }

   private DataDocument translateArray(Object originalValue) {
      if (translateFromArray && originalValue instanceof List<?>) {
         var originalValues = ((List<?>) originalValue).stream()
                                                       .map(value -> translations.getOrDefault(value.toString(), value.toString()).toString())
                                                       .collect(Collectors.toList());

         return new DataDocument(toAttribute.getId(), String.join(", ", originalValues));
      } else if (translateToArray && originalValue instanceof String) {
         var stringList = Stream.of(originalValue.toString().split(","))
                                .map(String::trim)
                                .map(value -> translations.getOrDefault(value, value))
                                .collect(Collectors.toList());
         return new DataDocument(toAttribute.getId(), stringList);
      }

      return null;
   }

   private DataDocument translate(Object originalValue) {
      if (!translations.isEmpty()) {
         var newValue = translations.getOrDefault(originalValue.toString(), ignoreMissing ? originalValue : "");

         if (!newValue.equals(originalValue.toString())) {
            return new DataDocument(toAttribute.getId(), newValue);
         }
      }
      return null;
   }
}
