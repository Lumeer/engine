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
import io.lumeer.api.model.InvitationType;
import io.lumeer.api.view.UserViews;
import io.lumeer.core.facade.GroupFacade;
import io.lumeer.remote.rest.annotation.HealthCheck;

import com.fasterxml.jackson.annotation.JsonView;

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/organizations/{organizationId:[0-9a-fA-F]{24}}/groups")
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
   @HealthCheck
   public Group createGroup(Group group) {
      return groupFacade.createGroup(group);
   }

   @PUT
   @Path("{groupId:[0-9a-fA-F]{24}}")
   @HealthCheck
   public Group updateGroup(@PathParam("groupId") String groupId, Group group) {
      return groupFacade.updateGroup(groupId, group);
   }

   @DELETE
   @Path("{groupId:[0-9a-fA-F]{24}}")
   public Response deleteGroup(@PathParam("groupId") String groupId) {
      groupFacade.deleteGroup(groupId);

      return Response.ok().link(getParentUri(groupId), "parent").build();
   }

   @POST
   @Path("projects/{projectId:[0-9a-fA-F]{24}}/groups/{invitationType}")
   @JsonView(UserViews.DefaultView.class)
   @HealthCheck
   public List<Group> createUsersInOrganization(@PathParam("organizationId") final String organizationId, @PathParam("projectId") final String projectId, @PathParam("invitationType") final InvitationType invitationType, final List<Group> groups) {
      return groupFacade.addGroupsToWorkspace(organizationId, projectId, groups, invitationType != null ? invitationType : InvitationType.JOIN_ONLY);
   }
}
