/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
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

@Path("/{organization}/groups")
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
