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

import io.lumeer.api.model.DefaultViewConfig;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.View;
import io.lumeer.core.facade.ViewFacade;
import io.lumeer.remote.rest.annotation.HealthCheck;

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
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/views")
public class ViewService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private ViewFacade viewFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   @HealthCheck
   public View createView(View view) {
      return viewFacade.createView(view);
   }

   @PUT
   @Path("{viewId:[0-9a-fA-F]{24}}")
   @HealthCheck
   public View updateView(@PathParam("viewId") String id, View view) {
      return viewFacade.updateView(id, view);
   }

   @DELETE
   @Path("{viewId:[0-9a-fA-F]{24}}")
   public Response deleteView(@PathParam("viewId") String id) {
      viewFacade.deleteView(id);

      return Response.ok().link(getParentUri(id), "parent").build();
   }

   @GET
   @Path("{viewId:[0-9a-fA-F]{24}}")
   public View getView(@PathParam("viewId") String id) {
      return viewFacade.getViewById(id);
   }

   @GET
   public List<View> getViews() {
      return viewFacade.getViews();
   }

   @GET
   @Path("{viewId:[0-9a-fA-F]{24}}/permissions")
   public Permissions getViewPermissions(@PathParam("viewId") String id) {
      return viewFacade.getViewPermissions(id);
   }

   @PUT
   @Path("{viewId:[0-9a-fA-F]{24}}/permissions")
   public Permissions updatePermissions(@PathParam("viewId") String id, Permissions permissions) {
      return viewFacade.updatePermissions(id, permissions);
   }

   @PUT
   @Path("{viewId:[0-9a-fA-F]{24}}/permissions/users")
   public Set<Permission> updateUserPermission(@PathParam("viewId") String id, Set<Permission> userPermission) {
      return viewFacade.updateUserPermissions(id, userPermission);
   }

   @DELETE
   @Path("{viewId:[0-9a-fA-F]{24}}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("viewId") String id, @PathParam("userId") String userId) {
      viewFacade.removeUserPermission(id, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{viewId:[0-9a-fA-F]{24}}/permissions/groups")
   public Set<Permission> updateGroupPermission(@PathParam("viewId") String id, Set<Permission> groupPermission) {
      return viewFacade.updateGroupPermissions(id, groupPermission);
   }

   @DELETE
   @Path("{viewId:[0-9a-fA-F]{24}}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("viewId") String id, @PathParam("groupId") String groupId) {
      viewFacade.removeGroupPermission(id, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

   @POST
   @Path("{viewId:[0-9a-fA-F]{24}}/favorite")
   public Response addFavoriteView(@PathParam("viewId") final String viewId) {
      viewFacade.addFavoriteView(viewId);

      return Response.ok().build();
   }

   @DELETE
   @Path("{viewId:[0-9a-fA-F]{24}}/favorite")
   public Response removeFavoriteView(@PathParam("viewId") final String viewId) {
      viewFacade.removeFavoriteView(viewId);

      return Response.ok().build();
   }

   @GET
   @Path("defaultConfigs/all")
   public List<DefaultViewConfig> getDefaultConfigs() {
      return this.viewFacade.getDefaultConfigs();
   }

   @PUT
   @Path("defaultConfigs/config")
   public DefaultViewConfig setDefaultConfig(DefaultViewConfig config) {
      return this.viewFacade.updateDefaultConfig(config);
   }
}
