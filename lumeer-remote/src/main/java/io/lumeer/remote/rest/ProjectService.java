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

import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.facade.ProjectFacade;

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
   public Project createProject(Project project) {
      Project storedProject = projectFacade.createProject(project);
      storedProject.setCollectionsCount(0);
      return storedProject;
   }

   @PUT
   @Path("{projectCode}")
   public Project updateProject(@PathParam("projectCode") String projectCode, Project project) {
      Project storedProject = projectFacade.updateProject(projectCode, project);
      storedProject.setCollectionsCount(projectFacade.getCollectionsCount(storedProject));
      return storedProject;
   }

   @DELETE
   @Path("{projectCode}")
   public Response deleteProject(@PathParam("projectCode") String projectCode) {
      projectFacade.deleteProject(projectCode);

      return Response.ok().link(getParentUri(projectCode), "parent").build();
   }

   @GET
   @Path("{projectCode}")
   public Project getProject(@PathParam("projectCode") String projectCode) {
      Project project = projectFacade.getProject(projectCode);
      project.setCollectionsCount(projectFacade.getCollectionsCount(project));
      return project;
   }

   @GET
   public List<Project> getProjects() {
      List<Project> projects = projectFacade.getProjects();
      projects.forEach(project -> project.setCollectionsCount(projectFacade.getCollectionsCount(project)));
      return projects;
   }

   @GET
   @Path("info/codes")
   public Set<String> getProjectsCodes() {
      return projectFacade.getProjectsCodes();
   }

   @GET
   @Path("{projectCode}/permissions")
   public Permissions getProjectPermissions(@PathParam("projectCode") String projectCode) {
      return projectFacade.getProjectPermissions(projectCode);
   }

   @PUT
   @Path("{projectCode}/permissions/users")
   public Set<Permission> updateUserPermission(@PathParam("projectCode") String projectCode, Permission... userPermission) {
      return projectFacade.updateUserPermissions(projectCode, userPermission);
   }

   @DELETE
   @Path("{projectCode}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("projectCode") String projectCode, @PathParam("userId") String userId) {
      projectFacade.removeUserPermission(projectCode, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{projectCode}/permissions/groups")
   public Set<Permission> updateGroupPermission(@PathParam("projectCode") String projectCode, Permission... groupPermission) {
      return projectFacade.updateGroupPermissions(projectCode, groupPermission);
   }

   @DELETE
   @Path("{projectCode}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("projectCode") String projectCode, @PathParam("groupId") String groupId) {
      projectFacade.removeGroupPermission(projectCode, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

}
