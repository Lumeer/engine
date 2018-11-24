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
package io.lumeer.storage.mongodb.dao;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.common.Resource;
import io.lumeer.engine.api.event.RemoveResourcePermissions;
import io.lumeer.api.model.Pagination;
import io.lumeer.storage.api.query.DatabaseQuery;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.event.Event;
import javax.inject.Inject;

public abstract class MongoDao {

   protected MongoDatabase database;

   @Inject
   private Event<RemoveResourcePermissions> removeResourcePermissionsEvent;

   public void setDatabase(final MongoDatabase database) {
      this.database = database;
   }

   public <T> void addPaginationToQuery(FindIterable<T> findIterable, DatabaseQuery query) {
      addPaginationToQuery(findIterable, query.getPagination());
   }

   public <T> void addPaginationToQuery(FindIterable<T> findIterable, Pagination pagination) {
      Integer page = pagination != null ? pagination.getPage() : null;
      Integer pageSize = pagination != null ? pagination.getPageSize() : null;

      if (page != null && pageSize != null) {
         findIterable.skip(page * pageSize)
                     .limit(pageSize);
      }
   }

   public void checkRemovedPermissions(final Resource originalResource, final Resource newResource) {
      if (originalResource != null) {
         final Set<String> removedUser = getRemovedPermissions(originalResource.getPermissions().getUserPermissions(), newResource.getPermissions().getUserPermissions());
         if (removedUser.size() > 0) {
            removeResourcePermissionsEvent.fire(new RemoveResourcePermissions(newResource, removedUser));
         }
      }
   }

   private Set<String> getRemovedPermissions(final Set<Permission> actualPermissions, final Set<Permission> newPermissions) {
      Set<String> actualUsers = actualPermissions.stream().map(Permission::getId).collect(Collectors.toSet());
      actualUsers.removeAll(newPermissions.stream().filter(permission -> permission.getRoles().size() > 0).map(Permission::getId).collect(Collectors.toSet()));
      return actualUsers;
   }
}
