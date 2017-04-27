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
import io.lumeer.engine.controller.OrganizationFacade;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
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
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@Path("/organizations")
@RequestScoped
public class OrganizationService implements Serializable {

   private static final long serialVersionUID = 3125094059637285633L;

   @Inject
   private OrganizationFacade organizationFacade;

   // TODO: RequestScoped?
   // TODO: Serializable?
   // TODO: dva druhy metod s roznym pathom? Ako na to?

   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, String> getOrganizations() {
      return organizationFacade.readOrganizationsMap();
   }

   @GET
   @Path("/{organizationName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String getOrganizationId(final @PathParam("organizationName") String organizationName) {
      if (organizationName == null) {
         throw new IllegalArgumentException();
      }
      // return organizationFacade.readOrganizationId(organizationName);
      return null;
   }

   @GET
   @Path("/{organizationId}")
   @Produces(MediaType.APPLICATION_JSON)
   public String getOrganizationName(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationName(organizationId);
   }

   @POST
   @Path("/{organizationId}/{organizationName}")
   public void createOrganization(final @PathParam("organizationId") String organizationId, final @PathParam("organizationName") String organizationName) {
      if (organizationId == null || organizationName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.createOrganization(organizationId, organizationName);
   }

   @PUT
   @Path("/{organizationId}/{newOrganizationName}")
   public void renameOrganization(final @PathParam("organizationId") String organizationId, final @PathParam("newOrganizationName") String newOrganizationName) {
      if (organizationId == null || newOrganizationName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.renameOrganization(organizationId, newOrganizationName);
   }

   @DELETE
   @Path("/{organizationId}")
   public void dropOrganization(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.dropOrganization(organizationId);
   }

   @GET
   @Path("/{organizationId}/meta/{attributeName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String readOrganizationMetadata(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationMetadata(organizationId, attributeName);
   }

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

   @DELETE
   @Path("/{organizationId}/meta/{attributeName}")
   public void dropOrganizationMetadata(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.dropOrganizationMetadata(organizationId, attributeName);
   }

   @GET
   @Path("/{organizationId}/data/")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument readOrganizationAdditionalInfo(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationInfoData(organizationId);
   }

   @GET
   @Path("/{organizationId}/data/{attributeName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String readOrganizationAdditionalInfo(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationInfoData(organizationId, attributeName);
   }

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

   @DELETE
   @Path("/{organizationId}/data/{attributeName}")
   public void dropOrganizationAdditionalInfo(final @PathParam("organizationId") String organizationId, final @PathParam("attributeName") String attributeName) {
      if (organizationId == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.dropOrganizationInfoDataAttribute(organizationId, attributeName);
   }

   @POST
   @Path("/{organizationId}/data")
   public void resetOrganizationInfoData(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.resetOrganizationInfoData(organizationId);
   }

   /* ************************************* Users & Roles **************************************** */

   /* ************* "/users/{userName}" **************** */
   @GET
   @Path("/{userName}")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, String> readUserOrganizations(final @PathParam("userName") String userName) {
      if (userName == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readUserOrganizations(userName);
   }

   /* ************* "/{organizationId}/users" ******************* */
   @GET
   @Path("/{organizationId}/users")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, List<String>> readOrganizationUsers(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readOrganizationUsers(organizationId);
   }

   /* ************* "/{organizationId}/users/{userName}" ****************** */
   @POST
   @Path("/{organizationId}/users/{userName}")  // TODO: aku adresu?
   @Consumes(MediaType.APPLICATION_JSON)
   public void addUserToOrganization(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName) {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      // organizationFacade.addUserToOrganization(organizationId, userName);
   }

   @POST
   @Path("/{organizationId}/users/{userName}") // TODO: aku adresu?
   @Consumes(MediaType.APPLICATION_JSON)
   public void addUserWithRolesToOrganization(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName, final List<String> userRoles) {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      // organizationFacade.addUserToOrganization(organizationId, userName, userRoles);
   }

   @DELETE
   @Path("/{organizationId}/users/{userName}")
   public void removeUserFromOrganization(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName) {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.removeUserFromOrganization(organizationId, userName);
   }

   /* ************* "/{organizationId}/users/{userName}/roles" ******************* */
   @GET
   @Path("/{organizationId}/users/{userName}/roles")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readUserRoles(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName) {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readUserRoles(organizationId, userName);
   }

   @POST
   @Path("/{organizationId}/users/{userName}/roles")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addRolesToUser(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName, final List<String> userRoles) {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.addRolesToUser(organizationId, userName, userRoles);
   }

   @DELETE
   @Path("/{organizationId}/users/{userName}/roles")
   @Consumes(MediaType.APPLICATION_JSON)
   public void removeRolesFromUser(final @PathParam("organizationId") String organizationId, final @PathParam("userName") String userName, final List<String> userRoles) {
      if (organizationId == null || userName == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.removeRolesFromUser(organizationId, userName, userRoles);
   }

   /* ************* "/{organizationId}/roles" *************** */
   @GET
   @Path("/{organizationId}/roles")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readDefaultRoles(final @PathParam("organizationId") String organizationId) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      return organizationFacade.readDefaultRoles(organizationId);
   }

   @POST
   @Path("/{organizationId}/roles")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setDefaultRoles(final @PathParam("organizationId") String organizationId, final List<String> userRoles) {
      if (organizationId == null) {
         throw new IllegalArgumentException();
      }
      organizationFacade.setDefaultRoles(organizationId, userRoles);
   }

}
