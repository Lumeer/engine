package io.lumeer.core.task.executor.operation.stage

import io.lumeer.api.model.FileAttachment
import io.lumeer.core.adapter.FileAttachmentAdapter
import io.lumeer.core.task.executor.ChangesTracker
import io.lumeer.core.task.executor.operation.AddFileAttachmentOperation
import io.lumeer.core.task.executor.operation.OperationExecutor
import io.lumeer.core.task.executor.operation.ViewPermissionsOperation

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

      val attachmentUpdates = operations.orEmpty().filter { operation -> operation is AddFileAttachmentOperation && operation.isComplete }
            .map { operation -> (operation as AddFileAttachmentOperation) }


      attachmentUpdates.forEach { operation ->
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

         changesTracker.addUpdatedDocuments(mutableListOf(operation.entity))
      }

      return changesTracker
   }

}