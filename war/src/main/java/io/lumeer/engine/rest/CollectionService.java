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
import io.lumeer.engine.api.dto.Attribute;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.CollectionMetadata;
import io.lumeer.engine.api.dto.Permission;
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
import io.lumeer.engine.controller.UserGroupFacade;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
   private UserGroupFacade userGroupFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dialect;

   @PathParam("organization")
   private String organizationCode;

   @PathParam("project")
   private String projectCode;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() {
      organizationFacade.setOrganizationCode(organizationCode);
      projectFacade.setCurrentProjectCode(projectCode);
   }

   /**
    * Returns a list of collection names in the database.
    *
    * @return the list of collection names
    */
   @GET
   @Path("/")
   public List<Collection> getCollections(@QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("0") int size) {
      int skip;
      if (size <= 0) {
         size = Integer.MAX_VALUE;
         skip = 0;
      } else {
         skip = page * size;
      }
      String user = userFacade.getUserEmail();
      List<String> groups = userGroupFacade.getGroupsOfUser(organizationCode, user);
      return collectionFacade.getCollections(user, groups, skip, size);
   }

   /**
    * Returns a collection from the database by it's code.
    * @param collectionCode
    *       collection code
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    * @return Collection with provided code
    */
   @GET
   @Path("{collectionCode}")
   public Collection getCollection(final @PathParam("collectionCode") String collectionCode) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      Collection collection = collectionFacade.getCollection(collectionCode);
      if (collection == null) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }
      if (!collection.getUserRoles().contains(LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      return collectionFacade.getCollection(collectionCode);
   }

   /**
    * Creates a new collection including its metadata collection with the specified name given by user.
    *
    * @param collection
    *       collection params to create
    * @return name of internal collection
    * @throws UserCollectionAlreadyExistsException
    *       When collection with given user name already exists.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    */
   @POST
   @Path("/")
   public String createCollection(final Collection collection) throws UserCollectionAlreadyExistsException, UnauthorizedAccessException {
      if (collection == null || collection.getName() == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      return collectionFacade.createCollection(collection);
   }

   /**
    * Updates a new collection including its metadata collection with the specified name given by user.
    *
    * @param collectionCode
    *       collection code to update
    * @param collection
    *       collection params to update
    * @throws UserCollectionAlreadyExistsException
    *       When collection with given user name already exists.
    * @throws UnauthorizedAccessException
    *       when user doesn't have appropriate role
    * @throws UserCollectionNotFoundException
    *       when user collection is not found in db
    */
   @PUT
   @Path("/{collectionCode}")
   public void updateCollection(@PathParam("collectionCode") final String collectionCode, final Collection collection) throws UserCollectionAlreadyExistsException, UnauthorizedAccessException, UserCollectionNotFoundException {
      if (collectionCode == null || collection == null || collection.getName() == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionFacade.updateCollection(collectionCode, collection);
   }

   /**
    * Drops the collection including its metadata collection with the specified name.
    *
    * @param collectionCode
    *       name of the collection to drop
    * @throws DbException
    *       When there is an error working with the database.
    */
   @DELETE
   @Path("/{collectionCode}")
   public void dropCollection(final @PathParam("collectionCode") String collectionCode) throws DbException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      collectionFacade.dropCollection(collectionCode);
   }

   /**
    * Renames existing attribute in collection metadata.
    * This method should be called only when also renaming attribute in documents,
    * and access rights should be checked there so they are not checked twice.
    *
    * @param collectionCode
    *       collection code
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
   @Path("/{collectionCode}/attributes/{oldName}/rename/{newName}")
   public void renameAttribute(final @PathParam("collectionCode") String collectionCode, final @PathParam("oldName") String oldName, final @PathParam("newName") String newName) throws AttributeAlreadyExistsException, UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionCode == null || oldName == null || newName == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionFacade.renameAttribute(collectionCode, oldName, newName);
   }

   /**
    * Removes given attribute from all existing document specified by its id.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       name of the attribute to remove
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to write to the collection.
    */
   @DELETE
   @Path("/{collectionCode}/attributes/{attributeName}")
   public void dropAttribute(final @PathParam("collectionCode") String collectionCode, final @PathParam("attributeName") String attributeName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null || attributeName == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionFacade.dropAttribute(collectionCode, attributeName);
   }

   /**
    * Searches the specified collection for specified documents using filter, sort, skip and limit option.
    *
    * @param collectionCode
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
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read to the collection data
    * @throws CollectionNotFoundException
    *       When the collection in which we want to search does not exist.
    */
   @POST
   @Path("/{collectionCode}/search/")
   public List<DataDocument> search(final @PathParam("collectionCode") String collectionCode, final @QueryParam("filter") String filter, final @QueryParam("sort") String sort, final @QueryParam("skip") int skip, final @QueryParam("limit") int limit) throws UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return searchFacade.search(collectionCode, dialect.documentFilter(filter == null ? "{}" : filter), dialect.documentSort(sort == null ? "{}" : sort), skip, limit);
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
   @Path("/run")
   public List<DataDocument> search(final @QueryParam("query") String query) {
      if (query == null) {
         throw new BadRequestException();
      }
      return searchFacade.search(query);
   }

   /**
    * Adds collection metadata document.
    *
    * @param collectionCode
    *       collection code
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
   @Path("/{collectionCode}/meta/{attributeName}")
   public void addCollectionMetadata(final @PathParam("collectionCode") String collectionCode, final @PathParam("attributeName") String attributeName, final DataDocument metadata) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null || attributeName == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.setCustomMetadata(collectionCode, new DataDocument(attributeName, metadata));
   }

   /**
    * Reads a metadata collection of given collection.
    *
    * @param collectionCode
    *       collection code
    * @return list of all documents from metadata collection
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    */
   @GET
   @Path("/{collectionCode}/meta/")
   public CollectionMetadata readCollectionMetadata(final @PathParam("collectionCode") String collectionCode) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return collectionMetadataFacade.getCollectionMetadata(collectionCode);
   }

   /**
    * Updates collection metadata document.
    *
    * @param collectionCode
    *       collection code
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
   @Path("/{collectionCode}/meta/{attributeName}")
   public void updateCollectionMetadata(final @PathParam("collectionCode") String collectionCode, final @PathParam("attributeName") String attributeName, final Object value) throws UnauthorizedAccessException, UserCollectionNotFoundException {
      if (collectionCode == null || attributeName == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.setCustomMetadata(collectionCode, new DataDocument(attributeName, value));
   }

   /**
    * Reads all collection attributes of given collection.
    *
    * @param collectionCode
    *       collection code
    * @return list of names of all attributes in the collection
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    */
   @GET
   @Path("/{collectionCode}/attributes")
   public List<Attribute> readCollectionAttributes(final @PathParam("collectionCode") String collectionCode) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return collectionFacade.readCollectionAttributes(collectionCode);
   }

   /**
    * Adds new constraint for the given attribute and checks if it is valid.
    *
    * @param collectionCode
    *       collection code
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
   @Path("/{collectionCode}/attributes/{attributeName}/constraints")
   public void setAttributeConstraint(final @PathParam("collectionCode") String collectionCode, final @PathParam("attributeName") String attributeName, final String constraintConfiguration) throws CollectionNotFoundException, InvalidConstraintException, UnauthorizedAccessException {
      if (collectionCode == null || attributeName == null || constraintConfiguration == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionFacade.addAttributeConstraint(collectionCode, attributeName, constraintConfiguration);
   }

   /**
    * Reads constraint for the given attribute.
    *
    * @param collectionCode
    *       collection code
    * @param attributeName
    *       attribute name
    * @return list of constraint configurations for the given attribute, empty list if constraints were not found
    * @throws UnauthorizedAccessException
    *       When current user is not allowed to read the collection.
    * @throws CollectionNotFoundException
    *       When the given collection does not exist.
    */
   @GET
   @Path("/{collectionCode}/attributes/{attributeName}/constraints")
   public List<String> readAttributeConstraint(final @PathParam("collectionCode") String collectionCode, final @PathParam("attributeName") String attributeName) throws UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionCode == null || attributeName == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return collectionMetadataFacade.getAttributeConstraintsConfigurations(collectionCode, attributeName);
   }

   /**
    * Drops constraint for the given attribute.
    *
    * @param collectionCode
    *       collection code
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
   @Path("/{collectionCode}/attributes/{attributeName}/constraints")
   public void dropAttributeConstraint(final @PathParam("collectionCode") String collectionCode, final @PathParam("attributeName") String attributeName, final String constraintConfiguration) throws UnauthorizedAccessException, CollectionNotFoundException {
      if (collectionCode == null || attributeName == null || constraintConfiguration == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.dropAttributeConstraint(collectionCode, attributeName, constraintConfiguration);
   }

   @GET
   @Path("/{collectionCode}/permissions")
   public Map<String, List<Permission>> getPermissions(final @PathParam("collectionCode") String collectionCode) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      return collectionMetadataFacade.getPermissions(projectCode, collectionCode);
   }

   @DELETE
   @Path("/{collectionCode}/permissions/users")
   public void removeUser(final @PathParam("collectionCode") String collectionCode, final @QueryParam("user") String user) throws UserCollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.removeUser(projectCode, collectionCode, user);
   }

   @DELETE
   @Path("/{collectionCode}/permissions/groups")
   public void removeGroup(final @PathParam("collectionCode") String collectionCode, final @QueryParam("group") String group) throws UserCollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.removeGroup(projectCode, collectionCode, group);
   }

   @PUT
   @Path("/{collectionCode}/permissions/users")
   public void setRolesToUser(final @PathParam("collectionCode") String collectionCode,
         final @QueryParam("user") String user, final @QueryParam("roles") List<String> roles) throws UserCollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.setRolesToUser(projectCode, collectionCode, roles, user);
   }

   @PUT
   @Path("/{collectionCode}/permissions/groups")
   public void setRolesToGroup(final @PathParam("collectionCode") String collectionCode,
         final @QueryParam("group") String group, final @QueryParam("roles") List<String> roles) throws UserCollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.setRolesToGroup(projectCode, collectionCode, roles, group);
   }

   @POST
   @Path("/{collectionCode}/permissions/users")
   public void addUserWithRoles(final @PathParam("collectionCode") String collectionCode,
         final @QueryParam("user") String user, final @QueryParam("roles") List<String> roles) throws UserCollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, roles, user);
   }

   @POST
   @Path("/{collectionCode}/permissions/groups")
   public void addGroupWithRoles(final @PathParam("collectionCode") String collectionCode,
         final @QueryParam("group") String group, final @QueryParam("roles") List<String> roles) throws UserCollectionNotFoundException, UnauthorizedAccessException {
      if (collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_MANAGE)) {
         throw new UnauthorizedAccessException();
      }

      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, roles, group);
   }

}