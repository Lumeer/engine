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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Query2 {

   private final List<QueryStem> stems;
   private final List<String> fulltexts;
   private final Integer page;
   private final Integer pageSize;

   @JsonCreator
   public Query2(@JsonProperty("stems") final List<QueryStem> stems,
         @JsonProperty("fulltexts") final List<String> fulltexts,
         @JsonProperty("page") final Integer page,
         @JsonProperty("pageSize") final Integer pageSize) {
      this.stems = stems != null ? stems : Collections.emptyList();
      this.fulltexts = fulltexts != null ? fulltexts : Collections.emptyList();
      this.page = page;
      this.pageSize = pageSize;
   }

   public Query2(List<QueryStem> stems) {
      this(stems, Collections.emptyList(), 0, 0);
   }

   public Query2(QueryStem stem) {
      this(Collections.singletonList(stem));
   }

   public Query2() {
      this(Collections.emptyList());
   }

   public List<QueryStem> getStems() {
      return stems;
   }

   public List<String> getFulltexts() {
      return fulltexts;
   }

   public Integer getPage() {
      return page;
   }

   public Integer getPageSize() {
      return pageSize;
   }

   public Set<String> getCollectionIds() {
      return getStems().stream().
            map(QueryStem::getCollectionId)
                       .collect(Collectors.toSet());
   }

   public Set<String> getLinkTypeIds() {
      return getStems().stream()
                       .map(QueryStem::getLinkTypeIds)
                       .flatMap(List::stream)
                       .collect(Collectors.toSet());
   }

   public Map<String, List<String>> getDocumentsIdsMap() {
      return getStems().stream()
                       .collect(Collectors.toMap(QueryStem::getCollectionId, QueryStem::getDocumentIds));
   }

   public Set<String> getDocumentsIds() {
      return getStems().stream()
                       .map(QueryStem::getDocumentIds)
                       .flatMap(List::stream)
                       .collect(Collectors.toSet());
   }

   public Map<String, List<AttributeFilter>> getAttributeFiltersMap() {
      return getStems().stream()
                       .collect(Collectors.toMap(QueryStem::getCollectionId, QueryStem::getFilters));
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Query2)) {
         return false;
      }
      final Query2 query2 = (Query2) o;
      return Objects.equals(stems, query2.stems) &&
            Objects.equals(fulltexts, query2.fulltexts) &&
            Objects.equals(page, query2.page) &&
            Objects.equals(pageSize, query2.pageSize);
   }

   @Override
   public int hashCode() {
      return Objects.hash(stems, fulltexts, page, pageSize);
   }

   @Override
   public String toString() {
      return "Query2{" +
            "stems=" + stems +
            ", fulltexts=" + fulltexts +
            ", page=" + page +
            ", pageSize=" + pageSize +
            '}';
   }
}
