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
import io.lumeer.api.model.Document;
import io.lumeer.core.facade.AuditFacade;
import io.lumeer.core.facade.DocumentFacade;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.remote.rest.annotation.HealthCheck;
import io.lumeer.remote.rest.annotation.PATCH;

import java.util.List;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/collections/{collectionId:[0-9a-fA-F]{24}}/documents")
public class DocumentService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @PathParam("collectionId")
   private String collectionId;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private AuditFacade auditFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   @HealthCheck
   public Document createDocument(Document document) {
      return documentFacade.createDocument(collectionId, document);
   }

   @POST
   @Path("duplicate")
   public List<Document> duplicateDocuments(List<String> documentIds) {
      return documentFacade.duplicateDocuments(collectionId, documentIds);
   }

   @PUT
   @Path("{documentId:[0-9a-fA-F]{24}}/data")
   public Document updateDocumentData(@PathParam("documentId") String documentId, DataDocument data) {
      return documentFacade.updateDocumentData(collectionId, documentId, data);
   }

   @PATCH
   @Path("{documentId:[0-9a-fA-F]{24}}/data")
   public Document patchDocumentData(@PathParam("documentId") String documentId, DataDocument data) {
      return documentFacade.patchDocumentData(collectionId, documentId, data);
   }

   @PUT
   @Path("{documentId:[0-9a-fA-F]{24}}/meta")
   public Document updateDocumentMetaData(@PathParam("documentId") final String documentId, final DataDocument metaData) {
      return documentFacade.updateDocumentMetaData(collectionId, documentId, metaData);
   }

   @PATCH
   @Path("{documentId:[0-9a-fA-F]{24}}/meta")
   public Document patchDocumentMetaData(@PathParam("documentId") final String documentId, final DataDocument metaData) {
      return documentFacade.patchDocumentMetaData(collectionId, documentId, metaData);
   }

   @GET
   @Path("{documentId:[0-9a-fA-F]{24}}/audit")
   public List<AuditRecord> getAuditLogs(@PathParam("documentId") String documentId) {
      return auditFacade.getAuditRecordsForDocument(collectionId, documentId);
   }

   @POST
   @Path("{documentId:[0-9a-fA-F]{24}}/audit/{auditLogId:[0-9a-fA-F]{24}}/revert")
   public Document revertAuditLog(@PathParam("documentId") String documentId, @PathParam("auditLogId") String auditLogId) {
      return auditFacade.revertLastDocumentAuditOperation(collectionId, documentId, auditLogId);
   }

   @DELETE
   @Path("{documentId:[0-9a-fA-F]{24}}")
   public Response deleteDocument(@PathParam("documentId") String documentId) {
      documentFacade.deleteDocument(collectionId, documentId);

      return Response.ok().link(getParentUri(documentId), "parent").build();
   }

   @GET
   @Path("{documentId:[0-9a-fA-F]{24}}")
   public Document getDocument(@PathParam("documentId") String documentId) {
      return documentFacade.getDocument(collectionId, documentId);
   }

   @POST
   @Path("{documentId:[0-9a-fA-F]{24}}/favorite")
   public Response addFavoriteDocument(@PathParam("documentId") String documentId) {
      documentFacade.addFavoriteDocument(collectionId, documentId);

      return Response.ok().build();
   }

   @DELETE
   @Path("{documentId:[0-9a-fA-F]{24}}/favorite")
   public Response removeFavoriteDocument(@PathParam("documentId") String documentId) {
      documentFacade.removeFavoriteDocument(collectionId, documentId);

      return Response.ok().build();
   }

   @POST
   @Path("{documentId:[0-9a-fA-F]{24}}/rule/{attributeId}")
   public void runRule(@PathParam("documentId") final String documentId, @PathParam("attributeId") final String attributeId, @QueryParam("actionName") final String actionName) {
      documentFacade.runRule(collectionId, documentId, attributeId, actionName);
   }

}
