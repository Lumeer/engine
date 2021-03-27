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
package io.lumeer.core.task.executor.operation.stage;

import io.lumeer.core.adapter.AuditAdapter;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.task.ContextualTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.executor.ChangesTracker;
import io.lumeer.core.task.executor.operation.Operation;
import io.lumeer.core.task.executor.operation.OperationExecutor;

import java.util.Set;
import java.util.concurrent.Callable;

abstract public class Stage implements Callable<ChangesTracker> {
   protected static final DefaultConfigurationProducer configurationProducer = new DefaultConfigurationProducer();
   protected static final ConstraintManager constraintManager = ConstraintManager.getInstance(configurationProducer);

   protected final OperationExecutor executor;
   protected final TaskExecutor taskExecutor;
   protected final ContextualTask task;
   protected Set<Operation<?>> operations;
   protected final ChangesTracker changesTracker = new ChangesTracker();
   protected final AuditAdapter auditAdapter;

   public Stage(final OperationExecutor executor) {
      this.executor = executor;
      this.taskExecutor = executor.getTaskExecutor();
      this.task = executor.getTask();
      this.operations = executor.getOperations();
      this.auditAdapter = AuditAdapter.getAuditAdapter(task.getDaoContextSnapshot());
   }

   abstract public ChangesTracker call(); // remove exception from method signature
}
