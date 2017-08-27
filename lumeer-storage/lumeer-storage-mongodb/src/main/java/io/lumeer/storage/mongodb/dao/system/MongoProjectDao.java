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
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.storage.api.DatabaseQuery;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MongoProject;
import io.lumeer.storage.mongodb.model.embedded.MongoPermission;
import io.lumeer.storage.mongodb.model.embedded.MongoPermissions;

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
public class MongoProjectDao extends SystemScopedDao implements ProjectDao {

   private Organization organization;

   @PostConstruct
   public void ensureIndexes() {
      datastore.ensureIndexes(MongoProject.class);
   }

   @Override
   public Project createProject(final Project project) {
      MongoProject mongoProject = new MongoProject(project);
      mongoProject.setOrganizationId(organization.getId());
      datastore.save(projectCollection(), mongoProject);
      return mongoProject;
   }

   @Override
   public Project getProjectByCode(final String projectCode) {
      Project project = datastore.createQuery(projectCollection(), MongoProject.class)
                                 .field(MongoProject.CODE).equal(projectCode)
                                 .get();
      if (project == null) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return project;
   }

   private Query<MongoProject> createProjectQuery(DatabaseQuery query) {
      Query<MongoProject> projectQuery = datastore.createQuery(projectCollection(), MongoProject.class);

      List<Criteria> criteria = new ArrayList<>();
      criteria.add(createUserCriteria(projectQuery, query.getUser()));
      query.getGroups().forEach(group -> criteria.add(createGroupCriteria(projectQuery, group)));
      projectQuery.or(criteria.toArray(new Criteria[] {}));

      return projectQuery;
   }

   private Criteria createUserCriteria(Query<MongoProject> projectQuery, String user) {
      return projectQuery.criteria(MongoProject.PERMISSIONS + "." + MongoPermissions.USER_ROLES)
                              .elemMatch(createPermissionQuery(user));
   }

   private Query<MongoPermission> createPermissionQuery(String name) {
      return datastore.createQuery(MongoPermission.class)
                      .filter(MongoPermission.NAME, name)
                      .field(MongoPermission.ROLES).in(Collections.singleton(Role.READ.toString()));
   }

   private Criteria createGroupCriteria(Query<MongoProject> projectQuery, String group) {
      return projectQuery.criteria(MongoProject.PERMISSIONS + "." + MongoPermissions.GROUP_ROLES)
                              .elemMatch(createPermissionQuery(group));
   }

   private FindOptions createFindOptions(DatabaseQuery query) {
      FindOptions findOptions = new FindOptions();
      Integer page = query.getPage();
      Integer pageSize = query.getPageSize();

      if (page != null && pageSize != null) {
         findOptions.skip(page * pageSize)
                    .limit(pageSize);
      }

      return findOptions;
   }

   @Override
   public List<Project> getProjects(final DatabaseQuery query) {
      Query<MongoProject> projectQuery = createProjectQuery(query);
      FindOptions findOptions = createFindOptions(query);

      return new ArrayList<>(projectQuery.asList(findOptions));
   }

   @Override
   public void deleteProject(final String projectId) {
      WriteResult writeResult = datastore.delete(projectCollection(), MongoProject.class, new ObjectId(projectId));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   @Override
   public Project updateProject(final String projectId, final Project project) {
      MongoProject mongoProject = new MongoProject(project);
      mongoProject.setId(new ObjectId(projectId));
      mongoProject.setOrganizationId(organization.getId());
      datastore.save(projectCollection(), mongoProject);
      return mongoProject;
   }

   @Override
   public void setOrganization(final Organization organization) {
      this.organization = organization;
   }

   String projectCollection() {
      return LumeerConst.Project.COLLECTION_NAME;
   }

}
