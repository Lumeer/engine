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
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

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
      checkPermissions(variable);
      variable.setId(null);

      return mapVariable(resourceVariableDao.create(variable));
   }

   public ResourceVariable update(final String id, ResourceVariable variable) {
      ResourceVariable currentVariable = resourceVariableDao.getVariable(id);
      currentVariable.patch(variable);

      checkPermissions(currentVariable);

      return mapVariable(resourceVariableDao.update(id, variable));
   }

   public void delete(String id) {
      ResourceVariable currentVariable = resourceVariableDao.getVariable(id);

      checkPermissions(currentVariable);

      resourceVariableDao.delete(currentVariable);
   }

   public List<ResourceVariable> getByProject(String projectId) {
      checkProjectPermissions(projectId);

      return resourceVariableDao.getByProject(getOrganization().getId(), projectId)
                                .stream().map(this::mapVariable)
                                .collect(Collectors.toList());
   }

   public ResourceVariable getVariable(String id) {
      ResourceVariable currentVariable = resourceVariableDao.getVariable(id);

      checkPermissions(currentVariable);

      return mapVariable(currentVariable);
   }

   private ResourceVariable mapVariable(ResourceVariable variable) {
      return adapter.mapVariable(variable);
   }

   private void checkPermissions(final ResourceVariable variable) {
      switch (variable.getResourceType()) {
         case ORGANIZATION:
            Organization organization = organizationDao.getOrganizationById(variable.getOrganizationId());
            permissionsChecker.checkRole(organization, RoleType.TechConfig);
            break;
         case PROJECT:
            checkProjectPermissions(variable.getProjectId());
            break;
         default:
            throw new UnsupportedOperationException("Resource type '" + variable.getResourceType() + "' is not supported now");
      }
   }

   private void checkProjectPermissions(final String projectId) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, RoleType.TechConfig);
   }

}
