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

import io.lumeer.engine.api.exception.UserAlreadyExistsException;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

/**
 * Service which manipulates with project related data.
 */
@Path("/{organization}/projects")
@RequestScoped
public class ProjectService implements Serializable {

   private static final long serialVersionUID = -1878165624068611361L;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @PathParam("organization")
   private String organizationId;

   @PostConstruct
   public void init() {
      organizationFacade.setOrganizationId(organizationId);
   }

   /**
    * @return Map of projects ids and names.
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, String> getProjects() {
      return projectFacade.readProjectsMap(organizationId);
   }

   /**
    * @param projectId
    *       Project id.
    * @return Name of given project.
    */
   @GET
   @Path("/{projectId}/name")
   @Produces(MediaType.APPLICATION_JSON)
   public String getProjectName(final @PathParam("projectId") String projectId) {
      if (projectId == null) {
         throw new IllegalArgumentException();
      }
      return projectFacade.readProjectName(projectId);
   }

   /**
    * @param projectId
    *       Project id.
    * @param projectName
    *       Project name.
    */
   @POST
   @Path("/{projectId}")
   public void createProject(final @PathParam("projectId") String projectId, final String projectName) {
      if (projectId == null || projectName == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.createProject(projectId, projectName);
   }

   /**
    * @param projectId
    *       Project id.
    * @param newProjectName
    *       Project name.
    */
   @PUT
   @Path("/{projectId}/name/{newProjectName}")
   public void renameProject(final @PathParam("projectId") String projectId, final @PathParam("newProjectName") String newProjectName) {
      if (projectId == null || newProjectName == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.renameProject(projectId, newProjectName);
   }

   /**
    * Updates project id.
    *
    * @param projectId
    *       Project id.
    * @param newProjectId
    *       New project id.
    */
   @PUT
   @Path("/{projectId}/id/{newProjectId}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateProjectId(final @PathParam("projectId") String projectId, final @PathParam("newProjectId") String newProjectId) {
      if (projectId == null || newProjectId == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.updateProjectId(projectId, newProjectId);
   }

   /**
    * @param projectId
    *       Project id.
    */
   @DELETE
   @Path("/{projectId}")
   public void dropProject(final @PathParam("projectId") String projectId) {
      if (projectId == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.dropProject(projectId);
   }

   /**
    * @param projectId
    *       Project id.
    * @param attributeName
    *       Name of metadata attribute.
    * @return Value of metadata attribute.
    */
   @GET
   @Path("/{projectId}/meta/{attributeName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String readProjectMetadata(final @PathParam("projectId") String projectId, final @PathParam("attributeName") String attributeName) {
      if (projectId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return projectFacade.readProjectMetadata(projectId, attributeName);
   }

   /**
    * Adds or updates metadata attribute.
    *
    * @param projectId
    *       Project id.
    * @param attributeName
    *       Name of metadata attribute.
    * @param value
    *       Value of metadata attribute.
    */
   @PUT
   @Path("/{projectId}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateProjectMetadata(final @PathParam("projectId") String projectId, final @PathParam("attributeName") String attributeName, final String value) {
      if (projectId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.updateProjectMetadata(projectId, attributeName, value);
   }

   /**
    * @param projectId
    *       Project id.
    * @param attributeName
    *       Name of metadata attribute.
    */
   @DELETE
   @Path("/{projectId}/meta/{attributeName}")
   public void dropProjectMetadata(final @PathParam("projectId") String projectId, final @PathParam("attributeName") String attributeName) {
      if (projectId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.dropProjectMetadata(projectId, attributeName);
   }

   /* ************************************* Users & Roles **************************************** */

   /**
    * @param userName
    *       User name.
    * @return List of projects with the user.
    */
   @GET
   @Path("/users/{userName}")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readUserProjects(final @PathParam("userName") String userName) {
      if (userName == null) {
         throw new IllegalArgumentException();
      }
      return projectFacade.readUserProjects(userName);
   }

   /**
    * @param projectId
    *       Project id.
    * @param userName
    *       User name.
    * @param userRoles
    *       List of user roles.
    * @throws UserAlreadyExistsException
    *       When user with given name already exists.
    */
   @POST
   @Path("/{projectId}/users/{userName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addUserToProject(final @PathParam("projectId") String projectId, final @PathParam("userName") String userName, final List<String> userRoles) throws UserAlreadyExistsException {
      if (projectId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      if (userRoles == null) {
         projectFacade.addUserToProject(projectId, userName);
      } else {
         projectFacade.addUserToProject(projectId, userName, userRoles);
      }
   }

   /**
    * @param projectId
    *       Project id.
    * @param userName
    *       User name.
    */
   @DELETE
   @Path("/{projectId}/users/{userName}")
   public void removeUserFromProject(final @PathParam("projectId") String projectId, final @PathParam("userName") String userName) {
      if (projectId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.dropUserFromProject(projectId, userName);
   }

   /**
    * @param projectId
    *       Project id.
    * @param userName
    *       User name.
    * @return List of roles for given user.
    */
   @GET
   @Path("/{projectId}/users/{userName}/roles")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readUserRoles(final @PathParam("projectId") String projectId, final @PathParam("userName") String userName) {
      if (projectId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      return projectFacade.readUserRoles(projectId, userName);
   }

   /**
    * @param projectId
    *       Project id.
    * @return Map of users and their roles.
    */
   @GET
   @Path("/{projectId}/usersroles")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, List<String>> readUsersRoles(final @PathParam("projectId") String projectId) {
      if (projectId == null) {
         throw new IllegalArgumentException();
      }
      return projectFacade.readUsersRoles(projectId);
   }

   /**
    * Checks if user has specific role.
    *
    * @param projectId
    *       Project id.
    * @param userName
    *       User name.
    * @param role
    *       Role to check.
    * @return True if user has specified role, false otherwise.
    */
   @GET
   @Path("/{projectId}/users/{userName}/roles/{role}")
   @Consumes(MediaType.APPLICATION_JSON)
   public boolean hasRole(final @PathParam("projectId") String projectId, final @PathParam("userName") String userName, final @PathParam("role") String role) {
      if (projectId == null || userName == null || role == null) {
         throw new IllegalArgumentException();
      }
      return projectFacade.hasUserRole(projectId, userName, role);
   }

   /**
    * @param projectId
    *       Project id.
    * @param userName
    *       User name.
    * @param role
    *       Role to add.
    */
   @POST
   @Path("/{projectId}/users/{userName}/roles/{role}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addRoleToUser(final @PathParam("projectId") String projectId, final @PathParam("userName") String userName, final @PathParam("role") String role) {
      if (projectId == null || userName == null || role == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.addRolesToUser(projectId, userName, Collections.singletonList(role));
   }

   /**
    * @param projectId
    *       Project id.
    * @param userName
    *       User name.
    * @param role
    *       Role to remove.
    */
   @DELETE
   @Path("/{projectId}/users/{userName}/roles/{role}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void removeRoleFromUser(final @PathParam("projectId") String projectId, final @PathParam("userName") String userName, final @PathParam("role") String role) {
      if (projectId == null || userName == null || role == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.removeRolesFromUser(projectId, userName, Collections.singletonList(role));
   }

   /**
    * @param projectId
    *       Project id.
    * @return List of default roles.
    */
   @GET
   @Path("/{projectId}/roles")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readDefaultRoles(final @PathParam("projectId") String projectId) {
      if (projectId == null) {
         throw new IllegalArgumentException();
      }
      return projectFacade.readDefaultRoles(projectId);
   }

   /**
    * @param projectId
    *       Project id.
    * @param userRoles
    *       List of default roles.
    */
   @PUT
   @Path("/{projectId}/roles")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setDefaultRoles(final @PathParam("projectId") String projectId, final List<String> userRoles) {
      if (projectId == null) {
         throw new IllegalArgumentException();
      }
      projectFacade.setDefaultRoles(projectId, userRoles);
   }

}
