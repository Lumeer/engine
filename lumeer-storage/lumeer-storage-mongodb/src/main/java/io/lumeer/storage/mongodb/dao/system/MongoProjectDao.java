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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.model.MongoProject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoProjectDao extends SystemScopedDao implements ProjectDao {

   @PostConstruct
   public void ensureIndexes() {
      datastore.ensureIndexes(MongoProject.class);
   }

   @Override
   public Project createProject(final Project project) {
      MongoProject mongoProject = new MongoProject(project);
      datastore.save(mongoProject);
      return mongoProject;
   }

   @Override
   public Project getProjectByCode(final String projectCode) {
      Project project = datastore.createQuery(MongoProject.class)
                                 .field(MongoProject.CODE).equal(projectCode)
                                 .get();
      if (project == null) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return project;
   }
}
