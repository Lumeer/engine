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
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.model.MongoOrganization;

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
}
