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

import io.lumeer.storage.api.filter.AttributeFilter;

import java.util.Collections;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SearchQuery extends DatabaseQuery {

   private final String fulltext;
   private final Set<String> collectionIds;
   private final Set<String> linkTypeIds;
   private final Set<String> documentIds;
   private final Set<AttributeFilter> filters;

   private SearchQuery(Builder builder) {
      super(builder);

      this.fulltext = builder.fulltext;
      this.collectionIds = builder.collectionIds;
      this.linkTypeIds = builder.linkTypeIds;
      this.documentIds = builder.documentIds;
      this.filters = builder.filters;
   }

   public String getFulltext() {
      return fulltext;
   }

   public Set<String> getLinkTypeIds() {
      return linkTypeIds != null ? Collections.unmodifiableSet(linkTypeIds) : Collections.emptySet();
   }

   public Set<String> getDocumentIds() {
      return documentIds != null ? Collections.unmodifiableSet(documentIds) : Collections.emptySet();
   }

   public Set<String> getCollectionIds() {
      return collectionIds != null ? Collections.unmodifiableSet(collectionIds) : Collections.emptySet();
   }

   public Set<AttributeFilter> getFilters() {
      return filters != null ? Collections.unmodifiableSet(filters) : Collections.emptySet();
   }

   public boolean isFulltextQuery() {
      return fulltext != null && !fulltext.isEmpty();
   }

   public boolean isCollectionIdsQuery() {
      return collectionIds != null && !collectionIds.isEmpty();
   }

   public boolean isLinkTypeIdsQuery() {
      return linkTypeIds != null && !linkTypeIds.isEmpty();
   }

   public boolean isDocumentIdsQuery() {
      return documentIds != null && !documentIds.isEmpty();
   }

   public boolean isFiltersQuery() {
      return filters != null && !filters.isEmpty();
   }

   public boolean isBasicQuery() {
      return !isFulltextQuery()  && !isLinkTypeIdsQuery() && !isDocumentIdsQuery() && !isCollectionIdsQuery();
   }

   public static Builder createBuilder(String... users) {
      return new Builder(users);
   }

   public static class Builder extends DatabaseQuery.Builder<Builder> {

      private String fulltext;
      private Set<String> collectionIds;
      private Set<String> linkTypeIds;
      private Set<String> documentIds;
      private Set<AttributeFilter> filters;

      private Builder(final String... users) {
         super(users);
      }

      public Builder fulltext(String fulltext) {
         this.fulltext = fulltext;
         return this;
      }

      public Builder collectionIds(Set<String> collectionIds) {
         this.collectionIds = collectionIds;
         return this;
      }

      public Builder linkTypeIds(Set<String> linkTypeIds) {
         this.linkTypeIds = linkTypeIds;
         return this;
      }

      public Builder documentIds(Set<String> documentIds) {
         this.documentIds = documentIds;
         return this;
      }

      public Builder filters(Set<AttributeFilter> attributeFilters) {
         this.filters = attributeFilters;
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
            "fulltext='" + fulltext + '\'' +
            ", collectionIds=" + collectionIds +
            ", linkTypeIds=" + linkTypeIds +
            ", documentIds=" + documentIds +
            ", filters=" + filters +
            '}';
   }
}