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

import io.lumeer.api.model.User;
import io.lumeer.api.view.UserViews;
import io.lumeer.core.facade.UserFacade;

import com.fasterxml.jackson.annotation.JsonView;

import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
@Path("/organizations/{organizationId}/users")
public class UserService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @Inject
   private UserFacade userFacade;

   @GET
   @JsonView(UserViews.DefaultView.class)
   public List<User> getUsers() {
      return userFacade.getUsers(organizationId);
   }

   @POST
   public User createUser(User user) {
      return userFacade.createUser(organizationId, user);
   }

   @PUT
   @Path("{userId}")
   public User updateUser(@PathParam("userId") String userId, User user) {
      return userFacade.updateUser(organizationId, userId, user);
   }

   @DELETE
   @Path("{userId}")
   public Response deleteUser(@PathParam("userId") String userId) {
      userFacade.deleteUser(organizationId, userId);

      return Response.ok().link(getParentUri(userId), "parent").build();
   }
}
