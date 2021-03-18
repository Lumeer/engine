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
package io.lumeer.core.util;

import static io.lumeer.api.util.ResourceUtils.findAttribute;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;

import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public abstract class CollectionPurposeUtils {

   private static final List<String> TRUTHY_VALUES = Arrays.asList("true", "yes", "ja", "ano", "áno", "sí", "si", "sim", "да", "是", "はい", "vâng", "כן");

   protected static final ZoneId utcZone = ZoneId.ofOffset("UTC", ZoneOffset.UTC);

   public static boolean isDoneState(final DataDocument data, final Collection collection) {
      final String stateAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_STATE_ATTRIBUTE_ID) : null;
      final List<Object> finalStates = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getArrayList(Collection.META_FINAL_STATES_LIST) : null;

      if (finalStates != null && data != null) {

         Attribute stateAttribute = findAttribute(collection.getAttributes(), stateAttributeId);
         if (StringUtils.isNotEmpty(stateAttributeId) && stateAttribute != null) {
            final Object states = data.getObject(stateAttributeId);
            if (Utils.computeIfNotNull(stateAttribute.getConstraint(), Constraint::getType) == ConstraintType.Boolean) {
               var finalState = finalStates.isEmpty() ? null : finalStates.get(0);
               return toBoolean(finalState) == toBoolean(states);
            } else if (states instanceof List) {
               final List<Object> stringStates = data.getArrayList(stateAttributeId);
               return stringStates.stream().anyMatch(finalStates::contains);
            } else {
               return finalStates.contains(states);
            }
         }
      }

      return false;
   }

   private static boolean toBoolean(Object value) {
      if (value == null) {
         return false;
      }
      if (Boolean.parseBoolean(value.toString())) {
         return true;
      }
      return TRUTHY_VALUES.stream().anyMatch(truthyValue -> truthyValue.equals(value));
   }

   public static ZonedDateTime getDueDate(final Document document, final Collection collection) {
      final String dueDateAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_DUE_DATE_ATTRIBUTE_ID) : null;

      if (StringUtils.isNotEmpty(dueDateAttributeId) && findAttribute(collection.getAttributes(), dueDateAttributeId) != null) {
         if (document.getData().get(dueDateAttributeId) instanceof Date) {
            final Date dueDate = document.getData().getDate(dueDateAttributeId);
            return ZonedDateTime.from(dueDate.toInstant().atZone(utcZone));
         }
      }

      return null;
   }
}
