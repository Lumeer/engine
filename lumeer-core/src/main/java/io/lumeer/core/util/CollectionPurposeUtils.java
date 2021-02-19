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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.DocumentEvent;

import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

public abstract class CollectionPurposeUtils {

   protected static final ZoneId utcZone = ZoneId.ofOffset("UTC", ZoneOffset.UTC);

   public static boolean isDoneState(final DataDocument data, final Collection collection) {
      final String stateAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_STATE_ATTRIBUTE_ID) : null;
      final List<String> finalStates = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getArrayList(Collection.META_FINAL_STATES_LIST, String.class) : null;

      if (finalStates != null && data != null) {

         if (StringUtils.isNotEmpty(stateAttributeId) && findAttribute(collection.getAttributes(), stateAttributeId) != null) {
            final Object states = data.getObject(stateAttributeId);
            if (states instanceof String) {
               return finalStates.contains(states);
            } else if (states instanceof List) {
               final List<String> stringStates = data.getArrayList(stateAttributeId, String.class);

               return stringStates.stream().anyMatch(finalStates::contains);
            }
         }
      }

      return false;
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
