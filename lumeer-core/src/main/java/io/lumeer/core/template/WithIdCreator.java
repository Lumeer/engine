/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.core.template;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.common.WithId;

import java.util.Iterator;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class WithIdCreator {

   protected final TemplateParser templateParser;

   protected WithIdCreator(final TemplateParser templateParser) {
      this.templateParser = templateParser;
   }

   protected void registerAttributes(final WithId resource, final java.util.Collection<Attribute> storedAttributes, final java.util.Collection<Attribute> templateAttributes) {
      final Iterator<Attribute> i1 = storedAttributes.iterator();
      final Iterator<Attribute> i2 = templateAttributes.iterator();
      while (i1.hasNext() && i2.hasNext()) {
         var a1 = i1.next();
         var a2 = i2.next();
         templateParser.getDict().addAttribute(resource, a2.getId(), a1);
      }
   }
}
