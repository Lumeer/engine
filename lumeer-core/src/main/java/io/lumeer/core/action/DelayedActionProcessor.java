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
package io.lumeer.core.action;

import io.lumeer.api.model.DelayedAction;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.NotificationChannel;
import io.lumeer.api.model.NotificationType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.ViewCursor;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.facade.EmailService;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.JsFunctionsParser;
import io.lumeer.core.util.PusherClient;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserNotificationDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.marvec.pusher.data.Event;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class DelayedActionProcessor {

   @Inject
   private DelayedActionDao delayedActionDao;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private UserNotificationDao userNotificationDao;

   @Inject
   private EmailService emailService;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   private PusherClient pusherClient;

   private boolean skipDelay = false;

   private Map<String, Organization> organizations = new HashMap<>();
   private Map<String, Project> projects = new HashMap<>();

   @PostConstruct
   public void init() {
      skipDelay = !(configurationProducer.getEnvironment() == DefaultConfigurationProducer.DeployEnvironment.PRODUCTION || configurationProducer.getEnvironment() == DefaultConfigurationProducer.DeployEnvironment.STAGING);
   }

   @Schedule(hour = "*", minute = "*/2")
   public void process() {
      delayedActionDao.deleteProcessedActions();
      delayedActionDao.resetTimeoutedActions();

      executeActions(delayedActionDao.getActionsForProcessing(skipDelay));
   }

   private void executeActions(final List<DelayedAction> actions) {
      final Map<String, User> users = getUsers(actions);
      final Map<String, Language> userLanguages = initializeLanguages(users.values());
      final Map<String, String> userIds = getUserIds(users.values());

      organizations.clear();
      projects.clear();

      actions.forEach(action -> {
         final Language lang = userLanguages.getOrDefault(action.getReceiver(), Language.EN);

         if (actionResourceExists(action)) {

            if (action.getNotificationChannel() == NotificationChannel.Email) {
               final String sender = userIds.containsKey(action.getInitiator()) ? emailService.formatUserReference(users.get(userIds.get(action.getInitiator()))) : "";
               final String recipient = action.getReceiver();
               final Map<String, Object> additionalData = processData(action.getData(), lang);

               emailService.sendEmailFromTemplate(getEmailTemplate(action), lang, sender, recipient, getEmailSubjectPart(action, additionalData), additionalData);
            } else if (action.getNotificationChannel() == NotificationChannel.Internal && userIds.containsKey(action.getReceiver())) {
               UserNotification notification = createUserNotification(userIds.get(action.getReceiver()), action, lang);
               notification = userNotificationDao.createNotification(notification);
               if (pusherClient != null) {
                  pusherClient.trigger(List.of(createUserNotificationEvent(notification, PusherFacade.CREATE_EVENT_SUFFIX, userIds.get(action.getReceiver()))));
               }
            }

            // reschedule past due actions
            if (!rescheduleDueDateAction(action)) {
               action.setCompleted(ZonedDateTime.now());
            }
         } else {
            action.setCompleted(ZonedDateTime.now());
         }

         delayedActionDao.updateAction(action);
      });
   }

   private boolean actionResourceExists(final DelayedAction action) {
      if (action.getNotificationType() == NotificationType.PAST_DUE_DATE || action.getNotificationType() == NotificationType.DUE_DATE_SOON) {
         final String orgId = action.getData().getString(DelayedAction.DATA_ORGANIZATION_ID);
         final String projId = action.getData().getString(DelayedAction.DATA_PROJECT_ID);
         final String docId = action.getData().getString(DelayedAction.DATA_DOCUMENT_ID);

         final Organization organization = organizations.computeIfAbsent(orgId, id -> organizationDao.getOrganizationById(orgId));
         workspaceKeeper.setOrganization(organization);
         final Project project = projects.computeIfAbsent(projId, id -> projectDao.getProjectById(projId));
         workspaceKeeper.setWorkspace(organization, project);

         try {
            final Document document = documentDao.getDocumentById(docId);
         } catch (ResourceNotFoundException rnfe) {
            return false;
         }
      }

      return true;
   }

   // reschedule past due actions until they are completed
   private boolean rescheduleDueDateAction(final DelayedAction action) {
      if (action.getNotificationType() == NotificationType.PAST_DUE_DATE && action.getData().getDate(DelayedAction.DATA_TASK_DUE_DATE) != null) {
         final Boolean completed = action.getData().getBoolean(DelayedAction.DATA_TASK_COMPLETED);

         if (completed != null && !completed) {
            action.setStartedProcessing(null);
            action.setCheckAfter(ZonedDateTime.now().plus(1, ChronoUnit.DAYS));

            return true;
         }
      }

      return false;
   }

   // format due date to string according to attribute constraint format and user language
   private Map<String, Object> processData(final DataDocument originalData, final Language language) {
      final Map<String, Object> data = new HashMap<>(originalData);

      if (originalData.getDate(DelayedAction.DATA_TASK_DUE_DATE) != null) {

         String format = language == Language.EN ? "MM/DD/YYYY" : "DD.MM.YYYY";
         if (StringUtils.isNotEmpty(originalData.getString(DelayedAction.DATA_DUE_DATE_FORMAT))) {
            format = originalData.getString(DelayedAction.DATA_DUE_DATE_FORMAT);
         }

         data.put(DelayedAction.DATA_TASK_DUE_DATE,
               JsFunctionsParser.formatMomentJsDate(
                     originalData.getDate(DelayedAction.DATA_TASK_DUE_DATE).getTime(),
                     format,
                     language.toString().toLowerCase()
               )
         );
      }

      final String query = new Query(List.of(new QueryStem(originalData.getString(DelayedAction.DATA_COLLECTION_ID))), null, null, null).toQueryString();
      data.put(DelayedAction.DATA_COLLECTION_QUERY, Utils.encodeQueryParam(query));

      final String cursor = new ViewCursor(
            originalData.getString(DelayedAction.DATA_COLLECTION_ID),
            null,
            originalData.getString(DelayedAction.DATA_DOCUMENT_ID),
            null,
            originalData.getString(DelayedAction.DATA_TASK_NAME_ATTRIBUTE), true).toQueryString();
      data.put(DelayedAction.DATA_DOCUMENT_CURSOR, Utils.encodeQueryParam(cursor));

      return data;
   }

   private String getEmailSubjectPart(final DelayedAction action, final Map<String, Object> additionalData) {
      switch (action.getNotificationType()) {
         case TASK_ASSIGNED:
         case DUE_DATE_SOON:
         case PAST_DUE_DATE:
         case STATE_UPDATE:
         case TASK_UPDATED:
         case TASK_REMOVED:
         case TASK_UNASSIGNED:
         case DUE_DATE_CHANGED:
         case TASK_COMMENTED:
         case TASK_MENTIONED:
         default:
            return additionalData.get(DelayedAction.DATA_TASK_NAME) != null ? additionalData.get(DelayedAction.DATA_TASK_NAME).toString() : "";
      }
   }

   private EmailService.EmailTemplate getEmailTemplate(final DelayedAction action) {
      switch (action.getNotificationType()) {
         case TASK_ASSIGNED:
            return EmailService.EmailTemplate.TASK_ASSIGNED;
         case DUE_DATE_SOON:
            return EmailService.EmailTemplate.DUE_DATE_SOON;
         case PAST_DUE_DATE:
            return EmailService.EmailTemplate.PAST_DUE_DATE;
         case STATE_UPDATE:
            return EmailService.EmailTemplate.STATE_UPDATE;
         case TASK_UPDATED:
            return EmailService.EmailTemplate.TASK_UPDATED;
         case TASK_REMOVED:
            return EmailService.EmailTemplate.TASK_REMOVED;
         case TASK_UNASSIGNED:
            return EmailService.EmailTemplate.TASK_UNASSIGNED;
         case DUE_DATE_CHANGED:
            return EmailService.EmailTemplate.DUE_DATE_CHANGED;
         case TASK_COMMENTED:
            return EmailService.EmailTemplate.TASK_COMMENTED;
         case TASK_MENTIONED:
            return EmailService.EmailTemplate.TASK_MENTIONED;
         default:
            return null;
      }
   }

   // translate action to user notification
   private UserNotification createUserNotification(final String userId, final DelayedAction action, final Language language) {
      return new UserNotification(userId, ZonedDateTime.now(), false, null, action.getNotificationType(), new DataDocument(processData(action.getData(), language)));
   }

   public void setPusherClient(final PusherClient pusherClient) {
      this.pusherClient = pusherClient;
   }

   // get map of user id -> user
   private Map<String, User> getUsers(final List<DelayedAction> actions) {
      return actions.stream()
             .map(DelayedAction::getReceiver)
             .distinct()
             .map(userDao::getUserByEmail)
             .filter(Objects::nonNull)
             .collect(Collectors.toMap(User::getId, Function.identity()));
   }

   // get map of user email -> user language
   private Map<String, Language> initializeLanguages(final Collection<User> users) {
      return users.stream()
                   .collect(
                         Collectors.toMap(
                               User::getEmail,
                               user -> Language.valueOf((user.getNotificationsLanguage() != null ? user.getNotificationsLanguage() : "en").toUpperCase())
                         )
                   );
   }

   // get map of user email -> user id
   private Map<String, String> getUserIds(final Collection<User> users) {
      return users.stream()
            .collect(
                  Collectors.toMap(User::getEmail, User::getId)
            );
   }

   private Event createUserNotificationEvent(final UserNotification notification, final String event, final String userId) {
      return new Event(PusherFacade.eventChannel(userId), UserNotification.class.getSimpleName() + event, notification);
   }

}
