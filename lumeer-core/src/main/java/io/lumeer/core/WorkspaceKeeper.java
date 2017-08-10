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
package io.lumeer.core;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;

import java.util.Optional;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class WorkspaceKeeper implements SelectedWorkspace {

   private Organization organization;
   private Project project;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Override
   public Optional<Organization> getOrganization() {
      return Optional.ofNullable(organization);
   }

   @Override
   public Optional<Project> getProject() {
      return Optional.ofNullable(project);
   }

   public void setOrganization(String organizationCode) {
      this.organization = organizationDao.getOrganizationByCode(organizationCode); // TODO cache this in the future
   }

   public void setProject(String projectCode) {
      this.project = projectDao.getProjectByCode(projectCode); // TODO cache this in the future
   }

   public void setWorkspace(String organizationCode, String projectCode) {
      setOrganization(organizationCode);
      setProject(projectCode);
   }
}
