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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.DelayedAction;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.NotificationChannel;
import io.lumeer.api.model.NotificationFrequency;
import io.lumeer.api.model.NotificationSetting;
import io.lumeer.api.model.NotificationType;
import io.lumeer.api.model.NotificationsSettings;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserNotification;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.action.DelayedActionProcessor;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserNotificationDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class DelayedActionIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String COLLECTION_NAME = "Testing collection";
   private static final String COLLECTION_ICON = "fa-eye";
   private static final String COLLECTION_COLOR = "#00ee00";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String USER2 = "rspath@lumeerio.com";
   private User user, user2;
   private String organizationId;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private UserNotificationDao userNotificationDao;

   @Inject
   private DelayedActionDao delayedActionDao;

   @Inject
   private DelayedActionProcessor delayedActionProcessor;

   private Collection collection;

   @Before
   public void configureCollection() {
      User user = new User(USER);
      setUserNotifications(user);
      this.user = userDao.createUser(user);

      User user2 = new User(USER2);
      setUserNotifications(user2);
      this.user2 = userDao.createUser(user2);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);
      organizationId = storedOrganization.getId();

      projectDao.setOrganization(storedOrganization);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      Permission user2Permission = Permission.buildWithRoles(this.user2.getId(), Organization.ROLES);
      organizationPermissions.addUserPermissions(Set.of(userPermission, user2Permission));
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.addUserPermissions(Set.of(new Permission(this.user.getId(), Project.ROLES), new Permission(this.user2.getId(), Project.ROLES)));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(storedOrganization.getId(), storedProject.getId());

      collectionDao.setProject(storedProject);
      collectionDao.createRepository(storedProject);

      Permissions collectionPermissions = new Permissions();
      collectionPermissions.addUserPermissions(Set.of(new Permission(this.user.getId(), Project.ROLES), new Permission(this.user2.getId(), Project.ROLES)));
      Collection jsonCollection = new Collection(null, COLLECTION_NAME, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
      jsonCollection.setLastAttributeNum(0);
      jsonCollection.setAttributes(List.of(
            new Attribute("a0", "Summary", null, new Constraint(ConstraintType.Text, null), null, null, 0, null),
            new Attribute("a1", "Assignee", null, new Constraint(ConstraintType.User, new org.bson.Document("multi", true).append("externalUsers", true)), null, null, 0, null),
            new Attribute("a2", "Due date", null, new Constraint(ConstraintType.DateTime, new org.bson.Document("format", "DD/MM/YYYY H:mm:ss")), null, null, 0, null),
            new Attribute("a3", "State", null, new Constraint(ConstraintType.Select, new org.bson.Document("options",
                  List.of(
                        new org.bson.Document("value", "New").append("displayValue", ""),
                        new org.bson.Document("value", "In Progress").append("displayValue", ""),
                        new org.bson.Document("value", "To Do").append("displayValue", ""),
                        new org.bson.Document("value", "Done").append("displayValue", ""),
                        new org.bson.Document("value", "Won't fix").append("displayValue", "")
                  )
            )), null, null, 0, null),
            new Attribute("a4", "Observers", null, new Constraint(ConstraintType.User, new org.bson.Document("multi", true).append("externalUsers", true)), null, null, 0, null),
            new Attribute("a5", "Something", null, null, null, null, 0, null)
      ));
      jsonCollection.setDefaultAttributeId("a0");
      jsonCollection.setPurpose(new CollectionPurpose(CollectionPurposeType.Tasks,
            new DataDocument(CollectionPurpose.META_ASSIGNEE_ATTRIBUTE_ID, "a1")
                  .append(CollectionPurpose.META_DUE_DATE_ATTRIBUTE_ID, "a2")
                  .append(CollectionPurpose.META_STATE_ATTRIBUTE_ID, "a3")
                  .append(CollectionPurpose.META_FINAL_STATES_LIST, List.of("Done", "Won't fix"))
                  .append(CollectionPurpose.META_OBSERVERS_ATTRIBUTE_ID, "a4"))
      );
      collection = collectionDao.createCollection(jsonCollection);
   }

   private void setUserNotifications(final User user) {
      user.setNotifications(new NotificationsSettings(List.of(
            new NotificationSetting(NotificationType.ORGANIZATION_SHARED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.PROJECT_SHARED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.COLLECTION_SHARED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.VIEW_SHARED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.BULK_ACTION, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_ASSIGNED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_REOPENED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_UPDATED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_REMOVED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_UNASSIGNED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.STATE_UPDATE, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.DUE_DATE_SOON, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.PAST_DUE_DATE, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.DUE_DATE_CHANGED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_COMMENTED, NotificationChannel.Internal, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_MENTIONED, NotificationChannel.Internal, NotificationFrequency.Immediately),

            new NotificationSetting(NotificationType.ORGANIZATION_SHARED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.PROJECT_SHARED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.COLLECTION_SHARED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.VIEW_SHARED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.BULK_ACTION, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_ASSIGNED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_REOPENED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_UPDATED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_REMOVED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_UNASSIGNED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.STATE_UPDATE, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.DUE_DATE_SOON, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.PAST_DUE_DATE, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.DUE_DATE_CHANGED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_COMMENTED, NotificationChannel.Email, NotificationFrequency.Immediately),
            new NotificationSetting(NotificationType.TASK_MENTIONED, NotificationChannel.Email, NotificationFrequency.Immediately)
      ), "cs"));
   }

   private Document createDocument(final String summary, final List<String> assignees, final Date dueDate, final String state, final List<String> observers, final String something) {
      DataDocument data = new DataDocument()
            .append("a0", summary)
            .append("a1", assignees)
            .append("a2", dueDate)
            .append("a3", state)
            .append("a4", observers)
            .append("a5", something);
      return documentFacade.createDocument(collection.getId(), new Document(data));
   }

   private <T, A> Map<T, Integer> countOccurrences(final List<A> actions, Function<A, T> fce) {
      return actions.stream().map(fce).reduce(
            new HashMap<>(),
            (map, type) -> {
               map.put(type, map.getOrDefault(type, 0) + 1);
               return map;
            },
            (map1, map2) -> {
               map2.entrySet().forEach(entry -> map1.put(entry.getKey(), map1.getOrDefault(entry.getKey(), 0) + entry.getValue()));
               return map1;
            }
      );
   }

   @Test
   public void testAssignment() {
      List<UserNotification> notifications = userNotificationDao.getRecentNotifications(user2.getId());
      assertThat(notifications.size()).isEqualTo(0);

      Document doc = createDocument("My cool task", List.of("evžen@vystrčil.cz", user2.getEmail()), new Date(ZonedDateTime.now().minus(DelayedActionDao.PROCESSING_DELAY_MINUTES, ChronoUnit.MINUTES).toInstant().toEpochMilli()), "To Do", List.of(), "so just another task");

      List<DelayedAction> actions = delayedActionDao.getActions();
      // now we should have:
      // evžen@vystrčil.cz - PAST_DUE_DATE x 3 channels
      // rspath@lumeerio.com (USER2 - observer) - PAST_DUE_DATE x 3 channels
      // evžen@vystrčil.cz - TASK_ASSIGNED x 3 channels
      // rspath@lumeerio.com (USER2 - observer) - TASK_ASSIGNED x 3 channels
      // evžen@vystrčil.cz - STATE_UPDATE x 3 channels
      // rspath@lumeerio.com (USER2 - observer) - STATE_UPDATE x 3 channels
      var types = countOccurrences(actions, DelayedAction::getNotificationType);
      assertThat(types.get(NotificationType.STATE_UPDATE)).isEqualTo(6);
      assertThat(types.get(NotificationType.TASK_ASSIGNED)).isEqualTo(6);
      assertThat(types.get(NotificationType.PAST_DUE_DATE)).isEqualTo(6);

      assertThat(countOccurrences(actions, DelayedAction::getStartedProcessing).get(null)).isEqualTo(18);
      assertThat(countOccurrences(actions, DelayedAction::getCompleted).get(null)).isEqualTo(18);

      assertThat(countOccurrences(actions, (action) -> action.getCheckAfter().isBefore(ZonedDateTime.now())).get(true)).isEqualTo(18);

      var channels = countOccurrences(actions, DelayedAction::getNotificationChannel);
      assertThat(channels.get(NotificationChannel.Email)).isEqualTo(6);
      assertThat(channels.get(NotificationChannel.Internal)).isEqualTo(6);
      assertThat(channels.get(NotificationChannel.Slack)).isEqualTo(6);

      assertThat(countOccurrences(actions, DelayedAction::getInitiator).get(user.getEmail())).isEqualTo(18);
      assertThat(countOccurrences(actions, DelayedAction::getReceiver).get(user2.getEmail())).isEqualTo(9);

      delayedActionProcessor.process();

      notifications = userNotificationDao.getRecentNotifications(user2.getId());
      // now TASK_ASSIGNED and STATE_UPDATE got aggregated to TASK_ASSIGNED
      // each user receives 2 internal notifications - aggregated TASK_ASSIGNED and PAST_DUE_DATE
      assertThat(notifications.size()).isEqualTo(2);
      types = countOccurrences(notifications, UserNotification::getType);
      assertThat(types.get(NotificationType.PAST_DUE_DATE)).isEqualTo(1);
      assertThat(types.get(NotificationType.TASK_ASSIGNED)).isEqualTo(1);

      actions = delayedActionDao.getActions();
      // each user (2) has 3 channels and one PAST_DUE_DATE message scheduled for future = 6
      assertThat(countOccurrences(actions, DelayedAction::getStartedProcessing).getOrDefault(null, 0)).isEqualTo(6);
      assertThat(countOccurrences(actions, DelayedAction::getCompleted).getOrDefault(null, 0)).isEqualTo(6);

      // Removing USER2 user from assignees
      Document patched = documentFacade.patchDocumentData(collection.getId(), doc.getId(), new DataDocument("a1", List.of("evžen@vystrčil.cz")));
      delayedActionProcessor.process();
      actions = delayedActionDao.getActions();
      // USER2 has TASK_UNASSIGNED for each channel (3) = 3
      // evžen@vystrčil.cz has previous PAST_DUE_DATE on each channel = 3

      types = countOccurrences(actions, DelayedAction::getNotificationType);
      assertThat(types.getOrDefault(NotificationType.STATE_UPDATE, 0)).isEqualTo(0);
      assertThat(types.getOrDefault(NotificationType.TASK_ASSIGNED, 0)).isEqualTo(0);
      assertThat(types.getOrDefault(NotificationType.TASK_UNASSIGNED, 0)).isEqualTo(3);
      assertThat(types.getOrDefault(NotificationType.PAST_DUE_DATE, 0)).isEqualTo(3);

      assertThat(countOccurrences(actions, DelayedAction::getStartedProcessing).getOrDefault(null, 0)).isEqualTo(3);
      assertThat(countOccurrences(actions, DelayedAction::getCompleted).getOrDefault(null, 0)).isEqualTo(3);

      assertThat(countOccurrences(actions, (action) -> action.getCheckAfter().isBefore(ZonedDateTime.now())).get(true)).isEqualTo(3);

      // Adding USER2 user back to assignees
      patched = documentFacade.patchDocumentData(collection.getId(), doc.getId(), new DataDocument("a1", List.of(user2.getEmail())));
      delayedActionProcessor.process();
      actions = delayedActionDao.getActions();

      types = countOccurrences(actions, DelayedAction::getNotificationType);
      assertThat(types.get(NotificationType.PAST_DUE_DATE)).isEqualTo(3);
      assertThat(types.get(NotificationType.TASK_ASSIGNED)).isEqualTo(3);

      assertThat(countOccurrences(actions, DelayedAction::getReceiver).get(user2.getEmail())).isEqualTo(6);

      notifications = userNotificationDao.getRecentNotifications(user2.getId());

      assertThat(notifications.size()).isEqualTo(2 + 3); // there should be three additional notifications - TASK_UNASSIGNED, TASK_ASSIGNED, PAST_DUE_DATE
      types = countOccurrences(notifications, UserNotification::getType);
      assertThat(types.containsKey(NotificationType.STATE_UPDATE)).isFalse(); // this was aggregated under ASSIGNED
      assertThat(types.get(NotificationType.PAST_DUE_DATE)).isEqualTo(1 + 1);
      assertThat(types.get(NotificationType.TASK_ASSIGNED)).isEqualTo(1 + 1);
      assertThat(types.get(NotificationType.TASK_UNASSIGNED)).isEqualTo(1);

      assertThat(actions.size()).isEqualTo(9); // three previously processed actions are removed
      assertThat(countOccurrences(actions, DelayedAction::getStartedProcessing).getOrDefault(null, 0)).isEqualTo(3);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.PAST_DUE_DATE)).isEqualTo(3);

      // Setting state to completed
      patched = documentFacade.patchDocumentData(collection.getId(), doc.getId(), new DataDocument("a3", "Done"));
      actions = delayedActionDao.getActions();

      var newActions = actions.stream().filter(action -> action.getStartedProcessing() == null).collect(Collectors.toList());
      assertThat(newActions.size()).isEqualTo(3); // past due date actions were replaced with state update
      assertThat(countOccurrences(newActions, DelayedAction::getNotificationType).get(NotificationType.STATE_UPDATE)).isEqualTo(3);

      delayedActionProcessor.process();

      // Setting due date in future, but the task is completed
      patched = documentFacade.patchDocumentData(collection.getId(), doc.getId(), new DataDocument("a2", new Date(ZonedDateTime.now().plus(1, ChronoUnit.DAYS).toInstant().toEpochMilli())));
      actions = delayedActionDao.getActions();

      assertThat(actions.stream().filter(action -> action.getStartedProcessing() == null).count()).isEqualTo(0);
      assertThat(actions.size()).isEqualTo(3); // not assigned to user, nothing has changed, except for the fact that the actions were processed
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.STATE_UPDATE)).isEqualTo(3);

      delayedActionProcessor.process();

      // Setting state as incomplete
      patched = documentFacade.patchDocumentData(collection.getId(), doc.getId(), new DataDocument("a3", "New"));

      actions = delayedActionDao.getActions();

      assertThat(actions.stream().filter(action -> action.getStartedProcessing() == null).count()).isEqualTo(9); // assignment and past due actions are back
      assertThat(actions.size()).isEqualTo(9);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.STATE_UPDATE)).isEqualTo(3);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.TASK_REOPENED)).isEqualTo(3);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.PAST_DUE_DATE)).isEqualTo(3);

      // Setting due date in the future again so expecting new notifications
      patched = documentFacade.patchDocumentData(collection.getId(), doc.getId(), new DataDocument("a2", new Date(ZonedDateTime.now().plus(4, ChronoUnit.DAYS).toInstant().toEpochMilli())));

      actions = delayedActionDao.getActions();

      assertThat(actions.stream().filter(action -> action.getStartedProcessing() == null).count()).isEqualTo(15); // we can even have due soon + due date changed
      assertThat(actions.size()).isEqualTo(15);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.STATE_UPDATE)).isEqualTo(3);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.TASK_REOPENED)).isEqualTo(3);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.PAST_DUE_DATE)).isEqualTo(3);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.DUE_DATE_SOON)).isEqualTo(3);
      assertThat(countOccurrences(actions, DelayedAction::getNotificationType).get(NotificationType.DUE_DATE_CHANGED)).isEqualTo(3);

      notifications = userNotificationDao.getRecentNotifications(user2.getId());
      assertThat(countOccurrences(notifications, UserNotification::getType).getOrDefault(NotificationType.TASK_CHANGED, 0)).isEqualTo(0);

      delayedActionProcessor.process();
      notifications = userNotificationDao.getRecentNotifications(user2.getId());
      assertThat(countOccurrences(notifications, UserNotification::getType).getOrDefault(NotificationType.TASK_CHANGED, 0)).isEqualTo(1);

      delayedActionDao.deleteAllScheduledActions(organizationId);
      actions = delayedActionDao.getActions();

      assertThat(actions.size()).isEqualTo(0);
   }

}
