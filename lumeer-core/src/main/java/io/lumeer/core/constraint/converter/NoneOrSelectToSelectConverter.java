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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class NoneOrSelectToSelectConverter extends AbstractTranslatingConverter {

   @Override
   @SuppressWarnings("unchecked")
   void initTranslationsTable(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      this.ignoreMissing = true;

      if (isConstraintWithConfig(toAttribute)) {
         Map<String, Object> config = (Map<String, Object>) toAttribute.getConstraint().getConfig();
         this.translateToArray = (fromAttribute.getConstraint() == null || fromAttribute.getConstraint().getType() == null || fromAttribute.getConstraint().getType().equals(ConstraintType.None))
               && (Boolean) Objects.requireNonNullElse(config.get("multi"), false);

         if (onlyOptionsChanged(fromAttribute, toAttribute)) {
            List<Map<String, Object>> options = getConfigOptions(config);

            if (options != null) {
               options.forEach(opt -> {
                  var displayValue = opt.get("displayValue");
                  if (displayValue != null && !"".equals(displayValue)) {
                     translations.put(opt.get("displayValue").toString(), opt.get("value"));
                  }
               });
            }
         }
      }
   }

   @SuppressWarnings("unchecked")
   private boolean onlyOptionsChanged(Attribute fromAttribute, Attribute toAttribute) {
      if (fromAttribute.getConstraint() == null || fromAttribute.getConstraint().getType().equals(ConstraintType.None) || fromAttribute.getConstraint().getConfig() == null) {
         return true;
      }

      Map<String, Object> previousConfig = (Map<String, Object>) fromAttribute.getConstraint().getConfig();
      Map<String, Object> config = (Map<String, Object>) toAttribute.getConstraint().getConfig();

      return !Objects.deepEquals(getConfigOptions(config), getConfigOptions(previousConfig));
   }

   @SuppressWarnings("unchecked")
   private List<Map<String, Object>> getConfigOptions(Map<String, Object> config) {
      return (List<Map<String, Object>>) config.get("options");
   }

   @Override
   public Set<ConstraintType> getFromTypes() {
      return Set.of(ConstraintType.None, ConstraintType.Select);
   }

   @Override
   public Set<ConstraintType> getToTypes() {
      return Set.of(ConstraintType.Select);
   }
}
