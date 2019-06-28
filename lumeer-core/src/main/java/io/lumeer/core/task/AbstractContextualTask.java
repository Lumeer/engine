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
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.WithId;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.util.PusherClient;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import org.marvec.pusher.data.BackupDataEvent;
import org.marvec.pusher.data.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public abstract class AbstractContextualTask implements ContextualTask {

   protected User initiator;
   protected DaoContextSnapshot daoContextSnapshot;
   protected PusherClient pusherClient;
   protected Task parent;

   @Override
   public ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient) {
      this.initiator = initiator;
      this.daoContextSnapshot = daoContextSnapshot;
      this.pusherClient = pusherClient;

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
         final Set<String> users1 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(0));
         final Set<String> users2 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(1));
         final Set<String> users = users1.stream().filter(userId -> users2.contains(userId)).collect(Collectors.toSet());
         final List<Event> events = users.stream().map(user -> createEventForLinkType(linkType, user)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   private Event createEventForCollection(final Collection collection, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(collection, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Collection.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(collection), null);
   }

   private Event createEventForDocument(final Document document, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(document, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Document.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(document), null);
   }

   private Event createEventForLinkType(final LinkType linkType, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(linkType, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkType.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(linkType), null);
   }

   private Event createEventForLinkInstance(final LinkInstance linkInstance, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(linkInstance, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkInstance.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(linkInstance), null);
   }

   private PusherFacade.ResourceId getResourceId(final WithId idObject) {
      return new PusherFacade.ResourceId(idObject.getId(), getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
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
         final Set<String> users1 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(0));
         final Set<String> users2 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(1));
         final Set<String> users = users1.stream().filter(userId -> users2.contains(userId)).collect(Collectors.toSet());

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
}
