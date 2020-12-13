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

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

public class StateChangeDetector extends AbstractPurposeChangeDetector {

   @Override
   public void detectChanges(final DocumentEvent documentEvent, final Collection collection) {
      final DataDocument meta = collection.getMetaData();

      if (meta != null) {
         final String stateAttr = meta.getString(Collection.META_STATE_ATTRIBUTE_ID);
         final List<String> finalStates = meta.getArrayList(Collection.META_FINAL_STATES_LIST, String.class);

         if (StringUtils.isNotEmpty(stateAttr) && isAttributeChanged(documentEvent, stateAttr)) {
            final boolean doneState = isDoneState(documentEvent, collection);

            if (!(documentEvent instanceof CreateDocument)) {
               // delete previous due date and assignee events on the document
               if (doneState) {
                  delayedActionDao.deleteScheduledActions(getResourcePath(documentEvent), Set.of(NotificationType.DUE_DATE_SOON, NotificationType.PAST_DUE_DATE, NotificationType.TASK_ASSIGNED, NotificationType.STATE_UPDATE));
               }
            }

            if (!(documentEvent instanceof RemoveDocument)) {
               // create new due date events on the document
               delayedActionDao.scheduleActions(getDelayedActions(documentEvent, collection, NotificationType.STATE_UPDATE, nowPlus()));
            }
         }
      }
   }
}
