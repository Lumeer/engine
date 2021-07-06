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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectContent;
import io.lumeer.api.model.ProjectDescription;
import io.lumeer.api.model.ProjectMeta;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.templateParse.CollectionWithId;
import io.lumeer.api.model.templateParse.DocumentWithId;
import io.lumeer.api.model.templateParse.LinkInstanceWithId;
import io.lumeer.api.model.templateParse.LinkTypeWithId;
import io.lumeer.api.model.templateParse.ViewWithId;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.AuditDao;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.SequenceDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ProjectFacade extends AbstractFacade {

   private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

   static {
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
   }

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private WorkspaceCache workspaceCache;

   @Inject
   private SequenceDao sequenceDao;

   @Inject
   private DelayedActionDao delayedActionDao;

   @Inject
   private AuditDao auditDao;

   @Inject
   private EventLogFacade eventLogFacade;

   void init(DaoContextSnapshot daoContextSnapshot) {
      this.collectionDao = daoContextSnapshot.getCollectionDao();
      this.documentDao = daoContextSnapshot.getDocumentDao();
      this.dataDao = daoContextSnapshot.getDataDao();
      this.projectDao = daoContextSnapshot.getProjectDao();
      this.viewDao = daoContextSnapshot.getViewDao();
      this.linkTypeDao = daoContextSnapshot.getLinkTypeDao();
      this.linkInstanceDao = daoContextSnapshot.getLinkInstanceDao();
      this.linkDataDao = daoContextSnapshot.getLinkDataDao();
      this.favoriteItemDao = daoContextSnapshot.getFavoriteItemDao();
      this.sequenceDao = daoContextSnapshot.getSequenceDao();
   }

   public Project createProject(Project project) {
      Utils.checkCodeSafe(project.getCode());
      checkOrganizationRole(RoleType.ProjectContribute);
      checkProjectCreate(project);

      Permission defaultUserPermission = Permission.buildWithRoles(getCurrentUserId(), Project.ROLES);
      project.getPermissions().updateUserPermissions(defaultUserPermission);

      Project storedProject = projectDao.createProject(project);

      createProjectScopedRepositories(storedProject);

      eventLogFacade.logEvent(authenticatedUser.getCurrentUser(), "Created project " + project.getCode());

      return storedProject;
   }

   public Project updateProject(final String projectId, final Project project) {
      Utils.checkCodeSafe(project.getCode());
      final Project storedProject = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(storedProject, RoleType.Manage);

      Project updatingProject = storedProject.copy();
      updatingProject.patch(project, permissionsChecker.getActualRoles(storedProject));
      keepUnmodifiableFields(updatingProject, storedProject);

      Project updatedProject = projectDao.updateProject(projectId, updatingProject, storedProject);
      workspaceCache.updateProject(projectId, updatedProject);

      return mapResource(updatedProject);
   }

   public void deleteProject(final String projectId) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkCanDelete(project);

      deleteProjectScopedRepositories(project);
      workspaceCache.removeProject(projectId);

      projectDao.deleteProject(project.getId());
   }

   public Project getProjectByCode(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, RoleType.Read);

      return mapResource(project);
   }

   public Project getProjectById(final String projectId) {
      return getProject(projectId, RoleType.Read);
   }

   private Project getProject(final String projectId, final RoleType role) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, role);

      return mapResource(project);
   }

   public Project getPublicProjectById(final String projectId) {
      final Project project = projectDao.getProjectById(projectId);

      if (!project.isPublic()) {
         throw new NoResourcePermissionException(project);
      }

      return setupPublicPermissions(project);
   }

   public List<Project> getProjects() {
      checkOrganizationRole(RoleType.Read);

      return getAllProjects().stream()
                             .filter(project -> permissionsChecker.hasRole(project, RoleType.Read))
                             .map(this::mapResource)
                             .collect(Collectors.toList());
   }

   public List<Project> getPublicProjects() {
      return getAllProjects().stream().filter(Project::isPublic).collect(Collectors.toList());
   }

   public Project getPublicProject(String code) {
      var project = getProjectByCode(code);
      if (project.isPublic()) {
         return project;
      }
      throw new ResourceNotFoundException(ResourceType.PROJECT);
   }

   private List<Project> getAllProjects() {
      return projectDao.getAllProjects();
   }

   public Set<String> getProjectsCodes() {
      return projectDao.getProjectsCodes();
   }

   public Permissions getProjectPermissions(final String projectId) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, RoleType.UserConfig);

      return mapResource(project).getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String projectId, final Set<Permission> userPermissions) {
      return updateUserPermissions(projectId, userPermissions, true);
   }

   public Set<Permission> addUserPermissions(final String projectId, final Set<Permission> userPermissions) {
      return updateUserPermissions(projectId, userPermissions, false);
   }

   public Set<Permission> updateUserPermissions(final String projectId, final Set<Permission> userPermissions, boolean update) {
      Project project = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(project, RoleType.UserConfig);

      final Project originalProject = project.copy();
      if (update) {
         project.getPermissions().updateUserPermissions(userPermissions);
      } else {
         project.getPermissions().addUserPermissions(userPermissions);
      }
      projectDao.updateProject(project.getId(), project, originalProject);
      workspaceCache.updateProject(projectId, project);

      return project.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String projectId, final String userId) {
      final Project storedProject = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(storedProject, RoleType.UserConfig);

      final Project project = storedProject.copy();
      project.getPermissions().removeUserPermission(userId);
      projectDao.updateProject(project.getId(), project, storedProject);
      workspaceCache.updateProject(projectId, project);
   }

   public Set<Permission> updateGroupPermissions(final String projectId, final Set<Permission> groupPermissions) {
      permissionsChecker.checkGroupsHandle();

      final Project storedProject = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(storedProject, RoleType.UserConfig);

      final Project project = storedProject.copy();
      project.getPermissions().updateGroupPermissions(groupPermissions);
      projectDao.updateProject(project.getId(), project, storedProject);
      workspaceCache.updateProject(projectId, project);

      return project.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String projectId, final String groupId) {
      permissionsChecker.checkGroupsHandle();

      final Project storedProject = projectDao.getProjectById(projectId);
      permissionsChecker.checkRole(storedProject, RoleType.UserConfig);

      final Project project = storedProject.copy();
      project.getPermissions().removeGroupPermission(groupId);
      projectDao.updateProject(project.getId(), project, storedProject);
      workspaceCache.updateProject(projectId, project);
   }

   private void createProjectScopedRepositories(Project project) {
      collectionDao.createRepository(project);
      documentDao.createRepository(project);
      viewDao.createRepository(project);
      linkInstanceDao.createRepository(project);
      linkTypeDao.createRepository(project);
      sequenceDao.createRepository(project);
      auditDao.createRepository(project);
   }

   private void deleteProjectScopedRepositories(Project project) {
      collectionDao.deleteRepository(project);
      documentDao.deleteRepository(project);
      viewDao.deleteRepository(project);
      linkTypeDao.deleteRepository(project);
      linkInstanceDao.deleteRepository(project);
      sequenceDao.deleteRepository(project);
      auditDao.deleteRepository(project);

      favoriteItemDao.removeFavoriteCollectionsByProjectFromUsers(project.getId());
      favoriteItemDao.removeFavoriteDocumentsByProjectFromUsers(project.getId());

      if (workspaceKeeper.getOrganization().isPresent()) {
         delayedActionDao.deleteAllScheduledActions(workspaceKeeper.getOrganization().get().getId() + "/" + project.getId());
      }
   }

   private void checkOrganizationRole(RoleType role) {
      if (workspaceKeeper.getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      Organization organization = workspaceKeeper.getOrganization().get();
      permissionsChecker.checkRole(organization, role);
   }

   public int getCollectionsCount(Project project) {
      collectionDao.setProject(project);
      return (int) collectionDao.getCollectionsCount();
   }

   private void checkProjectCreate(final Project project) {
      permissionsChecker.checkCreationLimits(project, projectDao.getProjectsCount());
   }

   public ProjectContent getRawProjectContent(final String projectId) {
      final Project storedProject = projectDao.getProjectById(projectId);
      if (!storedProject.isPublic() && !permissionsChecker.canReadAllInWorkspace()) {
         throw new NoResourcePermissionException(storedProject);
      }

      final ProjectContent content = new ProjectContent();

      content.setCollections(collectionDao.getAllCollections().stream().map(CollectionWithId::new).collect(Collectors.toList()));
      content.setViews(viewDao.getAllViews().stream().map(ViewWithId::new).collect(Collectors.toList()));
      content.setLinkTypes(linkTypeDao.getAllLinkTypes().stream().map(LinkTypeWithId::new).collect(Collectors.toList()));
      String userId = Utils.computeIfNotNull(authenticatedUser, AuthenticatedUser::getCurrentUserId);
      if (userId != null) {
         content.setFavoriteCollectionIds(favoriteItemDao.getFavoriteCollectionIds(userId, projectId));
         content.setFavoriteViewIds(favoriteItemDao.getFavoriteViewIds(userId, projectId));
      } else {
         content.setFavoriteCollectionIds(favoriteItemDao.getFavoriteCollectionIds(projectId));
         content.setFavoriteViewIds(favoriteItemDao.getFavoriteViewIds(projectId));
      }
      content.setSequences(sequenceDao.getAllSequences());

      final List<LinkInstanceWithId> linkInstances = new ArrayList<>();
      final Map<String, List<DataDocument>> linksData = new HashMap<>();
      content.getLinkTypes().forEach(lt -> {
         linksData.put(lt.getId(), linkDataDao.getDataStream(lt.getId()).map(this::translateDataDocument).collect(Collectors.toList()));
         linkInstances.addAll(linkInstanceDao.getLinkInstancesByLinkType(lt.getId()).stream().map(LinkInstanceWithId::new).collect(Collectors.toList()));
      });
      content.setLinkInstances(linkInstances);
      content.setLinkData(linksData);

      final List<DocumentWithId> documents = new ArrayList<>();
      final Map<String, List<DataDocument>> documentsData = new HashMap<>();
      content.getCollections().forEach(c -> {
         documentsData.put(c.getId(), dataDao.getDataStream(c.getId()).map(this::translateDataDocument).collect(Collectors.toList()));
         documents.addAll(documentDao.getDocumentsByCollection(c.getId()).stream().map(DocumentWithId::new).collect(Collectors.toList()));
      });
      content.setDocuments(documents);
      content.setData(documentsData);

      content.setTemplateMeta(
            new ProjectMeta(
                  storedProject.getCode(),
                  content.getCollections().size(),
                  content.getLinkTypes().size(),
                  content.getViews().size(),
                  content.getDocuments().size()
            ));

      return content;
   }

   private DataDocument translateDataDocument(final DataDocument doc) {
      doc.keySet().forEach(k -> {
         var v = doc.get(k);
         if (v instanceof Date) {
            doc.put(k, sdf.format(v));
         } else if (v instanceof DataDocument) {
            translateDataDocument((DataDocument) v);
         }
      });

      return doc;
   }

   public void switchOrganization() {
      workspaceKeeper.getOrganization().ifPresent(o -> {
         permissionsChecker.checkRole(o, RoleType.Read);
         projectDao.switchOrganization();
      });
   }

   public ProjectDescription getProjectDescription() {
      if (!permissionsChecker.isPublic() && !permissionsChecker.hasRole(getProject(), RoleType.Read)) {
         return null;
      }

      final List<Collection> collections = collectionDao.getAllCollections();

      long documentsCount = permissionsChecker.countDocuments();
      long maxFunctions = collections.stream().mapToLong(c ->
            c.getAttributes().stream().filter(a -> a.getFunction() != null).count()
      ).max().orElse(0);
      long maxRules = collections.stream().mapToLong(c -> c.getRules().size()).max().orElse(0);

      final List<LinkType> linkTypes = linkTypeDao.getAllLinkTypes();
      long maxLinkFunctions = linkTypes.stream().mapToLong(l ->
            l.getAttributes().stream().filter(a -> a.getFunction() != null).count()
      ).max().orElse(0);
      long maxLinkRules = linkTypes.stream().mapToLong(l -> l.getRules().size()).max().orElse(0);

      maxFunctions = Math.max(maxFunctions, maxLinkFunctions);
      maxRules = Math.max(maxRules, maxLinkRules);

      return new ProjectDescription(collections.size(), documentsCount, maxFunctions, maxRules);
   }

   public void emptyTemplateData(final String projectId) {
      Project project = projectDao.getProjectById(projectId);

      if (project != null) {
         permissionsChecker.checkRole(project, RoleType.Manage);

         final List<Document> documents = documentDao.getDocumentsWithTemplateId();
         final List<LinkInstance> links = linkInstanceDao.getLinkInstancesByDocumentIds(documents.stream().map(Document::getId).collect(Collectors.toSet()));

         links.forEach(l -> {
            linkInstanceDao.deleteLinkInstance(l.getId(), linkDataDao.getData(l.getLinkTypeId(), l.getId()));
            linkDataDao.deleteData(l.getLinkTypeId(), l.getId());
         });

         documents.forEach(d -> {
            documentDao.deleteDocument(d.getId(), dataDao.getData(d.getCollectionId(), d.getId()));
            dataDao.deleteData(d.getCollectionId(), d.getId());
         });
      }
   }
}
