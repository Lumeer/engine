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
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.UserDao;

import java.util.Map;
import java.util.Set;

public class PurposeChangeProcessor {

   private final DelayedActionDao delayedActionDao;
   private final UserDao userDao;
   private final SelectedWorkspace selectedWorkspace;
   private final User initiator;
   private final RequestDataKeeper requestDataKeeper;
   private final ConstraintManager constraintManager;
   private final DefaultConfigurationProducer.DeployEnvironment environment;

   private static final Map<CollectionPurposeType, Set<PurposeChangeDetector>> changeDetectors = Map.of(CollectionPurposeType.Tasks, Set.of(new AssigneeChangeDetector(), new DueDateChangeDetector(), new StateChangeDetector(), new TaskUpdateChangeDetector()));
   private static final Map<CollectionPurposeType, Set<PurposeChangeDetector>> commentChangeDetectors = Map.of(CollectionPurposeType.Tasks, Set.of(new CommentChangeDetector()), CollectionPurposeType.None, Set.of(new CommentChangeDetector()));

   public PurposeChangeProcessor(
         final DelayedActionDao delayedActionDao, final UserDao userDao, final SelectedWorkspace selectedWorkspace,
         final User initiator, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager,
         final DefaultConfigurationProducer.DeployEnvironment environment) {
      this.delayedActionDao = delayedActionDao;
      this.userDao = userDao;
      this.selectedWorkspace = selectedWorkspace;
      this.initiator = initiator;
      this.requestDataKeeper = requestDataKeeper;
      this.constraintManager = constraintManager;
      this.environment = environment;
   }

   public void processChanges(final DocumentEvent documentEvent, final Collection collection) {
      final Set<PurposeChangeDetector> detectors = changeDetectors.get(collection.getPurposeType());

      if (detectors != null) {
         detectors.forEach(detector -> {
            detector.setContext(delayedActionDao, userDao, selectedWorkspace, initiator, requestDataKeeper, constraintManager, environment);
            detector.detectChanges(documentEvent, collection);
         });
      }
   }

   public void processChanges(final ResourceComment comment, final Document document, final Collection collection) {
      final Set<PurposeChangeDetector> detectors = commentChangeDetectors.get(collection.getPurposeType());

      if (detectors != null) {
         detectors.forEach(detector -> {
            detector.setContext(delayedActionDao, userDao, selectedWorkspace, initiator, requestDataKeeper, constraintManager, environment);
            detector.detectChanges(comment, document, collection);
         });
      }
   }
}
