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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.AttributeNotFoundException;
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.InvalidCollectionAttributeTypeException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.SearchFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.controller.VersionFacade;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Path("/collections")
@RequestScoped
public class CollectionService implements Serializable {

   private static final long serialVersionUID = 7581114783619845412L;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private VersionFacade versionFacade;

   @Inject
   private UserFacade userFacade;

   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> getAllCollections() throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      return new ArrayList<>(collectionFacade.getAllCollections().values());
   }

   @POST
   @Path("/{name}")
   @Produces(MediaType.APPLICATION_JSON)
   public String createCollection(final @PathParam("name") String name) throws CollectionMetadataDocumentNotFoundException, CollectionNotFoundException, CollectionAlreadyExistsException, UserCollectionAlreadyExistsException {
      if (name == null) {
         throw new IllegalArgumentException();
      }
      return collectionFacade.createCollection(name);
   }

   @DELETE
   @Path("/{name}")
   public void dropCollection(final @PathParam("name") String name) throws DbException {
      if (name == null) {
         throw new IllegalArgumentException();
      }
      collectionFacade.dropCollection(getInternalName(name));
   }

   @PUT
   @Path("/{collectionName}/attributes/{oldName}/rename/{newName}")
   public void renameAttribute(final @PathParam("collectionName") String collectionName, final @PathParam("oldName") String oldName, final @PathParam("newName") String newName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, AttributeAlreadyExistsException {
      if (collectionName == null || oldName == null || newName == null
            || (collectionMetadataFacade.renameCollectionAttribute(getInternalName(collectionName), oldName, newName) == false)) {
         throw new IllegalArgumentException();
      }
   }

   @DELETE
   @Path("/{collectionName}/attributes/{attributeName}")
   public void dropAttribute(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, AttributeNotFoundException, UnauthorizedAccessException {
      if (collectionName == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      collectionFacade.dropAttribute(getInternalName(collectionName), attributeName);
   }

   @POST
   @Path("/{collectionName}/search/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> search(final @PathParam("collectionName") String collectionName, final @QueryParam("filter") String filter, final @QueryParam("sort") String sort, final @QueryParam("skip") int skip, final @QueryParam("limit") int limit) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null) {
         throw new IllegalArgumentException();
      }
      return searchFacade.search(getInternalName(collectionName), filter, sort, skip, limit);
   }

   @POST
   @Path("/{collectionName}/run/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> search(final @QueryParam("query") String query) {
      if (query == null) {
         throw new IllegalArgumentException();
      }
      return searchFacade.search(query);
   }

   @POST
   @Path("/{collectionName}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addCollectionMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final DataDocument metadata) {
      if (collectionName == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      // TODO: create specific method to facade (Alica)
   }

   @GET
   @Path("/{collectionName}/meta/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> readCollectionMetadata(final @PathParam("collectionName") String collectionName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException {
      if (collectionName == null) {
         throw new IllegalArgumentException();
      }
      return collectionFacade.readCollectionMetadata(getInternalName(collectionName));
   }

   @PUT
   @Path("/{collectionName}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateCollectionMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final Object value) {
      // TODO: create specific method to facade (Alica)
   }

   @GET
   @Path("/{collectionName}/attributes")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readCollectionAttributes(final @PathParam("collectionName") String collectionName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException {
      if (collectionName == null) {
         throw new IllegalArgumentException();
      }
      return collectionFacade.readCollectionAttributes(getInternalName(collectionName));
   }

   @GET
   @Path("/{collectionName}/rights")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument readAccessRights(final @PathParam("collectionName") String collectionName) {
      // TODO: implement method in facade to manage access rights of the given collection (Alica)
      return null;
   }

   @PUT
   @Path("/{collectionName}/rights")
   public void updateAccessRights(final @PathParam("collectionName") String collectionName) {
      // TODO: implement method in facade to manage access rights of the given collection (Alica)
   }

   @PUT
   @Path("/{collectionName}/attributes/{attributeName}/types/{newType}")
   @Produces(MediaType.APPLICATION_JSON)
   public boolean setAttributeType(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final @PathParam("newType") String newType) throws CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException, CollectionNotFoundException, InvalidCollectionAttributeTypeException {
      if (collectionName == null || attributeName == null || newType == null) {
         throw new IllegalArgumentException();
      }

      if (collectionMetadataFacade.retypeCollectionAttribute(getInternalName(collectionName), attributeName, newType) == false) {
         throw new InvalidCollectionAttributeTypeException(ErrorMessageBuilder.invalidCollectionAttributeType(newType, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_VALUES));
      }
      return true;
   }

   @GET
   @Path("/{collectionName}/attributes/{attributeName}/types")
   @Produces(MediaType.APPLICATION_JSON)
   public String readAttributeType(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName) throws CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return collectionMetadataFacade.getAttributeType(getInternalName(collectionName), attributeName);
   }

   @PUT
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final String constraintConfiguration) throws CollectionMetadataDocumentNotFoundException, CollectionNotFoundException, InvalidConstraintException, UnauthorizedAccessException {
      if (collectionName == null || attributeName == null || constraintConfiguration == null) {
         throw new IllegalArgumentException();
      }
      collectionMetadataFacade.addAttributeConstraint(getInternalName(collectionName), attributeName, constraintConfiguration);
   }

   @GET
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName) throws CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return collectionMetadataFacade.getAttributeConstraintsConfigurations(getInternalName(collectionName), attributeName);
   }

   @DELETE
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   @Consumes(MediaType.APPLICATION_JSON)
   public void dropAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final String constraintConfiguration) throws CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || attributeName == null || constraintConfiguration == null) {
         throw new IllegalArgumentException();
      }
      collectionMetadataFacade.dropAttributeConstraint(getInternalName(collectionName), attributeName, constraintConfiguration);
   }

   private String getInternalName(final String collectionOriginalName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      return collectionMetadataFacade.getInternalCollectionName(collectionOriginalName);
   }
}
