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
