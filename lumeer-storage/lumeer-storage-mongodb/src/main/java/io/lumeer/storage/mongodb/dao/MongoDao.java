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
package io.lumeer.storage.mongodb.dao;

import io.lumeer.api.model.Role;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.mongodb.model.MorphiaView;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

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
