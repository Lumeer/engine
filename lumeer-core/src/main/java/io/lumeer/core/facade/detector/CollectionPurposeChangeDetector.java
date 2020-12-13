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
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.ResourceEvent;
import io.lumeer.engine.api.event.UpdateResource;

public class CollectionPurposeChangeDetector extends AbstractCollectionChangeDetector {

   @Override
   public void detectChanges(final ResourceEvent resourceEvent) {
      if (resourceEvent instanceof UpdateResource) {
         final Collection originalCollection = (Collection) ((UpdateResource) resourceEvent).getOriginalResource();
         final Collection updatedCollection = (Collection) resourceEvent.getResource();

         if (originalCollection.getPurpose() != updatedCollection.getPurpose() && originalCollection.getPurpose() == CollectionPurpose.Tasks) {
            delayedActionDao.deleteAllScheduledActions(getResourcePath(resourceEvent));
         }
      }

      if (resourceEvent instanceof RemoveResource) {
         delayedActionDao.deleteAllScheduledActions(getResourcePath(resourceEvent));
      }
   }
}
