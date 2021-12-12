package io.lumeer.core.task.executor.operation.stage

import io.lumeer.api.model.FileAttachment
import io.lumeer.api.model.Organization
import io.lumeer.api.model.Project
import io.lumeer.core.adapter.FileAttachmentAdapter
import io.lumeer.core.task.executor.ChangesTracker
import io.lumeer.core.task.executor.operation.AddDocumentFileAttachmentOperation
import io.lumeer.core.task.executor.operation.AddLinkFileAttachmentOperation
import io.lumeer.core.task.executor.operation.OperationExecutor
import java.time.ZonedDateTime

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
class FileAttachmentsStage(executor: OperationExecutor) : Stage(executor) {

   private val fileAttachmentAdapter: FileAttachmentAdapter = task.fileAttachmentAdapter

   override fun call(): ChangesTracker {
      if (operations.isEmpty()) {
         return ChangesTracker()
      }

      val documentAttachmentUpdates = operations.orEmpty().filter { operation -> operation is AddDocumentFileAttachmentOperation && operation.isComplete }
            .map { operation -> (operation as AddDocumentFileAttachmentOperation) }

      documentAttachmentUpdates.forEach { operation ->
         // get existing attachments when overwrite is on
         val attachments = getExistingAttachments(
            task.daoContextSnapshot.organization,
            task.daoContextSnapshot.project,
            operation.entity.collectionId,
            operation.entity.id,
            operation.attrId,
            FileAttachment.AttachmentType.DOCUMENT,
            operation.fileAttachmentData.fileName
         )

         // create the file attachment
         val fileAttachment = FileAttachment(
               task.daoContextSnapshot.organizationId,
               task.daoContextSnapshot.projectId,
               operation.entity.collectionId,
               operation.entity.id,
               operation.attrId,
               operation.fileAttachmentData.fileName,
               System.currentTimeMillis().toString() + "_" + operation.fileAttachmentData.fileName,
               FileAttachment.AttachmentType.DOCUMENT
         )
         if (task.initiator != null) {
            fileAttachment.createdBy = task.initiator.id
         }
         fileAttachment.creationDate = ZonedDateTime.now()
         fileAttachmentAdapter.createFileAttachment(fileAttachment, operation.fileAttachmentData.data)

         // delete the attachments being overwritten
         if (operation.fileAttachmentData.isOverwriteExisting) {
            deleteExistingAttachment(attachments)
         }

         // update the document data
         val attachmentNames = fileAttachmentAdapter.getFileAttachmentNames(
               task.daoContextSnapshot.organization,
               task.daoContextSnapshot.project,
               operation.entity.collectionId,
               operation.entity.id,
               operation.attrId,
               FileAttachment.AttachmentType.DOCUMENT
         )
         operation.updateRelatedValue(attachmentNames)

         changesTracker.addUpdatedDocuments(mutableListOf(operation.entity))
      }

      val linkAttachmentUpdates = operations.orEmpty().filter { operation -> operation is AddLinkFileAttachmentOperation && operation.isComplete }
            .map { operation -> (operation as AddLinkFileAttachmentOperation) }

      linkAttachmentUpdates.forEach { operation ->
         // get existing attachments when overwrite is on
         val attachments = getExistingAttachments(
               task.daoContextSnapshot.organization,
               task.daoContextSnapshot.project,
               operation.entity.linkTypeId,
               operation.entity.id,
               operation.attrId,
               FileAttachment.AttachmentType.LINK,
               operation.fileAttachmentData.fileName
         )

         // create the file attachment
         val fileAttachment = FileAttachment(
               task.daoContextSnapshot.organizationId,
               task.daoContextSnapshot.projectId,
               operation.entity.linkTypeId,
               operation.entity.id,
               operation.attrId,
               operation.fileAttachmentData.fileName,
               System.currentTimeMillis().toString() + "_" + operation.fileAttachmentData.fileName,
               FileAttachment.AttachmentType.DOCUMENT
         )
         if (task.initiator != null) {
            fileAttachment.createdBy = task.initiator.id
         }
         fileAttachment.creationDate = ZonedDateTime.now()
         fileAttachmentAdapter.createFileAttachment(fileAttachment, operation.fileAttachmentData.data)

         // delete the attachments being overwritten
         if (operation.fileAttachmentData.isOverwriteExisting) {
            deleteExistingAttachment(attachments)
         }

         // update the document data
         val attachmentNames = fileAttachmentAdapter.getFileAttachmentNames(
               task.daoContextSnapshot.organization,
               task.daoContextSnapshot.project,
               operation.entity.linkTypeId,
               operation.entity.id,
               operation.attrId,
               FileAttachment.AttachmentType.LINK
         )
         operation.updateRelatedValue(attachmentNames)

         changesTracker.addUpdatedLinkInstances(mutableListOf(operation.entity))
      }

      return changesTracker
   }

   private fun deleteExistingAttachment(attachments: List<FileAttachment>) {
      attachments.forEach {
         fileAttachmentAdapter.removeFileAttachment(it)
      }
   }

   private fun getExistingAttachments(organization: Organization, project: Project, resourceId: String, documentId: String, attributeId: String, type: FileAttachment.AttachmentType, fileName: String): List<FileAttachment> =
      fileAttachmentAdapter.getAllFileAttachments(organization, project, resourceId, documentId, attributeId, type).filter { it.fileName == fileName }
}