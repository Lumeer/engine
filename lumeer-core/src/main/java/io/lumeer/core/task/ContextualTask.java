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
import io.lumeer.core.constraint.converter.ConstraintManager;
import io.lumeer.core.facade.ConfigurationFacade;
import io.lumeer.core.facade.FunctionFacade;
import io.lumeer.core.facade.TaskProcessingFacade;
import io.lumeer.core.facade.detector.PurposeChangeProcessor;
import io.lumeer.core.util.PusherClient;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import java.util.List;

public interface ContextualTask extends Task {

   ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager, ConfigurationFacade.DeployEnvironment environment);

   DaoContextSnapshot getDaoContextSnapshot();
   PusherClient getPusherClient();
   User getInitiator();
   ConstraintManager getConstraintManager();
   String getCurrentLocale();
   String getCorrelationId();
   PurposeChangeProcessor getPurposeChangeProcessor();

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
    * @param collectionChanged When true, send updated collection.
    */
   void sendPushNotifications(final Collection collection, final List<Document> documents, final boolean collectionChanged);

   /**
    * Send push notifications to document readers.
    * @param collection Parent collection.
    * @param documents List of documents.
    * @param suffix push notification suffix.
    * @param collectionChanged When true, send updated collection.
    */
   void sendPushNotifications(final Collection collection, final List<Document> documents, final String suffix, final boolean collectionChanged);

   /**
    * Send push notifications to link instance readers.
    * @param linkType Parent link type.
    * @param linkInstances List of link instances.
    * @param linkTypeChanged When true, send updated link type.
    */
   void sendPushNotifications(final LinkType linkType, final List<LinkInstance> linkInstances, final boolean linkTypeChanged);

   /**
    * Send push notifications to project managers.
    * @param sequenceName Name of updated sequence.
    */
   void sendPushNotifications(final String sequenceName);

   /**
    * Send push notifications with messages from a rule execution.
    * @param userMessages The messages to display.
    */
   void sendPushNotifications(final List<UserMessage> userMessages);

   FunctionFacade getFunctionFacade();

   TaskProcessingFacade getTaskProcessingFacade(final TaskExecutor taskExecutor, final FunctionFacade functionFacade);

}
