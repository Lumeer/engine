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
package io.lumeer.storage.api.query;

import java.util.List;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SuggestionQuery extends DatabaseQuery {

   private final String text;
   private final List<String> collectionIds;

   private SuggestionQuery(final Builder builder) {
      super(builder);

      this.text = builder.text;
      this.collectionIds = builder.collectionIds;
   }

   public String getText() {
      return text;
   }

   public List<String> getCollectionIds() {
      return collectionIds;
   }

   public static Builder createBuilder(String user) {
      return new Builder(user);
   }

   public static class Builder extends DatabaseQuery.Builder<SuggestionQuery.Builder> {

      private String text;
      private List<String> collectionIds;

      private Builder(final String user) {
         super(user);
      }

      public Builder text(String text) {
         this.text = text;
         return this;
      }

      public Builder collectionIds(List<String> collectionIds) {
         this.collectionIds = collectionIds;
         return this;
      }

      public SuggestionQuery build() {
         validate();

         return new SuggestionQuery(this);
      }
   }
}
