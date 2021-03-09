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
import io.lumeer.api.model.NotificationType;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.UpdateDocument;

import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class AssigneeChangeDetector extends AbstractPurposeChangeDetector {

   @Override
   public void detectChanges(final DocumentEvent documentEvent, final Collection collection) {
      final DataDocument meta = collection.getPurposeMetaData();

      if (meta != null) {
         final String assigneeAttr = meta.getString(Collection.META_ASSIGNEE_ATTRIBUTE_ID);
         final boolean doneState = isDoneState(documentEvent, collection);

         if (StringUtils.isNotEmpty(assigneeAttr) && isAttributeChanged(documentEvent, assigneeAttr)) {
            if (!(documentEvent instanceof CreateDocument)) {
               // delete previous due date and assignee events on the document
               delayedActionDao.deleteScheduledActions(getResourcePath(documentEvent), Set.of(NotificationType.DUE_DATE_SOON, NotificationType.PAST_DUE_DATE, NotificationType.TASK_ASSIGNED, NotificationType.DUE_DATE_CHANGED));

               if (!(documentEvent instanceof RemoveDocument) && !doneState) {
                  final ZonedDateTime dueDate = getDueDate(documentEvent, collection);

                  // no need to send DUE_DATE_CHANGED because due date is part of assigned message

                  if (dueDate != null) {
                     delayedActionDao.scheduleActions(getDelayedActions(documentEvent, collection, NotificationType.PAST_DUE_DATE, dueDate));

                     if (dueDate.minus(DUE_DATE_SOON_DAYS, ChronoUnit.DAYS).isAfter(ZonedDateTime.now())) {
                        delayedActionDao.scheduleActions(getDelayedActions(documentEvent, collection, NotificationType.DUE_DATE_SOON, dueDate.minus(DUE_DATE_SOON_DAYS, ChronoUnit.DAYS)));
                     }
                  }
               }
            }

            if (documentEvent instanceof UpdateDocument) {
               delayedActionDao.scheduleActions(getDelayedActions(documentEvent, collection, NotificationType.TASK_UNASSIGNED, nowPlus(), getRemovedAssignees(documentEvent, collection)));
            }

            if (!(documentEvent instanceof RemoveDocument) && !doneState) {
               // create new due date events on the document
               delayedActionDao.scheduleActions(getDelayedActions(documentEvent, collection, NotificationType.TASK_ASSIGNED, nowPlus(), getAddedAssignees(documentEvent, collection)));
            }
         }
      }
   }
}
