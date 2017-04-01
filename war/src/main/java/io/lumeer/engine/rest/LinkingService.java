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
import io.lumeer.engine.api.exception.CollectionNotFoundException;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.api.exception.DocumentNotFoundException;
import io.lumeer.engine.api.exception.UnauthorizedAccessException;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.LinkingFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.rest.dao.LinkDao;
import io.lumeer.engine.rest.dao.LinkTypeDao;
import io.lumeer.engine.util.ErrorMessageBuilder;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
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
@Path("/{organisation}/{project}/collections/{collectionName}/links")
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

   @PathParam("organisation")
   private String organisationId;

   @PathParam("project")
   private String projectId;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @PostConstruct
   public void init() {
      organizationFacade.setOrganizationId(organisationId);
      projectFacade.setCurrentProjectId(projectId);
   }

   /**
    * Gets all types of links between given collections.
    *
    * @param collectionName
    *       The name of the target/source collection (depending on the value of linkDirection}.
    * @param linkDirection
    *       Which link direction to work with.
    * @return All link types between given collections.
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public List<LinkTypeDao> getLinkTypes(final @PathParam("collectionName") String collectionName, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      checkCollectionForRead(internalCollectionName);

      final List<LinkTypeDao> links = new ArrayList<>();
      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readLinkTypes(internalCollectionName, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readLinkTypes(internalCollectionName, LumeerConst.Linking.LinkDirection.TO));
      }

      // translate internal collection names
      for (LinkTypeDao linkTypeDao : links) {
         linkTypeDao.setFromCollection(getOriginalName(linkTypeDao.getFromCollection()));
         linkTypeDao.setToCollection(getOriginalName(linkTypeDao.getToCollection()));
      }

      return links;
   }

   /**
    * Gets all links between given collections.
    *
    * @param collectionName
    *       The name of the source/target collection.
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
   public List<LinkDao> getLinks(final @PathParam("collectionName") String collectionName, final @PathParam("role") String role, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      checkCollectionForRead(internalCollectionName);
      final List<LinkDao> links = new ArrayList<>();

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readLinks(internalCollectionName, role, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readLinks(internalCollectionName, role, LumeerConst.Linking.LinkDirection.TO));
      }

      // translate internal collection names
      for (LinkDao linkDao : links) {
         linkDao.setFromCollection(getOriginalName(linkDao.getFromCollection()));
         linkDao.setToCollection(getOriginalName(linkDao.getToCollection()));
      }

      return links;
   }

   /**
    * Gets all documents linked from/to the given collection and document id with the given role.
    *
    * @param collectionName
    *       The source/target collection.
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
   public List<DataDocument> getLinkedDocuments(final @PathParam("collectionName") String collectionName, final @PathParam("role") String role, final @PathParam("id") String documentId, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      checkCollectionForRead(internalCollectionName);
      final List<DataDocument> links = new ArrayList<>();

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readDocumentLinksDocs(internalCollectionName, documentId, role, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readDocumentLinksDocs(internalCollectionName, documentId, role, LumeerConst.Linking.LinkDirection.TO));
      }

      return links;
   }

   /**
    * Get links between source and target documents of the given role.
    *
    * @param collectionName
    *       The source/target collection.
    * @param targetCollection
    *       The target collection.
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
   public List<LinkDao> getDocumentsLinks(final @PathParam("collectionName") String collectionName, final @PathParam("targetCollection") String targetCollection, final @PathParam("role") String role, final @PathParam("id") String documentId, final @PathParam("targetId") String targetDocumentId, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection)
         throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      checkCollectionForRead(internalCollectionName);
      String internalTargetCollectionName = getInternalName(targetCollection);

      final List<LinkDao> links = new ArrayList<>();
      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readDocByDocLinks(internalCollectionName, documentId, internalTargetCollectionName, targetDocumentId, role, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readDocByDocLinks(internalCollectionName, documentId, internalTargetCollectionName, targetDocumentId, role, LumeerConst.Linking.LinkDirection.TO));
      }

      return links;
   }

   /**
    * Removes all documents linked from/to the given collection and document id with the given role.
    *
    * @param collectionName
    *       The source/target collection.
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
   public void deleteLinks(final @PathParam("collectionName") String collectionName, final @PathParam("role") String role, final @PathParam("id") String documentId, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      checkCollectionForWrite(internalCollectionName);

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         linkingFacade.dropAllDocumentLinks(internalCollectionName, documentId, role, LumeerConst.Linking.LinkDirection.FROM);
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         linkingFacade.dropAllDocumentLinks(internalCollectionName, documentId, role, LumeerConst.Linking.LinkDirection.TO);
      }
   }

   /**
    * Removes given document linked from/to the given collection and document id with the given role.
    *
    * @param collectionName
    *       The source/target collection.
    * @param targetCollection
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
   public void deleteLink(final @PathParam("collectionName") String collectionName, final @PathParam("targetCollection") String targetCollection, final @PathParam("role") String role, final @PathParam("id") String documentId, final @PathParam("targetId") String targetDocumentId, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection)
         throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      checkCollectionForWrite(internalCollectionName);
      String internalTargetCollectionName = getInternalName(targetCollection);
      checkCollectionForWrite(internalTargetCollectionName);

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         linkingFacade.dropDocWithDocLink(internalCollectionName, documentId, internalTargetCollectionName, targetDocumentId, role, LumeerConst.Linking.LinkDirection.FROM);
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         linkingFacade.dropDocWithDocLink(internalCollectionName, documentId, internalTargetCollectionName, targetDocumentId, role, LumeerConst.Linking.LinkDirection.TO);
      }
   }

   /**
    * Adds a new link between given collections with the given role. Also links particular documents when these are specified.
    *
    * @param collectionName
    *       The source collection.
    * @param targetCollection
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
   public void addLink(final @PathParam("collectionName") String collectionName, final @PathParam("targetCollection") String targetCollection, final @PathParam("role") String role, final @PathParam("id") String fromId, final @PathParam("targetId") String toId, final DataDocument attributes, final @QueryParam("direction") @DefaultValue("FROM") LumeerConst.Linking.LinkDirection linkDirection)
         throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      checkCollectionForWrite(internalCollectionName);
      String internalTargetCollectionName = getInternalName(targetCollection);
      checkCollectionForWrite(internalTargetCollectionName);

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         hasDocumentRoleNotNull(internalCollectionName, fromId, internalTargetCollectionName, toId, role);
         linkingFacade.createDocWithDocLink(internalCollectionName, fromId, internalTargetCollectionName, toId, attributes, role, LumeerConst.Linking.LinkDirection.FROM);
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         hasDocumentRoleNotNull(internalCollectionName, fromId, internalTargetCollectionName, toId, role);
         linkingFacade.createDocWithDocLink(internalCollectionName, fromId, internalTargetCollectionName, toId, attributes, role, LumeerConst.Linking.LinkDirection.TO);
      }

   }

   private void hasDocumentRoleNotNull(final String firstCollectionName, final String firstDocumentId, final String secondCollectionName, final String secondDocumentId, final String role) throws DocumentNotFoundException {
      if (!(dataStorage.collectionHasDocument(firstCollectionName, dataStorageDialect.documentIdFilter(firstDocumentId))
            && dataStorage.collectionHasDocument(secondCollectionName, dataStorageDialect.documentIdFilter(secondDocumentId)))) {
         throw new DocumentNotFoundException(ErrorMessageBuilder.documentNotFoundString());
      }
      if (role == null) {
         throw new IllegalArgumentException(ErrorMessageBuilder.paramCanNotBeNullString(LumeerConst.Linking.MainTable.ATTR_ROLE));
      }
   }

   private void checkCollectionForRead(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!collectionMetadataFacade.checkCollectionForRead(collectionName, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
   }

   private void checkCollectionForWrite(final String collectionName) throws CollectionNotFoundException, UnauthorizedAccessException {
      if (!dataStorage.hasCollection(collectionName)) {
         throw new CollectionNotFoundException(ErrorMessageBuilder.collectionNotFoundString(collectionName));
      }
      if (!collectionMetadataFacade.checkCollectionForWrite(collectionName, userFacade.getUserEmail())) {
         throw new UnauthorizedAccessException();
      }
   }

   private String getInternalName(String collectionOriginalName) throws DbException {
      return collectionMetadataFacade.getInternalCollectionName(collectionOriginalName);
   }

   private String getOriginalName(final String fromCollection) throws DbException {
      return collectionMetadataFacade.getOriginalCollectionName(fromCollection);
   }
}
