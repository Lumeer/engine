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
import io.lumeer.api.model.Role;
import io.lumeer.api.model.SampleDataType;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.provider.DataStorageProvider;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshotFactory;

import java.util.Date;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class CopyFacade extends AbstractFacade {

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

   private Language language;

   @PostConstruct
   public void init() {
      language = Language.fromString(requestDataKeeper.getUserLocale());
   }

   public void deepCopySampleData(Project project, SampleDataType sampleType) {
      permissionsChecker.checkRole(project, Role.WRITE);

      var organizationId = templateFacade.getSampleDataOrganizationId(language);
      this.copyProjectByCode(project, organizationId, sampleType.toString());
   }

   public void deepCopyTemplate(Project project, String templateId) {
      permissionsChecker.checkRole(project, Role.WRITE);

      var organizationId = templateFacade.getTemplateOrganizationId(language);
      this.copyProjectById(project, organizationId, templateId);
   }

   public void deepCopyProject(Project project, String organizationId, String projectId) {
      permissionsChecker.checkRole(project, Role.WRITE);

      this.copyProjectById(project, organizationId, projectId);
   }

   private void copyProjectById(Project project, String organizationId, String projectId) {
      //uncomment to use predefined templates in JSON files
      //templateFacade.installTemplate(project, "PIVOT", Language.EN);
      copyProject(project, organizationId, dao -> dao.getProjectById(projectId));
   }

   private void copyProjectByCode(Project project, String organizationId, String projectCode) {
      copyProject(project, organizationId, dao -> dao.getProjectByCode(projectCode));
   }

   private void copyProject(Project project, String organizationId, java.util.function.Function<ProjectDao, Project> projectFunction) {
      var fromOrganization = organizationDao.getOrganizationById(organizationId);
      workspaceKeeper.push();
      workspaceKeeper.setOrganization(fromOrganization);

      var storage = dataStorageProvider.getUserStorage();
      var contextSnapshot = daoContextSnapshotFactory.getInstance(storage, workspaceKeeper);
      var fromProject = projectFunction.apply(contextSnapshot.getProjectDao());
      if (!fromProject.isPublic()) {
         permissionsChecker.checkRole(fromOrganization, Role.READ);
         permissionsChecker.checkRole(fromProject, Role.READ);
      }

      workspaceKeeper.setWorkspace(fromOrganization, fromProject);

      storage = dataStorageProvider.getUserStorage();
      contextSnapshot = daoContextSnapshotFactory.getInstance(storage, workspaceKeeper);
      var facade = new ProjectFacade();
      facade.init(contextSnapshot);
      var content = facade.getRawProjectContent(fromProject.getId());

      workspaceKeeper.pop();

      var relativeDateMillis = fromProject.getTemplateMetadata() != null ? fromProject.getTemplateMetadata().getRelativeDate() : null;
      var relativeDate = relativeDateMillis != null ? new Date(relativeDateMillis) : null;

      templateFacade.installTemplate(project, fromOrganization.getId(), content, relativeDate);
   }

}
