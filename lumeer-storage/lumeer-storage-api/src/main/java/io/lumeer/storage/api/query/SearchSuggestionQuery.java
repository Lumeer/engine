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
package io.lumeer.storage.api.query;

import java.util.Set;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SearchSuggestionQuery extends DatabaseQuery {

   private final String text;
   private final Set<String> collectionIds;
   private final Set<String> priorityCollectionIds;

   private SearchSuggestionQuery(final Builder builder) {
      super(builder);

      this.text = builder.text;
      this.collectionIds = builder.collectionIds;
      this.priorityCollectionIds = builder.priorityCollectionIds;
   }

   public String getText() {
      return text;
   }

   public Set<String> getCollectionIds() {
      return collectionIds;
   }

   public boolean hasCollectionIdsQuery(){
      return collectionIds != null && !collectionIds.isEmpty();
   }

   public Set<String> getPriorityCollectionIds() {
      return priorityCollectionIds;
   }

   public boolean hasPriorityCollectionIdsQuery(){
      return priorityCollectionIds != null && !priorityCollectionIds.isEmpty();
   }

   public static Builder createBuilder(String user) {
      return new Builder(user);
   }

   public static class Builder extends DatabaseQuery.Builder<SearchSuggestionQuery.Builder> {

      private String text;
      private Set<String> collectionIds;
      private Set<String> priorityCollectionIds;

      private Builder(final String user) {
         super(user);
      }

      public Builder text(String text) {
         this.text = text;
         return this;
      }

      public Builder collectionIds(Set<String> collectionIds) {
         this.collectionIds = collectionIds;
         return this;
      }

      public Builder priorityCollectionIds(Set<String> collectionIds) {
         this.priorityCollectionIds = collectionIds;
         return this;
      }

      public SearchSuggestionQuery build() {
         validate();

         return new SearchSuggestionQuery(this);
      }
   }
}
