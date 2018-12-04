/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.UserNotificationDao;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class UserNotificationFacade extends AbstractFacade {

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private UserNotificationDao dao;

   public List<UserNotification> getNotifications() {
      return dao.getRecentNotifications(authenticatedUser.getCurrentUserId());
   }

   public UserNotification updateNotification(final String notificationId, final UserNotification notification) {
      final UserNotification dbNotification = dao.getNotificationById(notificationId);

      if (dbNotification.getId().equals(notification.getId()) && dbNotification.getUserId().equals(authenticatedUser.getCurrentUserId())) {
         if (notification.isRead() && !dbNotification.isRead() && dbNotification.getFirstReadAt() == null) {
            dbNotification.setFirstReadAt(ZonedDateTime.now());
         }
         dbNotification.setRead(notification.isRead());

         return dao.updateNotification(dbNotification);
      }

      return null;
   }

   public List<UserNotification> createOrgenizationSharedNotifications(final Organization organization, final Permissions newUsers) {
      // TODO check that all newUsers are in resource permissions
      final DataDocument data = new DataDocument("organizationId", organization.getId());

      final List<UserNotification> notifications = newUsers.getUserPermissions().stream().map(p ->
            createNotification(p.getId(), UserNotification.NotificationType.ORGANIZATION_SHARED, data)
      ).collect(Collectors.toList());

      return dao.createNotificationsBatch(notifications);
   }

   private UserNotification createNotification(final String userId, final UserNotification.NotificationType type, final DataDocument data) {
      return new UserNotification(userId, ZonedDateTime.now(),false,null, type, data);
   }
}
