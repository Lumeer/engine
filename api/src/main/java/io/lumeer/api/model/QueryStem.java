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
import java.util.Objects;
import java.util.Set;

public class QueryStem {

   private final String collectionId;
   private final List<String> linkTypeIds;
   private final Set<String> documentIds;
   private final Set<AttributeFilter> filters;

   @JsonCreator
   public QueryStem(@JsonProperty("collectionId") final String collectionId,
         @JsonProperty("linkTypeIds") final List<String> linkTypeIds,
         @JsonProperty("documentIds") final Set<String> documentIds,
         @JsonProperty("filters") final Set<AttributeFilter> filters) {
      this.collectionId = collectionId;
      this.linkTypeIds = linkTypeIds != null ? linkTypeIds : Collections.emptyList();
      this.documentIds = documentIds != null ? documentIds : Collections.emptySet();
      this.filters = filters != null ? filters : Collections.emptySet();
   }

   public QueryStem(final String collectionId) {
      this(collectionId, null, null, null);
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

   public Set<AttributeFilter> getFilters() {
      return filters;
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
      return Objects.equals(collectionId, queryStem.collectionId) &&
            Objects.equals(linkTypeIds, queryStem.linkTypeIds) &&
            Objects.equals(documentIds, queryStem.documentIds) &&
            Objects.equals(filters, queryStem.filters);
   }

   @Override
   public int hashCode() {
      return Objects.hash(collectionId, linkTypeIds, documentIds, filters);
   }

   @Override
   public String toString() {
      return "QueryStem{" +
            "collectionId='" + collectionId + '\'' +
            ", linkTypeIds=" + linkTypeIds +
            ", documentIds=" + documentIds +
            ", filters=" + filters +
            '}';
   }
}
