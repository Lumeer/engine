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

import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ProjectContent;
import io.lumeer.api.model.ProjectDescription;
import io.lumeer.api.model.SampleDataType;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.facade.AuditFacade;
import io.lumeer.core.facade.CopyFacade;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.ProjectFacade;
import io.lumeer.core.facade.TemplateFacade;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects")
public class ProjectService extends AbstractService {

   public static final String DELETE_SAMPLE_DATA_CONFIRMATION = "PERMANENTLY DELETE";

   @PathParam("organizationId")
   private String organizationId;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private CopyFacade copyFacade;

   @Inject
   private TemplateFacade templateFacade;

   @Inject
   private AuditFacade auditFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setOrganizationId(organizationId);
   }

   @POST
   @HealthCheck
   public Project createProject(Project project) {
      Project storedProject = projectFacade.createProject(project);
      storedProject.setCollectionsCount(0);
      return storedProject;
   }

   @POST
   @Path("{projectId:[0-9a-fA-F]{24}}/templates/{templateId}")
   public Response installTemplate(@PathParam("projectId") final String projectId, @PathParam("templateId") final String templateId) {
      workspaceKeeper.setProjectId(projectId);

      if (workspaceKeeper.getOrganization().isPresent()) {
         final Project project = projectFacade.getProjectById(projectId);
         copyFacade.deepCopyTemplate(project, templateId);
         return Response.ok().build();
      }

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
   }

   @POST
   @Path("{projectId:[0-9a-fA-F]{24}}/copy")
   public Response copyProject(@PathParam("projectId") final String projectId, @QueryParam("organizationId") final String copyOrganizationId, @QueryParam("projectId") final String copyProjectId) {
      workspaceKeeper.setProjectId(projectId);

      if (workspaceKeeper.getOrganization().isPresent() && copyOrganizationId != null && copyProjectId != null) {
         final Project project = projectFacade.getProjectById(projectId);
         copyFacade.deepCopyProject(project, copyOrganizationId, copyProjectId);
         return Response.ok().build();
      }

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
   }

   @POST
   @Path("{projectId:[0-9a-fA-F]{24}}/sample/{type}")
   public Response copySampleData(@PathParam("projectId") final String projectId, @PathParam("type") final SampleDataType sampleDataType) {
      workspaceKeeper.setProjectId(projectId);

      if (workspaceKeeper.getOrganization().isPresent() && sampleDataType != null) {
         final Project project = projectFacade.getProjectById(projectId);
         copyFacade.deepCopySampleData(project, sampleDataType);
         return Response.ok().build();
      }

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
   }

   @PUT
   @Path("{projectId:[0-9a-fA-F]{24}}")
   @HealthCheck
   public Project updateProject(@PathParam("projectId") String projectId, Project project) {
      Project storedProject = projectFacade.updateProject(projectId, project);
      storedProject.setCollectionsCount(projectFacade.getCollectionsCount(storedProject));
      return storedProject;
   }

   @DELETE
   @Path("{projectId:[0-9a-fA-F]{24}}")
   public Response deleteProject(@PathParam("projectId") String projectId) {
      projectFacade.deleteProject(projectId);

      return Response.ok().link(getParentUri(projectId), "parent").build();
   }

   @GET
   @Path("{projectId:[0-9a-fA-F]{24}}")
   public Project getProject(@PathParam("projectId") String projectId) {
      Project project = projectFacade.getProjectById(projectId);
      project.setCollectionsCount(projectFacade.getCollectionsCount(project));
      return project;
   }

   @GET
   @Path("code/{projectCode:[a-zA-Z0-9_]{2,6}}")
   public Project getProjectByCode(@PathParam("projectCode") String projectCode) {
      Project project = projectFacade.getProjectByCode(projectCode);
      project.setCollectionsCount(projectFacade.getCollectionsCount(project));
      return project;
   }

   @GET
   public List<Project> getProjects() {
      List<Project> projects = projectFacade.getProjects();
      projects.forEach(project -> project.setCollectionsCount(projectFacade.getCollectionsCount(project)));
      return projects;
   }

   @DELETE
   @Path("{projectId:[0-9a-fA-F]{24}}/sample-data")
   public Response deleteTemplateProjectData(@PathParam("projectId") String projectId, @QueryParam("confirmation") final String confirmation) {
      if (DELETE_SAMPLE_DATA_CONFIRMATION.equals(confirmation)) {
         workspaceKeeper.setProjectId(projectId);

         projectFacade.emptyTemplateData(projectId);

         return Response.ok().build();
      }

      return Response.notModified().build();
   }

   @GET
   @Path("info/codes")
   public Set<String> getProjectsCodes() {
      return projectFacade.getProjectsCodes();
   }

   @GET
   @Path("{projectId:[0-9a-fA-F]{24}}/permissions")
   public Permissions getProjectPermissions(@PathParam("projectId") String projectId) {
      return projectFacade.getProjectPermissions(projectId);
   }

   @PUT
   @Path("{projectId:[0-9a-fA-F]{24}}/permissions/users")
   public Set<Permission> updateUserPermission(@PathParam("projectId") String projectId, Set<Permission> userPermission) {
      return projectFacade.updateUserPermissions(projectId, userPermission);
   }

   @DELETE
   @Path("{projectId:[0-9a-fA-F]{24}}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("projectId") String projectId, @PathParam("userId") String userId) {
      projectFacade.removeUserPermission(projectId, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{projectId:[0-9a-fA-F]{24}}/permissions/groups")
   public Set<Permission> updateGroupPermission(@PathParam("projectId") String projectId, Set<Permission> groupPermission) {
      return projectFacade.updateGroupPermissions(projectId, groupPermission);
   }

   @DELETE
   @Path("{projectId:[0-9a-fA-F]{24}}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("projectId") String projectId, @PathParam("groupId") String groupId) {
      projectFacade.removeGroupPermission(projectId, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

   @GET
   @Path("{projectId:[0-9a-fA-F]{24}}/raw")
   public ProjectContent getRawProjectContent(@PathParam("projectId") String projectId) {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
      return projectFacade.exportProjectContent(projectId);
   }

   @POST
   @Path("{projectId:[0-9a-fA-F]{24}}/raw")
   public Response addProjectContent(@PathParam("projectId") String projectId, final ProjectContent projectContent) {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);

      if (workspaceKeeper.getOrganization().isPresent()) {
         final Project project = projectFacade.getProjectById(projectId);
         copyFacade.installProjectContent(project, organizationId, projectContent);
         return Response.ok().build();
      }

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
   }

   @GET
   @Path("{projectId:[0-9a-fA-F]{24}}/limits")
   public List<Organization> canBeCopiedToOrganization(@PathParam("projectId") String projectId) {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);

      final ProjectDescription desc = projectFacade.getProjectDescription();

      if (desc == null) {
         return List.of();
      }

      if (templateFacade.getAllTemplateOrganizationIds().contains(organizationId)) {
         return organizationFacade.getOrganizationsCapableForProject(null);
      }

      return organizationFacade.getOrganizationsCapableForProject(desc);
   }

   @GET
   @Path("{projectId:[0-9a-fA-F]{24}}/audit")
   public List<AuditRecord> getAuditLogs(@PathParam("projectId") String projectId) {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);

      return auditFacade.getAuditRecordsForProject();
   }
}
