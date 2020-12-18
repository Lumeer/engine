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
import io.lumeer.core.util.MomentJsParser;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public abstract class AbstractDateConverter extends AbstractConstraintConverter {

   protected String format;
   protected String locale;
   protected boolean initialized = false;
   protected String userLocale;

   @Override
   @SuppressWarnings("unchecked")
   public void init(ConstraintManager cm, String userLocale, Attribute fromAttribute, Attribute toAttribute) {
      super.init(cm, userLocale, fromAttribute, toAttribute);

      if (isConstraintWithConfig(toAttribute) || isConstraintWithConfig(fromAttribute)) {
         var attr = isConstraintWithConfig(toAttribute) && toAttribute.getConstraint().getType() == ConstraintType.DateTime ? toAttribute : fromAttribute;

         var config = (Map<String, Object>) attr.getConstraint().getConfig();
         var format = config.get("format").toString();

         if (StringUtils.isNotEmpty(format)) {
            this.format = format;
            this.userLocale = userLocale;
            initialized = true;
         }
      }
   }

   public void close() {
      super.close();
   }
}
