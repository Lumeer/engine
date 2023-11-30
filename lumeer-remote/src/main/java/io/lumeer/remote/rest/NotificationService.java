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
package io.lumeer.remote.rest;

import io.lumeer.api.model.UserNotification;
import io.lumeer.core.facade.UserNotificationFacade;

import java.util.List;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("notifications")
public class NotificationService extends AbstractService {

   @Inject
   private UserNotificationFacade userNotificationFacade;

   @GET
   public List<UserNotification> getNotifications() {
      return userNotificationFacade.getNotifications();
   }

   @PUT
   @Path("{notificationId}")
   public UserNotification updateNotification(@PathParam("notificationId") String notificationId, UserNotification notification) {
      return userNotificationFacade.updateNotification(notificationId, notification);
   }

   @DELETE
   @Path("{notificationId}")
   public Response deleteNotification(@PathParam("notificationId") String notificationId) {
      userNotificationFacade.deleteNotification(notificationId);

      return Response.ok().build();
   }


}
