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

import static io.lumeer.api.util.ResourceUtils.findAttribute;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.DelayedAction;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.NotificationFrequency;
import io.lumeer.api.model.NotificationSetting;
import io.lumeer.api.model.NotificationType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.UserDao;

import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractPurposeChangeDetector implements PurposeChangeDetector {

   protected static final ZoneId utcZone = ZoneId.ofOffset("UTC", ZoneOffset.UTC);

   protected DelayedActionDao delayedActionDao;
   protected UserDao userDao;
   protected SelectedWorkspace selectedWorkspace;
   protected User currentUser;
   protected RequestDataKeeper requestDataKeeper;
   protected ConstraintManager constraintManager;

   @Override
   public void setContext(final DelayedActionDao delayedActionDao, final UserDao userDao, final SelectedWorkspace selectedWorkspace, final User currentUser, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager) {
      this.delayedActionDao = delayedActionDao;
      this.userDao = userDao;
      this.selectedWorkspace = selectedWorkspace;
      this.currentUser = currentUser;
      this.requestDataKeeper = requestDataKeeper;
      this.constraintManager = constraintManager;
   }

   protected boolean isAttributeChanged(final DocumentEvent documentEvent, final String attributeId) {
      if (documentEvent instanceof UpdateDocument) {
         final Document original = ((UpdateDocument) documentEvent).getOriginalDocument();
         final Document document = documentEvent.getDocument();

         if (original != null && document != null) {
            final Object originalAttr = original.getData() != null ? original.getData().get(attributeId) : null;
            final Object newAttr = document.getData() != null ? document.getData().get(attributeId) : null;

            if (originalAttr == null && newAttr == null) {
               return false;
            }

            if (originalAttr == null ^ newAttr == null) {
               return true;
            }

            return !originalAttr.equals(newAttr);
         } else {
            return false;
         }
      }

      return true;
   }

   protected String getResourcePath(final DocumentEvent documentEvent) {
      final Organization organization = selectedWorkspace.getOrganization().orElse(null);
      final Project project = selectedWorkspace.getProject().orElse(null);

      return (organization != null ? organization.getId() : "") + "/" +
            (project != null ? project.getId() : "") + "/" +
            documentEvent.getDocument().getCollectionId() + "/" +
            documentEvent.getDocument().getId();
   }

   protected Set<String> getAssignees(final DocumentEvent documentEvent, final Collection collection) {
      final String assigneeAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_ASSIGNEE_ATTRIBUTE_ID) : null;

      if (StringUtils.isNotEmpty(assigneeAttributeId) && findAttribute(collection.getAttributes(), assigneeAttributeId) != null) {
         return getUsersList(documentEvent.getDocument(), assigneeAttributeId);
      }

      return Set.of(currentUser.getEmail());
   }

   protected Set<String> getRemovedAssignees(final DocumentEvent documentEvent, final Collection collection) {
      if (documentEvent instanceof UpdateDocument) {

         final String assigneeAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_ASSIGNEE_ATTRIBUTE_ID) : null;

         if (StringUtils.isNotEmpty(assigneeAttributeId) && findAttribute(collection.getAttributes(), assigneeAttributeId) != null) {
            final Set<String> originalUsers = new HashSet<>(getUsersList(((UpdateDocument) documentEvent).getOriginalDocument(), assigneeAttributeId));
            final Set<String> newUsers = getUsersList(documentEvent.getDocument(), assigneeAttributeId);

            originalUsers.removeAll(newUsers);

            return originalUsers;
         }
      }

      return Set.of();
   }

   protected Set<String> getObservers(final DocumentEvent documentEvent, final Collection collection) {
      final String observersAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_OBSERVERS_ATTRIBUTE_ID) : null;

      if (StringUtils.isNotEmpty(observersAttributeId) && findAttribute(collection.getAttributes(), observersAttributeId) != null) {
         return getUsersList(documentEvent.getDocument(), observersAttributeId);
      }

      return Set.of();
   }

   private Set<String> getUsersList(final Document document, final String attributeId) {
      final Object usersObject = document.getData().getObject(attributeId);
      if (usersObject != null) {
         if (usersObject instanceof String) {
            return Set.of((String) usersObject);
         } else if (usersObject instanceof java.util.Collection) {
            return Set.copyOf((java.util.Collection<String>) usersObject);
         }
      }

      return Set.of();
   }

   protected ZonedDateTime getDueDate(final DocumentEvent documentEvent, final Collection collection) {
      final String dueDateAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_DUE_DATE_ATTRIBUTE_ID) : null;

      if (StringUtils.isNotEmpty(dueDateAttributeId) && findAttribute(collection.getAttributes(), dueDateAttributeId) != null) {
         if (documentEvent.getDocument().getData().get(dueDateAttributeId) instanceof Date) {
            final Date dueDate = documentEvent.getDocument().getData().getDate(dueDateAttributeId);
            return ZonedDateTime.from(dueDate.toInstant().atZone(utcZone));
         }
      }

      return null;
   }

   protected String getDueDateFormat(final DocumentEvent documentEvent, final Collection collection) {
      final String dueDateAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_DUE_DATE_ATTRIBUTE_ID) : null;

      if (StringUtils.isNotEmpty(dueDateAttributeId) && findAttribute(collection.getAttributes(), dueDateAttributeId) != null) {
         final Attribute attribute = collection.getAttributes().stream().filter(attr -> dueDateAttributeId.equals(attr.getId())).findFirst().orElse(null);

         if (attribute != null && attribute.getConstraint().getType() == ConstraintType.DateTime) {
            return ((Map<String, Object>) attribute.getConstraint().getConfig()).get("format").toString();
         }
      }

      return "";
   }

   protected String getState(final DocumentEvent documentEvent, final Collection collection) {
      final String stateAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_STATE_ATTRIBUTE_ID) : null;

      if (StringUtils.isNotEmpty(stateAttributeId) && findAttribute(collection.getAttributes(), stateAttributeId) != null) {
         final Object states = documentEvent.getDocument().getData().getObject(stateAttributeId);
         if (states instanceof String) {
            return states.toString();
         } else if (states instanceof List) {
            final List<String> stringStates = documentEvent.getDocument().getData().getArrayList(stateAttributeId, String.class);
            return String.join(", ", stringStates);
         }
      }

      return null;
   }

   protected boolean isDoneState(final DocumentEvent documentEvent, final Collection collection) {
      final String stateAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_STATE_ATTRIBUTE_ID) : null;
      final List<String> finalStates = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getArrayList(Collection.META_FINAL_STATES_LIST, String.class) : null;

      if (finalStates != null) {

         if (StringUtils.isNotEmpty(stateAttributeId) && findAttribute(collection.getAttributes(), stateAttributeId) != null) {
            final Object states = documentEvent.getDocument().getData().getObject(stateAttributeId);
            if (states instanceof String) {
               return finalStates.contains(states);
            } else if (states instanceof List) {
               final List<String> stringStates = documentEvent.getDocument().getData().getArrayList(stateAttributeId, String.class);

               return stringStates.stream().anyMatch(stringStates::contains);
            }
         }
      }

      return false;
   }

   protected Boolean wasDoneState(final DocumentEvent documentEvent, final Collection collection) {
      if (documentEvent instanceof UpdateDocument) {
         final String stateAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(Collection.META_STATE_ATTRIBUTE_ID) : null;
         final List<String> finalStates = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getArrayList(Collection.META_FINAL_STATES_LIST, String.class) : null;

         if (finalStates != null) {

            if (StringUtils.isNotEmpty(stateAttributeId) && findAttribute(collection.getAttributes(), stateAttributeId) != null) {
               final Object states = ((UpdateDocument) documentEvent).getOriginalDocument().getData().getObject(stateAttributeId);
               if (states instanceof String) {
                  return finalStates.contains(states);
               } else if (states instanceof List) {
                  final List<String> stringStates = ((UpdateDocument) documentEvent).getOriginalDocument().getData().getArrayList(stateAttributeId, String.class);

                  return stringStates.stream().anyMatch(stringStates::contains);
               }
            }
         }
      }

      return Boolean.FALSE;
   }

   protected String getDescriptionAttribute(final DocumentEvent documentEvent, final Collection collection) {
      return StringUtils.isNotEmpty(collection.getDefaultAttributeId()) ? collection.getDefaultAttributeId() : (collection.getAttributes().size() > 0 ? collection.getAttributes().iterator().next().getId() : null);
   }

   protected String getDescription(final DocumentEvent documentEvent, final Collection collection) {
      final String defaultAttributeId = getDescriptionAttribute(documentEvent, collection);

      if (defaultAttributeId != null && findAttribute(collection.getAttributes(), defaultAttributeId) != null) {
         final Object value = documentEvent.getDocument().getData().getObject(defaultAttributeId);

         if (value != null) {
            final Attribute attr = collection.getAttributes().stream().filter(attribute -> defaultAttributeId.equals(attribute.getId())).findFirst().orElse(null);
            return constraintManager.decode(value, Utils.computeIfNotNull(attr, Attribute::getConstraint)).toString();
         }
      }

      return "";
   }

   protected List<NotificationSetting> getChannels(final DocumentEvent documentEvent, final Collection collection, final NotificationType notificationType, final String assignee) {
      List<NotificationSetting> notifications;

      if (assignee.equals(currentUser.getEmail())) {
         notifications = currentUser.getNotifications();
      } else {
         final User user = userDao.getUserByEmail(assignee);
         notifications = user != null ? user.getNotifications() : List.of();
      }

      return notifications.stream().filter(notification -> notification.getNotificationType() == notificationType).collect(Collectors.toList());
   }

   protected ZonedDateTime roundTime(final ZonedDateTime dueDate, final NotificationFrequency notificationFrequency) {
      switch (notificationFrequency) {
         case Immediately:
         default:
            return dueDate;
      }
   }

   protected List<DelayedAction> getDelayedActions(final DocumentEvent documentEvent, final Collection collection, final NotificationType notificationType, final ZonedDateTime when) {
      final Set<String> assignees = getAssignees(documentEvent, collection);

      return getDelayedActions(documentEvent, collection, notificationType, when, assignees);
   }

   protected List<DelayedAction> getDelayedActions(final DocumentEvent documentEvent, final Collection collection, final NotificationType notificationType, final ZonedDateTime when, final Set<String> assignees) {
      final List<DelayedAction> actions = new ArrayList<>();

      System.out.println(notificationType + " /// " + assignees);

      if (assignees != null) {
         assignees.stream().filter(assignee ->
               notificationType == NotificationType.DUE_DATE_SOON ||
                     notificationType == NotificationType.PAST_DUE_DATE ||
                     !assignee.equals(currentUser.getEmail())
         ).forEach(assignee -> {
            final List<NotificationSetting> channels = getChannels(documentEvent, collection, notificationType, assignee);

            channels.forEach(settings -> {
               final DelayedAction action = new DelayedAction();

               action.setInitiator(currentUser.getEmail());
               action.setReceiver(assignee);
               action.setResourcePath(getResourcePath(documentEvent));
               action.setNotificationType(notificationType);
               action.setCheckAfter(roundTime(when, settings.getNotificationFrequency()));
               action.setNotificationChannel(settings.getNotificationChannel());
               action.setCorrelationId(requestDataKeeper.getCorrelationId());
               action.setData(getData(documentEvent, collection, assignees));

               actions.add(action);
            });
         });
      }

      return actions;
   }

   protected DataDocument getData(final DocumentEvent documentEvent, final Collection collection, final Set<String> assignees) {
      final DataDocument data = new DataDocument();

      if (selectedWorkspace.getOrganization().isPresent()) {
         final Organization organization = selectedWorkspace.getOrganization().get();
         data.append(DelayedAction.DATA_ORGANIZATION_ID, organization.getId());
         data.append(DelayedAction.DATA_ORGANIZATION_NAME, organization.getName());
         data.append(DelayedAction.DATA_ORGANIZATION_CODE, organization.getCode());
         data.append(DelayedAction.DATA_ORGANIZATION_ICON, organization.getIcon());
         data.append(DelayedAction.DATA_ORGANIZATION_COLOR, organization.getColor());
      }

      if (selectedWorkspace.getProject().isPresent()) {
         final Project project = selectedWorkspace.getProject().get();
         data.append(DelayedAction.DATA_PROJECT_ID, project.getId());
         data.append(DelayedAction.DATA_PROJECT_NAME, project.getName());
         data.append(DelayedAction.DATA_PROJECT_CODE, project.getCode());
         data.append(DelayedAction.DATA_PROJECT_ICON, project.getIcon());
         data.append(DelayedAction.DATA_PROJECT_COLOR, project.getColor());
      }

      data.append(DelayedAction.DATA_TASK_COMPLETED, isDoneState(documentEvent, collection));
      data.append(DelayedAction.DATA_TASK_STATE, getState(documentEvent, collection));
      data.append(DelayedAction.DATA_TASK_NAME, getDescription(documentEvent, collection));
      data.append(DelayedAction.DATA_TASK_NAME_ATTRIBUTE, getDescriptionAttribute(documentEvent, collection));

      var dueDate = getDueDate(documentEvent, collection);
      if (dueDate != null) {
         data.append(DelayedAction.DATA_TASK_DUE_DATE, new Date(dueDate.toInstant().toEpochMilli()));
      }
      data.append(DelayedAction.DATA_DUE_DATE_FORMAT, getDueDateFormat(documentEvent, collection));
      data.append(DelayedAction.DATA_ASSIGNEE, String.join(", ", assignees));

      data.append(DelayedAction.DATA_COLLECTION_ID, collection.getId());
      data.append(DelayedAction.DATA_COLLECTION_NAME, collection.getName());
      data.append(DelayedAction.DATA_COLLECTION_ICON, collection.getIcon());
      data.append(DelayedAction.DATA_COLLECTION_COLOR, collection.getColor());

      data.append(DelayedAction.DATA_DOCUMENT_ID, documentEvent.getDocument().getId());

      return data;
   }

   protected ZonedDateTime nowPlus() {
      return ZonedDateTime.now(); // .plus(2, ChronoUnit.MINUTES);
   }
}
