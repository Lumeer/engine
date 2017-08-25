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
package io.lumeer.api.dto;

import io.lumeer.api.model.Query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Set;

public class JsonQuery implements Query {

   private final Set<String> collectionCodes;
   private final Set<String> filters;
   private final String fulltext;
   private final Integer page;
   private final Integer pageSize;

   public JsonQuery() {
      this.collectionCodes = Collections.emptySet();
      this.filters = Collections.emptySet();
      this.fulltext = "";
      this.page = null;
      this.pageSize = null;
   }

   public JsonQuery(Integer page, Integer pageSize) {
      this.page = page;
      this.pageSize = pageSize;

      this.collectionCodes = Collections.emptySet();
      this.filters = Collections.emptySet();
      this.fulltext = "";
   }

   public JsonQuery(String collectionCode, Integer page, Integer pageSize) {
      this.collectionCodes = Collections.singleton(collectionCode);
      this.page = page;
      this.pageSize = pageSize;

      this.filters = Collections.emptySet();
      this.fulltext = "";
   }

   public JsonQuery(Query query) {
      this.collectionCodes = query.getCollectionCodes();
      this.filters = query.getFilters();
      this.fulltext = query.getFulltext();
      this.page = query.getPage();
      this.pageSize = query.getPageSize();
   }

   @JsonCreator
   public JsonQuery(@JsonProperty("collections") final Set<String> collectionCodes,
         @JsonProperty("filters") final Set<String> filters,
         @JsonProperty("fulltext") final String fulltext,
         @JsonProperty("page") final Integer page,
         @JsonProperty("pageSize") final Integer pageSize) {
      this.collectionCodes = collectionCodes != null ? collectionCodes : Collections.emptySet();
      this.filters = filters != null ? filters : Collections.emptySet();
      this.fulltext = fulltext;
      this.page = page;
      this.pageSize = pageSize;
   }

   @Override
   public Set<String> getCollectionCodes() {
      return collectionCodes;
   }

   @Override
   public Set<String> getFilters() {
      return filters;
   }

   @Override
   public String getFulltext() {
      return fulltext;
   }

   @Override
   public Integer getPage() {
      return page;
   }

   @Override
   public Integer getPageSize() {
      return pageSize;
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

      if (collectionCodes != null ? !collectionCodes.equals(query.getCollectionCodes()) : query.getCollectionCodes() != null) {
         return false;
      }
      if (getFilters() != null ? !getFilters().equals(query.getFilters()) : query.getFilters() != null) {
         return false;
      }
      if (getFulltext() != null ? !getFulltext().equals(query.getFulltext()) : query.getFulltext() != null) {
         return false;
      }
      if (getPage() != null ? !getPage().equals(query.getPage()) : query.getPage() != null) {
         return false;
      }
      return getPageSize() != null ? getPageSize().equals(query.getPageSize()) : query.getPageSize() == null;
   }

   @Override
   public int hashCode() {
      int result = collectionCodes != null ? collectionCodes.hashCode() : 0;
      result = 31 * result + (getFilters() != null ? getFilters().hashCode() : 0);
      result = 31 * result + (getFulltext() != null ? getFulltext().hashCode() : 0);
      result = 31 * result + (getPage() != null ? getPage().hashCode() : 0);
      result = 31 * result + (getPageSize() != null ? getPageSize().hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "JsonQuery{" +
            "collectionCodes=" + collectionCodes +
            ", filters=" + filters +
            ", fulltext='" + fulltext + '\'' +
            ", page=" + page +
            ", pageSize=" + pageSize +
            '}';
   }
}
