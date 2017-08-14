/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.remote.rest;

import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonView;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.View;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.facade.ViewFacade;

import java.net.URI;
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

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationCode, projectCode);
   }

   @POST
   public Response createView(JsonView view) {
      View storedView = viewFacade.createView(view);

      URI resourceUri = getResourceUri(storedView);
      return Response.created(resourceUri).build();
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
      return new JsonView(view);
   }

   @GET
   public List<JsonView> getAllViews() {
      return viewFacade.getAllViews().stream()
                       .map(JsonView::new)
                       .collect(Collectors.toList());
   }

   @GET
   @Path("{viewCode}/permissions")
   public JsonPermissions getViewPermissions(@PathParam("viewCode") String code) {
      Permissions permissions = viewFacade.getViewPermissions(code);
      return new JsonPermissions(permissions);
   }

   @PUT
   @Path("{viewCode}/permissions/users")
   public Set<JsonPermission> updateUserPermission(@PathParam("viewCode") String code, JsonPermission userPermission) {
      return viewFacade.updateUserPermissions(code, userPermission).stream()
                       .map(JsonPermission::new)
                       .collect(Collectors.toSet());
   }

   @DELETE
   @Path("{viewCode}/permissions/users/{user}")
   public Response removeUserPermission(@PathParam("viewCode") String code, @PathParam("user") String user) {
      viewFacade.removeUserPermission(code, user);

      return Response.ok().link(getParentUri("users", user), "parent").build();
   }

   @PUT
   @Path("{viewCode}/permissions/groups")
   public Set<JsonPermission> updateGroupPermission(@PathParam("viewCode") String code, JsonPermission groupPermission) {
      return viewFacade.updateGroupPermissions(code, groupPermission).stream()
                       .map(JsonPermission::new)
                       .collect(Collectors.toSet());
   }

   @DELETE
   @Path("{viewCode}/permissions/groups/{group}")
   public Response removeGroupPermission(@PathParam("viewCode") String code, @PathParam("group") String group) {
      viewFacade.removeGroupPermission(code, group);

      return Response.ok().link(getParentUri("groups", group), "parent").build();
   }
}
