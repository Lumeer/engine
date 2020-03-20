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

import io.lumeer.api.model.Group;
import io.lumeer.core.facade.GroupFacade;

import java.util.List;
import javax.annotation.PostConstruct;
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
@Path("/organizations/{organizationId}/groups")
public class GroupService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PostConstruct
   public void init() {
      workspaceKeeper.setOrganizationId(organizationId);
   }

   @Inject
   private GroupFacade groupFacade;

   @GET
   public List<Group> getGroups() {
      return groupFacade.getGroups();
   }

   @POST
   public Group createGroup(Group group) {
      return groupFacade.createGroup(group);
   }

   @PUT
   @Path("{groupId}")
   public Group updateGroup(@PathParam("groupId") String groupId, Group group) {
      return groupFacade.updateGroup(groupId, group);
   }

   @DELETE
   @Path("{groupId}")
   public Response deleteGroup(@PathParam("groupId") String groupId) {
      groupFacade.deleteGroup(groupId);

      return Response.ok().link(getParentUri(groupId), "parent").build();
   }
}
