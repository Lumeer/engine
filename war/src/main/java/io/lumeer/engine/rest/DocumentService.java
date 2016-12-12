/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
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
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.InvalidDocumentKeyException;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;
import io.lumeer.engine.api.exception.VersionUpdateConflictException;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.DocumentMetadataFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.VersionFacade;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Path("/documents")
@RequestScoped
public class DocumentService implements Serializable {

   private static final long serialVersionUID = 5645433756019847986L;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private VersionFacade versionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private SecurityFacade securityFacade;

   @POST
   @Path("/collections/{collectionName}")
   @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   public String createDocument(final @PathParam("collectionName") String collectionName, final DataDocument document) throws UnsuccessfulOperationException, CollectionNotFoundException, InvalidDocumentKeyException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null || document == null) {
         throw new IllegalArgumentException();
      }
      return documentFacade.createDocument(getInternalName(collectionName), document);
   }

   @GET
   @Path("/{documentId}/collections/{collectionName}")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument readDocument(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId) throws CollectionNotFoundException, DocumentNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null || documentId == null) {
         throw new IllegalArgumentException();
      }
      return documentFacade.readDocument(getInternalName(collectionName), documentId);
   }

   @DELETE
   @Path("/{documentId}/collections/{collectionName}")
   public void dropDocument(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId) throws CollectionNotFoundException, VersionUpdateConflictException, UnsuccessfulOperationException, DocumentNotFoundException, InvalidDocumentKeyException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null || documentId == null) {
         throw new IllegalArgumentException();
      }
      documentFacade.dropDocument(getInternalName(collectionName), documentId);
   }

   @PUT
   @Path("/{documentId}/collections/{collectionName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateDocument(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId, final DataDocument updatedDocument) throws CollectionNotFoundException, VersionUpdateConflictException, UnsuccessfulOperationException, InvalidDocumentKeyException, DocumentNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null || documentId == null || updatedDocument == null) {
         throw new IllegalArgumentException();
      }
      // TODO: could be the id parameter included in updateDocument in request body?
      updatedDocument.put("_id", documentId);
      documentFacade.updateDocument(getInternalName(collectionName), updatedDocument);
   }

   @POST
   @Path("/{documentId}/meta/{attributeName}/collections/{collectionName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addDocumentMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId, final @PathParam("attributeName") String attributeName, final Object value) throws CollectionNotFoundException, DocumentNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null || documentId == null || attributeName == null || value == null) {
         throw new IllegalArgumentException();
      }
      documentMetadataFacade.putDocumentMetadata(getInternalName(collectionName), documentId, attributeName, value);
   }

   @GET
   @Path("/{documentId}/meta//collections/{collectionName}")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, Object> readDocumentMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId) throws CollectionNotFoundException, DocumentNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null || documentId == null) {
         throw new IllegalArgumentException();
      }
      return documentMetadataFacade.readDocumentMetadata(getInternalName(collectionName), documentId);
   }

   @PUT
   @Path("/{documentId}/meta/collections/{collectionName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateDocumentMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId, final DataDocument metadata) throws CollectionNotFoundException, DocumentNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null || documentId == null || metadata == null) {
         throw new IllegalArgumentException();
      }
      documentMetadataFacade.updateDocumentMetadata(getInternalName(collectionName), documentId, metadata);
   }

   @GET
   @Path("/{documentId}/versions/collections/{collectionName}")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> searchHistoryChanges(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null || documentId == null) {
         throw new IllegalArgumentException();
      }
      return versionFacade.getDocumentVersions(getInternalName(collectionName), documentId);
   }

   @POST
   @Path("/{documentId}/versions/{versionId}/collections/{collectionName}")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument getDocumentVersion(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId, final @PathParam("versionId") int versionId) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, DocumentNotFoundException {
      if (collectionName == null || documentId == null) {
         throw new IllegalArgumentException();
      }
      return versionFacade.getOldDocumentVersion(getInternalName(collectionName), documentId, versionId);
   }

   @GET
   @Path("/{documentId}/rights/collections/{collectionName}")
   @Produces(MediaType.APPLICATION_JSON)
   public boolean readAccessRights(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId) throws DocumentNotFoundException {
      if (collectionName == null || documentId == null) {
         throw new IllegalArgumentException();
      }
      // TODO: user in the request body?
      // return securityFacade.checkForAddRights(collectionName, documentId, ?user?);
      return false;
   }

   @PUT
   @Path("/{documentId}/rights/collections/{collectionName}")
   public void updateAccessRights(final @PathParam("collectionName") String collectionName, final @PathParam("documentId") String documentId) {
      if (collectionName == null || documentId == null) {
         throw new IllegalArgumentException();
      }
      // TODO: which metod to use?
      // TODO: access rights to request body? any bean?
   }

   private String getInternalName(String collectionOriginalName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      return collectionMetadataFacade.getInternalCollectionName(collectionOriginalName);
   }
}
