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
import java.util.List;
import java.util.stream.Collectors;

public class SearchQueryStem {

   private final String collectionId;
   private final List<String> linkTypeIds;
   private final List<String> documentIds;
   private final List<AttributeFilter> filters;

   public SearchQueryStem(QueryStem stem) {
      this.collectionId = stem.getCollectionId();
      this.linkTypeIds = stem.getLinkTypeIds();
      this.documentIds = stem.getDocumentIds();
      this.filters = stem.getFilters() != null ? stem.getFilters().stream().map(this::convertFilter).collect(Collectors.toList()) : Collections.emptyList();
   }

   public SearchQueryStem(Builder builder) {
      this.collectionId = builder.collectionId;
      this.linkTypeIds = builder.linkTypeIds;
      this.documentIds = builder.documentIds;
      this.filters = builder.filters;
   }

   private AttributeFilter convertFilter(final io.lumeer.api.model.AttributeFilter attr) {
      ConditionType conditionType = ConditionType.fromString(attr.getOperator().toLowerCase());
      return new AttributeFilter(this.collectionId, conditionType, attr.getAttributeId(), attr.getValue());
   }

   public String getCollectionId() {
      return collectionId;
   }

   public List<String> getLinkTypeIds() {
      return linkTypeIds;
   }

   public List<String> getDocumentIds() {
      return documentIds;
   }

   public List<AttributeFilter> getFilters() {
      return filters;
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

   public static Builder createBuilder(String collectionId) {
      return new Builder(collectionId);
   }

   public static class Builder {

      private String collectionId;
      private List<String> linkTypeIds;
      private List<String> documentIds;
      private List<AttributeFilter> filters;

      private Builder(String collectionId) {
         this.collectionId = collectionId;
      }

      public Builder linkTypeIds(List<String> linkTypeIds) {
         this.linkTypeIds = linkTypeIds;
         return this;
      }

      public Builder documentIds(List<String> documentIds) {
         this.documentIds = documentIds;
         return this;
      }

      public Builder filters(List<AttributeFilter> filters) {
         this.filters = filters;
         return this;
      }

      public SearchQueryStem build() {
         return new SearchQueryStem(this);
      }
   }

}
