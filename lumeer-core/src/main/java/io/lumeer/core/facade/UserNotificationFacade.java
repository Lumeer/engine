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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.exception.AccessForbiddenException;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.storage.api.dao.UserNotificationDao;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
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

   private List<UserNotification> createResourceSharedNotifications(final Resource resource, final java.util.Collection<String> newUsers) {
      if (!workspaceKeeper.getOrganization().isPresent() && resource.getType() != ResourceType.ORGANIZATION) {
         return Collections.emptyList();
      }

      // TODO check that all newUsers are in resource permissions
      final DataDocument data = new DataDocument();

      if (resource.getType() == ResourceType.ORGANIZATION) {
         data.append(UserNotification.OrganizationShared.ORGANIZATION_ID, resource.getId());
      }

      if (resource.getType() == ResourceType.PROJECT) {
         data.append(UserNotification.ProjectShared.ORGANIZATION_ID, workspaceKeeper.getOrganization().get().getId());
         data.append(UserNotification.ProjectShared.PROJECT_ID, resource.getId());
         data.append(UserNotification.ProjectShared.PROJECT_CODE, resource.getCode());
         data.append(UserNotification.ProjectShared.PROJECT_NAME, resource.getName());
         data.append(UserNotification.ProjectShared.PROJECT_ICON, resource.getIcon());
         data.append(UserNotification.ProjectShared.PROJECT_COLOR, resource.getColor());
      }

      if (resource.getType() == ResourceType.COLLECTION) {
         data.append(UserNotification.CollectionShared.ORGANIZATION_ID, workspaceKeeper.getOrganization().get().getId());
         if (workspaceKeeper.getProject().isPresent()) {
            final Project project = workspaceKeeper.getProject().get();
            data.append(UserNotification.CollectionShared.PROJECT_ID, project.getId());
            data.append(UserNotification.CollectionShared.PROJECT_CODE, project.getCode());
            data.append(UserNotification.CollectionShared.PROJECT_NAME, project.getName());
            data.append(UserNotification.CollectionShared.PROJECT_ICON, project.getIcon());
            data.append(UserNotification.CollectionShared.PROJECT_COLOR, project.getColor());
         }
         data.append(UserNotification.CollectionShared.COLLECTION_ID, resource.getId());
         data.append(UserNotification.CollectionShared.COLLECTION_NAME, resource.getName());
         data.append(UserNotification.CollectionShared.COLLECTION_ICON, resource.getIcon());
         data.append(UserNotification.CollectionShared.COLLECTION_COLOR, resource.getColor());
      }

      if (resource.getType() == ResourceType.VIEW) {
         data.append(UserNotification.ViewShared.ORGANIZATION_ID, workspaceKeeper.getOrganization().get().getId());
         if (workspaceKeeper.getProject().isPresent()) {
            final Project project = workspaceKeeper.getProject().get();
            data.append(UserNotification.ViewShared.PROJECT_ID, project.getId());
            data.append(UserNotification.ViewShared.PROJECT_CODE, project.getCode());
            data.append(UserNotification.ViewShared.PROJECT_NAME, project.getName());
            data.append(UserNotification.ViewShared.PROJECT_ICON, project.getIcon());
            data.append(UserNotification.ViewShared.PROJECT_COLOR, project.getColor());
         }
         data.append(UserNotification.ViewShared.VIEW_CODE, resource.getCode());
         data.append(UserNotification.ViewShared.VIEW_PERSPECTIVE, ((View) resource).getPerspective());
         data.append(UserNotification.ViewShared.VIEW_NAME, resource.getName());
      }

      final List<UserNotification> notifications = newUsers.stream().map(userId ->
            createNotification(userId, getNotificationTypeByResource(resource), data)
      ).collect(Collectors.toList());

      return dao.createNotificationsBatch(notifications);
   }

   private UserNotification.NotificationType getNotificationTypeByResource(final Resource resource) {
      switch (resource.getType()) {
         case ORGANIZATION:
            return UserNotification.NotificationType.ORGANIZATION_SHARED;
         case PROJECT:
            return UserNotification.NotificationType.PROJECT_SHARED;
         case COLLECTION:
            return UserNotification.NotificationType.COLLECTION_SHARED;
         case VIEW:
            return UserNotification.NotificationType.VIEW_SHARED;
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

   private UserNotification createNotification(final String userId, final UserNotification.NotificationType type, final DataDocument data) {
      return new UserNotification(userId, ZonedDateTime.now(), false, null, type, data);
   }
}
