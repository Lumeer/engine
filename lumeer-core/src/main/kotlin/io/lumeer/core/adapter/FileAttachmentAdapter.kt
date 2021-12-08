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
package io.lumeer.core.adapter

import io.lumeer.api.model.FileAttachment
import io.lumeer.api.model.FileAttachment.AttachmentType
import io.lumeer.api.model.Organization
import io.lumeer.api.model.Project
import io.lumeer.core.util.LumeerS3Client
import io.lumeer.storage.api.dao.FileAttachmentDao
import java.util.function.Consumer

class FileAttachmentAdapter(val lumeerS3Client: LumeerS3Client, val fileAttachmentDao: FileAttachmentDao, val environment: String) {

   fun createFileAttachment(fileAttachment: FileAttachment, data: ByteArray): FileAttachment {
      val storedAttachment = fileAttachmentDao.createFileAttachment(fileAttachment)
      lumeerS3Client.putObject(getFileAttachmentKey(fileAttachment), data)
      return storedAttachment
   }

   fun getAllFileAttachments(organization: Organization, project: Project, collectionId: String, documentId: String, attributeId: String, type: AttachmentType): List<FileAttachment> =
      fileAttachmentDao.findAllFileAttachments(
            organization,
            project,
            collectionId, documentId, attributeId, type)

   fun getFileAttachmentNames(organization: Organization, project: Project, collectionId: String, documentId: String, attributeId: String, type: AttachmentType): String =
         fileAttachmentDao.findAllFileAttachments(
               organization,
               project,
               collectionId, documentId, attributeId, type
         ).joinToString(",", "[", "]") { "'" + "${it.id}:${it.fileName}".replace("([\\'\\\\])".toRegex(), "\\\\$1") + "'" }

   fun removeFileAttachments(attachmentLocation: String) {
      if (lumeerS3Client.isInitialized) {
         lumeerS3Client.deleteObjects(lumeerS3Client.listObjects(attachmentLocation))
      }
   }

   fun removeFileAttachment(fileAttachment: FileAttachment) {
      if (lumeerS3Client.isInitialized) {
         lumeerS3Client.deleteObject(getFileAttachmentKey(fileAttachment))
      }

      fileAttachmentDao.removeFileAttachment(fileAttachment)
   }

   fun removeFileAttachments(fileAttachments: Collection<FileAttachment>) {
      if (lumeerS3Client.isInitialized) {
         fileAttachments.forEach(Consumer { fileAttachment -> lumeerS3Client.deleteObject(getFileAttachmentKey(fileAttachment)) })
      }
      fileAttachmentDao.removeFileAttachments(fileAttachments.map { it.id })
   }

   fun getFileAttachmentLocation(organizationId: String, projectId: String, collectionId: String?, documentId: String?, attributeId: String?, type: AttachmentType): String {
      val sb = StringBuilder(environment + "/" + organizationId + "/" + projectId + "/" + type.name)
      if (collectionId != null) {
         sb.append("/").append(collectionId)
         if (attributeId != null) {
            sb.append("/").append(attributeId)
            if (documentId != null) {
               sb.append("/").append(documentId)
            }
         }
      }
      return sb.toString()
   }

   fun getFileAttachmentKey(fileAttachment: FileAttachment): String {
      return (getFileAttachmentLocation(fileAttachment.organizationId, fileAttachment.projectId, fileAttachment.collectionId, fileAttachment.documentId, fileAttachment.attributeId, fileAttachment.attachmentType) + "/"
            + fileAttachment.id)
   }

}