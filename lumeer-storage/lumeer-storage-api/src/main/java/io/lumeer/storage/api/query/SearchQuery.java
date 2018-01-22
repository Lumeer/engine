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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SearchQuery extends DatabaseQuery {

   private final String fulltext;
   private final Set<String> collectionCodes;
   private final Set<String> collectionIds;
   private final Set<String> linkTypeIds;
   private final Set<String> documentIds;

   private SearchQuery(Builder builder) {
      super(builder);

      this.fulltext = builder.fulltext;
      this.collectionCodes = builder.collectionCodes;
      this.collectionIds = builder.collectionIds;
      this.linkTypeIds = builder.linkTypeIds;
      this.documentIds = builder.documentIds;
   }

   public String getFulltext() {
      return fulltext;
   }

   public Set<String> getCollectionCodes() {
      return Collections.unmodifiableSet(collectionCodes);
   }

   public Set<String> getLinkTypeIds() {
      return Collections.unmodifiableSet(linkTypeIds);
   }

   public Set<String> getDocumentIds() {
      return Collections.unmodifiableSet(documentIds);
   }

   public Set<String> getCollectionIds() {
      return Collections.unmodifiableSet(collectionIds);
   }

   public boolean isFulltextQuery() {
      return fulltext != null && !fulltext.isEmpty();
   }

   public boolean isCollectionCodesQuery() {
      return collectionCodes != null && !collectionCodes.isEmpty();
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

   public boolean isBasicQuery() {
      return !isFulltextQuery() && !isCollectionCodesQuery() && !isLinkTypeIdsQuery() && !isDocumentIdsQuery() && !isCollectionIdsQuery();
   }

   public static Builder createBuilder(String user) {
      return new Builder(user);
   }

   public static class Builder extends DatabaseQuery.Builder<Builder> {

      private String fulltext;
      private Set<String> collectionCodes = new HashSet<>();
      private Set<String> collectionIds;
      private Set<String> linkTypeIds;
      private Set<String> documentIds;

      private Builder(final String user) {
         super(user);
      }

      public Builder fulltext(String fulltext) {
         this.fulltext = fulltext;
         return this;
      }

      public Builder collectionCodes(Set<String> collectionCodes) {
         this.collectionCodes = collectionCodes;
         return this;
      }


      public Builder collectionIds(Set<String> collectionIds){
         this.collectionIds = collectionIds;
         return this;
      }

      public Builder linkTypeIds(Set<String> linkTypeIds){
         this.linkTypeIds = linkTypeIds;
         return this;
      }

      public Builder documentIds(Set<String> documentIds){
         this.documentIds = documentIds;
         return this;
      }

      public SearchQuery build() {
         validate();

         return new SearchQuery(this);
      }
   }
}