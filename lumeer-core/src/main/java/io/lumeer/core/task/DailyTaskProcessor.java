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
package io.lumeer.core.task;

import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.AuditType;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.core.WorkspaceContext;
import io.lumeer.core.adapter.FileAttachmentAdapter;
import io.lumeer.core.util.LumeerS3Client;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.FileAttachmentDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class DailyTaskProcessor extends WorkspaceContext {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private FileAttachmentDao fileAttachmentDao;

   @Schedule(hour = "6") // every day at 6:00 am
   public void process() {

      final List<Organization> organizations = organizationDao.getAllOrganizations();
      final LumeerS3Client lumeerS3Client = new LumeerS3Client(configurationProducer);
      final FileAttachmentAdapter fileAttachmentAdapter = new FileAttachmentAdapter(lumeerS3Client, fileAttachmentDao, configurationProducer.getEnvironment().name());

      final List<FileAttachment> attachmentsToDelete = new ArrayList<>();

      organizations.forEach(organization -> {
         final DataStorage userDataStorage = getDataStorage(organization.getId());
         var cleanOlderThan = ZonedDateTime.now().minusMonths(1);

         final DaoContextSnapshot orgDao = getDaoContextSnapshot(userDataStorage, new Workspace(organization, null));
         final List<Project> projects = orgDao.getProjectDao().getAllProjects();

         projects.forEach(project -> {
            final DaoContextSnapshot projDao = getDaoContextSnapshot(userDataStorage, new Workspace(organization, project));

            List<AuditRecord> deletedAuditRecords = projDao.getAuditDao().findAuditRecords(cleanOlderThan, AuditType.Deleted);

            Set<String> documentIds = deletedAuditRecords.stream().filter(record -> ResourceType.DOCUMENT.equals(record.getResourceType()))
                                                         .map(AuditRecord::getResourceId).collect(Collectors.toSet());
            attachmentsToDelete.addAll(fileAttachmentAdapter.getAllFileAttachments(organization, project, documentIds, FileAttachment.AttachmentType.DOCUMENT));

            Set<String> linkIds = deletedAuditRecords.stream().filter(record -> ResourceType.LINK.equals(record.getResourceType()))
                                                     .map(AuditRecord::getResourceId).collect(Collectors.toSet());
            attachmentsToDelete.addAll(fileAttachmentAdapter.getAllFileAttachments(organization, project, linkIds, FileAttachment.AttachmentType.LINK));

            projDao.getAuditDao().cleanAuditRecords(cleanOlderThan);
         });

      });

      fileAttachmentAdapter.removeFileAttachments(attachmentsToDelete);
   }

}
