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
package io.lumeer.storage.mongodb.dao.project;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Project;
import io.lumeer.storage.mongodb.dao.organization.OrganizationScopedDao;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class ProjectScopedDao extends OrganizationScopedDao {

   @Inject
   private SelectedWorkspace selectedWorkspace;

   private Project project;

   @PostConstruct
   public void init() {
      super.init();

      if (selectedWorkspace.getProject().isPresent()) {
         this.project = selectedWorkspace.getProject().get();
      }
   }

   public Optional<Project> getProject() {
      return Optional.ofNullable(project);
   }

   public void setProject(final Project project) {
      this.project = project;
   }
}
