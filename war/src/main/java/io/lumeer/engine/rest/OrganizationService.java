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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.exception.UserAlreadyExistsException;
import io.lumeer.engine.controller.OrganizationFacade;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
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
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@Path("/organizations")
@ApplicationScoped
public class OrganizationService implements Serializable {

   private static final long serialVersionUID = 3125094059637285633L;

   @Inject
   private OrganizationFacade organizationFacade;

   /**
    * @return map of organizations' ids and names
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, String> getOrganizations() {
      return organizationFacade.readOrganizationsMap();
   }

   /**
    * @param organizationId organization id
    * @return name of given organization
    */
   @GET
   @Path("/{organizationId}/name")
   @Produces(MediaType.APPLICATION_JSON)
   public String getOrganizationName(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationName(organizationId);
   }

   /**
    * @param organizationId organization id
    * @param organizationName organization name
    */
   @POST
   @Path("/{organizationId}")
   public void createOrganization(final @PathParam("organizationId") String organizationId, final String organizationName) {
      if (organizationId == null || organizationName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.createOrganization(organizationId, organizationName);
   }

   /**
    * @param organizationId organization id
    * @param newOrganizationName organization name
    */
   @PUT
   @Path("/{organizationId}/name/{newOrganizationName}")
   public void renameOrganization(final @PathParam("organizationId") String organizationId, final @PathParam("newOrganizationName") String newOrganizationName) {
      if (organizationId == null || newOrganizationName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.renameOrganization(organizationId, newOrganizationName);
   }

   /**
    * @param organizationId organization id
    */
   @DELETE
   @Path("/{organizationId}")
   public void dropOrganization(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.dropOrganization(organizationId);
   }

   /**
    * @param organizationId organization id
    * @param attributeName name of metadata attribute
    * @return value of metadata attribute
    */
   @GET
   @Path("/{organizationId}/meta/{attributeName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String readOrganizationMetadata(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationMetadata(organizationId, attributeName);
   }

   /**
    * Adds or updates metadata attribute.
    * @param organizationId organization id
    * @param attributeName name of metadata attribute
    * @param value value of metadata attribute
    */
   @PUT
   @Path("/{organizationId}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateOrganizationMetadata(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName, final String value) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      DataDocument metaDocument = new DataDocument(attributeName, value);
      organizationFacade.updateOrganizationMetadata(organizationId, metaDocument);
   }

   /**
    * @param organizationId organization id
    * @param attributeName name of metadata attribute
    */
   @DELETE
   @Path("/{organizationId}/meta/{attributeName}")
   public void dropOrganizationMetadata(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.dropOrganizationMetadata(organizationId, attributeName);
   }

   /**
    * @param organizationId organization id
    * @return DataDocument with additional info
    */
   @GET
   @Path("/{organizationId}/data/")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument readOrganizationAdditionalInfo(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationInfoData(organizationId);
   }

   /**
    * @param organizationId organization id
    * @param attributeName name of attribute from additional info
    * @return value of the attribute
    */
   @GET
   @Path("/{organizationId}/data/{attributeName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String readOrganizationAdditionalInfo(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationInfoData(organizationId, attributeName);
   }

   /**
    * Creates or updates entry in additional info.
    * @param organizationId organization id
    * @param attributeName name of the attribute
    * @param value value of the attribute
    */
   @PUT
   @Path("/{organizationId}/data/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateOrganizationAdditionalInfo(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName, final String value) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      DataDocument infoDataDocument = new DataDocument(attributeName, value);
      organizationFacade.updateOrganizationInfoData(organizationId, infoDataDocument);
   }

   /**
    * Drops atttribute from additional info.
    * @param organizationId organization id
    * @param attributeName name of the attribute
    */
   @DELETE
   @Path("/{organizationId}/data/{attributeName}")
   public void dropOrganizationAdditionalInfo(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.dropOrganizationInfoDataAttribute(organizationId, attributeName);
   }

   /**
    * Drops all additional info.
    * @param organizationId organization id
    */
   @DELETE
   @Path("/{organizationId}/data")
   public void resetOrganizationInfoData(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.resetOrganizationInfoData(organizationId);
   }

   /* ************************************* Users & Roles **************************************** */

   /**
    * @param userName user name
    * @return list of organizations with the user
    */
   @GET
   @Path("/users/{userName}")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readUserOrganizations(final @PathParam("userName") String userName) {
      if (userName == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readUserOrganizations(userName);
   }

   /**
    * @param organizationId organization id
    * @return map of users in the organization with their roles
    */
   @GET
   @Path("/{organizationId}/users")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, List<String>> readOrganizationUsers(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationUsers(organizationId);
   }

   /**
    * @param organizationId organization id
    * @param userName user name
    * @param userRoles list of user roles
    * @throws UserAlreadyExistsException when user with given name already exists
    */
   @POST
   @Path("/{organizationId}/users/{userName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addUserToOrganization(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName, final List<String> userRoles) throws UserAlreadyExistsException {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      if (userRoles == null) {
         organizationFacade.addUserToOrganization(organizationId, userName);
      } else {
         organizationFacade.addUserToOrganization(organizationId, userName, userRoles);
      }
   }

   /**
    * @param organizationId organization id
    * @param userName user name
    */
   @DELETE
   @Path("/{organizationId}/users/{userName}")
   public void removeUserFromOrganization(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName) {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.removeUserFromOrganization(organizationId, userName);
   }

   /**
    * @param organizationId organization id
    * @param userName user name
    * @return list of roles for given user
    */
   @GET
   @Path("/{organizationId}/users/{userName}/roles")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readUserRoles(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName) {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readUserRoles(organizationId, userName);
   }

   /**
    * @param organizationId organization id
    * @param userName user name
    * @param role role to add
    */
   @POST
   @Path("/{organizationId}/users/{userName}/roles/{role}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addRoleToUser(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName, final @PathParam("role") String role) {
      if (organizationId == null || userName == null || role == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.addRolesToUser(organizationId, userName, Arrays.asList(role));
   }

   /**
    * @param organizationId organization id
    * @param userName user name
    * @param role role to remove
    */
   @DELETE
   @Path("/{organizationId}/users/{userName}/roles/{role}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void removeRoleFromUser(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName, final @PathParam("role") String role) {
      if (organizationId == null || userName == null || role == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.removeRolesFromUser(organizationId, userName, Arrays.asList(role));
   }

   /**
    * @param organizationId organization id
    * @return list of default roles
    */
   @GET
   @Path("/{organizationId}/roles")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readDefaultRoles(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readDefaultRoles(organizationId);
   }

   /**
    * @param organizationId organization id
    * @param userRoles list of default roles
    */
   @PUT
   @Path("/{organizationId}/roles")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setDefaultRoles(final @PathParam("organizationId") String organizationId, final List<String> userRoles) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.setDefaultRoles(organizationId, userRoles);
   }

}
