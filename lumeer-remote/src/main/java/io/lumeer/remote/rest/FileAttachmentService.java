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

import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.IdsBody;
import io.lumeer.core.facade.FileAttachmentFacade;
import io.lumeer.remote.rest.annotation.HealthCheck;

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
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/files")
public class FileAttachmentService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   @HealthCheck
   public List<FileAttachment> createFileAttachments(final List<FileAttachment> fileAttachment) {
      return fileAttachmentFacade.createFileAttachments(fileAttachment);
   }

   @DELETE
   public Response removeFileAttachment(final FileAttachment fileAttachment) {
      fileAttachmentFacade.removeFileAttachment(fileAttachment);

      return Response.ok().build();
   }

   @DELETE
   @Path("multiple")
   public Response removeFileAttachment(final IdsBody idsBody) {
      fileAttachmentFacade.removeFileAttachments(idsBody.getIds());

      return Response.ok().build();
   }

   @DELETE
   @Path("{attachmentId}")
   public Response removeFileAttachment(@PathParam("attachmentId") final String fileAttachmentId) {
      fileAttachmentFacade.removeFileAttachment(fileAttachmentId);

      return Response.ok().build();
   }

   @PUT
   @HealthCheck
   public FileAttachment renameFileAttachment(final FileAttachment fileAttachment) {
      return fileAttachmentFacade.renameFileAttachment(fileAttachment);
   }

   @GET
   @Path("{attachmentId}")
   public FileAttachment getFileAttachment(@PathParam("attachmentId") final String fileAttachmentId, @QueryParam("write") final Boolean write) {
      return fileAttachmentFacade.getFileAttachment(fileAttachmentId, write != null ? write : false);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("collection/{collectionId:[0-9a-fA-F]{24}}/{documentId:[0-9a-fA-F]{24}}/{attributeId}")
   public List<FileAttachment> getFileAttachmentsCollection(@PathParam("collectionId") final String collectionId, @PathParam("documentId") final String documentId, @PathParam("attributeId") final String attributeId) {
      return fileAttachmentFacade.getAllFileAttachments(collectionId, documentId, attributeId, FileAttachment.AttachmentType.DOCUMENT);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("link/{linkTypeId:[0-9a-fA-F]{24}}/{linkInstanceId:[0-9a-fA-F]{24}}/{attributeId}")
   public List<FileAttachment> getFileAttachmentsLink(@PathParam("linkTypeId") final String linkTypeId, @PathParam("linkInstanceId") final String linkInstanceId, @PathParam("attributeId") final String attributeId) {
      return fileAttachmentFacade.getAllFileAttachments(linkTypeId, linkInstanceId, attributeId, FileAttachment.AttachmentType.LINK);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("collection/{collectionId:[0-9a-fA-F]{24}}/{documentId:[0-9a-fA-F]{24}}")
   public List<FileAttachment> getFileAttachmentsCollection(@PathParam("collectionId") final String collectionId, @PathParam("documentId") final String documentId) {
      return fileAttachmentFacade.getAllFileAttachments(collectionId, documentId, FileAttachment.AttachmentType.DOCUMENT);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("link/{linkTypeId:[0-9a-fA-F]{24}}/{linkInstanceId:[0-9a-fA-F]{24}}")
   public List<FileAttachment> getFileAttachmentsLink(@PathParam("linkTypeId") final String linkTypeId, @PathParam("linkInstanceId") final String linkInstanceId) {
      return fileAttachmentFacade.getAllFileAttachments(linkTypeId, linkInstanceId, FileAttachment.AttachmentType.LINK);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("collection/{collectionId:[0-9a-fA-F]{24}}")
   public List<FileAttachment> getFileAttachmentsCollection(@PathParam("collectionId") final String collectionId) {
      return fileAttachmentFacade.getAllFileAttachments(collectionId, FileAttachment.AttachmentType.DOCUMENT);
   }

   // Gets the state of file attachments from DB.
   @GET
   @Path("link/{linkTypeId:[0-9a-fA-F]{24}}")
   public List<FileAttachment> getFileAttachmentsLink(@PathParam("linkTypeId") final String linkTypeId) {
      return fileAttachmentFacade.getAllFileAttachments(linkTypeId, FileAttachment.AttachmentType.LINK);
   }

   // Gets the real file attachments according to S3 content. The results might not have all corresponding DB entries, or some DB entries might not have their corresponding S3 keys.
   @GET
   @Path("collection/{collectionId:[0-9a-fA-F]{24}}/{documentId:[0-9a-fA-F]{24}}/{attributeId}/details")
   public List<FileAttachment> listFileAttachmentsCollection(@PathParam("collectionId") final String collectionId, @PathParam("documentId") final String documentId, @PathParam("attributeId") final String attributeId) {
      return fileAttachmentFacade.listFileAttachments(collectionId, documentId, attributeId, FileAttachment.AttachmentType.DOCUMENT);
   }

   // Gets the real file attachments according to S3 content. The results might not have all corresponding DB entries, or some DB entries might not have their corresponding S3 keys.
   @GET
   @Path("link/{linkTypeId:[0-9a-fA-F]{24}}/{linkInstanceId:[0-9a-fA-F]{24}}/{attributeId}/details")
   public List<FileAttachment> listFileAttachmentsLink(@PathParam("linkTypeId") final String linkTypeId, @PathParam("linkInstanceId") final String linkInstanceId, @PathParam("attributeId") final String attributeId) {
      return fileAttachmentFacade.listFileAttachments(linkTypeId, linkInstanceId, attributeId, FileAttachment.AttachmentType.LINK);
   }

}
