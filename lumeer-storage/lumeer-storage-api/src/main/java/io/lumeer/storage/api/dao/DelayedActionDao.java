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

import io.lumeer.api.model.DelayedAction;
import io.lumeer.api.model.NotificationType;

import java.util.List;
import java.util.Set;

public interface DelayedActionDao {

   int PROCESSING_DELAY_MINUTES = 2;

   List<DelayedAction> getActions();

   void deleteScheduledActions(final String resourcePath, final Set<NotificationType> notificationTypes);
   void deleteAllScheduledActions(final String partialResourcePath);
   void deleteAllScheduledActions(final String partialResourcePath, final Set<NotificationType> notificationTypes);
   void deleteProcessedActions();
   void resetTimeoutedActions();
   List<DelayedAction> getActionsForProcessing(final boolean skipDelay);
   DelayedAction updateAction(final DelayedAction action);
   DelayedAction scheduleAction(final DelayedAction delayedAction);
   List<DelayedAction> scheduleActions(final List<DelayedAction> delayedActions);
}
