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
import io.lumeer.engine.api.exception.CollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionMetadataDocumentNotFoundException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.InvalidCollectionAttributeTypeException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.SearchFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.controller.VersionFacade;
import io.lumeer.engine.rest.dao.AccessRightsDao;
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

   /**
    * Returns a list of collection names in the database.
    *
    * @return the list of collection names
    * @throws CollectionNotFoundException
    *       When some collection was not found.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When some metadata collection was not found.
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> getAllCollections() throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      return new ArrayList<>(collectionFacade.getAllCollections().values());
   }

   /**
    * Creates a new collection including its metadata collection with the specified name given by user.
    *
    * @param name
    *       name of the collection to create
    * @return name of internal collection
    * @throws CollectionNotFoundException
    *       When newly created metadata collection for the collection was not found and initial metadata could not be created.
    * @throws CollectionAlreadyExistsException
    *       If collection with created internal name already exists.
    * @throws UserCollectionAlreadyExistsException
    *       When collection with given user name already exists.
    */
   @POST
   @Path("/{name}")
   @Produces(MediaType.APPLICATION_JSON)
   public String createCollection(final @PathParam("name") String name) throws CollectionNotFoundException, CollectionAlreadyExistsException, UserCollectionAlreadyExistsException {
      if (name == null) {
         throw new IllegalArgumentException();
      }
      return collectionFacade.createCollection(name);
   }

   /**
    * Drops the collection including its metadata collection with the specified name.
    *
    * @param name
    *       name of the collection to drop
    * @throws DbException
    *       When there is an error working with the database.
    */
   @DELETE
   @Path("/{name}")
   public void dropCollection(final @PathParam("name") String name) throws DbException {
      if (name == null) {
         throw new IllegalArgumentException();
      }
      collectionFacade.dropCollection(getInternalName(name));
   }

   /**
    * Renames existing attribute in collection metadata.
    * This method should be called only when also renaming attribute in documents,
    * and access rights should be checked there so they are not checked twice.
    *
    * @param collectionName
    *       collection name
    * @param oldName
    *       old attribute name
    * @param newName
    *       new attribute name
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws AttributeAlreadyExistsException
    *       When attribute with new name already exists.
    */
   @PUT
   @Path("/{collectionName}/attributes/{oldName}/rename/{newName}")
   public void renameAttribute(final @PathParam("collectionName") String collectionName, final @PathParam("oldName") String oldName, final @PathParam("newName") String newName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, AttributeAlreadyExistsException {
      if (collectionName == null || oldName == null || newName == null
            || (collectionMetadataFacade.renameCollectionAttribute(getInternalName(collectionName), oldName, newName) == false)) {
         throw new IllegalArgumentException();
      }
   }

   /**
    * Removes given attribute from all existing document specified by its id.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       name of the attribute to remove
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    */
   @DELETE
   @Path("/{collectionName}/attributes/{attributeName}")
   public void dropAttribute(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException {
      if (collectionName == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      collectionFacade.dropAttribute(getInternalName(collectionName), attributeName);
   }

   /**
    * Searches the specified collection for specified documents using filter, sort, skip and limit option.
    *
    * @param collectionName
    *       name of the collection where the run will be performed
    * @param filter
    *       query predicate. If unspecified, then all documents in the collection will match the predicate.
    * @param sort
    *       sort specification for the ordering of the results
    * @param skip
    *       number of documents to skip
    * @param limit
    *       maximum number of documents to return
    * @return list of the found documents
    * @throws CollectionNotFoundException
    *       When the collection in which we want to search does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    */
   @POST
   @Path("/{collectionName}/search/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> search(final @PathParam("collectionName") String collectionName, final @QueryParam("filter") String filter, final @QueryParam("sort") String sort, final @QueryParam("skip") int skip, final @QueryParam("limit") int limit) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null) {
         throw new IllegalArgumentException();
      }
      return searchFacade.search(getInternalName(collectionName), filter, sort, skip, limit);
   }

   /**
    * Executes a query to find and return documents.
    *
    * @param query
    *       the database find command specified as a JSON string
    * @return the list of the found documents
    * @see <a href="https://docs.mongodb.com/v3.2/reference/command/find/#dbcmd.find">https://docs.mongodb.com/v3.2/reference/command/find/#dbcmd.find</a>
    */
   @POST
   @Path("/{collectionName}/run/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> search(final @QueryParam("query") String query) {
      if (query == null) {
         throw new IllegalArgumentException();
      }
      return searchFacade.search(query);
   }

   /**
    * Adds collection metadata document.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       metadata attribute name
    * @param metadata
    *       document with metadata values
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection
    */
   @POST
   @Path("/{collectionName}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addCollectionMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final DataDocument metadata) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException {
      DataDocument metadataDocument = new DataDocument(attributeName, metadata);
      if (collectionName == null || attributeName == null || !collectionMetadataFacade.setCustomMetadata(getInternalName(collectionName), metadataDocument)) {
         throw new IllegalArgumentException();
      }
   }

   /**
    * Reads a metadata collection of given collection.
    *
    * @param collectionName
    *       collection name
    * @return list of all documents from metadata collection
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    */
   @GET
   @Path("/{collectionName}/meta/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> readCollectionMetadata(final @PathParam("collectionName") String collectionName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException {
      if (collectionName == null) {
         throw new IllegalArgumentException();
      }
      return collectionFacade.readCollectionMetadata(getInternalName(collectionName));
   }

   /**
    * Updates collection metadata document.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       metadata attribute name
    * @param value
    *       value of the given meta attribute
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    */
   @PUT
   @Path("/{collectionName}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateCollectionMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final Object value) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException {
      DataDocument metadataDocument = new DataDocument(attributeName, value);
      if (collectionName == null || attributeName == null || !collectionMetadataFacade.setCustomMetadata(getInternalName(collectionName), metadataDocument)) {
         throw new IllegalArgumentException();
      }
   }

   /**
    * Reads all collection attributes of given collection.
    *
    * @param collectionName
    *       collection name
    * @return list of names of all attributes in the collection
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    */
   @GET
   @Path("/{collectionName}/attributes")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readCollectionAttributes(final @PathParam("collectionName") String collectionName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException {
      if (collectionName == null) {
         throw new IllegalArgumentException();
      }
      return collectionFacade.readCollectionAttributes(getInternalName(collectionName));
   }

   /*@GET
   @Path("/{collectionName}/rights")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument readAccessRights(final @PathParam("collectionName") String collectionName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null) {
         throw new IllegalArgumentException();
      }
      return collectionMetadataFacade.getAllAccessRights(getInternalName(collectionName));
   }*/

   /**
    * Gets access rights for all users of the given collection.
    *
    * @param collectionName
    *       collection name
    * @return list of access rights of the given collection
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    */
   @GET
   @Path("/{collectionName}/rights")
   @Produces(MediaType.APPLICATION_JSON)
   public List<AccessRightsDao> readAccessRights(final @PathParam("collectionName") String collectionName) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException {
      if (collectionName == null) {
         throw new IllegalArgumentException();
      }
      return collectionMetadataFacade.getAllAccessRights(getInternalName(collectionName));
   }

   /**
    * Updates access rights of the given collection for currently logged user.
    *
    * @param collectionName
    *       collection name
    * @param accessRights
    *       new access rights of the logged user
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to change rights for the collection.
    */
   @PUT
   @Path("/{collectionName}/rights")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateAccessRights(final @PathParam("collectionName") String collectionName, final AccessRightsDao accessRights) throws CollectionNotFoundException, CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException {
      if (collectionName == null || accessRights == null) {
         throw new IllegalArgumentException();
      }

      final String user = userFacade.getUserEmail();

      if (accessRights.isRead()) {
         collectionMetadataFacade.addCollectionRead(getInternalName(collectionName), user);
      } else {
         collectionMetadataFacade.removeCollectionRead(getInternalName(collectionName), user);
      }

      if (accessRights.isWrite()) {
         collectionMetadataFacade.addCollectionWrite(getInternalName(collectionName), user);
      } else {
         collectionMetadataFacade.removeCollectionWrite(getInternalName(collectionName), user);
      }

      if (accessRights.isExecute()) {
         collectionMetadataFacade.addCollectionExecute(getInternalName(collectionName), user);
      } else {
         collectionMetadataFacade.removeCollectionExecute(getInternalName(collectionName), user);
      }
   }

   /**
    * Changes attribute type of the given attribute in collection metadata.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       attribute name
    * @param newType
    *       new meta type
    * @return true if retype is successful, false if new type is not valid or when metadata about attribute was not found
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws InvalidCollectionAttributeTypeException
    *       When new type is not valid meta type.
    */
   @PUT
   @Path("/{collectionName}/attributes/{attributeName}/types/{newType}")
   @Produces(MediaType.APPLICATION_JSON)
   public boolean setAttributeType(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final @PathParam("newType") String newType) throws CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException, CollectionNotFoundException, InvalidCollectionAttributeTypeException {
      if (collectionName == null || attributeName == null || newType == null) {
         throw new IllegalArgumentException();
      }

      if (collectionMetadataFacade.retypeCollectionAttribute(getInternalName(collectionName), attributeName, newType) == false) {
         throw new InvalidCollectionAttributeTypeException(ErrorMessageBuilder.invalidCollectionAttributeTypeString(newType, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_VALUES));
      }
      return true;
   }

   /**
    * Reads attribute type of the given attribute in collection metadata.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       attribute name
    * @return type of the given attribute, default (String) if attribute is not found
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    */
   @GET
   @Path("/{collectionName}/attributes/{attributeName}/types")
   @Produces(MediaType.APPLICATION_JSON)
   public String readAttributeType(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName) throws CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return collectionMetadataFacade.getAttributeType(getInternalName(collectionName), attributeName);
   }

   /**
    * Adds new constraint for the given attribute and checks if it is valid.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       string with constraint configuration
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws InvalidConstraintException
    *       When new constraint is not valid or is in conflict with existing constraints.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    */
   @PUT
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final String constraintConfiguration) throws CollectionMetadataDocumentNotFoundException, CollectionNotFoundException, InvalidConstraintException, UnauthorizedAccessException {
      if (collectionName == null || attributeName == null || constraintConfiguration == null) {
         throw new IllegalArgumentException();
      }
      collectionMetadataFacade.addAttributeConstraint(getInternalName(collectionName), attributeName, constraintConfiguration);
   }

   /**
    * Reads constraint for the given attribute.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       attribute name
    * @return list of constraint configurations for the given attribute, empty list if constraints were not found
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    */
   @GET
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   @Produces(MediaType.APPLICATION_JSON)
   public List<String> readAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName) throws CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || attributeName == null) {
         throw new IllegalArgumentException();
      }
      return collectionMetadataFacade.getAttributeConstraintsConfigurations(getInternalName(collectionName), attributeName);
   }

   /**
    * Drops constraint for the given attribute.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       attribute name
    * @param constraintConfiguration
    *       constraint configuration to be removed
    * @throws CollectionMetadataDocumentNotFoundException
    *       When the metadata collection of the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    */
   @DELETE
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   @Consumes(MediaType.APPLICATION_JSON)
   public void dropAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final String constraintConfiguration) throws CollectionMetadataDocumentNotFoundException, UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || attributeName == null || constraintConfiguration == null) {
         throw new IllegalArgumentException();
      }
      collectionMetadataFacade.dropAttributeConstraint(getInternalName(collectionName), attributeName, constraintConfiguration);
   }

   /**
    * Returns internal name of the given collection stored in the database.
    *
    * @param collectionOriginalName
    *       original name of the collection given by user
    * @return internal name of the given collection
    * @throws UserCollectionNotFoundException
    *       When the given user collection does not exist.
    */
   private String getInternalName(final String collectionOriginalName) throws UserCollectionNotFoundException {
      return collectionMetadataFacade.getInternalCollectionName(collectionOriginalName);
   }
}
