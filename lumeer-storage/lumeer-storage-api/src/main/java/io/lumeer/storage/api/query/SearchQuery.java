/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
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

   private SearchQuery(Builder builder) {
      super(builder);

      this.fulltext = builder.fulltext;
      this.collectionCodes = builder.collectionCodes;
   }

   public String getFulltext() {
      return fulltext;
   }

   public Set<String> getCollectionCodes() {
      return Collections.unmodifiableSet(collectionCodes);
   }

   public boolean isFulltextQuery() {
      return fulltext != null && !fulltext.isEmpty();
   }

   public boolean isCollectionCodesQuery() {
      return collectionCodes != null && !collectionCodes.isEmpty();
   }

   public boolean isBasicQuery() {
      return !isFulltextQuery() && !isCollectionCodesQuery();
   }

   public static Builder createBuilder(String user) {
      return new Builder(user);
   }

   public static class Builder extends DatabaseQuery.Builder<Builder> {

      private String fulltext;
      private Set<String> collectionCodes = new HashSet<>();

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

      public SearchQuery build() {
         validate();

         return new SearchQuery(this);
      }
   }
}
