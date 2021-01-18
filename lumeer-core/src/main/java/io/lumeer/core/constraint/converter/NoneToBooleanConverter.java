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

import java.util.Set;

public class NoneToBooleanConverter extends AbstractTranslatingConverter {

   @Override
   @SuppressWarnings("unchecked")
   void initTranslationsTable(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      this.ignoreMissing = true;

      if (isConstraintWithConfig(toAttribute)) {
         translations.put("true", Boolean.TRUE);
         translations.put("yes", Boolean.TRUE);
         translations.put("ja", Boolean.TRUE);
         translations.put("ano", Boolean.TRUE);
         translations.put("áno", Boolean.TRUE);
         translations.put("sí", Boolean.TRUE);
         translations.put("si", Boolean.TRUE);
         translations.put("sim", Boolean.TRUE);
         translations.put("да", Boolean.TRUE);
         translations.put("是", Boolean.TRUE);
         translations.put("はい", Boolean.TRUE);
         translations.put("vâng", Boolean.TRUE);
         translations.put("כן", Boolean.TRUE);
         translations.put("1", Boolean.TRUE);
         translations.put("false", Boolean.FALSE);
         translations.put("0", Boolean.FALSE);
      }
   }

   @Override
   public Set<ConstraintType> getFromTypes() {
      return Set.of(ConstraintType.None);
   }

   @Override
   public Set<ConstraintType> getToTypes() {
      return Set.of(ConstraintType.Boolean);
   }
}
