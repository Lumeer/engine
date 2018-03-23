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
import java.util.Set;
import java.util.stream.Collectors;
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
   public Organization getOrganizationById(final String organizationId) {
      Organization organization = datastore.createQuery(MorphiaOrganization.class)
                                           .field(MorphiaOrganization.ID).equal(new ObjectId(organizationId))
                                           .get();
      if (organization == null) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return organization;
   }

   @Override
   public Set<String> getOrganizationsCodes() {
      return datastore.createQuery(MorphiaOrganization.class)
                      .asList().stream()
                      .map(MorphiaOrganization::getCode)
                      .collect(Collectors.toSet());
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
