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

import io.lumeer.engine.api.exception.UserAlreadyExistsException;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.UserGroupFacade;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */

@Path("/organizations/{organization}/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class UserService implements Serializable {

   @Inject
   private UserGroupFacade userGroupFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @PathParam("organization")
   private String organizationCode;

   @GET
   @Path("/")
   public Map<String, List<String>> getUsersAndGroups() {
      return userGroupFacade.getUsersAndGroups(organizationCode);
   }

   @POST
   @Path("/{user}")
   public void addUser(final @PathParam("user") String user, final List<String> groups) throws UserAlreadyExistsException {
      userGroupFacade.addUser(organizationCode, user, toArray(groups));
   }

   @DELETE
   @Path("/{user}")
   public void removeUser(final @PathParam("user") String user) {
      userGroupFacade.removeUser(organizationCode, user);
   }

   @GET
   @Path("/{user}/groups/")
   public List<String> getGroupsOfUser(final @PathParam("user") String user) {
      return userGroupFacade.getGroupsOfUser(organizationCode, user);
   }

   private String[] toArray(List<String> stringList) {
      if (stringList == null) {
         return new String[] {};
      }
      return stringList.toArray(new String[stringList.size()]);
   }
}
