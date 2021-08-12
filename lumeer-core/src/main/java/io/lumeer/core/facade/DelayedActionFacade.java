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
import io.lumeer.api.model.Document;
import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.facade.detector.CollectionChangeProcessor;
import io.lumeer.core.facade.detector.PurposeChangeProcessor;
import io.lumeer.core.util.Tuple;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateResourceComment;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.engine.api.event.UpdateResourceComment;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.UserDao;

import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
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
   private GroupDao groupDao;

   @Inject
   private SelectedWorkspace selectedWorkspace;

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   private CollectionChangeProcessor collectionChangeProcessor;
   private PurposeChangeProcessor purposeChangeProcessor;

   @PostConstruct
   public void init() {
      final ConstraintManager constraintManager = ConstraintManager.getInstance(configurationProducer);
      collectionChangeProcessor = new CollectionChangeProcessor(delayedActionDao, collectionDao, selectedWorkspace);
      purposeChangeProcessor = new PurposeChangeProcessor(delayedActionDao, userDao, groupDao, selectedWorkspace, authenticatedUser.getCurrentUser(), requestDataKeeper, constraintManager, configurationProducer.getEnvironment());
   }

   public void documentCreated(@Observes final CreateDocument createDocument) {
      processChanges(createDocument);
   }

   public void documentUpdated(@Observes final UpdateDocument updateDocument) {
      processChanges(updateDocument);
   }

   public void documentRemoved(@Observes final RemoveDocument removeDocument) {
      processChanges(removeDocument);
   }

   public void collectionUpdated(@Observes final UpdateResource collectionUpdated) {
      if (collectionUpdated.getResource().getType().equals(ResourceType.COLLECTION)) {
         collectionChangeProcessor.processChanges(collectionUpdated);
      }
   }

   public void collectionRemoved(@Observes final RemoveResource collectionRemoved) {
      if (collectionRemoved.getResource().getType().equals(ResourceType.COLLECTION)) {
         collectionChangeProcessor.processChanges(collectionRemoved);
      }
   }

   public void commentCreated(@Observes final CreateResourceComment commentEvent) {
      if (commentEvent.getResourceComment().getResourceType() == ResourceType.DOCUMENT) {
         processComments(commentEvent.getResourceComment());
      }
   }

   public void commentUpdated(@Observes final UpdateResourceComment commentEvent) {
      if (commentEvent.getResourceComment().getResourceType() == ResourceType.DOCUMENT) {
         processComments(commentEvent.getResourceComment());
      }
   }

   private void processChanges(final DocumentEvent documentEvent) {
      final Collection collection = getCollection(documentEvent);
      purposeChangeProcessor.processChanges(documentEvent, collection);
   }

   private void processComments(final ResourceComment comment) {
      final Tuple<Document, Collection> documentAndCollection = getDocumentAndCollection(comment);

      if (documentAndCollection != null) {
         purposeChangeProcessor.processChanges(comment, documentAndCollection.getFirst(), documentAndCollection.getSecond());
      }
   }

   private Collection getCollection(final DocumentEvent event) {
      return collectionDao.getCollectionById(event.getDocument().getCollectionId());
   }

   private Tuple<Document, Collection> getDocumentAndCollection(final ResourceComment comment) {
      final List<Document> docs = documentFacade.getDocuments(Set.of(comment.getResourceId()));

      if (docs.size() == 1) {
         final Document doc = docs.get(0);
         final Collection collection = collectionDao.getCollectionById(doc.getCollectionId());

         return new Tuple<>(doc, collection);
      }

      return null;
   }
}
