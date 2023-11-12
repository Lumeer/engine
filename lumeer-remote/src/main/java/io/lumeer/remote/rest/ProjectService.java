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
import io.lumeer.api.model.TemplateData;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.facade.AuditFacade;
import io.lumeer.core.facade.OrganizationFacade;
import io.lumeer.core.facade.ProjectFacade;
import io.lumeer.core.facade.TemplateFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.Utils;
import io.lumeer.remote.rest.annotation.HealthCheck;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
   private TemplateFacade templateFacade;

   @Inject
   private AuditFacade auditFacade;

  @Inject
  private RequestDataKeeper requestDataKeeper;

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

      if (workspaceKeeper.getOrganization().isPresent() && workspaceKeeper.getProject().isPresent()) {
         var organizationId = templateFacade.getTemplateOrganizationId(requestDataKeeper.getUserLanguage());
         if (this.isProduction()) {
            TemplateData templateData = templateFacade.getTemplateData(organizationId, templateId);
            templateFacade.installTemplate(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), organizationId, templateData);
         } else {
            TemplateData templateData = getTemplateDataFromProduction(organizationId, templateId);
            templateFacade.installTemplate(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), organizationId, templateData);
         }
         return Response.ok().build();
      }

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
   }

   @POST
   @Path("{projectId:[0-9a-fA-F]{24}}/copy")
   public Response copyProject(@PathParam("projectId") final String projectId, @QueryParam("organizationId") final String copyOrganizationId, @QueryParam("projectId") final String copyProjectId) {
      workspaceKeeper.setProjectId(projectId);

      if (workspaceKeeper.getOrganization().isPresent() && workspaceKeeper.getProject().isPresent() && copyOrganizationId != null && copyProjectId != null) {
         if (this.isProduction()) {
            TemplateData templateData = templateFacade.getTemplateData(copyOrganizationId, copyProjectId);
            templateFacade.installTemplate(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), copyOrganizationId, templateData);
         } else {
            TemplateData templateData = getTemplateDataFromProduction(copyOrganizationId, copyProjectId);
            templateFacade.installTemplate(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), copyOrganizationId, templateData);
         }
         return Response.ok().build();
      }

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
   }

   @POST
   @Path("{projectId:[0-9a-fA-F]{24}}/sample/{type}")
   public Response copySampleData(@PathParam("projectId") final String projectId, @PathParam("type") final SampleDataType sampleDataType) {
      workspaceKeeper.setProjectId(projectId);

      if (workspaceKeeper.getOrganization().isPresent() && workspaceKeeper.getProject().isPresent() && sampleDataType != null) {
         var copyOrganizationId = templateFacade.getSampleDataOrganizationId(requestDataKeeper.getUserLanguage());
         var copyProjectCode = sampleDataType.toString();
         if (this.isProduction()) {
            TemplateData templateData = templateFacade.getTemplateDataByCode(copyOrganizationId, copyProjectCode);
            templateFacade.installTemplate(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), copyOrganizationId, templateData);
         } else {
            TemplateData templateData = getTemplateDataByCodeFromProduction(copyOrganizationId, copyProjectCode);
            templateFacade.installTemplate(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), copyOrganizationId, templateData);
         }
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

   @POST
   @Path("code/{projectCode:[a-zA-Z0-9_]{2,6}}/check")
   public Boolean checkCode(@PathParam("projectCode") String projectCode) {
      return projectFacade.checkCode(projectCode);
   }

   @GET
   @Path("code/{projectCode:[a-zA-Z0-9_]{2,6}}/template")
   public TemplateData getRawProjectContentByCode(@PathParam("projectCode") String projectCode) {
     Project project = projectFacade.getPublicProjectByCode(projectCode);
     return templateFacade.getTemplateData(organizationId, project.getId());
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
  @Path("{projectId:[0-9a-fA-F]{24}}/template")
  public TemplateData getTemplateData(@PathParam("projectId") String projectId) {
    return templateFacade.getTemplateData(organizationId, projectId);
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

      if (workspaceKeeper.getOrganization().isPresent() && workspaceKeeper.getProject().isPresent()) {
         templateFacade.installProjectContent(workspaceKeeper.getOrganization().get(), workspaceKeeper.getProject().get(), projectContent);
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

   private TemplateData getTemplateDataFromProduction(final String organizationId, final String projectId) {
     try {
       return callProductionApi("templates/data/" + organizationId + "/" + projectId, new TypeReference<TemplateData>() {});
     } catch (Exception e) {
       throw new InternalServerErrorException(e);
     }
  }

  private TemplateData getTemplateDataByCodeFromProduction(final String organizationId, final String projectCode) {
    try {
      return callProductionApi("templates/data/code/" + organizationId + "/" + projectCode, new TypeReference<TemplateData>() {});
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

   private <T> T callProductionApi(String apiPath, TypeReference<T> responseType) throws IOException, InterruptedException {
      final String apiUrl = getConfiguration(DefaultConfigurationProducer.PRODUCTION_REST_URL) + apiPath;
      final HttpClient client = HttpClient.newHttpClient();
      final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .GET()
            .build();

      final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      final String responseBody = response.body();

      final ObjectMapper objectMapper = Utils.createObjectMapper();
      return objectMapper.readValue(responseBody, responseType);
   }
}
