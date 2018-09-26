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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonView;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.View;
import io.lumeer.core.facade.ViewFacade;

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
   public Response createView(JsonView view) {
      View storedView = viewFacade.createView(view);

      JsonView jsonView = new JsonView(storedView);
      return Response.ok(jsonView).build();
   }

   @PUT
   @Path("{viewCode}")
   public Response updateView(@PathParam("viewCode") String code, JsonView view) {
      View storedView = viewFacade.updateView(code, view);

      JsonView jsonView = new JsonView(storedView);
      return Response.ok(jsonView).build();
   }

   @DELETE
   @Path("{viewCode}")
   public Response deleteView(@PathParam("viewCode") String code) {
      viewFacade.deleteView(code);

      return Response.ok().link(getParentUri(code), "parent").build();
   }

   @GET
   @Path("{viewCode}")
   public JsonView getView(@PathParam("viewCode") String code) {
      View view = viewFacade.getViewByCode(code);
      return JsonView.convert(view);
   }

   @GET
   public List<JsonView> getViews(@QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
      Pagination pagination = new Pagination(page, pageSize);

      List<View> views = viewFacade.getViews(pagination);
      return JsonView.convert(views);
   }

   @GET
   @Path("{viewCode}/permissions")
   public JsonPermissions getViewPermissions(@PathParam("viewCode") String code) {
      Permissions permissions = viewFacade.getViewPermissions(code);
      return JsonPermissions.convert(permissions);
   }

   @PUT
   @Path("{viewCode}/permissions/users")
   public Set<JsonPermission> updateUserPermission(@PathParam("viewCode") String code, JsonPermission... userPermission) {
      Set<Permission> storedUserPermissions = viewFacade.updateUserPermissions(code, userPermission);
      return JsonPermission.convert(storedUserPermissions);
   }

   @DELETE
   @Path("{viewCode}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("viewCode") String code, @PathParam("userId") String userId) {
      viewFacade.removeUserPermission(code, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{viewCode}/permissions/groups")
   public Set<JsonPermission> updateGroupPermission(@PathParam("viewCode") String code, JsonPermission... groupPermission) {
      Set<Permission> storedGroupPermissions = viewFacade.updateGroupPermissions(code, groupPermission);
      return JsonPermission.convert(storedGroupPermissions);
   }

   @DELETE
   @Path("{viewCode}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("viewCode") String code, @PathParam("groupId") String groupId) {
      viewFacade.removeGroupPermission(code, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

   @GET
   @Path("all/collections")
   public List<JsonCollection> getViewsCollections(@QueryParam("viewCodes") String viewCodes) {
      System.out.println("@@@@@@@@@@@@########### " + viewCodes);

      return viewFacade.getViewsCollections().stream()
            .map(JsonCollection::convert)
            .collect(Collectors.toList());
   }
}
