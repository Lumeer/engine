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

import io.lumeer.api.model.DashboardData;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.storage.api.dao.DashboardDataDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Set;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class DashboardDataFacade extends AbstractFacade {

   @Inject
   private DashboardDataDao dashboardDataDao;

   public DashboardData update(DashboardData data) {
      checkPermissions();

      data.setUserId(getCurrentUserId());

      return dashboardDataDao.update(data);
   }

   public void deleteTypes(String type, Set<String> typeIds) {
      checkPermissions();

      dashboardDataDao.delete(type, typeIds, getCurrentUserId());
   }

   public List<DashboardData> getAll() {
      checkPermissions();

      return dashboardDataDao.getByUserId(getCurrentUserId());
   }

   public DashboardData getByType(String type, String typeId) {
      checkPermissions();

      return dashboardDataDao.getByTypeId(type, typeId, getCurrentUserId());
   }

   private Project checkPermissions() {
      if (workspaceKeeper.getProject().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }

      Project project = workspaceKeeper.getProject().get();
      permissionsChecker.checkRole(project, RoleType.Read);
      return project;
   }

}
