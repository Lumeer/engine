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
import java.util.Set;
import java.util.stream.Collectors;
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
   public Set<String> getProjectsCodes() {
      return datastore.createQuery(databaseCollection(), MorphiaProject.class)
                      .disableValidation()
                      .asList().stream()
                      .map(MorphiaProject::getCode)
                      .collect(Collectors.toSet());
   }

   @Override
   public Set<String> getProjectsCodes(Organization organization) {
      return datastore.createQuery(databaseCollection(organization), MorphiaProject.class)
                      .disableValidation()
                      .asList().stream()
                      .map(MorphiaProject::getCode)
                      .collect(Collectors.toSet());
   }

   @Override
   public Project createProject(final Project project) {
      ensureIndexes();

      MorphiaProject morphiaProject = new MorphiaProject(project);
      datastore.insert(databaseCollection(), morphiaProject);
      return morphiaProject;
   }

   @Override
   public Project getProjectById(final String projectId) {
      Project project = datastore.createQuery(databaseCollection(), MorphiaProject.class)
                                 .field(MorphiaProject.ID).equal(new ObjectId(projectId))
                                 .get();
      if (project == null) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return project;
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
                                 .disableValidation()
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

   @Override
   public long getProjectsCount() {
      return datastore.createQuery(databaseCollection(), MorphiaProject.class).disableValidation().count();
   }

   private Query<MorphiaProject> createProjectQuery(DatabaseQuery query) {
      Query<MorphiaProject> projectQuery = datastore.createQuery(databaseCollection(), MorphiaProject.class).disableValidation();

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
