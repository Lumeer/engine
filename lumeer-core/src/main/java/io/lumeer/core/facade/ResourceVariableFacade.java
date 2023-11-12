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
package io.lumeer.core.facade;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceVariable;
import io.lumeer.api.model.RoleType;
import io.lumeer.core.adapter.ResourceVariableAdapter;
import io.lumeer.core.exception.UnsupportedOperationException;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ResourceVariableDao;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class ResourceVariableFacade extends AbstractFacade {

   @Inject
   private ResourceVariableDao resourceVariableDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   private ResourceVariableAdapter adapter;

   @PostConstruct
   public void init() {
      adapter = new ResourceVariableAdapter();
   }

   public ResourceVariable create(ResourceVariable variable) {
      checkPermissions(variable, RoleType.TechConfig);
      variable.setId(null);

      return mapVariable(resourceVariableDao.create(variable));
   }

   public void storeResourceVariables(List<ResourceVariable> variables, String organizationId, String projectId) {
      List<ResourceVariable> allowedVariables = variables.stream().filter(this::hasPermissions).collect(Collectors.toList());

      resourceVariableDao.create(allowedVariables, organizationId, projectId);
   }

   public ResourceVariable update(final String id, ResourceVariable variable) {
      ResourceVariable currentVariable = resourceVariableDao.getVariable(id);
      currentVariable.patch(variable);

      checkPermissions(currentVariable, RoleType.TechConfig);

      return mapVariable(resourceVariableDao.update(id, variable));
   }

   public void delete(String id) {
      ResourceVariable currentVariable = resourceVariableDao.getVariable(id);

      checkPermissions(currentVariable, RoleType.TechConfig);

      resourceVariableDao.delete(currentVariable);
   }

   public List<ResourceVariable> getInProject(String projectId) {
      checkProjectPermissions(projectId, RoleType.Read);

      return resourceVariableDao.getInProject(getOrganization().getId(), projectId)
                                .stream().map(this::mapVariable)
                                .collect(Collectors.toList());
   }

   public ResourceVariable getVariable(String id) {
      ResourceVariable currentVariable = resourceVariableDao.getVariable(id);

      checkPermissions(currentVariable, RoleType.TechConfig);

      return mapVariable(currentVariable);
   }

   private ResourceVariable mapVariable(ResourceVariable variable) {
      return adapter.mapVariable(variable);
   }

   private void checkPermissions(final ResourceVariable variable, final RoleType roleType) {
      switch (variable.getResourceType()) {
         case ORGANIZATION:
            Organization organization = organizationDao.getOrganizationById(variable.getOrganizationId());
            permissionsChecker.checkRole(organization, roleType);
            break;
         case PROJECT:
            checkProjectPermissions(variable.getProjectId(), roleType);
            break;
         default:
            throw new UnsupportedOperationException("Resource type '" + variable.getResourceType() + "' is not supported now");
      }
   }

   private boolean hasPermissions(final ResourceVariable variable) {
      switch (variable.getResourceType()) {
         case ORGANIZATION:
            Organization organization = organizationDao.getOrganizationById(variable.getOrganizationId());
            return permissionsChecker.hasRole(organization, RoleType.TechConfig);
         case PROJECT:
            Project project = projectDao.getProjectById(variable.getProjectId());
            return permissionsChecker.hasRole(project, RoleType.TechConfig);
         default:
            return false;
      }
   }

   private void checkProjectPermissions(final String projectId, final RoleType roleType) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, roleType);
   }

}
