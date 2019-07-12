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
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class QueryStem {

   private final String collectionId;
   private final List<String> linkTypeIds;
   private final Set<String> documentIds;
   private final Set<CollectionAttributeFilter> filters;
   private final Set<LinkAttributeFilter> linkFilters;

   @JsonCreator
   public QueryStem(@JsonProperty("collectionId") final String collectionId,
         @JsonProperty("linkTypeIds") final List<String> linkTypeIds,
         @JsonProperty("documentIds") final Set<String> documentIds,
         @JsonProperty("filters") final Set<CollectionAttributeFilter> filters,
         @JsonProperty("linkFilters") final Set<LinkAttributeFilter> linkFilters) {
      this.collectionId = collectionId;
      this.linkTypeIds = linkTypeIds != null ? linkTypeIds : Collections.emptyList();
      this.documentIds = documentIds != null ? documentIds : Collections.emptySet();
      this.filters = filters != null ? filters : Collections.emptySet();
      this.linkFilters = linkFilters != null ? linkFilters : Collections.emptySet();
   }

   public QueryStem(final String collectionId) {
      this(collectionId, null, null, null, null);
   }

   public String getCollectionId() {
      return collectionId;
   }

   public List<String> getLinkTypeIds() {
      return linkTypeIds;
   }

   public Set<String> getDocumentIds() {
      return documentIds;
   }

   public Set<CollectionAttributeFilter> getFilters() {
      return filters;
   }

   public Set<LinkAttributeFilter> getLinkFilters() {
      return linkFilters;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof QueryStem)) {
         return false;
      }
      final QueryStem queryStem = (QueryStem) o;
      return Objects.equals(getCollectionId(), queryStem.getCollectionId()) &&
            Objects.equals(getLinkTypeIds(), queryStem.getLinkTypeIds()) &&
            Objects.equals(getDocumentIds(), queryStem.getDocumentIds()) &&
            Objects.equals(getFilters(), queryStem.getFilters()) &&
            Objects.equals(getLinkFilters(), queryStem.getLinkFilters());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getCollectionId(), getLinkTypeIds(), getDocumentIds(), getFilters(), getLinkFilters());
   }

   @Override
   public String toString() {
      return "QueryStem{" +
            "collectionId='" + collectionId + '\'' +
            ", linkTypeIds=" + linkTypeIds +
            ", documentIds=" + documentIds +
            ", filters=" + filters +
            ", linkFilters=" + linkFilters +
            '}';
   }
}
