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
package io.lumeer.api.util;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Constraint;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResourceUtils {

   private ResourceUtils() {
   }

   public static java.util.Collection<Attribute> incOrDecAttributes(java.util.Collection<Attribute> attributes, Set<String> attributesIdsToInc, Set<String> attributesIdsToDec) {
      Map<String, Attribute> oldAttributes = attributes.stream()
                                                       .collect(Collectors.toMap(Attribute::getId, Function.identity()));
      oldAttributes.keySet().forEach(attributeId -> {
         if (attributesIdsToInc.contains(attributeId)) {
            Attribute attribute = oldAttributes.get(attributeId);
            attribute.setUsageCount(attribute.getUsageCount() + 1);
         } else if (attributesIdsToDec.contains(attributeId)) {
            Attribute attribute = oldAttributes.get(attributeId);
            attribute.setUsageCount(Math.max(attribute.getUsageCount() - 1, 0));
         }

      });

      return oldAttributes.values();
   }

   public static java.util.Collection<Attribute> incAttributes(java.util.Collection<Attribute> attributes, Map<String, Integer> attributesToInc) {
      Map<String, Attribute> oldAttributes = attributes.stream()
                                                       .collect(Collectors.toMap(Attribute::getId, Function.identity()));
      oldAttributes.keySet().forEach(attributeId -> {
            Attribute attribute = oldAttributes.get(attributeId);
            attribute.setUsageCount(attribute.getUsageCount() + attributesToInc.computeIfAbsent(attributeId, aId -> 0));
      });

      return oldAttributes.values();
   }


   public static Constraint findConstraint(java.util.Collection<Attribute> attributes, String attributeId) {
      Attribute attribute = findAttribute(attributes, attributeId);
      if (attribute != null) {
         return attribute.getConstraint();
      }
      return null;
   }

   public static Attribute findAttribute(java.util.Collection<Attribute> attributes, String attributeId) {
      if (attributes == null || attributeId == null) {
         return null;
      }
      for (Attribute attribute : attributes) {
         if (attribute.getId().equals(attributeId)) {
            return attribute;
         }
      }
      return null;
   }

   public static Attribute findAttributeByName(java.util.Collection<Attribute> attributes, String attributeName) {
      if (attributes == null || attributeName == null) {
         return null;
      }
      for (Attribute attribute : attributes) {
         if (attribute.getName().equals(attributeName)) {
            return attribute;
         }
      }
      return null;
   }
}
