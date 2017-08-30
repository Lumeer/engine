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
package io.lumeer.storage.mongodb.dao.organization;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.mongodb.dao.MongoDao;

import com.mongodb.client.MongoDatabase;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class OrganizationScopedDao extends MongoDao {

   private Organization organization;

   @Inject
   private SelectedWorkspace selectedWorkspace;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @PostConstruct
   public void init() {
      this.database = (MongoDatabase) dataStorage.getDatabase();
      this.datastore = (AdvancedDatastore) dataStorage.getDataStore();

      if (selectedWorkspace.getOrganization().isPresent()) {
         this.organization = selectedWorkspace.getOrganization().get();
      }
   }

   public Optional<Organization> getOrganization() {
      return Optional.ofNullable(organization);
   }

   public void setOrganization(final Organization organization) {
      this.organization = organization;
   }
}
