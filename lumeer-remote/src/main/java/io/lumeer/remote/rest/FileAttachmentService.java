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

import io.lumeer.api.model.Document;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.core.facade.DocumentFacade;
import io.lumeer.core.facade.FileAttachmentFacade;
import io.lumeer.engine.api.data.DataDocument;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationId}/projects/{projectId}/collections/{collectionId}/files")
public class FileAttachmentService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @PathParam("collectionId")
   private String collectionId;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationId, projectId);
   }

   @PUT
   public FileAttachment createFileAttachment(final FileAttachment fileAttachment) {
      return fileAttachmentFacade.createFileAttachment(fileAttachment);
   }

   @DELETE
   public Response removeFileAttachment(final FileAttachment fileAttachment) {
      fileAttachmentFacade.removeFileAttachment(fileAttachment);

      return Response.ok().build();
   }

   @POST
   public FileAttachment renameFileAttachment(final FileAttachment fileAttachment) {
      return fileAttachmentFacade.renameFileAttachment(fileAttachment);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("dir/{documentId}/{attributeId}")
   public List<FileAttachment> getFileAttachments(@PathParam("documentId") final String documentId, @PathParam("attributeId") final String attributeId) {
      return fileAttachmentFacade.getAllFileAttachments(collectionId, documentId, attributeId);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("dir/{documentId}")
   public List<FileAttachment> getFileAttachments(@PathParam("documentId") final String documentId) {
      return fileAttachmentFacade.getAllFileAttachments(collectionId, documentId);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("dir")
   public List<FileAttachment> getFileAttachments() {
      return fileAttachmentFacade.getAllFileAttachments(collectionId);
   }

   // Gets the real file attachments according to S3 content. The results might not have all corresponding DB entries, or some DB entries might not have their corresponding S3 keys.
   @GET
   @Path("list/{attributeId}/{documentId}")
   public List<FileAttachment> listFileAttachments(@PathParam("attributeId") final String attributeId, @PathParam("documentId") final String documentId) {
      return fileAttachmentFacade.listFileAttachments(collectionId, documentId, attributeId);
   }

}
