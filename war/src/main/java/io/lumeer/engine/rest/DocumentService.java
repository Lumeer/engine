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
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.InvalidValueException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.api.exception.UserCollectionNotFoundException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.DocumentMetadataFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.VersionFacade;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 * <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
@Path("/organizations/{organization}/projects/{project}/collections/{collection}/documents")
@RequestScoped
public class DocumentService implements Serializable {

   private static final long serialVersionUID = 5645433756019847986L;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private VersionFacade versionFacade;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @PathParam("organization")
   private String organisationCode;

   @PathParam("project")
   private String projectCode;

   @PathParam("collection")
   private String collectionCode;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() throws DbException {
      organizationFacade.setOrganizationCode(organisationCode);
      projectFacade.setCurrentProjectCode(projectCode);
      if (!collectionFacade.hasCollection(collectionCode)) {
         throw new UserCollectionNotFoundException(ErrorMessageBuilder.userCollectionNotFoundString(collectionCode));
      }

   }

   /**
    * Reads all documents in given collection.
    *
    * @return the DataDocument object representing the read document
    * @throws DbException
    *       When there is an error working with the database.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> readDocuments() throws DbException, InvalidConstraintException {
      if (organisationCode == null || projectCode == null || collectionCode == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return documentFacade.getAllDocuments(collectionCode);
   }

   /**
    * Creates and inserts a new document to specified collection. The method creates the given collection if does not exist.
    *
    * @param document
    *       the DataDocument object representing a document to be created
    * @return json with the id of newly created document under "_id" key.
    * @throws DbException
    *       When there is an error working with the database.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    */
   @POST
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   public DataDocument createDocument(final DataDocument document) throws DbException, InvalidConstraintException {
      if (collectionCode == null || document == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      final DataDocument convertedDocument = collectionMetadataFacade.checkAndConvertAttributesValues(collectionCode, document);
      String id = documentFacade.createDocument(collectionCode, convertedDocument);

      final DataDocument idDocument = new DataDocument();
      idDocument.setId(id);

      return idDocument;
   }

   /**
    * Drops an existing document in given collection by its id.
    *
    * @param documentId
    *       the id of the document to drop
    * @throws DbException
    *       When there is an error working with the database.
    */
   @DELETE
   @Path("/{documentId}")
   public void dropDocument(final @PathParam("documentId") String documentId) throws DbException {
      if (collectionCode == null || documentId == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      documentFacade.dropDocument(collectionCode, documentId);
   }

   /**
    * Reads the specified document in given collection by its id.
    *
    * @param documentId
    *       the id of the read document
    * @return the DataDocument object representing the read document
    * @throws DbException
    *       When there is an error working with the database.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    */
   @GET
   @Path("/{documentId}")
   @Produces(MediaType.APPLICATION_JSON)
   public DataDocument readDocument(final @PathParam("documentId") String documentId) throws DbException, InvalidConstraintException {
      if (collectionCode == null || documentId == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return collectionMetadataFacade.decodeAttributeValues(collectionCode, documentFacade.readDocument(collectionCode, documentId));
   }

   /**
    * Modifies an existing document in given collection by its id and create collection if not exists.
    *
    * @param updatedDocument
    *       the DataDocument object representing a document with changes to update
    * @throws DbException
    *       When there is an error working with the data storage.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    */
   @PUT
   @Path("/")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateDocument(final DataDocument updatedDocument) throws DbException, InvalidConstraintException {
      if (collectionCode == null || updatedDocument == null || updatedDocument.getId() == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      final DataDocument convertedDocument = collectionMetadataFacade.checkAndConvertAttributesValues(collectionCode, updatedDocument);
      documentFacade.updateDocument(collectionCode, convertedDocument);
   }

   /**
    * Replace an existing document in given collection by its id and create collection if not exists.
    *
    * @param replaceDocument
    *       the DataDocument object representing a replacing document
    * @throws DbException
    *       When there is an error working with the data storage.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When the attribute values did not meet constraint requirements.
    */
   @PUT
   @Path("/replace/")
   @Consumes(MediaType.APPLICATION_JSON)
   public void replaceDocument(final DataDocument replaceDocument) throws DbException, InvalidConstraintException, InvalidValueException {
      if (collectionCode == null || replaceDocument == null || replaceDocument.getId() == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      final DataDocument convertedDocument = collectionMetadataFacade.checkAndConvertAttributesValues(collectionCode, replaceDocument);
      documentFacade.replaceDocument(collectionCode, convertedDocument);
   }

   /**
    * Put attribute and value to document metadata.
    *
    * @param documentId
    *       the id of the read document
    * @param attributeName
    *       the meta attribute to put
    * @param value
    *       the value of the given attribute
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   @POST
   @Path("/{documentId}/meta/{attributeName}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addDocumentMetadata(final @PathParam("documentId") String documentId, final @PathParam("attributeName") String attributeName, final Object value) throws DbException {
      if (collectionCode == null || documentId == null || attributeName == null || value == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      documentMetadataFacade.putDocumentMetadata(collectionCode, documentId, attributeName, value);
   }

   /**
    * Reads the metadata keys and values of specified document.
    *
    * @param documentId
    *       the id of the read document
    * @return the map where key is name of metadata attribute and its value
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   @GET
   @Path("/{documentId}/meta")
   @Produces(MediaType.APPLICATION_JSON)
   public Map<String, Object> readDocumentMetadata(final @PathParam("documentId") String documentId) throws DbException {
      if (collectionCode == null || documentId == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return documentMetadataFacade.readDocumentMetadata(collectionCode, documentId);
   }

   /**
    * Put attributes and its values to document metadata.
    *
    * @param documentId
    *       the id of the read document
    * @param metadata
    *       map with medatadata attributes and its values
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   @PUT
   @Path("/{documentId}/meta")
   @Consumes(MediaType.APPLICATION_JSON)
   public void updateDocumentMetadata(final @PathParam("documentId") String documentId, final DataDocument metadata) throws DbException {
      if (collectionCode == null || documentId == null || metadata == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      documentMetadataFacade.updateDocumentMetadata(collectionCode, documentId, metadata);
   }

   /**
    * Read all versions of the given document and returns it as a list.
    *
    * @param documentId
    *       id of the document
    * @return list of documents in different version
    * @throws DbException
    *       When there is an error working with the data storage.
    * @throws InvalidConstraintException
    *       When the constraint configuration was wrong.
    * @throws InvalidValueException
    *       When it was not possible to decode the attribute values.
    */
   @GET
   @Path("/{documentId}/versions")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> searchHistoryChanges(final @PathParam("documentId") String documentId) throws DbException, InvalidValueException, InvalidConstraintException {
      if (collectionCode == null || documentId == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      final List<DataDocument> docs = versionFacade.getDocumentVersions(collectionCode, documentId);
      final List<DataDocument> convertedDocs = new ArrayList<>();

      for (final DataDocument doc : docs) {
         convertedDocs.add(collectionMetadataFacade.decodeAttributeValues(collectionCode, doc));
      }

      return convertedDocs;
   }

   /**
    * Reverts old version of the given document.
    *
    * @param documentId
    *       id of document to revert
    * @param version
    *       old version to be reverted
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   @POST
   @Path("/{documentId}/versions/{version}")
   public void revertDocumentVersion(final @PathParam("documentId") String documentId, final @PathParam("version") int version) throws DbException {
      if (collectionCode == null || documentId == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      documentFacade.revertDocument(collectionCode, documentId, version);
   }

   /**
    * Drops specific document's attribute
    *
    * @param documentId
    *       id of document
    * @param attributeName
    *       attribute to delete
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   @DELETE
   @Path("/{documentId}/attribute/{attributeName}")
   public void dropDocumentAttribute(final @PathParam("documentId") String documentId, final @PathParam("attributeName") String attributeName) throws DbException {
      if (collectionCode == null || documentId == null || attributeName == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      documentFacade.dropAttribute(collectionCode, documentId, attributeName);
   }

   /**
    * Gets attribute names of a document.
    *
    * @param documentId
    *       id of document
    * @return set of document's attributes
    * @throws DbException
    *       When there is an error working with the data storage.
    */
   @GET
   @Path("/{documentId}/attributes/")
   public Set<String> readDocumentAttributes(final @PathParam("documentId") String documentId) throws DbException {
      if (collectionCode == null || documentId == null) {
         throw new BadRequestException();
      }

      if (!collectionMetadataFacade.hasRole(projectCode, collectionCode, LumeerConst.Security.ROLE_READ)) {
         throw new UnauthorizedAccessException();
      }

      return documentFacade.getDocumentAttributes(collectionCode, documentId);
   }

}
