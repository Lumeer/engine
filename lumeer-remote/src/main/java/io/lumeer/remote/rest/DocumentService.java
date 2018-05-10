/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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

import io.lumeer.api.dto.JsonDocument;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Pagination;
import io.lumeer.core.facade.DocumentFacade;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.remote.rest.annotation.PATCH;

import java.net.URI;
import java.util.List;
import java.util.Set;
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
@Path("organizations/{organizationCode}/projects/{projectCode}/collections/{collectionId}/documents")
public class DocumentService extends AbstractService {

   @PathParam("organizationCode")
   private String organizationCode;

   @PathParam("projectCode")
   private String projectCode;

   @PathParam("collectionId")
   private String collectionId;

   @Inject
   private DocumentFacade documentFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationCode, projectCode);
   }

   @POST
   public Response createDocument(JsonDocument document) {
      Document storedDocument = documentFacade.createDocument(collectionId, document);

      URI resourceUri = getResourceUri(storedDocument.getId());
      return Response.created(resourceUri).build();
   }

   @PUT
   @Path("{documentId}/data")
   public JsonDocument updateDocumentData(@PathParam("documentId") String documentId, DataDocument data) {
      Document storedDocument = documentFacade.updateDocumentData(collectionId, documentId, data);

      JsonDocument returnDocument = JsonDocument.convert(storedDocument);
      returnDocument.setFavorite(documentFacade.isFavorite(returnDocument.getId()));

      return returnDocument;
   }

   @PATCH
   @Path("{documentId}/data")
   public JsonDocument patchDocumentData(@PathParam("documentId") String documentId, DataDocument data) {
      Document storedDocument = documentFacade.patchDocumentData(collectionId, documentId, data);

      JsonDocument returnDocument = JsonDocument.convert(storedDocument);
      returnDocument.setFavorite(documentFacade.isFavorite(returnDocument.getId()));

      return returnDocument;
   }

   @DELETE
   @Path("{documentId}")
   public Response deleteDocument(@PathParam("documentId") String documentId) {
      documentFacade.deleteDocument(collectionId, documentId);

      return Response.ok().link(getParentUri(documentId), "parent").build();
   }

   @GET
   @Path("{documentId}")
   public JsonDocument getDocument(@PathParam("documentId") String documentId) {
      Document document = documentFacade.getDocument(collectionId, documentId);
      JsonDocument returnDocument = JsonDocument.convert(document);
      returnDocument.setFavorite(documentFacade.isFavorite(returnDocument.getId()));

      return returnDocument;
   }

   @GET
   public List<JsonDocument> getDocuments(@QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
      Pagination pagination = new Pagination(page, pageSize);

      List<Document> documents = documentFacade.getDocuments(collectionId, pagination);
      List<JsonDocument> returnDocuments = JsonDocument.convert(documents);

      Set<String> favoriteDocumentIds = documentFacade.getFavoriteDocumentsIds();
      returnDocuments.forEach(document -> document.setFavorite(favoriteDocumentIds.contains(document.getId())));

      return returnDocuments;
   }

   @POST
   @Path("{documentId}/favorite")
   public Response addFavoriteDocument(@PathParam("documentId") String documentId) {
      documentFacade.addFavoriteDocument(collectionId, documentId);

      return Response.ok().build();
   }

   @DELETE
   @Path("{documentId}/favorite")
   public Response removeFavoriteDocument(@PathParam("documentId") String documentId) {
      documentFacade.removeFavoriteDocument(collectionId, documentId);

      return Response.ok().build();
   }

}
