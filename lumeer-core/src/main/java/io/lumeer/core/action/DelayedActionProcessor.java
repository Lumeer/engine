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
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.ViewCursor;
import io.lumeer.core.WorkspaceContext;
import io.lumeer.core.adapter.PermissionAdapter;
import io.lumeer.core.facade.EmailService;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.facade.translate.TranslationManager;
import io.lumeer.core.util.JsFunctionsParser;
import io.lumeer.core.util.PusherClient;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserNotificationDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.marvec.pusher.data.Event;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class DelayedActionProcessor extends WorkspaceContext {

   @Inject
   private DelayedActionDao delayedActionDao;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private UserNotificationDao userNotificationDao;

   @Inject
   private EmailService emailService;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private TranslationManager translationManager;

   private PusherClient pusherClient;

   private boolean skipDelay = false;

   private final Map<String, Organization> organizations = new HashMap<>();
   private final Map<String, Project> projects = new HashMap<>();
   private final Map<String, Collection> collections = new HashMap<>();
   private final Map<String, PermissionAdapter> permissionAdapters = new HashMap<>();
   private final Map<String, DaoContextSnapshot> organizationDaoSnapshots = new HashMap<>();
   private final Map<String, DaoContextSnapshot> projectDaoSnapshots = new HashMap<>();

   final private static Set<NotificationType> AGGREGATION_TYPES = Set.of(NotificationType.TASK_ASSIGNED, NotificationType.TASK_REOPENED, NotificationType.DUE_DATE_CHANGED, NotificationType.STATE_UPDATE, NotificationType.TASK_UPDATED, NotificationType.TASK_COMMENTED);

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

   private Map<String, List<DelayedAction>> getActionsByTask(final List<DelayedAction> actions, final NotificationChannel notificationChannel) {
      return actions.stream().filter(a -> AGGREGATION_TYPES.contains(a.getNotificationType()) && a.getNotificationChannel() == notificationChannel).collect(Collectors.groupingBy(a -> a.getReceiver() + "/" + a.getData().getString(DelayedAction.DATA_DOCUMENT_ID)));
   }

   private List<DelayedAction> aggregateActions(final List<DelayedAction> actions) {
      var actionsByIds = actions.stream().collect(Collectors.toMap(DelayedAction::getId, Function.identity()));
      final List<DelayedAction> newActions = new ArrayList<>();

      for (final NotificationChannel channel : NotificationChannel.values()) {
         var actionsByUserAndTask = getActionsByTask(actions, channel);
         actionsByUserAndTask.forEach((k, v) -> {
            // for all chunks where there are more notifications than 1 for the same user
            if (v.size() > 1) {
               // check that we have the receiver and task id
               if (v.stream().allMatch(a -> a.getReceiver() != null && a.getData().getString(DelayedAction.DATA_DOCUMENT_ID) != null)) {
                  // sort the actions from oldest to newest to merge them together in the right order
                  v.sort(Comparator.comparing(DelayedAction::getCheckAfter));

                  // merge the actions
                  DelayedAction action = v.get(0);
                  boolean wasAssignee = action.getNotificationType() == NotificationType.TASK_ASSIGNED;
                  final Set<NotificationType> originalTypes = new HashSet<>();
                  final List<String> originalIds = new ArrayList<>();
                  originalTypes.add(action.getNotificationType());
                  originalIds.add(action.getId());
                  actionsByIds.remove(action.getId());
                  for (int i = 1; i < v.size(); i++) {
                     final DelayedAction other = v.get(i);
                     action = action.merge(other);
                     wasAssignee = wasAssignee || (other.getNotificationType() == NotificationType.TASK_ASSIGNED);
                     originalTypes.add(other.getNotificationType());
                     originalIds.add(other.getId());
                     actionsByIds.remove(other.getId());
                  }

                  // set the correct type of the new aggregated action
                  if (wasAssignee) {
                     action.setNotificationType(NotificationType.TASK_ASSIGNED);
                  } else {
                     action.setNotificationType(NotificationType.TASK_CHANGED);
                  }

                  action.getData()
                        .append(DelayedAction.DATA_ORIGINAL_ACTION_TYPES, new ArrayList(originalTypes))
                        .append(DelayedAction.DATA_ORIGINAL_ACTION_IDS, originalIds);
                  newActions.add(action);
               }
            }
         });
      }

      newActions.addAll(actionsByIds.values());

      return newActions;
   }

   private boolean isNotificationEnabled(final DelayedAction action, final User user) {
      if (action.getNotificationType() != NotificationType.TASK_CHANGED) {
         return user.hasNotificationEnabled(action.getNotificationType(), action.getNotificationChannel());
      }

      return action.getData().getArrayList(DelayedAction.DATA_ORIGINAL_ACTION_TYPES, NotificationType.class).stream().anyMatch(type -> user.hasNotificationEnabled(type, action.getNotificationChannel()));
   }

   private void executeActions(final List<DelayedAction> actions) {
      final Map<String, List<User>> userCache = new HashMap<>(); // org id -> users
      this.clearCache();

      aggregateActions(actions).forEach(action -> {
         final String organizationId = action.getData().getString(DelayedAction.DATA_ORGANIZATION_ID);
         final List<User> allUsers = userCache.computeIfAbsent(organizationId, orgId -> userDao.getAllUsers(orgId));
         allUsers.addAll(getUsersFromActions(actions, allUsers)); // mix in users from actions

         final Map<String, User> users = getUsers(allUsers); // id -> user
         final Map<String, Language> userLanguages = initializeLanguages(users.values());
         final Map<String, String> userIds = getUserIds(users.values()); // email -> id

         final Language lang = userLanguages.getOrDefault(action.getReceiver(), Language.EN);

         if (checkActionResourceExistsAndFillData(action)) {
            final User receiverUser = userIds.containsKey(action.getReceiver()) ? users.get(userIds.get(action.getReceiver())) : null;

            // if we do not know anything about the user, make sure to send the notification; otherwise check the user settings
            if (receiverUser == null || isNotificationEnabled(action, receiverUser)) {

               if (action.getNotificationChannel() == NotificationChannel.Email) {
                  final User user = userIds.containsKey(action.getInitiator()) ? users.get(userIds.get(action.getInitiator())) : null;
                  final String sender = user != null ? emailService.formatUserReference(user) : "";
                  final String from = user != null ? emailService.formatFrom(user) : "";
                  final String recipient = action.getReceiver();
                  final Map<String, Object> additionalData = processData(action.getData(), lang);

                  emailService.sendEmailFromTemplate(getEmailTemplate(action), lang, sender, from, recipient, getEmailSubjectPart(action, additionalData, lang), additionalData);
               } else if (action.getNotificationChannel() == NotificationChannel.Internal && userIds.containsKey(action.getReceiver())) {
                  UserNotification notification = createUserNotification(userIds.get(action.getReceiver()), action, lang);
                  notification = userNotificationDao.createNotification(notification);
                  if (pusherClient != null) {
                     pusherClient.trigger(List.of(createUserNotificationEvent(notification, PusherFacade.CREATE_EVENT_SUFFIX, userIds.get(action.getReceiver()))));
                  }
               }
            }

            // reschedule past due actions
            if (!rescheduleDueDateAction(action)) {
               markActionAsCompleted(actions, action);
            }
         } else {
            markActionAsCompleted(actions, action);
         }
      });
   }

   private void clearCache() {
      organizations.clear();
      projects.clear();
      collections.clear();
      permissionAdapters.clear();
      organizationDaoSnapshots.clear();
      projectDaoSnapshots.clear();
   }

   private void markActionAsCompleted(final List<DelayedAction> actions, final DelayedAction action) {
      if (action.getId() == null && action.getData().containsKey(DelayedAction.DATA_ORIGINAL_ACTION_IDS)) {
         var ids = action.getData().getArrayList(DelayedAction.DATA_ORIGINAL_ACTION_IDS, String.class);
         actions.forEach(a -> {
            if (ids.contains(a.getId())) {
               a.setCompleted(ZonedDateTime.now());
               delayedActionDao.updateAction(a);
            }
         });
      } else {
         action.setCompleted(ZonedDateTime.now());
         delayedActionDao.updateAction(action);
      }
   }

   private boolean checkActionResourceExistsAndFillData(final DelayedAction action) {
      final String organizationId = action.getData().getString(DelayedAction.DATA_ORGANIZATION_ID);
      final String projectId = action.getData().getString(DelayedAction.DATA_PROJECT_ID);
      final String collectionId = action.getData().getString(DelayedAction.DATA_COLLECTION_ID);
      final String documentId = action.getData().getString(DelayedAction.DATA_DOCUMENT_ID);

      try {
         if (organizationId != null) {
            final DataStorage userDataStorage = getDataStorage(organizationId);
            final Organization organization = organizations.computeIfAbsent(organizationId, id -> organizationDao.getOrganizationById(organizationId));

            action.getData().append(DelayedAction.DATA_ORGANIZATION_NAME, organization.getName());
            action.getData().append(DelayedAction.DATA_ORGANIZATION_CODE, organization.getCode());
            action.getData().append(DelayedAction.DATA_ORGANIZATION_ICON, organization.getIcon());
            action.getData().append(DelayedAction.DATA_ORGANIZATION_COLOR, organization.getColor());

            final DaoContextSnapshot organizationDaoSnapshot = organizationDaoSnapshots.computeIfAbsent(organizationId, id -> getDaoContextSnapshot(userDataStorage, new Workspace(organization, null)));

            if (projectId != null) {
               final Project project = projects.computeIfAbsent(projectId, id -> organizationDaoSnapshot.getProjectDao().getProjectById(projectId));
               action.getData().append(DelayedAction.DATA_PROJECT_NAME, project.getName());
               action.getData().append(DelayedAction.DATA_PROJECT_CODE, project.getCode());
               action.getData().append(DelayedAction.DATA_PROJECT_ICON, project.getIcon());
               action.getData().append(DelayedAction.DATA_PROJECT_COLOR, project.getColor());

               final String projectKey = organizationId + ":" + projectId;
               final DaoContextSnapshot projectDaoSnapshot = projectDaoSnapshots.computeIfAbsent(projectKey, key -> getDaoContextSnapshot(userDataStorage, new Workspace(organization, project)));

               final PermissionAdapter permissionAdapter = permissionAdapters.computeIfAbsent(projectKey, key -> new PermissionAdapter(projectDaoSnapshot.getUserDao(), projectDaoSnapshot.getGroupDao(), projectDaoSnapshot.getViewDao(), projectDaoSnapshot.getLinkTypeDao(), projectDaoSnapshot.getCollectionDao()));
               if (!permissionAdapter.canReadWorkspace(organization, project, action.getReceiver())) {
                  return false;
               }

               if (collectionId != null) {
                  final Collection collection = collections.computeIfAbsent(collectionId, id -> projectDaoSnapshot.getCollectionDao().getCollectionById(collectionId));
                  action.getData().append(DelayedAction.DATA_COLLECTION_NAME, collection.getName());
                  action.getData().append(DelayedAction.DATA_COLLECTION_ICON, collection.getIcon());
                  action.getData().append(DelayedAction.DATA_COLLECTION_COLOR, collection.getColor());

                  if (!permissionAdapter.hasRole(organization, project, collection, RoleType.Read, action.getReceiver())) {
                     return false;
                  }

                  if (documentId != null) {
                     final Document document = projectDaoSnapshot.getDocumentDao().getDocumentById(documentId);
                     if (!permissionAdapter.canReadDocument(organization, project, document, collection, action.getReceiver())) {
                        return false;
                     }
                  }
               }

            }
         }
      } catch (ResourceNotFoundException e) {
         return false;
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
            delayedActionDao.updateAction(action);

            return true;
         }
      }

      return false;
   }

   // format due date to string according to attribute constraint format and user language
   private Map<String, Object> processData(final DataDocument originalData, final Language language) {
      final Map<String, Object> data = new HashMap<>(originalData);

      if (originalData.getDate(DelayedAction.DATA_TASK_DUE_DATE) != null) {

         String format = translationManager.getDefaultDateFormat(language);
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

      data.remove(DelayedAction.DATA_ORIGINAL_ACTION_IDS);
      data.remove(DelayedAction.DATA_ORIGINAL_ACTION_TYPES);

      return data;
   }

   private String getEmailSubjectPart(final DelayedAction action, final Map<String, Object> additionalData, final Language language) {
      final StringBuilder subject = new StringBuilder();

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
         case TASK_REOPENED:
         case TASK_CHANGED:
         default:
            if (StringUtils.isNotEmpty((String) additionalData.get(DelayedAction.DATA_TASK_NAME))) {
               subject.append(additionalData.get(DelayedAction.DATA_TASK_NAME).toString());
            } else {
               subject.append(translationManager.getUnknownTaskName(language));
            }
      }

      if (additionalData.containsKey(DelayedAction.DATA_ORGANIZATION_CODE)) {
         if (subject.length() > 0) {
            subject.append(" [");
         }

         subject.append(additionalData.get(DelayedAction.DATA_ORGANIZATION_CODE));

         if (additionalData.containsKey(DelayedAction.DATA_PROJECT_CODE)) {
            subject.append("/");
            subject.append(additionalData.get(DelayedAction.DATA_PROJECT_CODE));
            subject.append("]");
         }
      }

      return subject.toString();
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
         case TASK_REOPENED:
            return EmailService.EmailTemplate.TASK_REOPENED;
         case TASK_CHANGED:
            return action.getData().getArrayList(DelayedAction.DATA_ORIGINAL_ACTION_TYPES, NotificationType.class).contains(NotificationType.TASK_ASSIGNED) ?
                  EmailService.EmailTemplate.TASK_ASSIGNED :
                  (action.getData().getArrayList(DelayedAction.DATA_ORIGINAL_ACTION_TYPES, NotificationType.class).contains(NotificationType.TASK_REOPENED) ?
                        EmailService.EmailTemplate.TASK_REOPENED :
                        EmailService.EmailTemplate.TASK_UPDATED);
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
   private Map<String, User> getUsers(final List<User> users) {
      return users.stream()
                  .collect(Collectors.toMap(User::getId, Function.identity()));
   }

   private List<User> getUsersFromActions(final List<DelayedAction> actions, final List<User> loadedUsers) {
      var loadedEmails = loadedUsers.stream().map(User::getEmail).collect(Collectors.toList());
      return actions.stream()
                    .map(DelayedAction::getReceiver)
                    .distinct()
                    .filter(email -> !loadedEmails.contains(email))
                    .map(userDao::getUserByEmail)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
   }

   // get map of user email -> user language
   private Map<String, Language> initializeLanguages(final java.util.Collection<User> users) {
      return users.stream()
                  .collect(
                        Collectors.toMap(
                              User::getEmail,
                              user -> Language.valueOf((user.getNotificationsLanguage() != null ? user.getNotificationsLanguage() : "en").toUpperCase())
                        )
                  );
   }

   // get map of user email -> user id
   private Map<String, String> getUserIds(final java.util.Collection<User> users) {
      return users.stream()
                  .collect(
                        Collectors.toMap(User::getEmail, User::getId)
                  );
   }

   private Event createUserNotificationEvent(final UserNotification notification, final String event, final String userId) {
      return new Event(PusherFacade.eventChannel(userId), UserNotification.class.getSimpleName() + event, notification);
   }

}
