/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.rest;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Service which manipulates with project related data.
 */
@Path("/organizations/{organization}/projects")
@RequestScoped
public class ProjectService implements Serializable {

   private static final long serialVersionUID = -1878165624068611361L;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private SecurityFacade securityFacade;

   @PathParam("organization")
   private String organizationCode;

   @PostConstruct
   public void init() {
      organizationFacade.setOrganizationCode(organizationCode);
   }

   /**
    * @return List of projects.
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<Project> getProjects() {
      return projectFacade.readProjects(organizationCode)
                          .stream()
                          .filter(o -> securityFacade.hasProjectRole(o.getCode(), LumeerConst.Security.ROLE_READ))
                          .collect(Collectors.toList());
   }

   /**
    * @param projectCode
    *       Project code;
    * @return Project data;
    */
   @GET
   @Path("/{projectCode}")
   @Produces(MediaType.APPLICATION_JSON)
   public Project readProject(final @PathParam("projectCode") String projectCode) throws UnauthorizedAccessException {
      if (projectCode == null) {
         throw new BadRequestException();
      }
      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return projectFacade.readProject(projectCode);
   }

   /**
    * @param project
    *       Project data.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @POST
   @Path("/")
   @Consumes(MediaType.APPLICATION_JSON)
   public void createProject(final Project project) throws UnauthorizedAccessException {
      if (project == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasOrganizationRole(organizationCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      projectFacade.createProject(project);
   }

   /**
    * @param projectCode
    *       Code identifying project.
    * @param project
    *       Project data.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @PUT
   @Path("/{projectCode}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateProject(final @PathParam("projectCode") String projectCode, final Project project) throws UnauthorizedAccessException {
      if (projectCode == null || project == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      projectFacade.updateProject(projectCode, project);
   }

   /**
    * @param projectCode
    *       Project code.
    * @return Name of given project.
    */
   @GET
   @Path("/{projectCode}/name")
   @Produces(MediaType.APPLICATION_JSON)
   public String getProjectName(final @PathParam("projectCode") String projectCode) throws UnauthorizedAccessException {
      if (projectCode == null) {
         throw new BadRequestException();
      }
      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }
      return projectFacade.readProjectName(projectCode);
   }

   /**
    * @param projectCode
    *       Project code.
    * @param newProjectName
    *       Project name.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @PUT
   @Path("/{projectCode}/name/{newProjectName}")
   public void renameProject(final @PathParam("projectCode") String projectCode, final @PathParam("newProjectName") String newProjectName) throws UnauthorizedAccessException {
      if (projectCode == null || newProjectName == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      projectFacade.renameProject(projectCode, newProjectName);
   }

   /**
    * Updates project code.
    *
    * @param projectCode
    *       Project code.
    * @param newProjectCode
    *       New project code.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @PUT
   @Path("/{projectCode}/code/{newProjectCode}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateProjectCode(final @PathParam("projectCode") String projectCode, final @PathParam("newProjectCode") String newProjectCode) throws UnauthorizedAccessException {
      if (projectCode == null || newProjectCode == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      projectFacade.updateProjectCode(projectCode, newProjectCode);
   }

   /**
    * @param projectCode
    *       Project code.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @DELETE
   @Path("/{projectCode}")
   public void dropProject(final @PathParam("projectCode") String projectCode) throws UnauthorizedAccessException {
      if (projectCode == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      projectFacade.dropProject(projectCode);
   }

   /**
    * @param projectCode
    *       Project code.
    * @param attributeName
    *       Name of metadata attribute.
    * @return Value of metadata attribute.
    */
   @GET
   @Path("/{projectCode}/meta/{attributeName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String readProjectMetadata(final @PathParam("projectCode") String projectCode, final @PathParam("attributeName") String attributeName) throws UnauthorizedAccessException {
      if (projectCode == null || attributeName == null) {
         throw new BadRequestException();
      }
      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }
      return projectFacade.readProjectMetadata(projectCode, attributeName);
   }

   /**
    * Adds or updates metadata attribute.
    *
    * @param projectCode
    *       Project code.
    * @param attributeName
    *       Name of metadata attribute.
    * @param value
    *       Value of metadata attribute.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @PUT
   @Path("/{projectCode}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateProjectMetadata(final @PathParam("projectCode") String projectCode, final @PathParam("attributeName") String attributeName, final String value) throws UnauthorizedAccessException {
      if (projectCode == null || attributeName == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      projectFacade.updateProjectMetadata(projectCode, attributeName, value);
   }

   /**
    * @param projectCode
    *       Project code.
    * @param attributeName
    *       Name of metadata attribute.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @DELETE
   @Path("/{projectCode}/meta/{attributeName}")
   public void dropProjectMetadata(final @PathParam("projectCode") String projectCode, final @PathParam("attributeName") String attributeName) throws UnauthorizedAccessException {
      if (projectCode == null || attributeName == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      projectFacade.dropProjectMetadata(projectCode, attributeName);
   }

}
