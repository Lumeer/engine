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
package io.lumeer.core.task.executor.operation;

import io.lumeer.core.task.ContextualTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.executor.ChangesTracker;
import io.lumeer.core.task.executor.operation.stage.CleanupChangesStage;
import io.lumeer.core.task.executor.operation.stage.FileAttachmentsStage;
import io.lumeer.core.task.executor.operation.stage.RemoveDocumentsStage;
import io.lumeer.core.task.executor.operation.stage.SendSmtpEmailsStage;
import io.lumeer.core.task.executor.operation.stage.SequencesStage;
import io.lumeer.core.task.executor.operation.stage.SingleStage;
import io.lumeer.core.task.executor.operation.stage.Stage;
import io.lumeer.core.task.executor.operation.stage.ViewsUpdatingStage;

import java.util.List;
import java.util.concurrent.Callable;

public class OperationExecutor implements Callable<ChangesTracker> {

   private final TaskExecutor taskExecutor;
   private final ContextualTask task;
   private final List<Operation<?>> operations;

   public OperationExecutor(final TaskExecutor taskExecutor, final ContextualTask task, final List<Operation<?>> operations) {
      this.taskExecutor = taskExecutor;
      this.task = task;
      this.operations = operations;
   }

   public TaskExecutor getTaskExecutor() {
      return taskExecutor;
   }

   public ContextualTask getTask() {
      return task;
   }

   public List<Operation<?>> getOperations() {
      return operations;
   }

   @Override
   public ChangesTracker call() {
      final Stage fileAttachmentsStage = new FileAttachmentsStage(this);
      final Stage singleStage = new SingleStage(this);
      final Stage removeDocumentsStage = new RemoveDocumentsStage(this);
      final Stage viewsStage = new ViewsUpdatingStage(this);
      final Stage smtpEmailsStage = new SendSmtpEmailsStage(this);
      final Stage sequencesStage = new SequencesStage(this);
      final Stage cleanupChangesStage = new CleanupChangesStage(this);

      final ChangesTracker changes = fileAttachmentsStage.call();
      changes.merge(singleStage.call());
      changes.merge(removeDocumentsStage.call());
      changes.merge(viewsStage.call());
      changes.merge(smtpEmailsStage.call());
      changes.merge(sequencesStage.call());
      changes.merge(cleanupChangesStage.call());

      return changes;
   }
}
