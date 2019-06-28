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
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.core.util.Utils;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

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
   private WorkspaceCache workspaceCache;

   public Project createProject(Project project) {
      Utils.checkCodeSafe(project.getCode());
      checkOrganizationWriteRole();
      checkProjectCreate(project);

      Permission defaultUserPermission = Permission.buildWithRoles(authenticatedUser.getCurrentUserId(), Project.ROLES);
      project.getPermissions().updateUserPermissions(defaultUserPermission);

      Project storedProject = projectDao.createProject(project);

      createProjectScopedRepositories(storedProject);

      return storedProject;
   }

   public Project updateProject(final String projectId, final Project project) {
      Utils.checkCodeSafe(project.getCode());
      final Project storedProject = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(storedProject, Role.MANAGE);

      keepStoredPermissions(project, storedProject.getPermissions());
      keepUnmodifiableFields(project, storedProject);
      Project updatedProject = projectDao.updateProject(storedProject.getId(), project, storedProject);
      workspaceCache.updateProject(projectId, project);

      return mapResource(updatedProject);
   }

   public void deleteProject(final String projectId) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, Role.MANAGE);
      permissionsChecker.checkCanDelete(project);

      deleteProjectScopedRepositories(project);
      workspaceCache.removeProject(projectId);

      projectDao.deleteProject(project.getId());
   }

   public Project getProjectByCode(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.READ);

      return mapResource(project);
   }

   public Project getProjectById(final String projectId) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, Role.READ);

      return mapResource(project);
   }

   public List<Project> getProjects() {
      if (permissionsChecker.isManager()) {
         return getAllProjects();
      }
      return getProjectsByPermissions();
   }

   private List<Project> getAllProjects() {
      return projectDao.getAllProjects();
   }

   private List<Project> getProjectsByPermissions() {
      return projectDao.getProjects(createSimpleQuery()).stream()
                       .map(this::mapResource)
                       .collect(Collectors.toList());
   }

   public Set<String> getProjectsCodes() {
      return projectDao.getProjectsCodes();
   }

   public Permissions getProjectPermissions(final String projectId) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, Role.READ);

      return mapResource(project).getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String projectId, final Set<Permission> userPermissions) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, Role.MANAGE);

      final Project originalProject = project.copy();
      project.getPermissions().updateUserPermissions(userPermissions);
      projectDao.updateProject(project.getId(), project, originalProject);
      workspaceCache.updateProject(projectId, project);

      return project.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String projectId, final String userId) {
      final Project storedProject = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(storedProject, Role.MANAGE);

      final Project project = storedProject.copy();
      project.getPermissions().removeUserPermission(userId);
      projectDao.updateProject(project.getId(), project, storedProject);
      workspaceCache.updateProject(projectId, project);
   }

   public Set<Permission> updateGroupPermissions(final String projectId, final Set<Permission> groupPermissions) {
      final Project storedProject = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(storedProject, Role.MANAGE);

      final Project project = storedProject.copy();
      project.getPermissions().updateGroupPermissions(groupPermissions);
      projectDao.updateProject(project.getId(), project, storedProject);
      workspaceCache.updateProject(projectId, project);

      return project.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String projectId, final String groupId) {
      final Project storedProject = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(storedProject, Role.MANAGE);

      final Project project = storedProject.copy();
      project.getPermissions().removeGroupPermission(groupId);
      projectDao.updateProject(project.getId(), project, storedProject);
      workspaceCache.updateProject(projectId, project);
   }

   private void createProjectScopedRepositories(Project project) {
      collectionDao.createCollectionsRepository(project);
      documentDao.createDocumentsRepository(project);
      viewDao.createViewsRepository(project);
      linkInstanceDao.createLinkInstanceRepository(project);
      linkTypeDao.createLinkTypeRepository(project);
   }

   private void deleteProjectScopedRepositories(Project project) {
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
