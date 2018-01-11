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

import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.facade.ProjectFacade;

import java.net.URI;
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
@Path("organizations/{organizationCode}/projects")
public class ProjectService extends AbstractService {

   @PathParam("organizationCode")
   private String organizationCode;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setOrganization(organizationCode);
   }

   @POST
   public Response createProject(JsonProject project) {
      Project storedProject = projectFacade.createProject(project);

      URI resourceUri = getResourceUri(storedProject.getId());
      return Response.created(resourceUri).build();
   }

   @PUT
   @Path("{projectCode}")
   public Response updateProject(@PathParam("projectCode") String projectCode, JsonProject project) {
      Project storedProject = projectFacade.updateProject(projectCode, project);

      return Response.ok(JsonProject.convert(storedProject)).build();
   }

   @DELETE
   @Path("{projectCode}")
   public Response deleteProject(@PathParam("projectCode") String projectCode) {
      projectFacade.deleteProject(projectCode);

      return Response.ok().link(getParentUri(projectCode), "parent").build();
   }

   @GET
   @Path("{projectCode}")
   public JsonProject getProject(@PathParam("projectCode") String projectCode) {
      Project project = projectFacade.getProject(projectCode);
      return JsonProject.convert(project);
   }

   @GET
   public List<JsonProject> getProjects() {
      List<Project> projects = projectFacade.getProjects();
      return JsonProject.convert(projects);
   }

   @GET
   @Path("{projectCode}/permissions")
   public JsonPermissions getProjectPermissions(@PathParam("projectCode") String projectCode) {
      Permissions permissions = projectFacade.getProjectPermissions(projectCode);
      return JsonPermissions.convert(permissions);
   }

   @PUT
   @Path("{projectCode}/permissions/users")
   public Set<JsonPermission> updateUserPermission(@PathParam("projectCode") String projectCode, JsonPermission userPermission) {
      Set<Permission> storedUserPermissions = projectFacade.updateUserPermissions(projectCode, userPermission);
      return JsonPermission.convert(storedUserPermissions);
   }

   @DELETE
   @Path("{projectCode}/permissions/users/{user}")
   public Response removeUserPermission(@PathParam("projectCode") String projectCode, @PathParam("user") String user) {
      projectFacade.removeUserPermission(projectCode, user);

      return Response.ok().link(getParentUri("users", user), "parent").build();
   }

   @PUT
   @Path("{projectCode}/permissions/groups")
   public Set<JsonPermission> updateGroupPermission(@PathParam("projectCode") String projectCode, JsonPermission groupPermission) {
      Set<Permission> storedGroupPermissions = projectFacade.updateGroupPermissions(projectCode, groupPermission);
      return JsonPermission.convert(storedGroupPermissions);
   }

   @DELETE
   @Path("{projectCode}/permissions/groups/{group}")
   public Response removeGroupPermission(@PathParam("projectCode") String projectCode, @PathParam("group") String group) {
      projectFacade.removeGroupPermission(projectCode, group);

      return Response.ok().link(getParentUri("groups", group), "parent").build();
   }

}
