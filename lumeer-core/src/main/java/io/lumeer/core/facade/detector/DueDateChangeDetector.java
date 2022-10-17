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
package io.lumeer.core.facade.detector;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.NotificationType;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.RemoveDocument;

import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class DueDateChangeDetector extends AbstractPurposeChangeDetector {

   @Override
   public void detectChanges(final DocumentEvent documentEvent, final Collection collection) {
      final CollectionPurpose purpose = collection.getPurpose();

      final String dueDateAttr = purpose.getDueDateAttributeId();

      if (StringUtils.isNotEmpty(dueDateAttr) && isAttributeChanged(documentEvent, dueDateAttr)) {
         if (!(documentEvent instanceof CreateDocument)) {
            // delete previous due date events on the document
            deleteScheduledActions(getResourcePath(documentEvent), Set.of(NotificationType.DUE_DATE_SOON, NotificationType.PAST_DUE_DATE, NotificationType.DUE_DATE_CHANGED));
         }

         if (!(documentEvent instanceof RemoveDocument) && !isDoneState(documentEvent, collection)) {
            // create new due date events on the document
            final ZonedDateTime dueDate = getDueDate(documentEvent, collection);
            final String assigneeAttr = purpose.getAssigneeAttributeId();

            // due date has changed - but it goes with the assignee change message, so send it only if the assignee did not change
            if (isAttributeChanged(documentEvent, dueDateAttr) && !isAttributeChanged(documentEvent, assigneeAttr)) {
               scheduleActions(getDelayedActions(documentEvent, collection, NotificationType.DUE_DATE_CHANGED, nowPlus()));
            }

            // if it is not unset, then schedule past due and due date soon
            if (dueDate != null) {
               scheduleActions(getDelayedActions(documentEvent, collection, NotificationType.PAST_DUE_DATE, dueDate));

               if (dueDate.minus(DUE_DATE_SOON_DAYS, ChronoUnit.DAYS).isAfter(ZonedDateTime.now())) {
                  scheduleActions(getDelayedActions(documentEvent, collection, NotificationType.DUE_DATE_SOON, dueDate.minus(DUE_DATE_SOON_DAYS, ChronoUnit.DAYS)));
               }
            }
         }
      }
   }
}
