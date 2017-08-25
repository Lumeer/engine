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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MorphiaProject;

import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MorphiaProjectDao extends OrganizationScopedDao implements ProjectDao {

   private static final String PREFIX = "projects_o-";

   void ensureIndexes() {
      datastore.ensureIndexes(databaseCollection(), MorphiaProject.class); // TODO remove after createProjectsRepository() is implemented
   }

   @Override
   public void createProjectsRepository(Organization organization) {
      // TODO change the way user data storage is used
   }

   @Override
   public void deleteProjectsRepository(Organization organization) {
      // TODO change the way user data storage is used
   }

   @Override
   public Project createProject(final Project project) {
      ensureIndexes();

      MorphiaProject morphiaProject = new MorphiaProject(project);
      datastore.insert(databaseCollection(), morphiaProject);
      return morphiaProject;
   }

   @Override
   public Project updateProject(final String projectId, final Project project) {
      MorphiaProject morphiaProject = new MorphiaProject(project);
      morphiaProject.setId(projectId);
      datastore.save(databaseCollection(), morphiaProject);
      return morphiaProject;
   }

   @Override
   public void deleteProject(final String projectId) {
      WriteResult writeResult = datastore.delete(databaseCollection(), MorphiaProject.class, new ObjectId(projectId));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   @Override
   public Project getProjectByCode(final String projectCode) {
      Project project = datastore.createQuery(databaseCollection(), MorphiaProject.class)
                                 .field(MorphiaProject.CODE).equal(projectCode)
                                 .get();
      if (project == null) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return project;
   }

   @Override
   public List<Project> getProjects(final DatabaseQuery query) {
      Query<MorphiaProject> projectQuery = createProjectQuery(query);
      FindOptions findOptions = createFindOptions(query);

      return new ArrayList<>(projectQuery.asList(findOptions));
   }

   private Query<MorphiaProject> createProjectQuery(DatabaseQuery query) {
      Query<MorphiaProject> projectQuery = datastore.createQuery(databaseCollection(), MorphiaProject.class);

      projectQuery.or(createPermissionsCriteria(projectQuery, query));

      return projectQuery;
   }

   String databaseCollection() {
      if (!getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }
      return databaseCollection(getOrganization().get());
   }

   private String databaseCollection(Organization organization) {
      return PREFIX + organization.getId();
   }

}
