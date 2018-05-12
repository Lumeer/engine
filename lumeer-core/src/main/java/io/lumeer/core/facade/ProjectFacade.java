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
package io.lumeer.core.facade;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUserGroups;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
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

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public Project createProject(Project project) {
      checkOrganizationWriteRole();
      checkProjectCreate(project);

      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getCurrentUserId(), Project.ROLES);
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

      return mapResource(updatedProject);
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

      return mapResource(project);
   }

   public List<Project> getProjects() {
      String userId = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      DatabaseQuery query = DatabaseQuery.createBuilder(userId)
                                         .groups(groups)
                                         .build();

      return projectDao.getProjects(query).stream()
                       .map(this::mapResource)
                       .collect(Collectors.toList());
   }

   public Set<String> getProjectsCodes() {
      return projectDao.getProjectsCodes();
   }

   public Permissions getProjectPermissions(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);

      if (permissionsChecker.hasRole(project, Role.MANAGE)) {
         return project.getPermissions();
      } else if (permissionsChecker.hasRole(project, Role.READ)) {
         return keepOnlyActualUserRoles(project).getPermissions();
      }

      throw new NoPermissionException(project);
   }

   public Set<Permission> updateUserPermissions(final String projectCode, final Permission... userPermissions) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().updateUserPermissions(userPermissions);
      projectDao.updateProject(project.getId(), project);

      return project.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String projectCode, final String userId) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().removeUserPermission(userId);
      projectDao.updateProject(project.getId(), project);
   }

   public Set<Permission> updateGroupPermissions(final String projectCode, final Permission... groupPermissions) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().updateGroupPermissions(groupPermissions);
      projectDao.updateProject(project.getId(), project);

      return project.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String projectCode, final String groupId) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().removeGroupPermission(groupId);
      projectDao.updateProject(project.getId(), project);
   }

   private void createProjectScopedRepositories(Project project) {
      collectionDao.createCollectionsRepository(project);
      documentDao.createDocumentsRepository(project);
      viewDao.createViewsRepository(project);
      linkInstanceDao.createLinkInstanceRepository(project);
      linkTypeDao.createLinkTypeRepository(project);
   }

   private void deleleProjectScopedRepositories(Project project) {
      collectionDao.deleteCollectionsRepository(project);
      documentDao.deleteDocumentsRepository(project);
      viewDao.deleteViewsRepository(project);
      linkTypeDao.deleteLinkTypeRepository(project);
      linkInstanceDao.deleteLinkInstanceRepository(project);

      favoriteItemDao.removeFavoriteCollectionsByProjectFromUsers(project.getId());
      favoriteItemDao.removeFavoriteDocumentsByProjectFromUsers(project.getId());
   }

   private void checkOrganizationWriteRole() {
      if (!workspaceKeeper.getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      Organization organization = workspaceKeeper.getOrganization().get();
      permissionsChecker.checkRole(organization, Role.WRITE);
   }

   public int getCollectionsCount(Project project) {
      collectionDao.setProject(project);
      return (int) collectionDao.getCollectionsCount();
   }

   private void checkProjectCreate(final Project project) {
      permissionsChecker.checkCreationLimits(project, projectDao.getProjectsCount());
   }
}
