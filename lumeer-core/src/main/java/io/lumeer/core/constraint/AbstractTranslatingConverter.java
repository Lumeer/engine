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
import java.util.Map;

public abstract class AbstractTranslatingConverter extends AbstractConstraintConverter {

   protected Map<String, Object> translations = new HashMap<>();
   protected boolean ignoreMissing = false;

   @Override
   public void init(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      super.init(cm, userLocale, fromAttribute, toAttribute);

      initTranslationsTable(cm, userLocale, fromAttribute, toAttribute);
   }

   abstract void initTranslationsTable(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute);

   @Override
   public DataDocument getPatchDocument(DataDocument document) {
      if (translations.size() > 0) {
         if (document.containsKey(toAttribute.getId())) {
            var originalValue = document.get(fromAttribute.getId()).toString();

            if (originalValue != null) {
               var newValue = translations.getOrDefault(originalValue, ignoreMissing ? originalValue : "");

               if (!newValue.equals(originalValue)) {
                  return new DataDocument(toAttribute.getId(), newValue);
               }
            }
         }
      }
      return null;
   }
}
