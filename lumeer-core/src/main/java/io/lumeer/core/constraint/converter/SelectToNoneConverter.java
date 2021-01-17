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

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectToNoneConverter extends AbstractTranslatingConverter { ;

   @Override
   @SuppressWarnings("unchecked")
   void initTranslationsTable(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      if (isConstraintWithConfig(fromAttribute)) {
         this.translateFromArray = true;
         Map<String, Object> config = (Map<String, Object>) fromAttribute.getConstraint().getConfig();
         List<Map<String, Object>> options = (List<Map<String, Object>>) config.get("options");

         if (options != null) {
            options.forEach(opt -> {
               var displayValue = opt.get("displayValue");
               if (displayValue != null && !"".equals(displayValue)) {
                  translations.put(opt.get("value").toString(), displayValue.toString());
               }
            });
         }
      }
   }

   @Override
   public Set<ConstraintType> getFromTypes() {
      return Set.of(ConstraintType.Select);
   }

   @Override
   public Set<ConstraintType> getToTypes() {
      return Set.of(ConstraintType.None);
   }
}
