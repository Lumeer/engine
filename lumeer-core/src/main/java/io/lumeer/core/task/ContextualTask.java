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
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.FunctionFacade;
import io.lumeer.core.util.PusherClient;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import java.util.List;
import java.util.logging.Level;

public interface ContextualTask extends Task {

   ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager);

   DaoContextSnapshot getDaoContextSnapshot();
   PusherClient getPusherClient();
   User getInitiator();
   ConstraintManager getConstraintManager();

   /**
    * Send notifications to collection owners (i.e. managers).
    * @param collection Collection that has been updated.
    */
   void sendPushNotifications(final Collection collection);

   /**
    * Send notifications to link type's collection owners (i.e. managers).
    * @param linkType Collection that has been updated.
    */
   void sendPushNotifications(final LinkType linkType);

   /**
    * Send push notifications to document readers.
    * @param collection Parent collection.
    * @param documents List of documents.
    */
   void sendPushNotifications(final Collection collection, final List<Document> documents);

   /**
    * Send push notifications to link instance readers.
    * @param linkType Parent link type.
    * @param linkInstances List of link instances.
    */
   void sendPushNotifications(final LinkType linkType, final List<LinkInstance> linkInstances);

   /**
    * Send push notifications to project managers.
    * @param sequenceName Name of updated sequence.
    */
   void sendPushNotifications(final String sequenceName);

   FunctionFacade getFunctionFacade();
}
