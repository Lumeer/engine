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
package io.lumeer.core.facade;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.facade.detector.DueDateChangeDetector;
import io.lumeer.core.facade.detector.PurposeChangeDetector;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.UserDao;

import java.util.Map;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@RequestScoped
public class DelayedActionFacade {

   @Inject
   private DelayedActionDao delayedActionDao;

   @Inject
   private UserDao userDao;

   @Inject
   private SelectedWorkspace selectedWorkspace;

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private CollectionDao collectionDao;

   private static final Map<CollectionPurpose, Set<PurposeChangeDetector>> changeDetectors = Map.of(CollectionPurpose.Tasks, Set.of(new DueDateChangeDetector()));

   public void documentCreated(@Observes final CreateDocument createDocument) {
      processChanges(createDocument);
   }

   public void documentUpdated(@Observes final UpdateDocument updateDocument) {
      processChanges(updateDocument);
   }

   public void documentRemoved(@Observes final RemoveDocument removeDocument) {
      processChanges(removeDocument);
   }

   private void processChanges(final DocumentEvent documentEvent) {
      final Collection collection = getCollection(documentEvent);

      final Set<PurposeChangeDetector> detectors = changeDetectors.get(collection.getPurpose());

      if (detectors != null) {
         detectors.forEach(detector -> {
            detector.setContext(delayedActionDao, userDao, selectedWorkspace, authenticatedUser.getCurrentUser());
            detector.detectChanges(documentEvent, collection);
         });
      }
   }

   private Collection getCollection(final DocumentEvent event) {
      return collectionDao.getCollectionById(event.getDocument().getCollectionId());
   }
}
