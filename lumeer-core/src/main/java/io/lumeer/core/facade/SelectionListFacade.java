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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.SelectionList;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.SelectionListDao;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class SelectionListFacade extends AbstractFacade {

   @Inject
   private SelectionListDao selectionListDao;

   @Inject
   private ProjectDao projectDao;

   public SelectionList createList(SelectionList list) {
      checkProjectPermissions(list.getProjectId());
      list.setId(null);

      return selectionListDao.createList(list);
   }

   public SelectionList updateList(final String id, SelectionList list) {
      SelectionList currentList = selectionListDao.getList(id);
      checkProjectPermissions(currentList.getProjectId());

      currentList.patch(list);

      // TODO check attribute options snapshots

      return selectionListDao.updateList(list.getId(), currentList);
   }

   public void deleteList(String id) {
      SelectionList currentList = selectionListDao.getList(id);
      checkProjectPermissions(currentList.getProjectId());

      selectionListDao.deleteList(currentList);
   }

   public List<SelectionList> getLists() {
      List<String> readableProjects = projectDao.getAllProjects().stream()
                                                .filter(project -> permissionsChecker.hasRole(project, RoleType.Read))
                                                .map(Project::getId)
                                                .collect(Collectors.toList());

      if (readableProjects.isEmpty()) {
         return Collections.emptyList();
      }

      return selectionListDao.getAllLists(readableProjects);
   }

   private Project checkProjectPermissions(final String projectId) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, RoleType.TechConfig);

      return project;
   }

}
