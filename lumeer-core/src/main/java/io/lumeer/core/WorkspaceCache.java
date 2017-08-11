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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class WorkspaceCache {

   @Inject
   private CacheFactory cacheFactory;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   private Cache<Organization> organizationCache;
   private Cache<Project> projectCache;

   @PostConstruct
   public void initCaches() {
      organizationCache = cacheFactory.getCache();
      projectCache = cacheFactory.getCache();
   }

   public Organization getOrganization(String organizationCode) {
      return organizationCache.computeIfAbsent(organizationCode, code -> organizationDao.getOrganizationByCode(code));
   }

   public Project getProject(String projectCode) {
      return projectCache.computeIfAbsent(projectCode, code -> projectDao.getProjectByCode(code));
   }

   public void updateOrganization(String organizationCode, Organization organization) {
      organizationCache.set(organizationCode, organization);
   }

   public void updateProject(String projectCode, Project project) {
      projectCache.set(projectCode, project);
   }

   public void removeOrganization(String organizationCode) {
      organizationCache.remove(organizationCode);
   }

   public void removeProject(String projectCode) {
      projectCache.remove(projectCode);
   }

   public void clear() {
      organizationCache.clear();
      projectCache.clear();
   }

}
