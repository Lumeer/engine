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
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.DelayedAction;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.NotificationChannel;
import io.lumeer.api.model.NotificationFrequency;
import io.lumeer.api.model.NotificationType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.User;
import io.lumeer.api.util.CollectionUtil;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.CollectionPurposeUtils;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.DocumentCommentedEvent;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.UserDao;

import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

public abstract class AbstractPurposeChangeDetector implements PurposeChangeDetector {

   protected static final int DUE_DATE_SOON_DAYS = 3;

   protected DelayedActionDao delayedActionDao;
   protected UserDao userDao;
   protected GroupDao groupDao;
   protected SelectedWorkspace selectedWorkspace;
   protected User currentUser;
   protected RequestDataKeeper requestDataKeeper;
   protected ConstraintManager constraintManager;
   protected DefaultConfigurationProducer.DeployEnvironment environment;

   private List<User> users;
   private List<Group> teams;

   @Override
   public void setContext(final DelayedActionDao delayedActionDao, final UserDao userDao, final GroupDao groupDao, final SelectedWorkspace selectedWorkspace, final User currentUser, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager, final DefaultConfigurationProducer.DeployEnvironment environment) {
      this.delayedActionDao = delayedActionDao;
      this.userDao = userDao;
      this.groupDao = groupDao;
      this.selectedWorkspace = selectedWorkspace;
      this.currentUser = currentUser;
      this.requestDataKeeper = requestDataKeeper;
      this.constraintManager = constraintManager;
      this.environment = environment;
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

   protected Set<Assignee> getAssignees(final DocumentEvent documentEvent, final Collection collection) {
      final String assigneeAttributeId = collection.getPurpose().getAssigneeAttributeId();

      if (StringUtils.isNotEmpty(assigneeAttributeId)) {
         final Attribute assigneeAttribute = findAttribute(collection.getAttributes(), assigneeAttributeId);
         if (assigneeAttribute != null) {
            return DocumentUtils.getUsersList(documentEvent.getDocument(), assigneeAttribute, getTeams(), getUsers());
         }
      }

      return Set.of(new Assignee(currentUser.getEmail().toLowerCase(), false));
   }

   protected Set<Assignee> getRemovedAssignees(final DocumentEvent documentEvent, final Collection collection) {
      if (documentEvent instanceof UpdateDocument) {

         final String assigneeAttributeId = collection.getPurpose().getAssigneeAttributeId();

         if (StringUtils.isNotEmpty(assigneeAttributeId)) {
            final Attribute assigneeAttribute = findAttribute(collection.getAttributes(), assigneeAttributeId);
            if (assigneeAttribute != null) {
               final Set<Assignee> originalUsers = new HashSet<>(DocumentUtils.getUsersList(((UpdateDocument) documentEvent).getOriginalDocument(), assigneeAttribute, getTeams(), getUsers()));
               final Set<Assignee> newUsers = DocumentUtils.getUsersList(documentEvent.getDocument(), assigneeAttribute, getTeams(), getUsers());

               newUsers.forEach(assignee -> {
                  originalUsers.remove(new Assignee(assignee.getEmail(), true));
                  originalUsers.remove(new Assignee(assignee.getEmail(), false));
               });

               return originalUsers;
            }
         }
      }

      return Set.of();
   }

   protected Set<Assignee> getAddedAssignees(final DocumentEvent documentEvent, final Collection collection) {
      final String assigneeAttributeId = collection.getPurpose().getAssigneeAttributeId();

      if (StringUtils.isNotEmpty(assigneeAttributeId)) {
         final Attribute assigneeAttribute = findAttribute(collection.getAttributes(), assigneeAttributeId);
         if (assigneeAttribute != null) {
            if (documentEvent instanceof UpdateDocument) {
               final Set<Assignee> originalUsers = DocumentUtils.getUsersList(((UpdateDocument) documentEvent).getOriginalDocument(), assigneeAttribute, getTeams(), getUsers());
               final Set<Assignee> newUsers = new HashSet<>(DocumentUtils.getUsersList(documentEvent.getDocument(), assigneeAttribute, getTeams(), getUsers()));

               originalUsers.forEach(assignee -> {
                  newUsers.remove(new Assignee(assignee.getEmail(), true));
                  newUsers.remove(new Assignee(assignee.getEmail(), false));
               });

               return newUsers;
            } else if (documentEvent instanceof CreateDocument) {
               return DocumentUtils.getUsersList(documentEvent.getDocument(), assigneeAttribute, getTeams(), getUsers());
            }
         }
      }

      return Set.of();
   }

   protected Set<Assignee> getObservers(final DocumentEvent documentEvent, final Collection collection) {
      final String observersAttributeId = collection.getPurposeMetaData() != null ? collection.getPurposeMetaData().getString(CollectionPurpose.META_OBSERVERS_ATTRIBUTE_ID) : null;

      if (StringUtils.isNotEmpty(observersAttributeId)) {
         final Attribute observersAttribute = findAttribute(collection.getAttributes(), observersAttributeId);
         if (observersAttribute != null) {
            return DocumentUtils.getUsersList(documentEvent.getDocument(), observersAttribute, getTeams(), getUsers());
         }
      }

      return Set.of();
   }

   protected ZonedDateTime getDueDate(final DocumentEvent documentEvent, final Collection collection) {
      return CollectionPurposeUtils.getDueDate(documentEvent.getDocument(), collection);
   }

   @SuppressWarnings("unchecked,unused")
   protected String getDueDateFormat(final DocumentEvent documentEvent, final Collection collection) {
      final String dueDateAttributeId = collection.getPurpose().getDueDateAttributeId();

      if (StringUtils.isNotEmpty(dueDateAttributeId) && findAttribute(collection.getAttributes(), dueDateAttributeId) != null) {
         final Attribute attribute = collection.getAttributes().stream().filter(attr -> dueDateAttributeId.equals(attr.getId())).findFirst().orElse(null);

         if (attribute != null && attribute.getConstraint().getType() == ConstraintType.DateTime) {
            return ((Map<String, Object>) attribute.getConstraint().getConfig()).get("format").toString();
         }
      }

      return "";
   }

   protected String getState(final DocumentEvent documentEvent, final Collection collection) {
      final String stateAttributeId = collection.getPurpose().getStateAttributeId();
      final Attribute attribute = findAttribute(collection.getAttributes(), stateAttributeId);

      if (StringUtils.isNotEmpty(stateAttributeId) && attribute != null) {
         final Object states = documentEvent.getDocument().getData().getObject(stateAttributeId);
         if (states == null && attribute.getConstraint().getType() == ConstraintType.Boolean) {
            return "\u2610";
         } else if (states instanceof Boolean) {
            return ((Boolean) states) ? "\u2611" : "\u2610";
         } else if (states instanceof String) {
            return states.toString();
         } else if (states instanceof List) {
            final List<String> stringStates = documentEvent.getDocument().getData().getArrayList(stateAttributeId, String.class);
            return String.join(", ", stringStates);
         }
      }

      return null;
   }

   protected boolean isDoneState(final DocumentEvent documentEvent, final Collection collection) {
      return CollectionPurposeUtils.isDoneState(Utils.computeIfNotNull(documentEvent.getDocument(), Document::getData), collection);
   }

   protected Boolean wasDoneState(final DocumentEvent documentEvent, final Collection collection) {
      if (documentEvent instanceof UpdateDocument) {
         return CollectionPurposeUtils.isDoneState(Utils.computeIfNotNull(((UpdateDocument) documentEvent).getOriginalDocument(), Document::getData), collection);
      }

      return Boolean.FALSE;
   }

   @SuppressWarnings("unused")
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

   protected ZonedDateTime roundTime(final ZonedDateTime dueDate, final NotificationFrequency notificationFrequency) {
      switch (notificationFrequency) {
         case Immediately:
         default:
            return dueDate;
      }
   }

   protected List<DelayedAction> getDelayedActions(final DocumentEvent documentEvent, final Collection collection, final NotificationType notificationType, final ZonedDateTime when) {
      final Set<Assignee> assignees = getAssignees(documentEvent, collection);

      return getDelayedActions(documentEvent, collection, notificationType, when, assignees);
   }

   protected List<DelayedAction> getDelayedActions(final DocumentEvent documentEvent, final Collection collection, final NotificationType notificationType, final ZonedDateTime when, final Set<Assignee> assignees) {
      final List<DelayedAction> actions = new ArrayList<>();

      if (assignees != null) {
         assignees.stream().map(Assignee::getEmail).collect(Collectors.toSet()).stream().filter(assignee -> // collect to set to have each value just once
               (notificationType == NotificationType.DUE_DATE_SOON ||
                     notificationType == NotificationType.PAST_DUE_DATE ||
                     !assignee.equals(currentUser.getEmail().toLowerCase()) && StringUtils.isNotEmpty(assignee))
         ).forEach(assignee -> {
            ZonedDateTime timeZonedWhen = when;

            // when the actions are scheduled way ahead (due date soon, past due date), consider the user's time zone
            // but only when just date is visible
            if ((notificationType == NotificationType.DUE_DATE_SOON || notificationType == NotificationType.PAST_DUE_DATE) && CollectionUtil.isDueDateInUTC(collection) && !CollectionUtil.hasDueDateFormatTimeOptions(collection)) {
               final Optional<String> userTimeZone = assignees.stream().filter(a -> a.getEmail().equals(assignee) && StringUtils.isNotEmpty(a.getTimeZone())).map(Assignee::getTimeZone).findFirst();
               if (userTimeZone.isPresent()) {
                  final TimeZone tz = TimeZone.getTimeZone(userTimeZone.get());
                  timeZonedWhen = when.withZoneSameLocal(tz.toZoneId());
               }
            }

            timeZonedWhen = roundTime(timeZonedWhen, NotificationFrequency.Immediately);  // in the future, this can be removed and checked in DelayedActionProcessor
            final String resourcePath = getResourcePath(documentEvent);
            final String correlationId = StringUtils.isNotBlank(requestDataKeeper.getSecondaryCorrelationId()) ? requestDataKeeper.getSecondaryCorrelationId() : requestDataKeeper.getCorrelationId();
            final DataDocument data = getData(documentEvent, collection, assignee, assignees);

            for (NotificationChannel channel : NotificationChannel.values()) {
               final DelayedAction action = new DelayedAction();

               action.setInitiator(currentUser.getEmail());
               action.setReceiver(assignee);
               action.setResourcePath(resourcePath);
               action.setNotificationType(notificationType);
               action.setCheckAfter(timeZonedWhen);
               action.setNotificationChannel(channel);
               action.setCorrelationId(correlationId);
               action.setData(data);

               actions.add(action);
            }
         });
      }

      return actions;
   }

   protected DataDocument getData(final DocumentEvent documentEvent, final Collection collection, final String currentAssignee, final Set<Assignee> assignees) {
      final DataDocument data = new DataDocument();

      if (selectedWorkspace.getOrganization().isPresent()) {
         final Organization organization = selectedWorkspace.getOrganization().get();
         data.append(DelayedAction.DATA_ORGANIZATION_ID, organization.getId());
      }

      if (selectedWorkspace.getProject().isPresent()) {
         final Project project = selectedWorkspace.getProject().get();
         data.append(DelayedAction.DATA_PROJECT_ID, project.getId());
      }

      data.append(DelayedAction.DATA_TASK_COMPLETED, isDoneState(documentEvent, collection));
      data.append(DelayedAction.DATA_TASK_STATE, getState(documentEvent, collection));
      data.append(DelayedAction.DATA_TASK_NAME, getDescription(documentEvent, collection));
      data.append(DelayedAction.DATA_TASK_NAME_ATTRIBUTE, getDescriptionAttribute(documentEvent, collection));

      var dueDate = getDueDate(documentEvent, collection);
      if (dueDate != null) {

         // update the time according to the user's time zone if there is also time stored
         if (!CollectionUtil.isDueDateInUTC(collection)) {
            final Optional<String> userTimeZone = assignees.stream().filter(a -> a.getEmail().equals(currentAssignee) && StringUtils.isNotEmpty(a.getTimeZone())).map(Assignee::getTimeZone).findFirst();
            if (userTimeZone.isPresent()) {
               final TimeZone tz = TimeZone.getTimeZone(userTimeZone.get());
               dueDate = dueDate.withZoneSameInstant(tz.toZoneId());
            }
         }

         data.append(DelayedAction.DATA_TASK_DUE_DATE, new Date(dueDate.toInstant().toEpochMilli()));
      }
      data.append(DelayedAction.DATA_DUE_DATE_FORMAT, getDueDateFormat(documentEvent, collection));
      data.append(DelayedAction.DATA_ASSIGNEE, String.join(", ", assignees.stream().map(Assignee::getEmail).collect(Collectors.toSet()))); // collect to set so that each value is present only once
      data.append(DelayedAction.DATA_ASSIGNEE_VIA_TEAM_ONLY, assignees.contains(new Assignee(currentAssignee, true)) && !assignees.contains(new Assignee(currentAssignee, false)));

      data.append(DelayedAction.DATA_COLLECTION_ID, collection.getId());
      data.append(DelayedAction.DATA_DOCUMENT_ID, documentEvent.getDocument().getId());

      if (documentEvent instanceof DocumentCommentedEvent) {
         data.append(DelayedAction.DATA_TASK_COMMENT, ((DocumentCommentedEvent) documentEvent).getComment().getComment());
      }

      return data;
   }

   protected ZonedDateTime nowPlus() {
      if (environment == DefaultConfigurationProducer.DeployEnvironment.PRODUCTION || environment == DefaultConfigurationProducer.DeployEnvironment.STAGING) {
         return ZonedDateTime.now().plus(DelayedActionDao.PROCESSING_DELAY_MINUTES, ChronoUnit.MINUTES);
      }
      return ZonedDateTime.now();
   }

   private List<User> getUsers() {
      if (users == null) {
         users = userDao.getAllUsers(selectedWorkspace.getOrganization().get().getId());
      }
      return users;
   }

   private List<Group> getTeams() {
      if (teams == null) {
         teams = groupDao.getAllGroups(selectedWorkspace.getOrganization().get().getId());
      }
      return teams;
   }
}
