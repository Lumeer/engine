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

import io.lumeer.api.model.Language;
import io.lumeer.api.model.NotificationChannel;
import io.lumeer.api.model.NotificationType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.AccessForbiddenException;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserNotificationDao;

import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@RequestScoped
public class UserNotificationFacade extends AbstractFacade {

   @Inject
   private Logger log;

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private UserNotificationDao dao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private EmailService emailService;

   @Inject
   private UserDao userDao;

   public List<UserNotification> getNotifications() {
      return dao.getRecentNotifications(authenticatedUser.getCurrentUserId());
   }

   public void deleteNotification(final String notificationId) {
      final UserNotification userNotification = dao.getNotificationById(notificationId);

      if (authenticatedUser.getCurrentUserId().equals(userNotification.getUserId())) {
         dao.deleteNotification(userNotification);
      } else {
         throw new AccessForbiddenException("Cannot delete user notification that does not belong to current user.");
      }
   }

   public UserNotification updateNotification(final String notificationId, final UserNotification notification) {
      final UserNotification dbNotification = dao.getNotificationById(notificationId);

      if (dbNotification.getUserId().equals(authenticatedUser.getCurrentUserId())) {
         if (notification.isRead() && !dbNotification.isRead() && dbNotification.getFirstReadAt() == null) {
            dbNotification.setFirstReadAt(ZonedDateTime.now());
         }
         dbNotification.setRead(notification.isRead());

         return dao.updateNotification(dbNotification);
      }

      return null;
   }

   private void appendOrganization(final DataDocument data) {
      if (workspaceKeeper.getOrganization().isPresent()) {
         final Organization organization = workspaceKeeper.getOrganization().get();

         data.append(UserNotification.OrganizationShared.ORGANIZATION_ID, organization.getId());
         data.append(UserNotification.OrganizationShared.ORGANIZATION_CODE, organization.getCode());
         data.append(UserNotification.OrganizationShared.ORGANIZATION_ICON, organization.getIcon());
         data.append(UserNotification.OrganizationShared.ORGANIZATION_COLOR, organization.getColor());
         data.append(UserNotification.OrganizationShared.ORGANIZATION_NAME, organization.getName());
      }
   }

   private void appendProject(final DataDocument data) {
      if (workspaceKeeper.getProject().isPresent()) {
         final Project project = workspaceKeeper.getProject().get();

         data.append(UserNotification.ProjectShared.PROJECT_ID, project.getId());
         data.append(UserNotification.ProjectShared.PROJECT_CODE, project.getCode());
         data.append(UserNotification.ProjectShared.PROJECT_NAME, project.getName());
         data.append(UserNotification.ProjectShared.PROJECT_ICON, project.getIcon());
         data.append(UserNotification.ProjectShared.PROJECT_COLOR, project.getColor());
      }
   }

   private DataDocument getResourceDescription(final Resource resource) {
      final DataDocument data = new DataDocument();

      if (resource.getType() == ResourceType.ORGANIZATION) {
         data.append(UserNotification.OrganizationShared.ORGANIZATION_ID, resource.getId());
         data.append(UserNotification.OrganizationShared.ORGANIZATION_CODE, resource.getCode());
         data.append(UserNotification.OrganizationShared.ORGANIZATION_ICON, resource.getIcon());
         data.append(UserNotification.OrganizationShared.ORGANIZATION_COLOR, resource.getColor());
         data.append(UserNotification.OrganizationShared.ORGANIZATION_NAME, resource.getName());
      }

      if (resource.getType() == ResourceType.PROJECT) {
         appendOrganization(data);
         data.append(UserNotification.ProjectShared.PROJECT_ID, resource.getId());
         data.append(UserNotification.ProjectShared.PROJECT_CODE, resource.getCode());
         data.append(UserNotification.ProjectShared.PROJECT_ICON, resource.getIcon());
         data.append(UserNotification.ProjectShared.PROJECT_COLOR, resource.getColor());
         data.append(UserNotification.ProjectShared.PROJECT_NAME, resource.getName());
      }

      if (resource.getType() == ResourceType.COLLECTION) {
         appendOrganization(data);
         appendProject(data);
         data.append(UserNotification.CollectionShared.COLLECTION_ID, resource.getId());
         data.append(UserNotification.CollectionShared.COLLECTION_NAME, resource.getName());
         data.append(UserNotification.CollectionShared.COLLECTION_ICON, resource.getIcon());
         data.append(UserNotification.CollectionShared.COLLECTION_COLOR, resource.getColor());

         final String query = new Query(List.of(new QueryStem(resource.getId(), null, null, null, null)), null, null, null).toQueryString();
         data.append(UserNotification.CollectionShared.COLLECTION_QUERY, Utils.encodeQueryParam(query));
      }

      if (resource.getType() == ResourceType.VIEW) {
         appendOrganization(data);
         appendProject(data);
         data.append(UserNotification.ViewShared.VIEW_CODE, resource.getCode());
         data.append(UserNotification.ViewShared.VIEW_PERSPECTIVE, ((View) resource).getPerspective());
         data.append(UserNotification.ViewShared.VIEW_NAME, resource.getName());
      }

      return data;
   }

   private List<UserNotification> createResourceSharedNotifications(final Resource resource, final java.util.Collection<String> newUsers) {
      if (workspaceKeeper.getOrganization().isEmpty() && resource.getType() != ResourceType.ORGANIZATION) {
         return Collections.emptyList();
      }

      final DataDocument data = getResourceDescription(resource);

      final List<UserNotification> notifications =
            newUsers.stream()
                    .filter(userId -> permissionsChecker.hasRole(resource, Role.READ, userId) && filterNotificationsByManagers(resource, userId))
                    .map(userId -> createNotification(userId, getNotificationTypeByResource(resource), data)
      ).collect(Collectors.toList());

      return dao.createNotificationsBatch(notifications);
   }

   private EmailService.EmailTemplate getEmailTemplate(final Resource resource) {
      if (resource.getType() == ResourceType.ORGANIZATION) {
         return EmailService.EmailTemplate.ORGANIZATION_SHARED;
      }

      if (resource.getType() == ResourceType.PROJECT) {
         return EmailService.EmailTemplate.PROJECT_SHARED;
      }

      if (resource.getType() == ResourceType.COLLECTION) {
         return EmailService.EmailTemplate.COLLECTION_SHARED;
      }

      if (resource.getType() == ResourceType.VIEW) {
         return EmailService.EmailTemplate.VIEW_SHARED;
      }

      return null;
   }

   private boolean hasUserEnabledNotifications(final Resource resource, final User user) {
      NotificationType type = null;
      switch (resource.getType()) {
         case ORGANIZATION:
            type = NotificationType.ORGANIZATION_SHARED;
            break;
         case PROJECT:
            type = NotificationType.PROJECT_SHARED;
            break;
         case COLLECTION:
            type = NotificationType.COLLECTION_SHARED;
            break;
         case VIEW:
            type = NotificationType.VIEW_SHARED;
            break;
      }

      if (type != null) {
         final NotificationType finalType = type;
         return user.getNotificationsSettingsList().stream().anyMatch(notification -> notification.getNotificationType() == finalType && notification.getNotificationChannel() == NotificationChannel.Email);
      }

      return false;
   }

   private void sendResourceSharedEmails(final Resource resource, final java.util.Collection<String> newUsers) {
      if (workspaceKeeper.getOrganization().isPresent() || resource.getType() == ResourceType.ORGANIZATION) {
         final Map<String, User> users = getUsers(newUsers);
         final Map<String, Language> languages = initializeLanguages(users.values());

         newUsers.stream()
                 .filter(user -> hasUserEnabledNotifications(resource, users.get(user)) && permissionsChecker.hasRole(resource, Role.READ, user) && filterNotificationsByManagers(resource, user))
                 .forEach(user ->
                       emailService.sendEmailFromTemplate(
                             getEmailTemplate(resource),
                             languages.getOrDefault(user, Language.EN),
                             emailService.formatUserReference(authenticatedUser.getCurrentUser()),
                             users.get(user).getEmail(),
                             StringUtils.isNotEmpty(resource.getName()) ? resource.getName() : resource.getCode(),
                             getResourceDescription(resource))
         );
      }
   }

   // get map of user id -> user
   private Map<String, User> getUsers(final java.util.Collection<String> userIds) {
      return userIds.stream()
                    .distinct()
                    .map(userDao::getUserById)
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

   private boolean filterNotificationsByManagers(final Resource resource, final String userId) {
      if (resource.getType() == ResourceType.ORGANIZATION) {
         return true;
      }

      if (resource.getType() == ResourceType.PROJECT) {
         if (workspaceKeeper.getOrganization().isPresent()) {
            return !permissionsChecker.hasRole(workspaceKeeper.getOrganization().get(), Role.MANAGE, userId);
         } else {
            return true;
         }
      }

      // for collection, view, document
      return !permissionsChecker.isManager(userId);
   }

   private NotificationType getNotificationTypeByResource(final Resource resource) {
      switch (resource.getType()) {
         case ORGANIZATION:
            return NotificationType.ORGANIZATION_SHARED;
         case PROJECT:
            return NotificationType.PROJECT_SHARED;
         case COLLECTION:
            return NotificationType.COLLECTION_SHARED;
         case VIEW:
            return NotificationType.VIEW_SHARED;
         default:
            return null;
      }
   }

   private Set<String> getManagers(final Resource resource) {
      if (resource.getType() == ResourceType.ORGANIZATION) {
         return Collections.emptySet();
      } else if (resource.getType() == ResourceType.PROJECT) {
         return permissionsChecker.getOrganizationManagers();
      }
      return permissionsChecker.getWorkspaceManagers();
   }

   /* Managers are handled on resource creation, they may or may not be in the resource permissions.
      At the beginning, we know they are not there. Later, we can remove all managers from notifications
      upon resource update because they were already notified. If we did not do that, we would never
      be able to to tell whether we already sent a notification to the manager.
    */
   public void createResource(@Observes final CreateResource createResource) {
      try {
         Set<String> managers = getManagers(createResource.getResource());
         managers.remove(authenticatedUser.getCurrentUserId());

         if (managers.size() > 0) {
            createResourceSharedNotifications(createResource.getResource(), managers);
            sendResourceSharedEmails(createResource.getResource(), managers);
         }
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to create notification: ", e);
      }
   }

   public void updateResource(@Observes final UpdateResource updateResource) {
      try {
         final Set<String> managers = getManagers(updateResource.getResource());
         final Set<String> removedUsers = ResourceUtils.getRemovedPermissions(updateResource.getOriginalResource(), updateResource.getResource());
         removedUsers.removeAll(managers);
         removedUsers.remove(authenticatedUser.getCurrentUserId());

         if (removedUsers.size() > 0) {
            removeNotifications(updateResource.getResource(), removedUsers);
         }

         final Set<String> addedUsers = ResourceUtils.getAddedPermissions(updateResource.getOriginalResource(), updateResource.getResource());
         addedUsers.removeAll(managers);
         addedUsers.remove(authenticatedUser.getCurrentUserId());

         if (addedUsers.size() > 0) {
            createResourceSharedNotifications(updateResource.getResource(), addedUsers);
            sendResourceSharedEmails(updateResource.getResource(), addedUsers);
         }

         updateExistingNotifications(updateResource.getOriginalResource(), updateResource.getResource());
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to create notification: ", e);
      }
   }

   public void removeResource(@Observes final RemoveResource removedResource) {
      removeNotifications(removedResource.getResource());
   }

   private void removeNotifications(Resource resource) {
      removeNotifications(resource, Collections.emptySet());
   }

   private void removeNotifications(Resource resource, Set<String> users) {
      switch (resource.getType()) {
         case ORGANIZATION:
            dao.removeNotifications(UserNotification.DATA + "." + UserNotification.OrganizationShared.ORGANIZATION_ID, resource.getId(), users);
            break;
         case PROJECT:
            dao.removeNotifications(UserNotification.DATA + "." + UserNotification.ProjectShared.PROJECT_ID, resource.getId(), users);
            break;
         case COLLECTION:
            dao.removeNotifications(UserNotification.DATA + "." + UserNotification.CollectionShared.COLLECTION_ID, resource.getId(), users);
            break;
         case VIEW:
            dao.removeNotifications(UserNotification.DATA + "." + UserNotification.ViewShared.VIEW_CODE, resource.getCode(), users);
            break;
      }
   }

   private void updateExistingNotifications(final Resource original, final Resource updated) {
      // we do not carry any detailed information on these types in notifications
      // original is null when we are in tests
      if (original == null || updated.getType() == ResourceType.ORGANIZATION || updated.getType() == ResourceType.DOCUMENT) {
         return;
      }

      if (isResourceUpdated(original, updated)) {
         switch (original.getType()) {
            case PROJECT:
               dao.updateNotifications(
                     UserNotification.DATA + "." + UserNotification.ProjectShared.PROJECT_ID,
                     original.getId(),
                     Map.of(
                           UserNotification.DATA + "." + UserNotification.ProjectShared.PROJECT_CODE, updated.getCode(),
                           UserNotification.DATA + "." + UserNotification.ProjectShared.PROJECT_COLOR, updated.getColor(),
                           UserNotification.DATA + "." + UserNotification.ProjectShared.PROJECT_ICON, updated.getIcon(),
                           UserNotification.DATA + "." + UserNotification.ProjectShared.PROJECT_NAME, updated.getName()
                     )
               );
               break;
            case COLLECTION:
               dao.updateNotifications(
                     UserNotification.DATA + "." + UserNotification.CollectionShared.COLLECTION_ID,
                     original.getId(),
                     Map.of(
                           UserNotification.DATA + "." + UserNotification.CollectionShared.COLLECTION_COLOR, updated.getColor(),
                           UserNotification.DATA + "." + UserNotification.CollectionShared.COLLECTION_ICON, updated.getIcon(),
                           UserNotification.DATA + "." + UserNotification.CollectionShared.COLLECTION_NAME, updated.getName()
                     )
               );
               break;
            case VIEW:
               dao.updateNotifications(
                     UserNotification.DATA + "." + UserNotification.ViewShared.VIEW_CODE,
                     original.getCode(),
                     Map.of(
                           UserNotification.DATA + "." + UserNotification.ViewShared.VIEW_NAME, updated.getName(),
                           UserNotification.DATA + "." + UserNotification.ViewShared.VIEW_PERSPECTIVE, ((View) updated).getPerspective()
                     )
               );
               break;
         }
      }
   }

   private boolean isResourceUpdated(final Resource original, final Resource updated) {
      boolean commonCheck = Objects.equals(original.getCode(), updated.getCode())
            && Objects.equals(original.getColor(), updated.getColor())
            && Objects.equals(original.getIcon(), updated.getIcon())
            && Objects.equals(original.getName(), updated.getName())
            && (original.getType() != ResourceType.VIEW || (((View) original).getPerspective().equals(((View) updated).getPerspective())));

      return !commonCheck;
   }

   private UserNotification createNotification(final String userId, final NotificationType type, final DataDocument data) {
      return new UserNotification(userId, ZonedDateTime.now(), false, null, type, data);
   }
}
