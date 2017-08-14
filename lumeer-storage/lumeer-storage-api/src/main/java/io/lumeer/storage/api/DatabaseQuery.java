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
package io.lumeer.storage.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DatabaseQuery {

   private final String user;
   private final Set<String> groups;
   private final Integer page;
   private final Integer pageSize;

   private DatabaseQuery(Builder builder) {
      this.user = builder.user;
      this.groups = builder.groups;
      this.page = builder.page;
      this.pageSize = builder.pageSize;
   }

   public String getUser() {
      return user;
   }

   public Set<String> getGroups() {
      return Collections.unmodifiableSet(groups);
   }

   public Integer getPage() {
      return page;
   }

   public Integer getPageSize() {
      return pageSize;
   }

   public static class Builder {

      private final String user;
      private Set<String> groups = new HashSet<>();
      private Integer page;
      private Integer pageSize;

      public Builder(String user) {
         this.user = user;
      }

      public Builder groups(Set<String> groups) {
         this.groups = new HashSet<>(groups);
         return this;
      }

      public Builder page(int page) {
         this.page = page;
         return this;
      }

      public Builder pageSize(int pageSize) {
         this.pageSize = pageSize;
         return this;
      }

      public DatabaseQuery build() {
         if ((page != null && pageSize == null) || (page == null && pageSize != null)) {
            throw new IllegalArgumentException("both page and pageSize must be set");
         }
         if (page != null && page < 0) {
            throw new IllegalArgumentException("page must not be negative");
         }
         if (pageSize != null && pageSize < 0) {
            throw new IllegalArgumentException("pageSize must not be negative");
         }

         return new DatabaseQuery(this);
      }

   }

}
