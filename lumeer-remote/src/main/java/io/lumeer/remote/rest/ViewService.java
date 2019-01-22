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
@Path("organizations/{organizationCode}/projects/{projectCode}/views")
public class ViewService extends AbstractService {

   @PathParam("organizationCode")
   private String organizationCode;

   @PathParam("projectCode")
   private String projectCode;

   @Inject
   private ViewFacade viewFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationCode, projectCode);
   }

   @POST
   public View createView(View view) {
      return viewFacade.createView(view);
   }

   @PUT
   @Path("{viewCode}")
   public View updateView(@PathParam("viewCode") String code, View view) {
      return viewFacade.updateView(code, view);
   }

   @DELETE
   @Path("{viewCode}")
   public Response deleteView(@PathParam("viewCode") String code) {
      viewFacade.deleteView(code);

      return Response.ok().link(getParentUri(code), "parent").build();
   }

   @GET
   @Path("{viewCode}")
   public View getView(@PathParam("viewCode") String code) {
      return viewFacade.getViewByCode(code);
   }

   @GET
   public List<View> getViews() {
      return viewFacade.getViews();
   }

   @GET
   @Path("{viewCode}/permissions")
   public Permissions getViewPermissions(@PathParam("viewCode") String code) {
      return viewFacade.getViewPermissions(code);
   }

   @PUT
   @Path("{viewCode}/permissions/users")
   public Set<Permission> updateUserPermission(@PathParam("viewCode") String code, Set<Permission> userPermission) {
      return viewFacade.updateUserPermissions(code, userPermission);
   }

   @DELETE
   @Path("{viewCode}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("viewCode") String code, @PathParam("userId") String userId) {
      viewFacade.removeUserPermission(code, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{viewCode}/permissions/groups")
   public Set<Permission> updateGroupPermission(@PathParam("viewCode") String code, Set<Permission> groupPermission) {
      return viewFacade.updateGroupPermissions(code, groupPermission);
   }

   @DELETE
   @Path("{viewCode}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("viewCode") String code, @PathParam("groupId") String groupId) {
      viewFacade.removeGroupPermission(code, groupId);

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
