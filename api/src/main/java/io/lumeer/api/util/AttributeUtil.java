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

package io.lumeer.api.util;

import io.lumeer.api.model.Attribute;

import java.util.Set;

public class AttributeUtil {

   public static void renameChildAttributes(Set<? extends Attribute> attributes, String oldParentFullName, String newParentFullName) {
      String prefix = oldParentFullName + '.';
      attributes.forEach(attribute -> {
         if (attribute.getFullName().startsWith(prefix)) {
            renameChildAttribute(attribute, oldParentFullName, newParentFullName);
         }
      });
   }

   private static void renameChildAttribute(Attribute attribute, String oldParentFullName, String newParentFullName) {
      String[] parts = attribute.getFullName().split(oldParentFullName, 2);
      String newFullName = newParentFullName.concat(parts[1]);
      attribute.setFullName(newFullName);
   }

}
