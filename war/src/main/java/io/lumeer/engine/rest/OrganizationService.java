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
import io.lumeer.engine.rest.dao.Organization;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
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
    * @return list of organizations
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<Organization> getOrganizations() {
      return organizationFacade.readOrganizations().stream().map(Organization::new).collect(Collectors.toList());
   }

   /**
    * @param organizationCode
    *       organization code
    * @return name of given organization
    */
   @GET
   @Path("/{organizationCode}/name")
   @Produces(MediaType.APPLICATION_JSON)
   public String getOrganizationName(final @PathParam("organizationCode") String organizationCode) {
      if (organizationCode == null) {
         throw new BadRequestException();
      }
      return organizationFacade.readOrganizationName(organizationCode);
   }

   /**
    * @param organizationCode
    *       organization code
    * @param organizationName
    *       organization name
    */
   @POST
   @Path("/{organizationCode}")
   public void createOrganization(final @PathParam("organizationCode") String organizationCode, final String organizationName) {
      if (organizationCode == null || organizationName == null) {
         throw new BadRequestException();
      }
      organizationFacade.createOrganization(organizationCode, organizationName);
   }

   /**
    * @param organizationCode
    *       organization code
    * @param newOrganizationName
    *       organization name
    */
   @PUT
   @Path("/{organizationCode}/name/{newOrganizationName}")
   public void renameOrganization(final @PathParam("organizationCode") String organizationCode, final @PathParam("newOrganizationName") String newOrganizationName) {
      if (organizationCode == null || newOrganizationName == null) {
         throw new BadRequestException();
      }
      organizationFacade.renameOrganization(organizationCode, newOrganizationName);
   }

   /**
    * @param organizationCode
    *       organization code
    * @param newCode
    *       new organization code
    */
   @PUT
   @Path("/{organizationCode}/code/{newCode}")
   public void updateOrganizationCode(final @PathParam("organizationCode") String organizationCode, final @PathParam("newCode") String newCode) {
      if (organizationCode == null || newCode == null) {
         throw new BadRequestException();
      }
      organizationFacade.updateOrganizationCode(organizationCode, newCode);
   }

   /**
    * @param organizationCode
    *       organization code
    */
   @DELETE
   @Path("/{organizationCode}")
   public void dropOrganization(final @PathParam("organizationCode") String organizationCode) {
      if (organizationCode == null) {
         throw new BadRequestException();
      }
      organizationFacade.dropOrganization(organizationCode);
   }

   /**
    * @param organizationCode
    *       organization code
    * @param attributeName
    *       name of metadata attribute
    * @return value of metadata attribute
    */
   @GET
   @Path("/{organizationCode}/meta/{attributeName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String readOrganizationMetadata(final @PathParam("organizationCode") String organizationCode, final @PathParam("attributeName") String attributeName) {
      if (organizationCode == null || attributeName == null) {
         throw new BadRequestException();
      }
      return organizationFacade.readOrganizationMetadata(organizationCode, attributeName);
   }

   /**
    * Adds or updates metadata attribute.
    *
    * @param organizationCode
    *       organization code
    * @param attributeName
    *       name of metadata attribute
    * @param value
    *       value of metadata attribute
    */
   @PUT
   @Path("/{organizationCode}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateOrganizationMetadata(final @PathParam("organizationCode") String organizationCode, final @PathParam("attributeName") String attributeName, final String value) {
      if (organizationCode == null || attributeName == null) {
         throw new BadRequestException();
      }
      DataDocument metaDocument = new DataDocument(attributeName, value);
      organizationFacade.updateOrganizationMetadata(organizationCode, metaDocument);
   }

   /**
    * @param organizationCode
    *       organization code
    * @param attributeName
    *       name of metadata attribute
    */
   @DELETE
   @Path("/{organizationCode}/meta/{attributeName}")
   public void dropOrganizationMetadata(final @PathParam("organizationCode") String organizationCode, final @PathParam("attributeName") String attributeName) {
      if (organizationCode == null || attributeName == null) {
         throw new BadRequestException();
      }
      organizationFacade.dropOrganizationMetadata(organizationCode, attributeName);
   }

   /**
    * @param organizationCode
    *       organization code
    * @return DataDocument with additional info
    */
   @GET
   @Path("/{organizationCode}/data/")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument readOrganizationAdditionalInfo(final @PathParam("organizationCode") String organizationCode) {
      if (organizationCode == null) {
         throw new BadRequestException();
      }
      return organizationFacade.readOrganizationInfoData(organizationCode);
   }

   /**
    * @param organizationCode
    *       organization code
    * @param attributeName
    *       name of attribute from additional info
    * @return value of the attribute
    */
   @GET
   @Path("/{organizationCode}/data/{attributeName}")
   @Produces(MediaType.APPLICATION_JSON)
   public String readOrganizationAdditionalInfo(final @PathParam("organizationCode") String organizationCode, final @PathParam("attributeName") String attributeName) {
      if (organizationCode == null || attributeName == null) {
         throw new BadRequestException();
      }
      return organizationFacade.readOrganizationInfoData(organizationCode, attributeName);
   }

   /**
    * Creates or updates entry in additional info.
    *
    * @param organizationCode
    *       organization code
    * @param attributeName
    *       name of the attribute
    * @param value
    *       value of the attribute
    */
   @PUT
   @Path("/{organizationCode}/data/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateOrganizationAdditionalInfo(final @PathParam("organizationCode") String organizationCode, final @PathParam("attributeName") String attributeName, final String value) {
      if (organizationCode == null || attributeName == null) {
         throw new BadRequestException();
      }
      DataDocument infoDataDocument = new DataDocument(attributeName, value);
      organizationFacade.updateOrganizationInfoData(organizationCode, infoDataDocument);
   }

   /**
    * Drops attribute from additional info.
    *
    * @param organizationCode
    *       organization code
    * @param attributeName
    *       name of the attribute
    */
   @DELETE
   @Path("/{organizationCode}/data/{attributeName}")
   public void dropOrganizationAdditionalInfo(final @PathParam("organizationCode") String organizationCode, final @PathParam("attributeName") String attributeName) {
      if (organizationCode == null || attributeName == null) {
         throw new BadRequestException();
      }
      organizationFacade.dropOrganizationInfoDataAttribute(organizationCode, attributeName);
   }

   /**
    * Drops all additional info.
    *
    * @param organizationCode
    *       organization code
    */
   @DELETE
   @Path("/{organizationCode}/data")
   public void resetOrganizationInfoData(final @PathParam("organizationCode") String organizationCode) {
      if (organizationCode == null) {
         throw new BadRequestException();
      }
      organizationFacade.resetOrganizationInfoData(organizationCode);
   }

}
