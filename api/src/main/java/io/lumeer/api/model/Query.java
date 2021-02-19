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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Query {

   private final List<QueryStem> stems;
   private final Set<String> fulltexts;
   private final Integer page;
   private final Integer pageSize;

   @JsonCreator
   public Query(@JsonProperty("stems") final List<QueryStem> stems,
         @JsonProperty("fulltexts") final Set<String> fulltexts,
         @JsonProperty("page") final Integer page,
         @JsonProperty("pageSize") final Integer pageSize) {
      this.stems = stems != null ? stems : Collections.emptyList();
      this.fulltexts = fulltexts != null ? fulltexts : Collections.emptySet();
      this.page = page;
      this.pageSize = pageSize;
   }

   public Query(List<QueryStem> stems) {
      this(stems, Collections.emptySet(), 0, 0);
   }

   public Query(QueryStem stem) {
      this(Collections.singletonList(stem));
   }

   public Query() {
      this(Collections.emptyList());
   }

   @JsonIgnore
   public boolean isEmpty() {
      return stems.isEmpty() && fulltexts.isEmpty();
   }

   @JsonIgnore
   public boolean containsStems() {
      return !getStems().isEmpty();
   }

   @JsonIgnore
   public boolean containsAnyFilter() {
      if (getFulltexts() != null && getFulltexts().size() > 0) {
         return true;
      }
      return getStems().stream().anyMatch(QueryStem::containsAnyFilter);
   }

   public List<QueryStem> getStems() {
      return stems != null ? stems.stream().filter(stem -> stem.getCollectionId() != null).collect(Collectors.toList()) : new ArrayList<>();
   }

   public Set<String> getFulltexts() {
      return fulltexts != null ? fulltexts : new HashSet<>();
   }

   public Integer getPage() {
      return page;
   }

   public Integer getPageSize() {
      return pageSize;
   }

   @JsonIgnore
   public Pagination getPagination() {
      return new Pagination(page, pageSize);
   }

   @JsonIgnore
   public Set<String> getCollectionIds() {
      return getStems().stream().
            map(QueryStem::getCollectionId)
                       .collect(Collectors.toSet());
   }

   @JsonIgnore
   public Set<String> getLinkTypeIds() {
      return getStems().stream()
                       .map(QueryStem::getLinkTypeIds)
                       .flatMap(List::stream)
                       .collect(Collectors.toSet());
   }

   @JsonIgnore
   public Set<String> getDocumentsIds() {
      return getStems().stream()
                       .map(QueryStem::getDocumentIds)
                       .flatMap(Set::stream)
                       .collect(Collectors.toSet());
   }

   @JsonIgnore
   public Set<CollectionAttributeFilter> getAttributeFilters() {
      return getStems().stream()
                       .map(QueryStem::getFilters)
                       .flatMap(Set::stream)
                       .collect(Collectors.toSet());
   }

   @JsonIgnore
   public Set<LinkAttributeFilter> getLinkAttributeFilters() {
      return getStems().stream()
                       .map(QueryStem::getLinkFilters)
                       .flatMap(Set::stream)
                       .collect(Collectors.toSet());
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Query)) {
         return false;
      }
      final Query query = (Query) o;
      return Objects.equals(stems, query.stems) &&
            Objects.equals(fulltexts, query.fulltexts) &&
            Objects.equals(page, query.page) &&
            Objects.equals(pageSize, query.pageSize);
   }

   @Override
   public int hashCode() {
      return Objects.hash(stems, fulltexts, page, pageSize);
   }

   @Override
   public String toString() {
      return "Query{" +
            "stems=" + stems +
            ", fulltexts=" + fulltexts +
            ", page=" + page +
            ", pageSize=" + pageSize +
            '}';
   }

   /**
    * Incomplete implementation that needs to be extended for more use cases.
    *
    * @return partial URL query string representing this stem
    */
   public String toQueryString() {
      return "{\"s\":[" + stems.stream().map(QueryStem::toQueryString).collect(Collectors.joining(", ")) + "]}";
   }
}
