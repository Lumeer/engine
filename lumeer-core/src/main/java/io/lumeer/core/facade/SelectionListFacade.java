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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.SelectOption;
import io.lumeer.api.model.SelectionList;
import io.lumeer.api.util.AttributeUtil;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.util.SelectionListUtils;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.SelectionListDao;

import java.util.ArrayList;
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

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   public SelectionList createList(SelectionList list) {
      checkWritePermissions(list.getProjectId());
      list.setId(null);

      return selectionListDao.createList(list);
   }

   public void createSampleLists(String projectId) {
      checkWritePermissions(projectId);

      if (selectionListDao.getAllLists(List.of(projectId)).isEmpty()) {
         var predefinedLists = SelectionListUtils.getPredefinedLists(requestDataKeeper.getUserLanguage(), getOrganization().getId(), projectId);
         selectionListDao.createLists(predefinedLists, projectId);
      }

   }

   public SelectionList updateList(final String id, SelectionList list) {
      SelectionList currentList = selectionListDao.getList(id);
      checkWritePermissions(currentList.getProjectId());

      List<SelectOption> previousOptions = new ArrayList<>(list.getOptions());
      currentList.patch(list);

      SelectionList newList = selectionListDao.updateList(list.getId(), currentList);
      checkAttributesSelectOptionsSnapshot(newList.getProjectId(), newList.getId(), previousOptions, newList.getOptions());
      return newList;
   }

   private void checkAttributesSelectOptionsSnapshot(final String projectId, final String selectionId, final List<SelectOption> previousOptions, final List<SelectOption> currentOptions) {
      if (previousOptions == null || currentOptions == null || previousOptions.equals(currentOptions)) {
         return;
      }

      selectedWorkspace.setProjectId(projectId);
      List<Collection> collections = collectionDao.getAllCollections();
      collections.forEach(collection -> {
         var shouldUpdate = false;
         for (Attribute attribute : collection.getAttributes()) {
            if (AttributeUtil.isSelectWithSelectionList(attribute, selectionId)) {
               shouldUpdate = true;
               AttributeUtil.setSelectConfigOptions(attribute, currentOptions);
            }
         }

         if (shouldUpdate) {
            collectionDao.updateCollection(collection.getId(), collection, collection, false);
         }

      });

   }

   public void deleteList(String id) {
      SelectionList currentList = selectionListDao.getList(id);
      checkWritePermissions(currentList.getProjectId());

      selectionListDao.deleteList(currentList);
   }

   public List<SelectionList> getLists(String projectId) {
      checkReadPermissions(projectId);

      return selectionListDao.getAllLists(List.of(projectId));
   }

   public SelectionList getList(String id) {
      SelectionList currentList = selectionListDao.getList(id);
      checkReadPermissions(currentList.getProjectId());

      return currentList;
   }

   public List<SelectionList> getAllLists() {
      List<String> readableProjects = projectDao.getAllProjects().stream()
                                                .filter(project -> permissionsChecker.hasRole(project, RoleType.Read))
                                                .map(Project::getId)
                                                .collect(Collectors.toList());

      if (readableProjects.isEmpty()) {
         return Collections.emptyList();
      }

      return selectionListDao.getAllLists(readableProjects);
   }

   private Project checkReadPermissions(final String projectId) {
      return checkPermissions(projectId, RoleType.Read);
   }

   private Project checkWritePermissions(final String projectId) {
      return checkPermissions(projectId, RoleType.TechConfig);
   }

   private Project checkPermissions(final String projectId, final RoleType roleType) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, roleType);

      return project;
   }

}
