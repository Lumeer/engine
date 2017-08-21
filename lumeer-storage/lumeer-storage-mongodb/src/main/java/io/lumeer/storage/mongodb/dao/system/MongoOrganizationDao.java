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
package io.lumeer.storage.mongodb.dao.system;

import io.lumeer.api.model.Organization;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.DatabaseQuery;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MongoOrganization;
import io.lumeer.storage.mongodb.model.embedded.MongoPermission;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;

import com.mongodb.WriteResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoOrganizationDao extends SystemScopedDao implements OrganizationDao {

   @PostConstruct
   public void ensureIndexes() {
      datastore.ensureIndexes(MongoOrganization.class);
   }

   @Override
   public Organization createOrganization(final Organization organization) {
      MongoOrganization mongoOrganization = new MongoOrganization(organization);
      datastore.save(mongoOrganization);
      return mongoOrganization;
   }

   @Override
   public Organization getOrganizationByCode(final String organizationCode) {
      Organization organization = datastore.createQuery(MongoOrganization.class)
                                           .field(MongoOrganization.CODE).equal(organizationCode)
                                           .get();
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return organization;
   }

   private Query<MongoOrganization> createOrganizationQuery(DatabaseQuery query) {
      Query<MongoOrganization> organizationQuery = datastore.createQuery(MongoOrganization.class);

      List<Criteria> criteria = new ArrayList<>();
      criteria.add(createUserCriteria(organizationQuery, query.getUser()));
      query.getGroups().forEach(group -> criteria.add(createGroupCriteria(organizationQuery, group)));
      organizationQuery.or(criteria.toArray(new Criteria[] {}));

      return organizationQuery;
   }

   private Criteria createUserCriteria(Query<MongoOrganization> organizationQuery, String user) {
      return organizationQuery.criteria(MongoOrganization.PERMISSIONS + "." + MongoPermissions.USER_ROLES)
                              .elemMatch(createPermissionQuery(user));
   }

   private Query<MongoPermission> createPermissionQuery(String name) {
      return datastore.createQuery(MongoPermission.class)
                      .filter(MongoPermission.NAME, name)
                      .field(MongoPermission.ROLES).in(Collections.singleton(Role.READ.toString()));
   }

   private Criteria createGroupCriteria(Query<MongoOrganization> organizationQuery, String group) {
      return organizationQuery.criteria(MongoOrganization.PERMISSIONS + "." + MongoPermissions.GROUP_ROLES)
                              .elemMatch(createPermissionQuery(group));
   }

   private FindOptions createFindOptions(DatabaseQuery query) {
      FindOptions findOptions = new FindOptions();
      Integer page = query.getPage();
      Integer pageSize = query.getPageSize();

      if (page != null && pageSize != null) {
         findOptions.skip(page * pageSize)
                    .limit(pageSize);
      }

      return findOptions;
   }

   @Override
   public List<Organization> getOrganizations(final DatabaseQuery query) {
      Query<MongoOrganization> organizationQuery = createOrganizationQuery(query);
      FindOptions findOptions = createFindOptions(query);

      return new ArrayList<>(organizationQuery.asList(findOptions));
   }

   @Override
   public void deleteOrganization(final String organizationId) {
      WriteResult writeResult = datastore.delete(MongoOrganization.class, new ObjectId(organizationId));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   @Override
   public Organization updateOrganization(final String organizationId, final Organization organization) {
      MongoOrganization mongoOrganization = new MongoOrganization(organization);
      mongoOrganization.setId(new ObjectId(organizationId));
      datastore.save(mongoOrganization);
      return mongoOrganization;
   }

}
