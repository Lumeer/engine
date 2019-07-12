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
package io.lumeer.storage.mongodb.dao.system;

import static io.lumeer.storage.mongodb.util.MongoFilters.codeFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.mongodb.codecs.OrganizationCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@ApplicationScoped
public class MongoOrganizationDao extends SystemScopedDao implements OrganizationDao {

   public static final String COLLECTION_NAME = "organizations";

   @Inject
   private Event<CreateResource> createResourceEvent;

   @Inject
   private Event<UpdateResource> updateResourceEvent;

   @Inject
   private Event<RemoveResource> removeResourceEvent;

   @PostConstruct
   public void checkRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(databaseCollectionName())) {
         createOrganizationsRepository();
      }
   }

   public void createOrganizationsRepository() {
      database.createCollection(databaseCollectionName());

      MongoCollection<Document> userCollection = database.getCollection(databaseCollectionName());
      userCollection.createIndex(Indexes.ascending(OrganizationCodec.CODE), new IndexOptions().unique(true));
   }

   public void deleteOrganizationsRepository() {
      database.getCollection(databaseCollectionName()).drop();
   }

   @Override
   public Organization createOrganization(final Organization organization) {
      try {
         databaseCollection().insertOne(organization);
         if (createResourceEvent != null) {
            createResourceEvent.fire(new CreateResource(organization));
         }
         return organization;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create organization: " + organization, ex);
      }
   }

   @Override
   public Organization getOrganizationByCode(final String organizationCode) {
      return getOrganizationByFilter(codeFilter(organizationCode));
   }

   @Override
   public Organization getOrganizationById(final String organizationId) {
      return getOrganizationByFilter(idFilter(organizationId));
   }

   private Organization getOrganizationByFilter(Bson filter) {
      MongoCursor<Organization> mongoCursor = databaseCollection().find(filter).iterator();
      if (!mongoCursor.hasNext()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return mongoCursor.next();
   }

   @Override
   public Set<String> getOrganizationsCodes() {
      return databaseCollection().find().projection(Projections.include(OrganizationCodec.CODE))
                                 .into(new ArrayList<>()).stream()
                                 .map(Resource::getCode)
                                 .collect(Collectors.toSet());
   }

   @Override
   public List<Organization> getOrganizations(final DatabaseQuery query) {
      Bson filter = organizationsSearchFilter(query);
      FindIterable<Organization> iterable = databaseCollection().find(filter);
      addPaginationToQuery(iterable, query);

      return iterable.into(new ArrayList<>());
   }

   @Override
   public List<Organization> getAllOrganizations() {
      return databaseCollection().find().into(new ArrayList<>());
   }

   private static Bson organizationsSearchFilter(final DatabaseQuery query) {
      return MongoFilters.permissionsFilter(query);
   }

   @Override
   public void deleteOrganization(final String organizationId) {
      final Organization organization = databaseCollection().findOneAndDelete(idFilter(organizationId));
      if (organization == null) {
         throw new StorageException("Organization '" + organizationId + "' has not been deleted.");
      }
      if (removeResourceEvent != null) {
         removeResourceEvent.fire(new RemoveResource(organization));
      }
   }

   @Override
   public Organization updateOrganization(final String organizationId, final Organization organization) {
      return updateOrganization(organizationId, organization, null);
   }

   @Override
   public Organization updateOrganization(final String organizationId, final Organization organization, final Organization originalOrganization) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

      try {
         Bson update = new Document("$set", organization).append("$inc", new Document(OrganizationCodec.VERSION, 1L));
         Organization updatedOrganization = databaseCollection().findOneAndUpdate(idFilter(organizationId), update, options);
         if (updatedOrganization == null) {
            throw new StorageException("Organization '" + organizationId + "' has not been updated.");
         }

         if (updateResourceEvent != null) {
            updateResourceEvent.fire(new UpdateResource(updatedOrganization, originalOrganization));
         }
         return updatedOrganization;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update organization: " + organization, ex);
      }
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<Organization> databaseCollection() {
      return database.getCollection(databaseCollectionName(), Organization.class);
   }
}
