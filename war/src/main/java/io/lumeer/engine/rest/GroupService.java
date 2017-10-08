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
package io.lumeer.engine.rest;

import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.UserGroupFacade;

import java.io.Serializable;
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

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */

@Path("/organizations/{organization}/groups")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class GroupService implements Serializable {

   @Inject
   private UserGroupFacade userGroupFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @PathParam("organization")
   private String organizationCode;

   @GET
   @Path("/")
   public List<String> getGroups() {
      return userGroupFacade.getGroups(organizationCode);
   }

   @POST
   @Path("/{group}")
   public void addGroup(final @PathParam("group") String group) {
      userGroupFacade.addGroups(organizationCode, group);
   }

   @DELETE
   @Path("/{group}")
   public void removeGroup(final @PathParam("group") String group) {
      userGroupFacade.removeGroups(organizationCode, group);
   }

   @GET
   @Path("/{group}/users/")
   public List<String> getUsersInGroup(final @PathParam("group") String group) {
      return userGroupFacade.getUsersInGroup(organizationCode, group);
   }

   @PUT
   @Path("/{group}/users/{user}")
   public void addUserToGroup(final @PathParam("user") String user, final @PathParam("group") String group) {
      userGroupFacade.addUserToGroups(organizationCode, user, group);
   }

   @DELETE
   @Path("/{group}/users/{user}")
   public void removeUserFromGroup(final @PathParam("user") String user, final @PathParam("group") String group) {
      userGroupFacade.removeUserFromGroups(organizationCode, user, group);
   }
}
