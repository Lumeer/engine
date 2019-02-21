/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
import io.lumeer.api.model.User;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.util.PusherClient;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

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
   public void sendPushNotifications(final Collection collection) {
      if (getPusherClient() != null) {
         final Set<String> users = getDaoContextSnapshot().getCollectionManagers(collection.getId());
         final List<Event> events = users.stream().map(user -> createEventForCollection(collection, user)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   private Event createEventForCollection(Collection collection, String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(collection, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      return new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Collection.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message);
   }

   @Override
   public void sendPushNotifications(final Collection collection, final List<Document> documents) {
      final Set<String> users = getDaoContextSnapshot().getCollectionReaders(collection.getId());
      final List<Event> events = new ArrayList<>();

      users.forEach(userId -> {
         documents.forEach(doc -> events.add(createEventForDocument(doc, userId)));
         events.add(createEventForCollection(collection, userId));
      });

      getPusherClient().trigger(events);
   }

   private Event createEventForDocument(Document document, String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(document, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      return new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, document.getClass().getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message);
   }
}
