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
import io.lumeer.api.model.ConstraintType;

import java.util.ArrayList;
import java.util.List;

public class ConstraintConverterFactory {

   private List<ConstraintConverter> converters = new ArrayList<>();

   private ConstraintManager constraintManager;
   private String userLocale;

   public ConstraintConverterFactory(ConstraintManager constraintManager, String userLocale) {
      this.constraintManager = constraintManager;
      this.userLocale = userLocale;

      registerConverter(new NoneOrSelectToSelectConverter());
      registerConverter(new SelectToNoneConverter());
      registerConverter(new NoneToColorConverter());
      registerConverter(new NoneToDurationConverter());
      registerConverter(new DurationToNoneConverter());
      registerConverter(new NoneToDateConverter());
      registerConverter(new DateToNoneConverter());
      registerConverter(new NoneToPercentageConverter());
      registerConverter(new NoneToBooleanConverter());
   }

   public ConstraintConverter getConstraintConverter(final Attribute fromAttribute, final Attribute toAttribute) {
      final ConstraintType fromType = fromAttribute != null && fromAttribute.getConstraint() != null ? fromAttribute.getConstraint().getType() : ConstraintType.None;
      final ConstraintType toType = toAttribute != null && toAttribute.getConstraint() != null ? toAttribute.getConstraint().getType() : ConstraintType.None;

      final ConstraintConverter converter = converters.stream().filter(c -> c.getFromTypes().contains(fromType) && c.getToTypes().contains(toType)).findFirst().orElse(null);

      if (converter != null) {
         converter.init(constraintManager, userLocale, fromAttribute, toAttribute);
      }

      return converter;
   }

   private void registerConverter(final ConstraintConverter constraintConverter) {
      converters.add(constraintConverter);
   }
}
