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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.DocumentsAndLinks;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.View;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.core.facade.FileAttachmentFacade;
import io.lumeer.core.facade.LinkTypeFacade;
import io.lumeer.core.facade.ProjectFacade;
import io.lumeer.core.facade.SearchFacade;
import io.lumeer.core.facade.ViewFacade;
import io.lumeer.core.util.Tuple;

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("p/organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}")
public class PublicService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private LinkTypeFacade linkTypeFacade;

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setOrganizationId(organizationId);
      workspaceKeeper.setProjectId(projectId);
   }

   @GET
   public Project getProject() {
      return projectFacade.getPublicProjectById(projectId);
   }

   @GET
   @Path("collections")
   public List<Collection> getCollections() {
      return collectionFacade.getPublicCollections();
   }

   @GET
   @Path("views")
   public List<View> getViews() {
      return viewFacade.getViewsPublic();
   }

   @GET
   @Path("link-types")
   public List<LinkType> getLinkTypes() {
      return linkTypeFacade.getLinkTypesPublic();
   }

   @GET
   @Path("link-instances")
   public List<LinkInstance> getLinkInstances() {
      return searchFacade.getLinkInstancesPublic(new Query());
   }

   @GET
   @Path("documents")
   public List<Document> getDocuments() {
      return searchFacade.searchDocumentsPublic(new Query());
   }

   @GET
   @Path("documentsAndLinks")
   public DocumentsAndLinks getDocumentsAndLinks() {
      final Tuple<List<Document>, List<LinkInstance>> tuple = searchFacade.searchDocumentsAndLinksPublic(new Query());
      return new DocumentsAndLinks(tuple.getFirst(), tuple.getSecond());
   }

   @GET
   @Path("tasks")
   public DocumentsAndLinks getTaskDocumentsAndLinks() {
      final Tuple<List<Document>, List<LinkInstance>> tuple = searchFacade.searchTasksDocumentsAndLinksPublic(new Query());
      return new DocumentsAndLinks(tuple.getFirst(), tuple.getSecond());
   }

   @GET
   @Path("files/collection/{collectionId:[0-9a-fA-F]{24}}")
   public List<FileAttachment> getFileAttachmentsCollection(@PathParam("collectionId") final String collectionId) {
      return fileAttachmentFacade.getAllFileAttachments(collectionId, FileAttachment.AttachmentType.DOCUMENT);
   }

   @GET
   @Path("files/link/{linkTypeId:[0-9a-fA-F]{24}}")
   public List<FileAttachment> getFileAttachmentsLink(@PathParam("linkTypeId") final String linkTypeId) {
      return fileAttachmentFacade.getAllFileAttachments(linkTypeId, FileAttachment.AttachmentType.LINK);
   }

   @GET
   @Path("files/{attachmentId}")
   public FileAttachment getFileAttachment(@PathParam("attachmentId") final String fileAttachmentId) {
      return fileAttachmentFacade.getFileAttachment(fileAttachmentId, false);
   }
}
