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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Sequence;
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.WithId;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.AbstractConstraintConverter;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.FunctionFacade;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.util.PusherClient;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import org.marvec.pusher.data.BackupDataEvent;
import org.marvec.pusher.data.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractContextualTask implements ContextualTask {

   private static Logger log = Logger.getLogger(AbstractConstraintConverter.class.getName());

   protected User initiator;
   protected DaoContextSnapshot daoContextSnapshot;
   protected PusherClient pusherClient;
   protected Task parent;
   protected RequestDataKeeper requestDataKeeper;
   protected ConstraintManager constraintManager;

   @Override
   public ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager) {
      this.initiator = initiator;
      this.daoContextSnapshot = daoContextSnapshot;
      this.pusherClient = pusherClient;
      this.requestDataKeeper = requestDataKeeper;
      this.constraintManager = constraintManager;

      return this;
   }

   @Override
   public DaoContextSnapshot getDaoContextSnapshot() {
      return daoContextSnapshot;
   }

   @Override
   public PusherClient getPusherClient() {
      return pusherClient;
   }

   @Override
   public ConstraintManager getConstraintManager() {
      return constraintManager;
   }

   @Override
   public User getInitiator() {
      return initiator;
   }

   @Override
   public Task getParent() {
      return parent;
   }

   @Override
   public void setParent(final Task parent) {
      this.parent = parent;
   }

   @Override
   public void sendPushNotifications(final Collection collection) {
      if (getPusherClient() != null) {
         final Set<String> users = getDaoContextSnapshot().getCollectionManagers(collection.getId());
         final List<Event> events = users.stream().map(user -> createEventForCollection(collection, user)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   @Override
   public void sendPushNotifications(final LinkType linkType) {
      if (getPusherClient() != null) {
         linkType.setLinksCount(getDaoContextSnapshot().getLinkInstanceDao().getLinkInstancesCountByLinkType(linkType.getId()));
         final Set<String> users1 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(0));
         final Set<String> users2 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(1));
         final Set<String> users = users1.stream().filter(users2::contains).collect(Collectors.toSet());
         final List<Event> events = users.stream().map(user -> createEventForLinkType(linkType, user)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   private Event createEventForCollection(final Collection collection, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(collection, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Collection.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(collection, null), null);
   }

   private Event createEventForDocument(final Document document, final String userId) {
      document.setCommentsCount(daoContextSnapshot.getResourceCommentDao().getCommentsCount(ResourceType.DOCUMENT, document.getId()));
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(document, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Document.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(document, document.getCollectionId()), null);
   }

   private Event createEventForLinkType(final LinkType linkType, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(linkType, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkType.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(linkType, null), null);
   }

   private Event createEventForLinkInstance(final LinkInstance linkInstance, final String userId) {
      linkInstance.setCommentsCount(daoContextSnapshot.getResourceCommentDao().getCommentsCount(ResourceType.LINK, linkInstance.getId()));
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(linkInstance, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkInstance.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(linkInstance, linkInstance.getLinkTypeId()), null);
   }

   private Event createEventForSequence(final Sequence sequence, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(sequence, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Sequence.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(sequence, null), null);
   }

   private PusherFacade.ResourceId getResourceId(final WithId idObject, final String extraId) {
      return new PusherFacade.ResourceId(idObject.getId(), getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId(), extraId);
   }

   @Override
   public void sendPushNotifications(final Collection collection, final List<Document> documents) {
      final Set<String> users = getDaoContextSnapshot().getCollectionReaders(collection);
      final List<Event> events = new ArrayList<>();
      final List<Event> collectionEvents = new ArrayList<>();

      users.forEach(userId -> {
         documents.forEach(doc -> events.add(createEventForDocument(doc, userId)));
         collectionEvents.add(createEventForCollection(collection, userId));
      });

      getPusherClient().trigger(events);
      getPusherClient().trigger(collectionEvents);
   }

   @Override
   public void sendPushNotifications(final LinkType linkType, final List<LinkInstance> linkInstances) {
      if (linkType.getCollectionIds().size() == 2) {
         linkType.setLinksCount(getDaoContextSnapshot().getLinkInstanceDao().getLinkInstancesCountByLinkType(linkType.getId()));
         final Set<String> users1 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(0));
         final Set<String> users2 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(1));
         final Set<String> users = users1.stream().filter(users2::contains).collect(Collectors.toSet());

         final List<Event> events = new ArrayList<>();
         final List<Event> linkInstanceEvents = new ArrayList<>();

         users.forEach(userId -> {
            linkInstances.forEach(link -> events.add(createEventForLinkInstance(link, userId)));
            linkInstanceEvents.add(createEventForLinkType(linkType, userId));
         });

         getPusherClient().trigger(events);
         getPusherClient().trigger(linkInstanceEvents);
      }
   }

   @Override
   public void sendPushNotifications(final String sequenceName) {
      final Sequence sequence = getDaoContextSnapshot().getSequenceDao().getSequence(sequenceName);

      final Set<String> managers = getDaoContextSnapshot().getProjectManagers();

      final List<Event> events = new ArrayList<>();
      managers.forEach(manager -> {
         events.add(createEventForSequence(sequence, manager));
      });

      getPusherClient().trigger(events);
   }

   @Override
   public void propagateChanges(final List<Document> documents, final List<LinkInstance> links) {
      if (parent != null) {
         parent.propagateChanges(documents, links);
      }
   }

   private void injectCorrelationId(final PusherFacade.ObjectWithParent obj) {
      if (requestDataKeeper != null) {
         obj.setCorrelationId(requestDataKeeper.getCorrelationId());
      }
   }

   @Override
   public FunctionFacade getFunctionFacade() {
      return FunctionFacade.getInstance(
            getDaoContextSnapshot().getFunctionDao(),
            getDaoContextSnapshot().getCollectionDao(),
            getDaoContextSnapshot().getDocumentDao(),
            getDaoContextSnapshot().getLinkInstanceDao(),
            getDaoContextSnapshot().getLinkTypeDao(),
            new LocalContextualTaskFactory()
            );
   }

   class LocalContextualTaskFactory extends ContextualTaskFactory {

      public <T extends ContextualTask> T getInstance(final Class<T> clazz) {
         try {
            T t = clazz.getConstructor().newInstance();
            t.initialize(getInitiator(), getDaoContextSnapshot(), getPusherClient(), new RequestDataKeeper(requestDataKeeper), getConstraintManager());

            return t;
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to instantiate a task: ", e);
         }

         return null;
      }
   }
}
