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
import io.lumeer.core.provider.DataStorageProvider;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshotFactory;

import java.util.Date;
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

   public void deepCopyTemplate(Project project, String templateId, Language language) {
      var organizationId = templateFacade.getTemplateOrganizationId(language);
      this.copyProject(project, organizationId, templateId);
   }

   public void deepCopyProject(Project project, String organizationId, String projectId) {
      this.copyProject(project, organizationId, projectId);
   }

   private void copyProject(Project project, String organizationId, String projectId) {
      var fromOrganization = organizationDao.getOrganizationById(organizationId);

      workspaceKeeper.push();
      workspaceKeeper.setOrganization(fromOrganization);

      var storage = dataStorageProvider.getUserStorage();
      var contextSnapshot = daoContextSnapshotFactory.getInstance(storage, workspaceKeeper);
      var fromProject = contextSnapshot.getProjectDao().getProjectById(projectId);

      workspaceKeeper.setWorkspace(fromOrganization, fromProject);

      contextSnapshot = daoContextSnapshotFactory.getInstance(storage, workspaceKeeper);
      var facade = new ProjectFacade();
      facade.init(contextSnapshot);
      var content = facade.getRawProjectContent(fromProject.getId());

      workspaceKeeper.pop();

      var relativeDateMillis = fromProject.getTemplateMetadata() != null ? fromProject.getTemplateMetadata().getRelativeDate() : null;
      var relativeDate = relativeDateMillis != null ? new Date(relativeDateMillis) : null;

      templateFacade.installTemplate(project, organizationId, content, relativeDate);
   }

}
