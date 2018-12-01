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

import io.lumeer.api.model.ConditionType;
import io.lumeer.api.model.QueryStem;
import io.lumeer.storage.api.filter.AttributeFilter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SearchQueryStem {

   private final String collectionId;
   private final List<String> linkTypeIds;
   private final Set<String> documentIds;
   private final Set<AttributeFilter> filters;
   private final Set<String> fulltexts;

   public SearchQueryStem(QueryStem stem, Set<String> fulltexts) {
      this.collectionId = stem.getCollectionId();
      this.linkTypeIds = stem.getLinkTypeIds();
      this.documentIds = stem.getDocumentIds();
      this.filters = stem.getFilters() != null ? stem.getFilters().stream().map(this::convertFilter).collect(Collectors.toSet()) : Collections.emptySet();
      this.fulltexts = fulltexts;
   }

   public SearchQueryStem(Builder builder) {
      this.collectionId = builder.collectionId;
      this.linkTypeIds = builder.linkTypeIds;
      this.documentIds = builder.documentIds != null && !builder.documentIds.isEmpty() ? builder.documentIds : new HashSet<>();
      this.filters = builder.filters;
      this.fulltexts = builder.fulltexts;
   }

   private AttributeFilter convertFilter(final io.lumeer.api.model.AttributeFilter attr) {
      ConditionType conditionType = ConditionType.fromString(attr.getOperator().toLowerCase());
      return new AttributeFilter(attr.getCollectionId(), conditionType, attr.getAttributeId(), attr.getValue());
   }

   public String getCollectionId() {
      return collectionId;
   }

   public List<String> getLinkTypeIds() {
      return linkTypeIds != null ? Collections.unmodifiableList(linkTypeIds) : Collections.emptyList();
   }

   public Set<String> getDocumentIds() {
      return documentIds != null ? Collections.unmodifiableSet(documentIds) : Collections.emptySet();
   }

   public Set<AttributeFilter> getFilters() {
      return filters != null ? Collections.unmodifiableSet(filters) : Collections.emptySet();
   }

   public Set<String> getFulltexts() {
      return fulltexts != null ? Collections.unmodifiableSet(fulltexts) : Collections.emptySet();
   }

   public boolean containsLinkTypeIdsQuery() {
      return linkTypeIds != null && !linkTypeIds.isEmpty();
   }

   public boolean containsDocumentIdsQuery() {
      return documentIds != null && !documentIds.isEmpty();
   }

   public boolean containsFiltersQuery() {
      return filters != null && !filters.isEmpty();
   }

   public boolean containsFulltextsQuery() {
      return fulltexts != null && !fulltexts.isEmpty();
   }

   public void appendDocumentIds(Set<String> documentIds) {
      this.documentIds.addAll(documentIds);
   }

   public void intersectDocumentIds(Set<String> documentIds) {
      Set<String> copyCurrentIds = new HashSet<>(this.documentIds);
      copyCurrentIds.retainAll(documentIds);
      this.documentIds.clear();
      this.documentIds.addAll(copyCurrentIds);
   }

   public static Builder createBuilder(String collectionId) {
      return new Builder(collectionId);
   }

   public static class Builder {

      private String collectionId;
      private List<String> linkTypeIds;
      private Set<String> documentIds;
      private Set<AttributeFilter> filters;
      private Set<String> fulltexts;

      private Builder(String collectionId) {
         this.collectionId = collectionId;
      }

      public Builder linkTypeIds(List<String> linkTypeIds) {
         this.linkTypeIds = linkTypeIds;
         return this;
      }

      public Builder documentIds(Set<String> documentIds) {
         this.documentIds = documentIds;
         return this;
      }

      public Builder filters(Set<AttributeFilter> filters) {
         this.filters = filters;
         return this;
      }

      public Builder fulltexts(Set<String> fulltexts) {
         this.fulltexts = fulltexts;
         return this;
      }

      public SearchQueryStem build() {
         return new SearchQueryStem(this);
      }
   }

   @Override
   public String toString() {
      return "SearchQueryStem{" +
            "collectionId='" + collectionId + '\'' +
            ", linkTypeIds=" + linkTypeIds +
            ", documentIds=" + documentIds +
            ", filters=" + filters +
            ", fulltexts=" + fulltexts +
            '}';
   }
}
