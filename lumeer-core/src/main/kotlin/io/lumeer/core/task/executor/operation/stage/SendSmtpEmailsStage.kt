package io.lumeer.core.task.executor.operation.stage

import io.lumeer.api.model.FileAttachment
import io.lumeer.core.task.executor.ChangesTracker
import io.lumeer.core.task.executor.operation.AddDocumentFileAttachmentOperation
import io.lumeer.core.task.executor.operation.OperationExecutor
import io.lumeer.core.task.executor.operation.SendSmtpEmailOperation
import io.lumeer.core.task.executor.request.SendSmtpEmailRequest
import io.lumeer.core.util.EmailPart
import io.lumeer.core.util.EmailService

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
class SendSmtpEmailsStage(executor: OperationExecutor) : Stage(executor) {
   override fun call(): ChangesTracker {
      if (operations.isEmpty()) {
         return ChangesTracker()
      }

      val sendSmtpEmailRequests = operations.orEmpty().filter { operation -> operation is SendSmtpEmailOperation && operation.isComplete }
            .map { operation -> (operation as SendSmtpEmailOperation) }

      sendSmtpEmailRequests.forEach { req ->
         val emailService = EmailService.fromSmtpConfiguration(req.entity.smtpConfiguration)
         val emailAttachments = mutableListOf<EmailPart>()

         if (req.entity.document != null) {
            emailAttachments.addAll(
               task.fileAttachmentAdapter.getAllFileAttachments(
                     task.daoContextSnapshot.organization,
                     task.daoContextSnapshot.project,
                     req.entity.document.collectionId,
                     req.entity.document.id,
                     req.entity.attributeId,
                     FileAttachment.AttachmentType.DOCUMENT
               ).map { attachment ->
                  val bytes = task.fileAttachmentAdapter.readFileAttachment(attachment)
                  // TODO determine mimeType
                  EmailPart(attachment.fileName, "", bytes)
               }
            )
         }

         if (req.entity.link != null) {
            emailAttachments.addAll(
                  task.fileAttachmentAdapter.getAllFileAttachments(
                        task.daoContextSnapshot.organization,
                        task.daoContextSnapshot.project,
                        req.entity.link.linkTypeId,
                        req.entity.link.id,
                        req.entity.attributeId,
                        FileAttachment.AttachmentType.LINK
                  ).map { attachment ->
                     val bytes = task.fileAttachmentAdapter.readFileAttachment(attachment)
                     // TODO determine mimeType
                     EmailPart(attachment.fileName, "", bytes)
                  }
            )
         }

         emailService.sendEmail(req.entity.subject, req.entity.email, req.entity.body, req.entity.fromName, emailAttachments)
      }

      return changesTracker
   }
}