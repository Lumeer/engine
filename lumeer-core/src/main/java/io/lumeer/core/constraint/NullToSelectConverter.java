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
import io.lumeer.api.model.ConstraintType;
import io.lumeer.engine.api.data.DataDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NullToSelectConverter extends AbstractConstraintConverter {

   private Map<String, String> translations = new HashMap<>();

   @Override
   @SuppressWarnings("unchecked")
   public void init(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      super.init(cm, userLocale, fromAttribute, toAttribute);

      if (toAttribute != null && toAttribute.getConstraint() != null && toAttribute.getConstraint().getConfig() != null) {
         Map<String, Object> config = (Map<String, Object>) toAttribute.getConstraint().getConfig();
         List<Map<String, Object>> options = (List<Map<String, Object>>) config.get("options");

         if (options != null) {
            options.forEach(opt -> {
               if (opt.get("displayValue") != null) {
                  translations.put(opt.get("displayValue").toString(), opt.get("value").toString());
               }
            });
         }
      }
   }

   @Override
   public ConstraintType getFromType() {
      return null;
   }

   @Override
   public ConstraintType getToType() {
      return ConstraintType.Select;
   }

   @Override
   public DataDocument getPatchDocument(DataDocument document) {
      if (translations.size() > 0) {
         if (document.containsKey(toAttribute.getId())) {
            var originalValue = document.get(fromAttribute.getId()).toString();

            if (originalValue != null) {
               var newValue = translations.getOrDefault(originalValue, "");

               return new DataDocument("$set", new DataDocument(toAttribute.getId(), newValue));
            }
         }
      }
      return null;
   }
}
