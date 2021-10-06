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
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.SelectOption;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AttributeUtil {

   public static String cleanAttributeName(String name) {
      StringBuilder sb = new StringBuilder();
      for (char letter : name.toCharArray()) {
         if (!Attribute.ILLEGAL_CHARS.contains(letter)) {
            sb.append(letter);
         }
      }
      return sb.toString();
   }

   public static boolean isEqualOrChild(Attribute attribute, String attributeName) {
      return attribute.getName().equals(attributeName) || attribute.getName().startsWith(attributeName + '.');
   }

   public static void renameChildAttributes(java.util.Collection<Attribute> attributes, String oldParentName, String newParentName) {
      String prefix = oldParentName + '.';
      attributes.forEach(attribute -> {
         if (attribute.getName().startsWith(prefix)) {
            renameChildAttribute(attribute, oldParentName, newParentName);
         }
      });
   }

   private static void renameChildAttribute(Attribute attribute, String oldParentName, String newParentName) {
      String[] parts = attribute.getName().split(oldParentName, 2);
      String newName = newParentName.concat(parts[1]);
      attribute.setName(newName);
   }

   public static java.util.Collection<Attribute> filterMostRelevantAttributes(java.util.Collection<Attribute> attributes, String text, int limit) {
      var distance = LevenshteinDistance.getDefaultInstance();
      return attributes.stream()
                       .filter(a -> a.getName().toLowerCase().contains(text))
                       .sorted(Comparator.comparingInt(a -> distance.apply(a.getName().toLowerCase(), text.toLowerCase())))
                       .limit(limit)
                       .collect(Collectors.toList());
   }

   public static AttributesDiff checkAttributesDiff(java.util.Collection<Attribute> originalAttributes, java.util.Collection<Attribute> currentAttributes) {
      Map<String, Attribute> originalAttributesMap = convertAttributesToMap(originalAttributes);
      Map<String, Attribute> currentAttributesMap = convertAttributesToMap(currentAttributes);
      Set<String> allAttributeIds = new HashSet<>(originalAttributesMap.keySet());
      allAttributeIds.addAll(currentAttributesMap.keySet());

      List<Attribute> createdFunction = new ArrayList<>();
      List<Attribute> updatedFunction = new ArrayList<>();
      List<String> removedFunction = new ArrayList<>();
      List<String> removedIds = new ArrayList<>();

      for (String attributeId : allAttributeIds) {
         if (originalAttributesMap.containsKey(attributeId) && !currentAttributesMap.containsKey(attributeId)) {
            removedIds.add(attributeId);
         } else if (!originalAttributesMap.containsKey(attributeId) && currentAttributesMap.containsKey(attributeId)) {
            Attribute attribute = currentAttributesMap.get(attributeId);
            if (attribute.getFunction() != null && attribute.getFunction().getJs() != null) {
               createdFunction.add(attribute);
            }
         } else {
            Attribute originalAttribute = originalAttributesMap.get(attributeId);
            Attribute currentAttribute = currentAttributesMap.get(attributeId);

            if (!functionIsDefined(originalAttribute) && functionIsDefined(currentAttribute)) {
               createdFunction.add(currentAttribute);
            } else if (functionIsDefined(originalAttribute) && !functionIsDefined(currentAttribute)) {
               removedFunction.add(attributeId);
            } else if (functionIsDefined(originalAttribute) && functionIsDefined(currentAttribute)) {
               if (!originalAttribute.getFunction().getXml().equals(currentAttribute.getFunction().getXml())) {
                  updatedFunction.add(currentAttribute);
               }
            }
         }

      }

      return new AttributesDiff(createdFunction, updatedFunction, removedFunction, removedIds);
   }

   public static boolean functionIsDefined(Attribute attribute) {
      return attribute.getFunction() != null && attribute.getFunction().getJs() != null && !attribute.getFunction().getJs().isEmpty();
   }

   private static Map<String, Attribute> convertAttributesToMap(java.util.Collection<Attribute> attributes) {
      if (attributes == null) {
         return new HashMap<>();
      }
      return new HashMap<>(attributes.stream().collect(Collectors.toMap(Attribute::getId, a -> a)));
   }

   public static boolean isConstraintWithConfig(final Attribute attribute) {
      return attribute != null && attribute.getConstraint() != null && attribute.getConstraint().getConfig() != null;
   }

   public static boolean isMultiselect(final Attribute attribute) {
      if (isConstraintWithConfig(attribute) && (attribute.getConstraint().getType() == ConstraintType.Select || attribute.getConstraint().getType() == ConstraintType.User)) {
         @SuppressWarnings("unchecked") final Map<String, Object> config = (Map<String, Object>) attribute.getConstraint().getConfig();
         return (Boolean) Objects.requireNonNullElse(config.get("multi"), false);
      }

      return false;
   }

   public static boolean isSelectWithSelectionList(final Attribute attribute, final String selectionListId) {
      if (isConstraintWithConfig(attribute) && attribute.getConstraint().getType() == ConstraintType.Select) {
         @SuppressWarnings("unchecked") final Map<String, Object> config = (Map<String, Object>) attribute.getConstraint().getConfig();
         return selectionListId.equals(config.get("selectionListId"));
      }

      return false;
   }

   public static void setSelectConfigOptions(final Attribute attribute, final List<SelectOption> options) {
      if (isConstraintWithConfig(attribute) && attribute.getConstraint().getType() == ConstraintType.Select) {
         @SuppressWarnings("unchecked") final Map<String, Object> config = (Map<String, Object>) attribute.getConstraint().getConfig();
         config.put("options", options);
      }
   }

   public static boolean isUTC(final Attribute attribute) {
      if (isConstraintWithConfig(attribute) && attribute.getConstraint().getType() == ConstraintType.DateTime) {
         @SuppressWarnings("unchecked") final Map<String, Object> config = (Map<String, Object>) attribute.getConstraint().getConfig();
         return (Boolean) Objects.requireNonNullElse(config.get("asUtc"), false);
      }

      return false;
   }

   public static boolean formatHasTimeOptions(final Attribute attribute) {
      if (isConstraintWithConfig(attribute) && attribute.getConstraint().getType() == ConstraintType.DateTime) {
         @SuppressWarnings("unchecked") final Map<String, Object> config = (Map<String, Object>) attribute.getConstraint().getConfig();
         final String format = (String) config.get("format");
         return formatHasTimeOptions(format);
      }

      return false;
   }

   private static boolean formatHasTimeOptions(final String format) {
      if (StringUtils.isNotEmpty(format)) {
         return format.toLowerCase().contains("s") || format.toLowerCase().contains("h") || format.contains("k") || format.contains("m") || format.contains("a");
      }

      return false;
   }

}
