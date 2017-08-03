/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.LinkInstance;
import io.lumeer.engine.api.dto.LinkType;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.LinkingFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Takes care of links between documents.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
@Path("/organizations/{organization}/projects/{project}/collections/{collection}/links")
public class LinkingService {

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private LinkingFacade linkingFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

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

   @Inject
   private SecurityFacade securityFacade;

   @PostConstruct
   public void init() throws DbException {
      organizationFacade.setOrganizationCode(organisationCode);
      projectFacade.setCurrentProjectCode(projectCode);
   }

   /**
    * Gets all types of links between given collections.
    *
    * @param linkDirection
    *       Which link direction to work with.
    * @return All link types between given collections.
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<LinkType> getLinkTypes(final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      if (!securityFacade.hasCollectionRole(projectCode, collectionCode, LumeerConst.Security.ROLE_SHARE)) {
         throw new UnauthorizedAccessException();
      }

      final List<LinkType> links = new ArrayList<>();
      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readLinkTypesForCollection(collectionCode, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readLinkTypesForCollection(collectionCode, LumeerConst.Linking.LinkDirection.TO));
      }

      return links;
   }

   /**
    * Gets all links between given collections.
    *
    * @param role
    *       The role of the link.
    * @param linkDirection
    *       Which link direction to work with.
    * @return All links of given role from/to the given collection.
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @GET
   @Path("/{role}")
   @Produces(MediaType.APPLICATION_JSON)
   public List<LinkInstance> getLinks(final @PathParam("role") String role, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      if (!securityFacade.hasCollectionRole(projectCode, collectionCode, LumeerConst.Security.ROLE_SHARE)) {
         throw new UnauthorizedAccessException();
      }

      final List<LinkInstance> links = new ArrayList<>();

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readLinkInstancesForCollection(collectionCode, role, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readLinkInstancesForCollection(collectionCode, role, LumeerConst.Linking.LinkDirection.TO));
      }

      return links;
   }

   /**
    * Gets all documents linked from/to the given collection and document id with the given role.
    *
    * @param role
    *       The link role.
    * @param documentId
    *       The source/target document id.
    * @param linkDirection
    *       Which link direction to work with.
    * @return Required links.
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @GET
   @Path("/{role}/documents/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public List<DataDocument> getLinkedDocuments(final @PathParam("role") String role, final @PathParam("id") String documentId, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      if (!securityFacade.hasCollectionRole(projectCode, collectionCode, LumeerConst.Security.ROLE_SHARE)) {
         throw new UnauthorizedAccessException();
      }

      final List<DataDocument> links = new ArrayList<>();

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readLinkedDocumentsForDocument(collectionCode, documentId, role, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readLinkedDocumentsForDocument(collectionCode, documentId, role, LumeerConst.Linking.LinkDirection.TO));
      }

      return links;
   }

   /**
    * Get links between source and target documents of the given role.
    *
    * @param targetCollectionCode
    *       The target collection code.
    * @param role
    *       The link role.
    * @param documentId
    *       The source/target document id.
    * @param targetDocumentId
    *       The document that the link leads to.
    * @param linkDirection
    *       Which link direction to work with.
    * @return Required links.
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @GET
   @Path("/{role}/collections/{targetCollection}/documents/{id}/target/{targetId}")
   @Produces(MediaType.APPLICATION_JSON)
   public List<LinkInstance> getDocumentsLinks(final @PathParam("targetCollection") String targetCollectionCode, final @PathParam("role") String role, final @PathParam("id") String documentId, final @PathParam("targetId") String targetDocumentId,
         final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection)
         throws DbException {
      if (targetCollectionCode == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasCollectionRole(projectCode, collectionCode, LumeerConst.Security.ROLE_SHARE)
            || !securityFacade.hasCollectionRole(projectCode, targetCollectionCode, LumeerConst.Security.ROLE_SHARE)) {
         throw new UnauthorizedAccessException();
      }

      final List<LinkInstance> links = new ArrayList<>();
      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readLinkInstancesBetweenDocuments(collectionCode, documentId, targetCollectionCode, targetDocumentId, role, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readLinkInstancesBetweenDocuments(collectionCode, documentId, targetCollectionCode, targetDocumentId, role, LumeerConst.Linking.LinkDirection.TO));
      }

      return links;
   }

   /**
    * Removes all documents linked from/to the given collection and document id with the given role.
    *
    * @param role
    *       The link role.
    * @param documentId
    *       The source/target document id.
    * @param linkDirection
    *       Which link direction to work with.
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @DELETE
   @Path("/{role}/documents/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public void deleteLinks(final @PathParam("role") String role, final @PathParam("id") String documentId, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      if (!securityFacade.hasCollectionRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         linkingFacade.dropLinksForDocument(collectionCode, documentId, role, LumeerConst.Linking.LinkDirection.FROM);
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         linkingFacade.dropLinksForDocument(collectionCode, documentId, role, LumeerConst.Linking.LinkDirection.TO);
      }
   }

   /**
    * Removes given document linked from/to the given collection and document id with the given role.
    *
    * @param targetCollectionCode
    *       The target collection.
    * @param role
    *       The link role.
    * @param documentId
    *       The source/target document id.
    * @param targetDocumentId
    *       The target/source document id.
    * @param linkDirection
    *       Which link direction to work with.
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @DELETE
   @Path("/{role}/collections/{targetCollection}/documents/{id}/targets/{targetId}")
   @Produces(MediaType.APPLICATION_JSON)
   public void deleteLink(final @PathParam("targetCollection") String targetCollectionCode, final @PathParam("role") String role, final @PathParam("id") String documentId, final @PathParam("targetId") String targetDocumentId, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection)
         throws DbException {
      if (targetCollectionCode == null) {
         throw new BadRequestException();
      }

      if (!securityFacade.hasCollectionRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)
            || !securityFacade.hasCollectionRole(projectCode, targetCollectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         linkingFacade.dropLinksBetweenDocuments(collectionCode, documentId, targetCollectionCode, targetDocumentId, role, LumeerConst.Linking.LinkDirection.FROM);
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         linkingFacade.dropLinksBetweenDocuments(collectionCode, documentId, targetCollectionCode, targetDocumentId, role, LumeerConst.Linking.LinkDirection.TO);
      }
   }

   /**
    * Adds a new link between given collections with the given role. Also links particular documents when these are specified.
    *
    * @param targetCollectionCode
    *       The target collection.
    * @param role
    *       The role name.
    * @param fromId
    *       The source document id.
    * @param toId
    *       The target document id.
    * @param attributes
    *       The link attributes.
    * @param linkDirection
    *       Which link direction to work with.
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @POST
   @Path("/{role}/collections/{targetCollection}/documents/{id}/targets/{targetId}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addLink(final @PathParam("targetCollection") String targetCollectionCode, final @PathParam("role") String role, final @PathParam("id") String fromId, final @PathParam("targetId") String toId, final DataDocument attributes, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection)
         throws DbException {
      if (targetCollectionCode == null || role == null) {
         throw new BadRequestException();
      }

      if (!hasDocument(collectionCode, fromId) || !hasDocument(targetCollectionCode, toId)) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }

      if (!securityFacade.hasCollectionRole(projectCode, collectionCode, LumeerConst.Security.ROLE_WRITE)
            || !securityFacade.hasCollectionRole(projectCode, targetCollectionCode, LumeerConst.Security.ROLE_WRITE)) {
         throw new UnauthorizedAccessException();
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         linkingFacade.createLinkInstanceBetweenDocuments(collectionCode, fromId, targetCollectionCode, toId, attributes, role, LumeerConst.Linking.LinkDirection.FROM);
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         linkingFacade.createLinkInstanceBetweenDocuments(collectionCode, fromId, targetCollectionCode, toId, attributes, role, LumeerConst.Linking.LinkDirection.TO);
      }

   }

   private boolean hasDocument(final String collectionName, final String documentId) {
      return dataStorage.collectionHasDocument(collectionName, dataStorageDialect.documentIdFilter(documentId));
   }

}
