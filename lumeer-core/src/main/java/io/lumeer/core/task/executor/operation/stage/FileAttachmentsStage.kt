package io.lumeer.core.task.executor.operation.stage

import io.lumeer.api.model.FileAttachment
import io.lumeer.api.model.Organization
import io.lumeer.api.model.Project
import io.lumeer.core.adapter.FileAttachmentAdapter
import io.lumeer.core.task.executor.ChangesTracker
import io.lumeer.core.task.executor.operation.AddDocumentFileAttachmentOperation
import io.lumeer.core.task.executor.operation.AddLinkFileAttachmentOperation
import io.lumeer.core.task.executor.operation.OperationExecutor

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
         // remove existing attachments when overwrite is on
         if (operation.fileAttachmentData.isOverwriteExisting) {
            deleteExistingAttachment(
                  task.daoContextSnapshot.organization,
                  task.daoContextSnapshot.project,
                  operation.entity.collectionId,
                  operation.entity.id,
                  operation.attrId,
                  FileAttachment.AttachmentType.DOCUMENT,
                  operation.fileAttachmentData.fileName
            )
         }

         // create the file attachment
         val fileAttachment = FileAttachment(
               task.daoContextSnapshot.organizationId,
               task.daoContextSnapshot.projectId,
               operation.entity.collectionId,
               operation.entity.id,
               operation.attrId,
               operation.fileAttachmentData.fileName,
               FileAttachment.AttachmentType.DOCUMENT
         )
         fileAttachmentAdapter.createFileAttachment(fileAttachment, operation.fileAttachmentData.data)

         // update the document data
         operation.entity.data.put(operation.attrId, fileAttachmentAdapter.getFileAttachmentNames(
               task.daoContextSnapshot.organization,
               task.daoContextSnapshot.project,
               operation.entity.collectionId,
               operation.entity.id,
               operation.attrId,
               FileAttachment.AttachmentType.DOCUMENT
         ))

         changesTracker.addUpdatedDocuments(mutableListOf(operation.entity))
      }

      val linkAttachmentUpdates = operations.orEmpty().filter { operation -> operation is AddLinkFileAttachmentOperation && operation.isComplete }
            .map { operation -> (operation as AddLinkFileAttachmentOperation) }

      linkAttachmentUpdates.forEach { operation ->
         // remove existing attachments when overwrite is on
         if (operation.fileAttachmentData.isOverwriteExisting) {
            deleteExistingAttachment(
                  task.daoContextSnapshot.organization,
                  task.daoContextSnapshot.project,
                  operation.entity.linkTypeId,
                  operation.entity.id,
                  operation.attrId,
                  FileAttachment.AttachmentType.LINK,
                  operation.fileAttachmentData.fileName
            )
         }

         // create the file attachment
         val fileAttachment = FileAttachment(
               task.daoContextSnapshot.organizationId,
               task.daoContextSnapshot.projectId,
               operation.entity.linkTypeId,
               operation.entity.id,
               operation.attrId,
               operation.fileAttachmentData.fileName,
               FileAttachment.AttachmentType.DOCUMENT
         )
         fileAttachmentAdapter.createFileAttachment(fileAttachment, operation.fileAttachmentData.data)

         // update the document data
         operation.entity.data.put(operation.attrId, fileAttachmentAdapter.getFileAttachmentNames(
               task.daoContextSnapshot.organization,
               task.daoContextSnapshot.project,
               operation.entity.linkTypeId,
               operation.entity.id,
               operation.attrId,
               FileAttachment.AttachmentType.LINK
         ))

         changesTracker.addUpdatedLinkInstances(mutableListOf(operation.entity))
      }

      return changesTracker
   }

   private fun deleteExistingAttachment(organization: Organization, project: Project, resourceId: String, documentId: String, attributeId: String, type: FileAttachment.AttachmentType, fileName: String) {
      val attachments = fileAttachmentAdapter.getAllFileAttachments(organization, project, resourceId, documentId, attributeId, type)

      attachments.filter { it.fileName == fileName } .forEach {
         fileAttachmentAdapter.removeFileAttachment(it)
      }
   }

}