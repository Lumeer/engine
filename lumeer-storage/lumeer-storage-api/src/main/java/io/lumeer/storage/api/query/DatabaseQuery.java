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
package io.lumeer.storage.api.query;

import io.lumeer.api.model.Pagination;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DatabaseQuery {

   private final Set<String> users;
   private final Set<String> groups;
   private final Integer page;
   private final Integer pageSize;

   protected DatabaseQuery(Builder builder) {
      this.users = builder.users;
      this.groups = builder.groups;
      this.page = builder.page;
      this.pageSize = builder.pageSize;
   }

   public Set<String> getUsers() {
      return Collections.unmodifiableSet(users);
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

   public Pagination getPagination() {
      return new Pagination(page, pageSize);
   }

   public static Builder createBuilder(String... users) {
      return new Builder(users);
   }

   public static class Builder<T extends Builder<T>> {

      private final Set<String> users;
      private Set<String> groups = new HashSet<>();
      private Integer page;
      private Integer pageSize;

      protected Builder(String... users) {
         this.users = Arrays.asList(users).stream().collect(Collectors.toSet());
      }

      public T groups(Set<String> groups) {
         this.groups = new HashSet<>(groups);
         return (T) this;
      }

      public T page(Integer page) {
         this.page = page;
         return (T) this;
      }

      public T pageSize(Integer pageSize) {
         this.pageSize = pageSize;
         return (T) this;
      }

      protected void validate() {
         if ((page != null && pageSize == null) || (page == null && pageSize != null)) {
            throw new IllegalArgumentException("both page and pageSize must be set");
         }
         if (page != null && page < 0) {
            throw new IllegalArgumentException("page must not be negative");
         }
         if (pageSize != null && pageSize < 0) {
            throw new IllegalArgumentException("pageSize must not be negative");
         }
      }

      public DatabaseQuery build() {
         validate();

         return new DatabaseQuery(this);
      }

   }

   public boolean hasPagination() {
      return page != null && pageSize != null;
   }

}
