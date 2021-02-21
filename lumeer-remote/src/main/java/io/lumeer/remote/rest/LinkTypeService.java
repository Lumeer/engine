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
import io.lumeer.api.model.LinkType;
import io.lumeer.core.facade.LinkTypeFacade;
import io.lumeer.core.facade.ViewFacade;
import io.lumeer.remote.rest.annotation.HealthCheck;

import java.util.ArrayList;
import java.util.List;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
   private ViewFacade viewFacade;

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
   public List<LinkType> getLinkTypes(@QueryParam("fromViews") Boolean includeViewLinkTypes) {
      final List<LinkType> linkTypes = linkTypeFacade.getLinkTypes();

      if (includeViewLinkTypes != null && includeViewLinkTypes && !isManager()) {
         linkTypes.addAll(viewFacade.getViewsLinkTypes());
      }

      return linkTypes;
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
}
