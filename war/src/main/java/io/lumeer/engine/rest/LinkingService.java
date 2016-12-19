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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.LinkingFacade;
import io.lumeer.engine.rest.dao.LinkDao;
import io.lumeer.engine.rest.dao.LinkTypeDao;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
 */
@Path("/collections/{collectionName}/links")
public class LinkingService {

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private LinkingFacade linkingFacade;

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
   public List<LinkTypeDao> getLinkTypes(final @PathParam("collectionName") String collectionName, final @QueryParam("direction") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);

      final List<LinkTypeDao> links = new ArrayList<>();
      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.getLinkTypes(internalCollectionName, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.getLinkTypes(internalCollectionName, LumeerConst.Linking.LinkDirection.TO));
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
   public List<LinkDao> getLinks(final @PathParam("collectionName") String collectionName, final @PathParam("role") String role, final @QueryParam("direction") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      final List<LinkDao> links = new ArrayList<>();

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.getLinks(internalCollectionName, role, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.getLinks(internalCollectionName, role, LumeerConst.Linking.LinkDirection.TO));
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
   public List<DataDocument> getLinkedDocuments(final @PathParam("collectionName") String collectionName, final @PathParam("role") String role, final @PathParam("id") String documentId, final @QueryParam("direction") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      final List<DataDocument> links = new ArrayList<>();

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         links.addAll(linkingFacade.readDocumentLinks(internalCollectionName, documentId, role, LumeerConst.Linking.LinkDirection.FROM));
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         links.addAll(linkingFacade.readDocumentLinks(internalCollectionName, documentId, role, LumeerConst.Linking.LinkDirection.TO));
      }

      return links;
   }

   /**
    * Get links between source and target documents of the given role.
    *
    * @param collectionName
    *       The source/target collection.
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
   @Path("/{role}/documents/{id}/target/{targetId}")
   @Produces(MediaType.APPLICATION_JSON)
   public List<LinkDao> getLinkedDocuments(final @PathParam("collectionName") String collectionName, final @PathParam("role") String role, final @PathParam("id") String documentId, final @PathParam("targetId") String targetDocumentId, final @QueryParam("direction") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      final List<LinkDao> links = new ArrayList<>();
      // TODO translate user collection names

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         // TODO přidat do links dokumenty z vazby role vychházející z kolekce collectionName z dokumentu _id kamkoliv
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         // TODO přidat do links dokumenty z vazby role směřující do kolekce collectionName do dokumentu _id odkudkoliv
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
   public void deleteLinks(final @PathParam("collectionName") String collectionName, final @PathParam("role") String role, final @PathParam("id") String documentId, final @QueryParam("direction") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);

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
   @Path("/{role}/documents/{id}/targets/{targetId}")
   @Produces(MediaType.APPLICATION_JSON)
   public void deleteLink(final @PathParam("collectionName") String collectionName, final @PathParam("role") String role, final @PathParam("id") String documentId, final @PathParam("targetId") String targetDocumentId, final @QueryParam("direction") LumeerConst.Linking.LinkDirection linkDirection) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      // TODO translate user collection names
      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.FROM) {
         // TODO smazat z links dokument targetId z vazby role vychházející z kolekce collectionName z dokumentu _id
      }

      if (linkDirection == null || linkDirection == LumeerConst.Linking.LinkDirection.BOTH || linkDirection == LumeerConst.Linking.LinkDirection.TO) {
         // TODO smazat z links dokument targetId z vazby role směřující do kolekce collectionName do dokumentu _id
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
    * @throws DbException
    *       When there is an issue when communicating with the data storage.
    */
   @POST
   @Path("/{role}/collections/{targetCollection}")
   @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   public void addLink(final @PathParam("collectionName") String collectionName, final @PathParam("targetCollection") String targetCollection, final @PathParam("role") String role, final @QueryParam("fromId") String fromId, final @QueryParam("toId") String toId, final DataDocument attributes) throws DbException {
      String internalCollectionName = getInternalName(collectionName);
      String internalTargetCollectionName = getInternalName(targetCollection);

      // it is possible to use the method without ids just to create the link with the given role
      if (!((fromId == null || fromId.isEmpty()) && (toId == null || toId.isEmpty()))) {
         linkingFacade.createDocWithDocLink(internalCollectionName, fromId, internalTargetCollectionName, toId, attributes, role, LumeerConst.Linking.LinkDirection.FROM);
      }
   }

   private String getInternalName(String collectionOriginalName) throws DbException {
      return collectionMetadataFacade.getInternalCollectionName(collectionOriginalName);
   }
}
