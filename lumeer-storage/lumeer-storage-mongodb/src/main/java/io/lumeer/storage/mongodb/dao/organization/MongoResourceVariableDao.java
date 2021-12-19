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
package io.lumeer.storage.mongodb.dao.organization;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.ResourceVariable;
import io.lumeer.engine.api.event.CreateResourceVariable;
import io.lumeer.engine.api.event.ReloadResourceVariables;
import io.lumeer.engine.api.event.RemoveResourceVariable;
import io.lumeer.engine.api.event.UpdateResourceVariable;
import io.lumeer.storage.api.dao.ResourceVariableDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.ResourceVariableCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoResourceVariableDao extends MongoOrganizationScopedDao implements ResourceVariableDao {

   private static final String PREFIX = "resourceVariables_o-";

   @Inject
   private Event<CreateResourceVariable> createEvent;

   @Inject
   private Event<UpdateResourceVariable> updateEvent;

   @Inject
   private Event<RemoveResourceVariable> removeEvent;

   @Inject
   private Event<ReloadResourceVariables> reloadEvent;

   @Override
   public void createRepository(Organization organization) {
      database.createCollection(databaseCollectionName(organization));

      ensureIndexes(organization);
   }

   @Override
   public void deleteRepository(Organization organization) {
      database.getCollection(databaseCollectionName(organization)).drop();
   }

   @Override
   public void ensureIndexes(final Organization organization) {
      MongoCollection<Document> collection = database.getCollection(databaseCollectionName(organization));
      collection.createIndex(Indexes.ascending(ResourceVariableCodec.ORGANIZATION_ID, ResourceVariableCodec.PROJECT_ID, ResourceVariableCodec.RESOURCE_TYPE, ResourceVariableCodec.RESOURCE_ID, ResourceVariableCodec.KEY), new IndexOptions().unique(true));
   }

   @Override
   public ResourceVariable create(final ResourceVariable variable) {
      try {
         databaseCollection().insertOne(variable);

         if (createEvent != null) {
            createEvent.fire(new CreateResourceVariable(variable));
         }
         return variable;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create resource variable " + variable, ex);
      }
   }

   @Override
   public void create(final List<ResourceVariable> variables, final String organizationId, final String projectId) {
      if (variables != null && variables.size() > 0) {
         databaseCollection().insertMany(variables);

         if (reloadEvent != null) {
            reloadEvent.fire(new ReloadResourceVariables(organizationId, projectId));
         }
      }
   }

   @Override
   public ResourceVariable update(final String id, final ResourceVariable variable) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         ResourceVariable returnedVariable = databaseCollection().findOneAndReplace(idFilter(id), variable, options);
         if (returnedVariable == null) {
            throw new StorageException("Resource variable '" + id + "' has not been updated.");
         }
         if (updateEvent != null) {
            updateEvent.fire(new UpdateResourceVariable(returnedVariable));
         }
         return returnedVariable;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update resource variable " + variable, ex);
      }
   }

   @Override
   public void delete(final ResourceVariable variable) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(variable.getId()));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("Resource variable '" + variable.getId() + "' has not been deleted.");
      }
      if (removeEvent != null) {
         removeEvent.fire(new RemoveResourceVariable(variable));
      }
   }

   @Override
   public void deleteInProject(final String organizationId, final String projectId) {
      Bson filter = inProjectFilter(organizationId, projectId);
      databaseCollection().deleteMany(filter);
   }

   @Override
   public List<ResourceVariable> getInProject(final String organizationId, final String projectId) {
      Bson filter = inProjectFilter(organizationId, projectId);
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   private Bson inProjectFilter(final String organizationId, final String projectId) {
      Bson inProjectBson = Filters.and(Filters.eq(ResourceVariableCodec.ORGANIZATION_ID, organizationId), Filters.eq(ResourceVariableCodec.PROJECT_ID, projectId), Filters.in(ResourceVariableCodec.RESOURCE_TYPE, List.of(ResourceType.LINK_TYPE.toString(), ResourceType.COLLECTION.toString(), ResourceType.VIEW.toString())));
      Bson projectBson = Filters.and(Filters.eq(ResourceVariableCodec.ORGANIZATION_ID, organizationId), Filters.eq(ResourceVariableCodec.RESOURCE_TYPE, ResourceType.PROJECT.toString()), Filters.eq(ResourceVariableCodec.RESOURCE_ID, projectId));
      return Filters.or(inProjectBson, projectBson);
   }

   @Override
   public ResourceVariable getVariable(final String id) {
      MongoCursor<ResourceVariable> mongoCursor = databaseCollection().find(MongoFilters.idFilter(id)).iterator();
      if (!mongoCursor.hasNext()) {
         throw new StorageException("Resource variable '" + id + "' could not be found.");
      }
      return mongoCursor.next();
   }

   @Override
   public ResourceVariable getVariableByName(final String organizationId, final String projectId, final String varName) {
      final Bson filter = inProjectFilter(organizationId, projectId);
      final Bson nameFilter = Filters.eq(ResourceVariableCodec.KEY, varName);

      return databaseCollection().find(Filters.and(filter, nameFilter)).first();
   }

   MongoCollection<ResourceVariable> databaseCollection() {
      return database.getCollection(databaseCollectionName(), ResourceVariable.class);
   }

   String databaseCollectionName() {
      if (getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return databaseCollectionName(getOrganization().get());
   }

   private String databaseCollectionName(Organization organization) {
      return databaseCollectionName(organization.getId());
   }

   private String databaseCollectionName(String organizationId) {
      return PREFIX + organizationId;
   }
}
