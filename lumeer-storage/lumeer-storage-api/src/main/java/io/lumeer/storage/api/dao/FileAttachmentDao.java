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
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;

import java.util.List;

public interface FileAttachmentDao {

   FileAttachment createFileAttachment(FileAttachment fileAttachment);

   FileAttachment updateFileAttachment(FileAttachment fileAttachment);

   FileAttachment findFileAttachment(FileAttachment fileAttachment);

   List<FileAttachment> findAllFileAttachments(Organization organization, Project project, String collectionId);

   List<FileAttachment> findAllFileAttachments(Organization organization, Project project, String collectionId, String documentId);

   List<FileAttachment> findAllFileAttachments(Organization organization, Project project, String collectionId, String documentId, String attributeId);

   void removeFileAttachment(FileAttachment fileAttachment);

   void removeAllFileAttachments(Organization organization, Project project);

   void removeAllFileAttachments(Organization organization, Project project, String collectionId);

   void removeAllFileAttachments(Organization organization, Project project, String collectionId, String attributeId);

   void removeAllFileAttachments(Organization organization, Project project, String collectionId, String documentId, String attributeId);

   void createFileAttachmentRepository();
}
