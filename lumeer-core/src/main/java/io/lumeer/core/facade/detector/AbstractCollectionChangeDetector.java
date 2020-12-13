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

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.ResourceEvent;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DelayedActionDao;

public abstract class AbstractCollectionChangeDetector implements CollectionChangeDetector {

   protected DelayedActionDao delayedActionDao;
   protected CollectionDao collectionDao;
   protected SelectedWorkspace selectedWorkspace;

   @Override
   public void setContext(final DelayedActionDao delayedActionDao, final CollectionDao collectionDao, final SelectedWorkspace selectedWorkspace) {
      this.delayedActionDao = delayedActionDao;
      this.collectionDao = collectionDao;
      this.selectedWorkspace = selectedWorkspace;
   }

   protected String getResourcePath(final ResourceEvent resourceEvent) {
      final Organization organization = selectedWorkspace.getOrganization().orElse(null);
      final Project project = selectedWorkspace.getProject().orElse(null);

      return (organization != null ? organization.getId() : "") + "/" +
            (project != null ? project.getId() : "") + "/" +
            resourceEvent.getResource().getId();
   }

}
