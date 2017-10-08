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
