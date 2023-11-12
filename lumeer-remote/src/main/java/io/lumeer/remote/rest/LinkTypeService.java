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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Rule;
import io.lumeer.core.facade.AuditFacade;
import io.lumeer.core.facade.LinkTypeFacade;
import io.lumeer.remote.rest.annotation.HealthCheck;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/link-types")
public class LinkTypeService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private LinkTypeFacade linkTypeFacade;

   @Inject
   private AuditFacade auditFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   @HealthCheck
   public LinkType createLinkType(LinkType linkType) {
      return linkTypeFacade.createLinkType(linkType);
   }

   @PUT
   @Path("{linkTypeId:[0-9a-fA-F]{24}}")
   @HealthCheck
   public LinkType updateLinkType(@PathParam("linkTypeId") String id, LinkType linkType) {
      return linkTypeFacade.updateLinkType(id, linkType);
   }

   @DELETE
   @Path("{linkTypeId:[0-9a-fA-F]{24}}")
   public Response deleteLinkType(@PathParam("linkTypeId") String id) {
      linkTypeFacade.deleteLinkType(id);

      return Response.ok().link(getParentUri(id), "parent").build();
   }

   @GET
   @Path("{linkTypeId:[0-9a-fA-F]{24}}")
   public LinkType getLinkType(@PathParam("linkTypeId") String id) {
      return linkTypeFacade.getLinkType(id);
   }

   @GET
   public List<LinkType> getLinkTypes() {
      return linkTypeFacade.getAllLinkTypes();
   }

   @PUT
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/rule/{ruleId}")
   public LinkType upsertRule(@PathParam("linkTypeId") String linkTypeId, @PathParam("ruleId") String ruleId, Rule rule) {
      return linkTypeFacade.upsertRule(linkTypeId, ruleId, rule);
   }

   @POST
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/attributes")
   @HealthCheck
   public List<Attribute> createLinkTypeAttributes(@PathParam("linkTypeId") String linkTypeId, List<Attribute> attributes) {
      return new ArrayList<>(linkTypeFacade.createLinkTypeAttributes(linkTypeId, attributes));
   }

   @PUT
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/attributes/{attributeId}")
   @HealthCheck
   public Attribute updateLinkTypeAttribute(@PathParam("linkTypeId") String linkTypeId, @PathParam("attributeId") String attributeId, Attribute attribute) {
      return linkTypeFacade.updateLinkTypeAttribute(linkTypeId, attributeId, attribute);
   }

   @DELETE
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/attributes/{attributeId}")
   public Response deleteCollectionAttribute(@PathParam("linkTypeId") String linkTypeId, @PathParam("attributeId") String attributeId) {
      if (attributeId == null) {
         throw new BadRequestException("attributeId");
      }

      linkTypeFacade.deleteLinkTypeAttribute(linkTypeId, attributeId);

      return Response.ok().link(getParentUri(attributeId), "parent").build();
   }

   @GET
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/audit")
   public List<AuditRecord> getAuditLogs(@PathParam("linkTypeId") String linkTypeId) {
      return auditFacade.getAuditRecordsForLinkType(linkTypeId);
   }
}
