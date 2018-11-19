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

import static io.lumeer.storage.mongodb.util.MongoFilters.codeFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.mongodb.codecs.ProjectCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.FindOneAndReplaceOptions;
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
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoProjectDao extends OrganizationScopedDao implements ProjectDao {

   private static final String PREFIX = "projects_o-";

   @Inject
   private Event<CreateResource> createResourceEvent;

   @Inject
   private Event<UpdateResource> updateResourceEvent;

   @Inject
   private Event<RemoveResource> removeResourceEvent;

   @Override
   public void createProjectsRepository(final Organization organization) {
      database.createCollection(databaseCollectionName(organization));

      MongoCollection<Document> collection = database.getCollection(databaseCollectionName(organization));
      collection.createIndex(Indexes.ascending(ProjectCodec.CODE), new IndexOptions().unique(true));
   }

   @Override
   public void deleteProjectsRepository(final Organization organization) {
      database.getCollection(databaseCollectionName()).drop();
   }

   @Override
   public Set<String> getProjectsCodes() {
      return databaseCollection().find().projection(Projections.include(ProjectCodec.CODE))
                                 .into(new ArrayList<>()).stream()
                                 .map(Resource::getCode)
                                 .collect(Collectors.toSet());
   }

   @Override
   public Project createProject(final Project project) {
      try {
         databaseCollection().insertOne(project);
         createResourceEvent.fire(new CreateResource(project));
         return project;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create project: " + project, ex);
      }
   }

   @Override
   public Project getProjectById(final String projectId) {
      return getProjectByFilter(idFilter(projectId));
   }

   @Override
   public Project getProjectByCode(final String projectCode) {
      return getProjectByFilter(codeFilter(projectCode));
   }

   private Project getProjectByFilter(Bson filter) {
      MongoCursor<Project> mongoCursor = databaseCollection().find(filter).iterator();
      if (!mongoCursor.hasNext()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return mongoCursor.next();
   }

   @Override
   public List<Project> getProjects(final DatabaseQuery query) {
      Bson filter = organizationsSearchFilter(query);
      FindIterable<Project> iterable = databaseCollection().find(filter);
      addPaginationToQuery(iterable, query);

      return iterable.into(new ArrayList<>());
   }

   private static Bson organizationsSearchFilter(final DatabaseQuery query) {
      return MongoFilters.permissionsFilter(query);
   }

   @Override
   public long getProjectsCount() {
      return databaseCollection().find().into(new ArrayList<>()).size();
   }

   @Override
   public void deleteProject(final String projectId) {
      final Project project = databaseCollection().findOneAndDelete(idFilter(projectId));
      if (project == null) {
         throw new StorageException("Project '" + projectId + "' has not been deleted.");
      }
      removeResourceEvent.fire(new RemoveResource(project));
   }

   @Override
   public Project updateProject(final String projectId, final Project project) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);

      try {
         Project updatedProject = databaseCollection().findOneAndReplace(idFilter(projectId), project, options);
         if (updatedProject == null) {
            throw new StorageException("Project '" + projectId + "' has not been updated.");
         }
         updateResourceEvent.fire(new UpdateResource(updatedProject));
         return updatedProject;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update project: " + project, ex);
      }
   }

   String databaseCollectionName() {
      if (!getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return databaseCollectionName(getOrganization().get());
   }

   private String databaseCollectionName(Organization organization) {
      return PREFIX + organization.getId();
   }

   MongoCollection<Project> databaseCollection() {
      return database.getCollection(databaseCollectionName(), Project.class);
   }

}
