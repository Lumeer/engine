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
import io.lumeer.api.model.DocumentLinks;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.core.facade.AuditFacade;
import io.lumeer.core.facade.LinkInstanceFacade;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.remote.rest.annotation.PATCH;
import io.lumeer.remote.rest.request.LinkInstanceDuplicationRequest;

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
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
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/link-instances")
public class LinkInstanceService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @Inject
   private AuditFacade auditFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   public LinkInstance createLinkInstance(LinkInstance linkInstance) {
      return linkInstanceFacade.createLinkInstance(linkInstance);
   }

   @POST
   @Path("duplicate")
   public List<LinkInstance> duplicateLinkInstances(final LinkInstanceDuplicationRequest duplicationRequest) {
      return linkInstanceFacade.duplicateLinkInstances(
            duplicationRequest.getOriginalMasterDocument(),
            duplicationRequest.getNewMasterDocument(),
            duplicationRequest.getLinkInstanceIds(),
            duplicationRequest.getDocumentMap());
   }

   @POST
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/documentLinks")
   public List<LinkInstance> setLinkInkInstances(@PathParam("linkTypeId") String linkTypeId, final DocumentLinks links) {
      return linkInstanceFacade.setDocumentLinks(linkTypeId, links);
   }

   @GET
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/{linkInstanceId:[0-9a-fA-F]{24}}")
   public LinkInstance updateLinkInstanceData(@PathParam("linkTypeId") String linkTypeId, @PathParam("linkInstanceId") String linkInstanceId) {
      final LinkInstance link = linkInstanceFacade.getLinkInstance(linkTypeId, linkInstanceId);
      return linkInstanceFacade.mapLinkInstanceData(link);
   }

   @PUT
   @Path("{linkInstanceId:[0-9a-fA-F]{24}}")
   public LinkInstance updateLinkInstance(@PathParam("linkInstanceId") String id, LinkInstance linkInstance) {
      final LinkInstance link = linkInstanceFacade.updateLinkInstance(id, linkInstance);
      return linkInstanceFacade.mapLinkInstanceData(link);
   }

   @PUT
   @Path("{linkInstanceId:[0-9a-fA-F]{24}}/data")
   public LinkInstance updateLinkInstanceData(@PathParam("linkInstanceId") String id, DataDocument data) {
      final LinkInstance link = linkInstanceFacade.updateLinkInstanceData(id, data);
      return linkInstanceFacade.mapLinkInstanceData(link);
   }

   @PATCH
   @Path("{linkInstanceId:[0-9a-fA-F]{24}}/data")
   public LinkInstance patchLinkInstanceData(@PathParam("linkInstanceId") String id, DataDocument data) {
      final LinkInstance link = linkInstanceFacade.patchLinkInstanceData(id, data);
      return linkInstanceFacade.mapLinkInstanceData(link);
   }

   @GET
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/{linkInstanceId:[0-9a-fA-F]{24}}/audit")
   public List<AuditRecord> getAuditLogs(@PathParam("linkTypeId") String linkTypeId, @PathParam("linkInstanceId") String linkInstanceId) {
      return auditFacade.getAuditRecordsForLink(linkTypeId, linkInstanceId);
   }

   @DELETE
   @Path("{linkInstanceId:[0-9a-fA-F]{24}}")
   public Response deleteLinkInstance(@PathParam("linkInstanceId") String id) {
      linkInstanceFacade.deleteLinkInstance(id);

      return Response.ok().link(getParentUri(id), "parent").build();
   }

   @POST
   @Path("{linkTypeId:[0-9a-fA-F]{24}}/{linkInstanceId:[0-9a-fA-F]{24}}/rule/{attributeId}")
   public void runRule(@PathParam("linkTypeId") final String linkTypeId, @PathParam("linkInstanceId") final String linkInstanceId, @PathParam("attributeId") final String attributeId, @QueryParam("actionName") final String actionName) {
      linkInstanceFacade.runRule(linkTypeId, linkInstanceId, attributeId, actionName);
   }

}
