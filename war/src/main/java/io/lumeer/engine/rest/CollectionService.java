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

import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.exception.AttributeAlreadyExistsException;
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SearchFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.rest.dao.CollectionMetadata;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
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
@Path("/organizations/{organization}/projects/{project}/collections")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
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
   private UserFacade userFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dialect;

   @PathParam("organization")
   private String organisationCode;

   @PathParam("project")
   private String projectCode;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() {
      organizationFacade.setOrganizationCode(organisationCode);
      projectFacade.setCurrentProjectCode(projectCode);
   }

   /**
    * Returns a list of collection names in the database.
    *
    * @return the list of collection names
    */
   @GET
   @Path("/")
   public List<String> getAllCollections() {
      List<String> collections = new ArrayList<>();
      String projectId = projectFacade.getCurrentProjectId();
      collections.addAll(collectionFacade.getAllCollections().entrySet().stream()
                                         .filter(c -> securityFacade.hasCollectionRole(projectId, c.getKey(),
                                               LumeerConst.Security.ROLE_READ)).map(Map.Entry::getValue)
                                         .collect(Collectors.toList()));
      return collections;
   }

   /**
    * Creates a new collection including its metadata collection with the specified name given by user.
    *
    * @param name
    *       name of the collection to create
    * @return name of internal collection
    * @throws UserCollectionAlreadyExistsException
    *       When collection with given user name already exists.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @POST
   @Path("/{name}")
   public String createCollection(final @PathParam("name") String name) throws UserCollectionAlreadyExistsException, UnauthorizedAccessException {
      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      if (name == null) {
         throw new BadRequestException();
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
         throw new BadRequestException();
      }

      String internalName = getInternalName(name);
      if (!securityFacade.hasCollectionRole(projectCode, internalName, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      collectionFacade.dropCollection(internalName);
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
    * @throws AttributeAlreadyExistsException
    *       When attribute with new name already exists.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    */
   @PUT
   @Path("/{collectionName}/attributes/{oldName}/rename/{newName}")
   public void renameAttribute(final @PathParam("collectionName") String collectionName, final @PathParam("oldName") String oldName, final @PathParam("newName") String newName) throws AttributeAlreadyExistsException, UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || oldName == null || newName == null) {
         throw new BadRequestException();
      }

      String internalName = getInternalName(collectionName);
      if (!securityFacade.hasCollectionRole(projectCode, internalName, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionFacade.renameAttribute(internalName, oldName, newName);
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
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    */
   @DELETE
   @Path("/{collectionName}/attributes/{attributeName}")
   public void dropAttribute(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionName == null || attributeName == null) {
         throw new BadRequestException();
      }

      String internalName = getInternalName(collectionName);
      if (!securityFacade.hasCollectionRole(projectCode, internalName, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionFacade.dropAttribute(internalName, attributeName);
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
    */
   @POST
   @Path("/{collectionName}/search/")
   public List<DataDocument> search(final @PathParam("collectionName") String collectionName, final @QueryParam("filter") String filter, final @QueryParam("sort") String sort, final @QueryParam("skip") int skip, final @QueryParam("limit") int limit) throws CollectionNotFoundException {
      if (collectionName == null) {
         throw new BadRequestException();
      }
      String internalCollectionName = getInternalName(collectionName);
      if (!dataStorage.hasCollection(internalCollectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      return searchFacade.search(internalCollectionName, dialect.documentFilter(filter == null ? "{}" : filter), dialect.documentSort(sort == null ? "{}" : sort), skip, limit);
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
   public List<DataDocument> search(final @QueryParam("query") String query) {
      if (query == null) {
         throw new BadRequestException();
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
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection
    */
   @POST
   @Path("/{collectionName}/meta/{attributeName}")
   public void addCollectionMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final DataDocument metadata) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionName == null || attributeName == null) {
         throw new BadRequestException();
      }

      String internalName = getInternalName(collectionName);

      if (!securityFacade.hasCollectionRole(projectCode, internalName, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      DataDocument metadataDocument = new DataDocument(attributeName, metadata);
      collectionMetadataFacade.setCustomMetadata(internalName, metadataDocument);
   }

   /**
    * Reads a metadata collection of given collection.
    *
    * @param collectionName
    *       collection name
    * @return list of all documents from metadata collection
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    */
   @GET
   @Path("/{collectionName}/meta/")
   public CollectionMetadata readCollectionMetadata(final @PathParam("collectionName") String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionName == null) {
         throw new BadRequestException();
      }
      return collectionMetadataFacade.getCollectionMetadata(getInternalName(collectionName));
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
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    * @throws UserCollectionNotFoundException
    *       When the given collection does not exist.
    */
   @PUT
   @Path("/{collectionName}/meta/{attributeName}")
   public void updateCollectionMetadata(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final Object value) throws UnauthorizedAccessException, UserCollectionNotFoundException {
      if (collectionName == null || attributeName == null) {
         throw new BadRequestException();
      }
      DataDocument metadataDocument = new DataDocument(attributeName, value);
      collectionMetadataFacade.setCustomMetadata(getInternalName(collectionName), metadataDocument);
   }

   /**
    * Reads all collection attributes of given collection.
    *
    * @param collectionName
    *       collection name
    * @return list of names of all attributes in the collection
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    */
   @GET
   @Path("/{collectionName}/attributes")
   public Set<String> readCollectionAttributes(final @PathParam("collectionName") String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionName == null) {
         throw new BadRequestException();
      }
      return collectionFacade.readCollectionAttributes(getInternalName(collectionName)).keySet();
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
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws InvalidConstraintException
    *       When new constraint is not valid or is in conflict with existing constraints.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    */
   @PUT
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   public void setAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final String constraintConfiguration) throws CollectionNotFoundException, InvalidConstraintException, UnauthorizedAccessException {
      if (collectionName == null || attributeName == null || constraintConfiguration == null) {
         throw new BadRequestException();
      }

      String internalName = getInternalName(collectionName);
      if (!securityFacade.hasCollectionRole(projectCode, internalName, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.addAttributeConstraint(internalName, attributeName, constraintConfiguration);
   }

   /**
    * Reads constraint for the given attribute.
    *
    * @param collectionName
    *       collection name
    * @param attributeName
    *       attribute name
    * @return list of constraint configurations for the given attribute, empty list if constraints were not found
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    */
   @GET
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   public List<String> readAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName) throws UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || attributeName == null) {
         throw new BadRequestException();
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
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    */
   @DELETE
   @Path("/{collectionName}/attributes/{attributeName}/constraints")
   public void dropAttributeConstraint(final @PathParam("collectionName") String collectionName, final @PathParam("attributeName") String attributeName, final String constraintConfiguration) throws UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionName == null || attributeName == null || constraintConfiguration == null) {
         throw new BadRequestException();
      }

      String internalName = getInternalName(collectionName);
      if (!securityFacade.hasCollectionRole(projectCode, internalName, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
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