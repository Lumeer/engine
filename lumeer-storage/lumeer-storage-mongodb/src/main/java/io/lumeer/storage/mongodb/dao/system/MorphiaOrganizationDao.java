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
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaOrganization;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MorphiaOrganizationDao extends SystemScopedDao implements OrganizationDao {

   @PostConstruct
   public void ensureIndexes() {
      datastore.ensureIndexes(MorphiaOrganization.class);
   }

   @Override
   public Organization createOrganization(final Organization organization) {
      MorphiaOrganization morphiaOrganization = new MorphiaOrganization(organization);
      datastore.insert(morphiaOrganization);
      return morphiaOrganization;
   }

   @Override
   public Organization updateOrganization(final String organizationId, final Organization organization) {
      MorphiaOrganization morphiaOrganization = new MorphiaOrganization(organization);
      morphiaOrganization.setId(organizationId);
      datastore.save(morphiaOrganization);
      return morphiaOrganization;
   }

   @Override
   public void deleteOrganization(final String organizationId) {
      WriteResult writeResult = datastore.delete(MorphiaOrganization.class, new ObjectId(organizationId));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   @Override
   public Organization getOrganizationByCode(final String organizationCode) {
      Organization organization = datastore.createQuery(MorphiaOrganization.class)
                                           .field(MorphiaOrganization.CODE).equal(organizationCode)
                                           .get();
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return organization;
   }

   @Override
   public List<Organization> getOrganizations(final DatabaseQuery query) {
      Query<MorphiaOrganization> organizationQuery = createOrganizationQuery(query);
      FindOptions findOptions = createFindOptions(query);

      return new ArrayList<>(organizationQuery.asList(findOptions));
   }

   private Query<MorphiaOrganization> createOrganizationQuery(DatabaseQuery query) {
      Query<MorphiaOrganization> organizationQuery = datastore.createQuery(MorphiaOrganization.class);

      List<Criteria> criteria = new ArrayList<>();
      criteria.add(createUserCriteria(organizationQuery, query.getUser()));
      query.getGroups().forEach(group -> criteria.add(createGroupCriteria(organizationQuery, group)));
      organizationQuery.or(criteria.toArray(new Criteria[] {}));

      return organizationQuery;
   }

   private Criteria createUserCriteria(Query<MorphiaOrganization> organizationQuery, String user) {
      return organizationQuery.criteria(MorphiaOrganization.PERMISSIONS + "." + MorphiaPermissions.USER_ROLES)
                              .elemMatch(createPermissionQuery(user));
   }

   private Query<MorphiaPermission> createPermissionQuery(String name) {
      return datastore.createQuery(MorphiaPermission.class)
                      .filter(MorphiaPermission.NAME, name)
                      .field(MorphiaPermission.ROLES).in(Collections.singleton(Role.READ.toString()));
   }

   private Criteria createGroupCriteria(Query<MorphiaOrganization> organizationQuery, String group) {
      return organizationQuery.criteria(MorphiaOrganization.PERMISSIONS + "." + MorphiaPermissions.GROUP_ROLES)
                              .elemMatch(createPermissionQuery(group));
   }

}
