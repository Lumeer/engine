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
import io.lumeer.api.model.DelayedAction;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.NotificationChannel;
import io.lumeer.api.model.NotificationFrequency;
import io.lumeer.api.model.NotificationType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.UserDao;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractPurposeChangeDetector implements PurposeChangeDetector {

   protected static final ZoneId utcZone = ZoneId.ofOffset("UTC", ZoneOffset.UTC);

   protected DelayedActionDao delayedActionDao;
   protected UserDao userDao;
   protected SelectedWorkspace selectedWorkspace;
   protected User currentUser;

   @Override
   public void setContext(final DelayedActionDao delayedActionDao, final UserDao userDao, final SelectedWorkspace selectedWorkspace, final User currentUser) {
      this.delayedActionDao = delayedActionDao;
      this.userDao = userDao;
      this.selectedWorkspace = selectedWorkspace;
      this.currentUser = currentUser;
   }

   protected boolean isAttributeChanged(final DocumentEvent documentEvent, final String attributeId) {
      if (documentEvent instanceof UpdateDocument) {
         final Document original = ((UpdateDocument) documentEvent).getOriginalDocument();
         final Document document = documentEvent.getDocument();
         final Object originalAttr = original.getData() != null ? original.getData().get(attributeId) : null;
         final Object newAttr = document.getData() != null ? document.getData().get(attributeId) : null;

         if (originalAttr == null && newAttr == null) {
            return false;
         }

         if (originalAttr == null ^ newAttr == null) {
            return true;
         }

         return !originalAttr.equals(newAttr);
      }

      return true;
   }

   protected String getResourcePath(final DocumentEvent documentEvent) {
      final Organization organization = selectedWorkspace.getOrganization().get();
      final Project project = selectedWorkspace.getProject().get();

      return (organization != null ? organization.getId() : "") + "/" +
            (project != null ? project.getId() : "") + "/" +
            documentEvent.getDocument().getCollectionId() + "/" +
            documentEvent.getDocument().getId();
   }

   protected String getAssignee(final DocumentEvent documentEvent, final Collection collection) {
      final String assigneeAttributeId = collection.getMetaData() != null ? collection.getMetaData().getString(Collection.META_ASSIGNEE_ATTRIBUTE_ID) : null;

      if (assigneeAttributeId != null && !"".equals(assigneeAttributeId)) {
         return documentEvent.getDocument().getData().getString(assigneeAttributeId);
      }

      return currentUser.getEmail();
   }

   protected ZonedDateTime getDueDate(final DocumentEvent documentEvent, final Collection collection) {
      final String dueDateAttributeId = collection.getMetaData() != null ? collection.getMetaData().getString(Collection.META_DUE_DATE_ATTRIBUTE_ID) : null;

      if (dueDateAttributeId != null && !"".equals(dueDateAttributeId)) {
         final Date dueDate = documentEvent.getDocument().getData().getDate(dueDateAttributeId);
         return ZonedDateTime.from(dueDate.toInstant().atZone(utcZone));
      }

      return null;
   }

   protected Map<NotificationChannel, NotificationFrequency> getChannels(final DocumentEvent documentEvent, final Collection collection) {
      final String assignee = getAssignee(documentEvent, collection);
      if (assignee.equals(currentUser.getEmail())) {
         return currentUser.getNotifications();
      } else {
         final User user = userDao.getUserByEmail(assignee);
         return user != null ? user.getNotifications() : Map.of();
      }
   }

   protected ZonedDateTime roundTime(final ZonedDateTime dueDate, final NotificationFrequency NotificationFrequency) {
      return dueDate;
   }

   protected List<DelayedAction> getDelayedActions(final DocumentEvent documentEvent, final Collection collection, final NotificationType notificationType, final ZonedDateTime when) {
      final Map<NotificationChannel, NotificationFrequency> channels = getChannels(documentEvent, collection);
      final List<DelayedAction> actions = new ArrayList<>();

      channels.forEach((channel, frequency) -> {
         final DelayedAction action = new DelayedAction();

         action.setInitiator(currentUser.getEmail());
         action.setReceiver(getAssignee(documentEvent, collection));
         action.setResourcePath(getResourcePath(documentEvent));
         action.setNotificationType(notificationType);
         action.setCheckAfter(roundTime(when, frequency));
         action.setNotificationChannel(channel);

         actions.add(action);
      });

      return actions;
   }
}
