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
package io.lumeer.engine.rest;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.dto.Role;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;

import java.io.Serializable;
import java.util.List;
import java.util.zip.DataFormatException;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/roles/organizations/{organization}")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SecurityService implements Serializable {

   @PathParam("organization")
   private String organizationCode;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() {
      if (organizationCode == null) {
         throw new BadRequestException();
      }

      organizationFacade.setOrganizationCode(organizationCode);
   }

   /**
    * Add a role for users and groups for an organization.
    *
    * @param role
    *       the role to be added
    * @param users
    *       the users to be added the role for
    * @param groups
    *       the groups to be added the role for
    * @throws UnauthorizedAccessException
    *       when user doesn't have permission to manage rights
    */
   @PUT
   @Path("/roles/{role}")
   public void addOrganizationUsersGroupsRole(
         final @PathParam("role") String role,
         final @QueryParam("users") List<String> users,
         final @QueryParam("groups") List<String> groups) throws UnauthorizedAccessException {
      if (role == null || !isRoleCorrect(LumeerConst.Security.ORGANIZATION_RESOURCE, role)) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasOrganizationRole(organizationCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      if (users != null && !users.isEmpty()) {
         securityFacade.addOrganizationUsersRole(organizationCode, users, role);
      }
      if (groups != null && !groups.isEmpty()) {
         securityFacade.addOrganizationGroupsRole(organizationCode, groups, role);
      }
   }

   /**
    * Get roles of an organization.
    *
    * @return the role object with all roles for the organization
    * @throws DataFormatException
    *       if obtained data cannot be transformed to a role object
    */
   @GET
   @Path("/roles")
   public List<Role> getOrganizationRoles() throws DataFormatException {
      return securityFacade.getOrganizationRoles(organizationCode);
   }

   /**
    * Delete a role for users and groups for an organization.
    *
    * @param role
    *       the role to be deleted
    * @param users
    *       the users to be deleted the role for
    * @param groups
    *       the groups to be deleted the role for
    * @throws UnauthorizedAccessException
    *       when user doesn't have permission to manage rights
    */
   @DELETE
   @Path("/roles/{role}")
   public void removeOrganizationUsersGroupsRole(
         final @PathParam("role") String role,
         final @QueryParam("users") List<String> users,
         final @QueryParam("groups") List<String> groups) throws UnauthorizedAccessException {
      if (role == null || !isRoleCorrect(LumeerConst.Security.ORGANIZATION_RESOURCE, role)) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasOrganizationRole(organizationCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      if (users != null && !users.isEmpty()) {
         securityFacade.removeOrganizationUsersRole(organizationCode, users, role);
      }
      if (groups != null && !groups.isEmpty()) {
         securityFacade.removeOrganizationGroupsRole(organizationCode, groups, role);
      }
   }

   /**
    * Add a role for users and groups for a project.
    *
    * @param projectCode
    *       the project code to be added the role for
    * @param role
    *       the role to be added
    * @param users
    *       the users to be added the role for
    * @param groups
    *       the groups to be added the role for
    * @throws UnauthorizedAccessException
    *       when user doesn't have permission to manage rights
    */
   @PUT
   @Path("/projects/{project}/roles/{role}")
   public void addProjectUsersGroupsRole(
         final @PathParam("project") String projectCode,
         final @PathParam("role") String role,
         final @QueryParam("users") List<String> users,
         final @QueryParam("groups") List<String> groups) throws UnauthorizedAccessException {
      if (projectCode == null || role == null ||
            !isRoleCorrect(LumeerConst.Security.PROJECT_RESOURCE, role)) {
         throw new BadRequestException();
      }

      projectFacade.setCurrentProjectCode(projectCode);
      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }


      if (users != null && !users.isEmpty()) {
         securityFacade.addProjectUsersRole(projectCode, users, role);
      }
      if (groups != null && !groups.isEmpty()) {
         securityFacade.addProjectGroupsRole(projectCode, groups, role);
      }
   }

   /**
    * Get roles of a project.
    *
    * @param projectCode
    *       the project code to be added the role for
    * @return the role object with all roles for the project
    * @throws DataFormatException
    *       if obtained data cannot be transformed to a role object
    */
   @GET
   @Path("/projects/{project}/roles")
   public List<Role> getProjectRoles(final @PathParam("project") String projectCode) throws DataFormatException {
      if (projectCode == null) {
         throw new BadRequestException();
      }

      projectFacade.setCurrentProjectCode(projectCode);
      return securityFacade.getProjectRoles(projectCode);
   }

   /**
    * Delete a role for users and groups for a project.
    *
    * @param projectCode
    *       the project code to be deleted the role for
    * @param role
    *       the role to be deleted
    * @param users
    *       the users to be deleted the role for
    * @param groups
    *       the groups to be deleted the role for
    * @throws UnauthorizedAccessException
    *       when user doesn't have permission to manage rights
    */
   @DELETE
   @Path("/projects/{project}/roles/{role}")
   public void removeProjectUsersGroupsRole(
         final @PathParam("project") String projectCode,
         final @PathParam("role") String role,
         final @QueryParam("users") List<String> users,
         final @QueryParam("groups") List<String> groups) throws UnauthorizedAccessException {
      if (projectCode == null || role == null ||
            !isRoleCorrect(LumeerConst.Security.PROJECT_RESOURCE, role)) {
         throw new BadRequestException();
      }

      projectFacade.setCurrentProjectCode(projectCode);
      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      if (users != null && !users.isEmpty()) {
         securityFacade.removeProjectUsersRole(projectCode, users, role);
      }
      if (groups != null && !groups.isEmpty()) {
         securityFacade.removeProjectGroupsRole(projectCode, groups, role);
      }
   }

   /**
    * Add a role for users and groups for a view.
    *
    * @param projectCode
    *       the project code to be added the role for
    * @param viewId
    *       the view id to be added the role for
    * @param role
    *       the role to be added
    * @param users
    *       the users to be added the role for
    * @param groups
    *       the groups to be added the role for
    * @throws UnauthorizedAccessException
    *       when user doesn't have permission to manage rights
    */
   @PUT
   @Path("/projects/{project}/views/{view}/roles/{role}")
   public void addViewUsersGroupsRole(
         final @PathParam("project") String projectCode,
         final @PathParam("view") Integer viewId,
         final @PathParam("role") String role,
         final @QueryParam("users") List<String> users,
         final @QueryParam("groups") List<String> groups) throws UnauthorizedAccessException {
      if (projectCode == null || viewId == null || role == null ||
            !isRoleCorrect(LumeerConst.Security.VIEW_RESOURCE, role)) {
         throw new BadRequestException();
      }

      projectFacade.setCurrentProjectCode(projectCode);
      if (!securityFacade.hasViewRole(projectCode, viewId, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      if (users != null && !users.isEmpty()) {
         securityFacade.addViewUsersRole(projectCode, viewId, users, role);
      }
      if (groups != null && !groups.isEmpty()) {
         securityFacade.addViewGroupsRole(projectCode, viewId, groups, role);
      }
   }

   /**
    * Get roles of a view.
    *
    * @param projectCode
    *       the project code to be obtained the roles for
    * @param viewId
    *       the view id to be obtained the role for
    * @return the role object with all roles for the view
    * @throws DataFormatException
    *       if obtained data cannot be transformed to a role object
    * @throws UnauthorizedAccessException
    *       when user doesn't have permission to read rights
    */
   @GET
   @Path("/projects/{project}/views/{view}/roles")
   public List<Role> getViewRoles(
         final @PathParam("project") String projectCode,
         final @PathParam("view") Integer viewId) throws DataFormatException, UnauthorizedAccessException {
      if (projectCode == null || viewId == null) {
         throw new BadRequestException();
      }

      projectFacade.setCurrentProjectCode(projectCode);
      if (!securityFacade.hasViewRole(projectCode, viewId, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return securityFacade.getViewRoles(projectCode, viewId);
   }

   /**
    * Delete a role for users and groups for a view.
    *
    * @param projectCode
    *       the project code to be deleted the role for
    * @param viewId
    *       the view id to be deleted the role for
    * @param role
    *       the role to be deleted
    * @param users
    *       the users to be deleted the role for
    * @param groups
    *       the groups to be deleted the role for
    * @throws UnauthorizedAccessException
    *       when user doesn't have permission to manage rights
    */
   @DELETE
   @Path("/projects/{project}/views/{view}/roles/{role}")
   public void removeViewUsersGroupsRole(
         final @PathParam("project") String projectCode,
         final @PathParam("view") Integer viewId,
         final @PathParam("role") String role,
         final @QueryParam("users") List<String> users,
         final @QueryParam("groups") List<String> groups) throws UnauthorizedAccessException {
      if (projectCode == null || viewId == null || role == null ||
            !isRoleCorrect(LumeerConst.Security.VIEW_RESOURCE, role)) {
         throw new BadRequestException();
      }

      projectFacade.setCurrentProjectCode(projectCode);
      if (!securityFacade.hasViewRole(projectCode, viewId, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      if (users != null && !users.isEmpty()) {
         securityFacade.removeViewUsersRole(projectCode, viewId, users, role);
      }
      if (groups != null && !groups.isEmpty()) {
         securityFacade.removeViewGroupsRole(projectCode, viewId, groups, role);
      }

   }

   private boolean isRoleCorrect(String resource, String roleName) {
      return LumeerConst.Security.RESOURCE_ROLES.get(resource).contains(roleName);
   }

}