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
package io.lumeer.core.cache;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class WorkspaceCache {

   @Inject
   private CacheFactory cacheFactory;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   private Cache<Organization> organizationCache;
   private Cache<Project> projectCache;
   private Cache<ServiceLimits> serviceLimitsCache;
   private Cache<List<String>> userCollections;
   private Cache<List<String>> systemCollections;

   @PostConstruct
   public void initCaches() {
      organizationCache = cacheFactory.getCache();
      projectCache = cacheFactory.getCache();
      serviceLimitsCache = cacheFactory.getCache();
      userCollections = cacheFactory.getCache();
      systemCollections = cacheFactory.getCache();
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

   public void setServiceLimits(final String organizationCode, final ServiceLimits serviceLimits) {
      serviceLimitsCache.set(organizationCode, serviceLimits);
   }

   public ServiceLimits getServiceLimits(final String organizationCode) {
      return serviceLimitsCache.get(organizationCode);
   }

   public void removeServiceLimits(final String organizationCode) {
      serviceLimitsCache.remove(organizationCode);
   }

   public void clear() {
      organizationCache.clear();
      projectCache.clear();
   }
}
