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
import io.lumeer.core.constraint.manager.ConstraintManager;

public abstract class AbstractConstraintConverter implements ConstraintConverter {

   protected ConstraintManager constraintManager;
   protected String userLocale;
   Attribute fromAttribute;
   Attribute toAttribute;

   @Override
   public void init(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      this.constraintManager = cm;
      this.userLocale = userLocale;
      this.fromAttribute = fromAttribute;
      this.toAttribute = toAttribute;
   }

   protected boolean isConstraintWithConfig(final Attribute attribute) {
      return attribute != null && attribute.getConstraint() != null && attribute.getConstraint().getConfig() != null;
   }
}
