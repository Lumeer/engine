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
package io.lumeer.remote.rest;

import io.lumeer.api.model.DefaultWorkspace;
import io.lumeer.api.model.Feedback;
import io.lumeer.api.model.User;
import io.lumeer.api.view.UserViews;
import io.lumeer.core.facade.UserFacade;
import io.lumeer.remote.rest.annotation.PATCH;

import com.fasterxml.jackson.annotation.JsonView;

import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("users")
public class UserService extends AbstractService {

   @Inject
   private UserFacade userFacade;

   @GET
   @JsonView(UserViews.DefaultView.class)
   @Path("organizations/{organizationId}/users")
   public List<User> getUsers(@PathParam("organizationId") String organizationId) {
      return userFacade.getUsers(organizationId);
   }

   @POST
   @Path("organizations/{organizationId}/users")
   public User createUserInOrganization(@PathParam("organizationId") String organizationId, User user) {
      return userFacade.createUser(organizationId, user);
   }

   @PUT
   @Path("organizations/{organizationId}/users/{userId}")
   public User updateUserInOrganization(@PathParam("organizationId") String organizationId,
         @PathParam("userId") String userId, User user) {
      return userFacade.updateUser(organizationId, userId, user);
   }

   @DELETE
   @Path("organizations/{organizationId}/users/{userId}")
   public Response deleteUserFromOrganization(@PathParam("organizationId") String organizationId,
         @PathParam("userId") String userId) {
      userFacade.deleteUser(organizationId, userId);

      return Response.ok().link(getParentUri(userId), "parent").build();
   }

   @GET
   @Path("current")
   @JsonView(UserViews.FullView.class)
   public User getCurrentUser() {
      return userFacade.getCurrentUser();
   }

   @PATCH
   @Path("current")
   @JsonView(UserViews.FullView.class)
   public User patchCurrentUser(User user) {
      return userFacade.patchCurrentUser(user, getFirstUrlPathPart());
   }

   @PUT
   @Path("workspace")
   public Response updateWorkspace(DefaultWorkspace defaultWorkspace) {
      if (defaultWorkspace == null) {
         throw new BadRequestException("defaultWorkspace");
      }

      userFacade.setDefaultWorkspace(defaultWorkspace);

      return Response.ok().build();
   }

   @POST
   @Path("feedback")
   public Response createFeedback(Feedback feedback) {
      userFacade.createFeedback(feedback);

      return Response.ok().build();
   }
}
