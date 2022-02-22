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

import io.lumeer.api.model.Language;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectContent;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.SampleDataType;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.core.provider.DataStorageProvider;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshotFactory;

import java.util.Date;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class CopyFacade extends AbstractFacade {

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private TemplateFacade templateFacade;

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Inject
   private DaoContextSnapshotFactory daoContextSnapshotFactory;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private EventLogFacade eventLogFacade;

   @Inject
   private WorkspaceCache workspaceCache;

   private Language language;

   @PostConstruct
   public void init() {
      language = requestDataKeeper.getUserLanguage();
   }

   public void deepCopySampleData(Project project, SampleDataType sampleType) {
      checkProjectContribute(project);

      var organizationId = templateFacade.getSampleDataOrganizationId(language);
      copyProjectByCode(project, organizationId, sampleType.toString());
   }

   public void deepCopyTemplate(Project project, String templateId) {
      checkProjectContribute(project);

      var organizationId = templateFacade.getTemplateOrganizationId(language);
      copyProjectById(project, organizationId, templateId);
   }

   public void deepCopyProject(Project project, String organizationId, String projectId) {
      checkProjectContribute(project);

      copyProjectById(project, organizationId, projectId);
   }

   private void copyProjectById(Project project, String organizationId, String projectId) {
      //uncomment to use predefined templates in JSON files
      //templateFacade.installTemplate(project, "PIVOT", Language.EN);
      copyProject(project, organizationId, dao -> dao.getProjectById(projectId));
   }

   private void copyProjectByCode(Project project, String organizationId, String projectCode) {
      copyProject(project, organizationId, dao -> dao.getProjectByCode(projectCode));
   }

   public void installProjectContent(final Project project, final String organizationId, final ProjectContent projectContent) {
      checkProjectContribute(project);

      templateFacade.installTemplate(project, organizationId, projectContent, new Date());

      eventLogFacade.logEvent(authenticatedUser.getCurrentUser(), "Imported to project: " + getOrganization().getCode() + " / " + project.getCode());
   }

   private void copyProject(Project project, String organizationId, java.util.function.Function<ProjectDao, Project> projectFunction) {
      final StringBuilder sb = new StringBuilder();

      var fromOrganization = organizationDao.getOrganizationById(organizationId);
      workspaceKeeper.push();
      workspaceKeeper.setOrganization(fromOrganization);

      var storage = dataStorageProvider.getUserStorage();
      var contextSnapshot = daoContextSnapshotFactory.getInstance(storage, workspaceKeeper);
      var fromProject = projectFunction.apply(contextSnapshot.getProjectDao());
      workspaceKeeper.setWorkspace(fromOrganization, fromProject);
      sb.append("Copied project from ").append(fromOrganization.getCode()).append("/").append(fromProject.getCode());

      storage = dataStorageProvider.getUserStorage();
      contextSnapshot = daoContextSnapshotFactory.getInstance(storage, workspaceKeeper);
      var facade = new ProjectFacade();
      facade.init(authenticatedUser, contextSnapshot, workspaceKeeper);
      var content = facade.getRawProjectContent(fromProject.getId());

      workspaceKeeper.pop();

      var relativeDateMillis = fromProject.getTemplateMetadata() != null ? fromProject.getTemplateMetadata().getRelativeDate() : null;
      var relativeDate = relativeDateMillis != null ? new Date(relativeDateMillis) : null;

      templateFacade.installTemplate(project, fromOrganization.getId(), content, relativeDate);

      eventLogFacade.logEvent(authenticatedUser.getCurrentUser(), sb.toString());
   }

   private void checkProjectContribute(final Project project) {
      permissionsChecker.checkAllRoles(project, Set.of(RoleType.LinkContribute, RoleType.ViewContribute, RoleType.CollectionContribute));
   }

}
