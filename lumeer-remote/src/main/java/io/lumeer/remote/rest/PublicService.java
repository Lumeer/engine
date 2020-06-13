package io.lumeer.remote.rest;/*
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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
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

import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
      Set<String> favoriteCollectionIds = collectionFacade.getFavoriteCollectionsIds();
      List<Collection> collections = collectionFacade.getCollections();
      collections.forEach(collection -> collection.setFavorite(favoriteCollectionIds.contains(collection.getId())));

      return collections;
   }

   @GET
   @Path("views")
   public List<View> getViews() {
      final Set<String> favoriteViewIds = viewFacade.getFavoriteViewsIds();
      final List<View> views = viewFacade.getViews();

      if (favoriteViewIds != null && favoriteViewIds.size() > 0) {
         views.forEach(v -> {
            if (favoriteViewIds.contains(v.getId())) {
               v.setFavorite(true);
            }
         });
      }

      return views;
   }

   @GET
   @Path("link-types")
   public List<LinkType> getLinkTypes() {
      return linkTypeFacade.getLinkTypesPublic();
   }

   @GET
   @Path("link-instances")
   public List<LinkInstance> getLinkInstances() {
      return searchFacade.getLinkInstances(new Query());
   }

   @GET
   @Path("documents")
   public List<Document> getDocuments() {
      return searchFacade.searchDocuments(new Query());
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
