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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Set;

public class SuggestionQuery {

   private final String text;
   private final SuggestionType type;
   private final Set<String> priorityCollectionIds;

   @JsonCreator
   public SuggestionQuery(@JsonProperty("text") final String text,
         @JsonProperty("type") final String type,
         @JsonProperty("priorityCollectionIds") final Set<String> priorityCollectionIds) {
      this.text = text;
      this.type = SuggestionType.fromString(type);
      this.priorityCollectionIds = priorityCollectionIds != null ? priorityCollectionIds : Collections.emptySet();
   }

   public SuggestionQuery(final String text, final SuggestionType type) {
      this(text, type.toString(), null);
   }

   public String getText() {
      return text;
   }

   public SuggestionType getType() {
      return type;
   }

   public Set<String> getPriorityCollectionIds() {
      return priorityCollectionIds;
   }
}
