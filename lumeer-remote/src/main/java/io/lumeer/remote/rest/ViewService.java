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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.View;
import io.lumeer.core.facade.ViewFacade;

import java.util.List;
import java.util.Set;
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
@Path("organizations/{organizationId}/projects/{projectId}/views")
public class ViewService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private ViewFacade viewFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationId, projectId);
   }

   @POST
   public View createView(View view) {
      return viewFacade.createView(view);
   }

   @PUT
   @Path("{viewId}")
   public View updateView(@PathParam("viewId") String id, View view) {
      return viewFacade.updateView(id, view);
   }

   @DELETE
   @Path("{viewId}")
   public Response deleteView(@PathParam("viewId") String id) {
      viewFacade.deleteView(id);

      return Response.ok().link(getParentUri(id), "parent").build();
   }

   @GET
   @Path("{viewId}")
   public View getView(@PathParam("viewId") String id) {
      return viewFacade.getViewById(id);
   }

   @GET
   public List<View> getViews() {
      return viewFacade.getViews();
   }

   @GET
   @Path("{viewId}/permissions")
   public Permissions getViewPermissions(@PathParam("viewId") String id) {
      return viewFacade.getViewPermissions(id);
   }

   @PUT
   @Path("{viewId}/permissions/users")
   public Set<Permission> updateUserPermission(@PathParam("viewId") String id, Set<Permission> userPermission) {
      return viewFacade.updateUserPermissions(id, userPermission);
   }

   @DELETE
   @Path("{viewId}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("viewId") String id, @PathParam("userId") String userId) {
      viewFacade.removeUserPermission(id, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{viewId}/permissions/groups")
   public Set<Permission> updateGroupPermission(@PathParam("viewId") String id, Set<Permission> groupPermission) {
      return viewFacade.updateGroupPermissions(id, groupPermission);
   }

   @DELETE
   @Path("{viewId}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("viewId") String id, @PathParam("groupId") String groupId) {
      viewFacade.removeGroupPermission(id, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

   @GET
   @Path("all/collections")
   public List<Collection> getViewsCollections() {
      return viewFacade.getViewsCollections();
   }

   @GET
   @Path("all/linkTypes")
   public List<LinkType> getViewsLinkTypes() {
      return viewFacade.getViewsLinkTypes();
   }
}
