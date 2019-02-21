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
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import org.marvec.pusher.data.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
         final List<Event> events = new ArrayList<>();

         users.forEach(userId -> {
            final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(collection, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
            events.add(new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Collection.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message));
         });

         getPusherClient().trigger(events);
      }
   }

   @Override
   public void sendPushNotifications(final String collectionId, final List<String> documentIds) {
      final Set<String> users = getDaoContextSnapshot().getCollectionReaders(collectionId);
      final List<Document> documents = getDaoContextSnapshot().getDocumentDao().getDocumentsByIds(documentIds.toArray(new String[0]));
      final Map<String, DataDocument> dataMap = getDaoContextSnapshot().getDataDao().getData(collectionId, new HashSet<>(documentIds))
                                                                       .stream().collect(Collectors.toMap(DataDocument::getId, Function.identity()));
      documents.forEach(document -> document.setData(dataMap.get(document.getId())));

      final List<Event> events = new ArrayList<>();

      users.forEach(userId ->
            documents.forEach(doc -> {
                     final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(doc, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
                     events.add(new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, doc.getClass().getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message));
                  }
            ));

      getPusherClient().trigger(events);
   }
}
