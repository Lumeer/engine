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
package io.lumeer.core.facade;

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ProjectFacade extends AbstractFacade {

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ViewDao viewDao;

   public Project createProject(Project project) {
      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getCurrentUsername(), Project.ROLES);
      project.getPermissions().updateUserPermissions(defaultUserPermission);

      Project storedProject = projectDao.createProject(project);

      createProjectScopedRepositories(storedProject);

      return storedProject;
   }

   public Project updateProject(final String projectCode, final Project project) {
      Project storedProject = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(storedProject, Role.MANAGE);

      keepStoredPermissions(project, storedProject.getPermissions());
      Project updatedProject = projectDao.updateProject(storedProject.getId(), project);

      return keepOnlyActualUserRoles(updatedProject);
   }

   public void deleteProject(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      deleleProjectScopedRepositories(project);

      projectDao.deleteProject(project.getId());
   }

   public Project getProject(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.READ);

      return keepOnlyActualUserRoles(project);
   }

   public List<Project> getProjects() {
      User user = authenticatedUser.getCurrentUser();
      DatabaseQuery query = DatabaseQuery.createBuilder(user.getUsername())
                                         .groups(user.getGroups())
                                         .build();

      return projectDao.getProjects(query).stream()
                       .map(this::keepOnlyActualUserRoles)
                       .collect(Collectors.toList());
   }

   public Permissions getProjectPermissions(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      return project.getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String projectCode, final Permission... userPermissions) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().updateUserPermissions(userPermissions);
      projectDao.updateProject(project.getId(), project);

      return project.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String projectCode, final String user) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().removeUserPermission(user);
      projectDao.updateProject(project.getId(), project);
   }

   public Set<Permission> updateGroupPermissions(final String projectCode, final Permission... groupPermissions) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().updateGroupPermissions(groupPermissions);
      projectDao.updateProject(project.getId(), project);

      return project.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String projectCode, final String group) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().removeGroupPermission(group);
      projectDao.updateProject(project.getId(), project);
   }

   private void createProjectScopedRepositories(Project project) {
      collectionDao.createCollectionsRepository(project);
      documentDao.createDocumentsRepository(project);
      viewDao.createViewsRepository(project);
   }

   private void deleleProjectScopedRepositories(Project project) {
      collectionDao.deleteCollectionsRepository(project);
      documentDao.deleteDocumentsRepository(project);
      viewDao.deleteViewsRepository(project);
   }
}
