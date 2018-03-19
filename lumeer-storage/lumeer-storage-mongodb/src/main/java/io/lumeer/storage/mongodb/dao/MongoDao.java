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

import io.lumeer.api.model.Role;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.model.MorphiaView;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class MongoDao {

   protected MongoDatabase database;
   protected AdvancedDatastore datastore;

   public void setDatabase(final MongoDatabase database) {
      this.database = database;
   }

   public void setDatastore(final AdvancedDatastore datastore) {
      this.datastore = datastore;
   }

   protected <T> Criteria[] createPermissionsCriteria(Query<T> mongoQuery, DatabaseQuery databaseQuery) {
      List<Criteria> criteria = new ArrayList<>();
      criteria.add(createUserCriteria(mongoQuery, databaseQuery.getUser()));
      databaseQuery.getGroups().forEach(group -> criteria.add(createGroupCriteria(mongoQuery, group)));
      return criteria.toArray(new Criteria[] {});
   }

   private <T> Criteria createUserCriteria(Query<T> query, String user) {
      return query.criteria(MorphiaView.PERMISSIONS + "." + MorphiaPermissions.USER_ROLES)
                  .elemMatch(createPermissionQuery(user));
   }

   private <T> Criteria createGroupCriteria(Query<T> query, String group) {
      return query.criteria(MorphiaView.PERMISSIONS + "." + MorphiaPermissions.GROUP_ROLES)
                  .elemMatch(createPermissionQuery(group));
   }

   private Query<MorphiaPermission> createPermissionQuery(String name) {
      return datastore.createQuery(MorphiaPermission.class)
                      .filter(MorphiaPermission.NAME, name)
                      .field(MorphiaPermission.ROLES).in(Collections.singleton(Role.READ.toString()));
   }

   public <T> void addPaginationToSuggestionQuery(FindIterable<T> findIterable, DatabaseQuery query) {
      Integer page = query.getPage();
      Integer pageSize = query.getPageSize();

      if (page != null && pageSize != null) {
         findIterable.skip(page * pageSize)
                     .limit(pageSize);
      }
   }

   protected static FindOptions createFindOptions(DatabaseQuery query) {
      FindOptions findOptions = new FindOptions();
      Integer page = query.getPage();
      Integer pageSize = query.getPageSize();

      if (page != null && pageSize != null) {
         findOptions.skip(page * pageSize)
                    .limit(pageSize);
      }

      return findOptions;
   }

}
