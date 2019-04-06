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

import io.lumeer.api.model.QueryStem;
import io.lumeer.storage.api.filter.LinkAttributeFilter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SearchQuery extends DatabaseQuery {

   private final List<SearchQueryStem> stems;

   private SearchQuery(Builder builder) {
      super(builder);

      this.stems = builder.stems;
   }

   public List<SearchQueryStem> getStems() {
      return stems != null ? Collections.unmodifiableList(stems) : Collections.emptyList();
   }

   public Set<LinkAttributeFilter> getLinkAttributeFilters() {
      return getStems().stream()
                       .map(SearchQueryStem::getLinkFilters)
                       .flatMap(Set::stream)
                       .collect(Collectors.toSet());
   }

   public static Builder createBuilder(String... users) {
      return new Builder(users);
   }

   public static class Builder extends DatabaseQuery.Builder<Builder> {

      private List<SearchQueryStem> stems;

      private Builder(final String... users) {
         super(users);
      }

      public Builder stems(List<SearchQueryStem> stems) {
         this.stems = stems;
         return this;
      }

      public Builder queryStems(List<QueryStem> stems, Set<String> fulltexts) {
         this.stems = stems.stream()
                           .map(stem -> new SearchQueryStem(stem, fulltexts)).collect(Collectors.toList());
         return this;
      }

      public SearchQuery build() {
         validate();

         return new SearchQuery(this);
      }
   }

   @Override
   public String toString() {
      return "SearchQuery{" +
            "stems=" + stems +
            '}';
   }
}