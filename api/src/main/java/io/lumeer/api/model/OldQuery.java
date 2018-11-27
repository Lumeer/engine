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

import java.util.Collections;
import java.util.Set;

public class OldQuery {

   private final Set<String> filters;
   private final Set<String> collectionIds;
   private final Set<String> documentIds;
   private final Set<String> linkTypeIds;
   private final String fulltext;
   private final Integer page;
   private final Integer pageSize;

   public OldQuery() {
      this.filters = Collections.emptySet();
      this.collectionIds = Collections.emptySet();
      this.documentIds = Collections.emptySet();
      this.linkTypeIds = Collections.emptySet();
      this.fulltext = null;
      this.page = null;
      this.pageSize = null;
   }

   public OldQuery(final Set<String> filters,
         final Set<String> collectionIds,
         final Set<String> linkTypeIds,
         final Set<String> documentIds,
         final String fulltext,
         final Integer page,
         final Integer pageSize) {
      this.filters = filters != null ? filters : Collections.emptySet();
      this.collectionIds = collectionIds != null ? collectionIds : Collections.emptySet();
      this.linkTypeIds = linkTypeIds != null ? linkTypeIds : Collections.emptySet();
      this.documentIds = documentIds != null ? documentIds : Collections.emptySet();
      this.fulltext = fulltext;
      this.page = page;
      this.pageSize = pageSize;
   }

   public Set<String> getFilters() {
      return filters;
   }

   public Set<String> getDocumentIds() {
      return documentIds;
   }

   public Set<String> getLinkTypeIds() {
      return linkTypeIds;
   }

   public Set<String> getCollectionIds() {
      return collectionIds;
   }

   public String getFulltext() {
      return fulltext;
   }

   public Integer getPage() {
      return page;
   }

   public Integer getPageSize() {
      return pageSize;
   }

}
