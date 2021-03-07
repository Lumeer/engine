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

import io.lumeer.api.model.User;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.FunctionFacade;
import io.lumeer.core.facade.TaskProcessingFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.facade.detector.PurposeChangeProcessor;
import io.lumeer.core.util.PusherClient;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import java.util.Map;

public interface ContextualTask extends Task {

   ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager, DefaultConfigurationProducer.DeployEnvironment environment);

   DaoContextSnapshot getDaoContextSnapshot();
   PusherClient getPusherClient();
   User getInitiator();
   ConstraintManager getConstraintManager();
   String getCurrentLocale();
   String getCorrelationId();
   PurposeChangeProcessor getPurposeChangeProcessor();
   String getTimeZone();

   FunctionFacade getFunctionFacade();

   TaskProcessingFacade getTaskProcessingFacade(final TaskExecutor taskExecutor, final FunctionFacade functionFacade);
}
