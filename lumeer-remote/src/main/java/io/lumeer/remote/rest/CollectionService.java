/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.core.facade.ViewFacade;

import java.util.HashSet;
import java.util.List;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationId}/projects/{projectId}/collections")
public class CollectionService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private ViewFacade viewFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationId, projectId);
   }

   @POST
   public Collection createCollection(Collection collection) {
      Collection storedCollection = collectionFacade.createCollection(collection);
      storedCollection.setFavorite(false);
      return storedCollection;
   }

   @PUT
   @Path("{collectionId}")
   public Collection updateCollection(@PathParam("collectionId") String collectionId, Collection collection) {
      Collection storedCollection = collectionFacade.updateCollection(collectionId, collection);
      storedCollection.setFavorite(collectionFacade.isFavorite(storedCollection.getId()));
      return storedCollection;
   }

   @DELETE
   @Path("{collectionId}")
   public Response deleteCollection(@PathParam("collectionId") String collectionId) {
      collectionFacade.deleteCollection(collectionId);

      return Response.ok().link(getParentUri(collectionId), "parent").build();
   }

   @GET
   @Path("{collectionId}")
   public Collection getCollection(@PathParam("collectionId") String collectionId) {
      Collection collection = collectionFacade.getCollection(collectionId);
      collection.setFavorite(collectionFacade.isFavorite(collection.getId()));
      return collection;
   }

   @GET
   public List<Collection> getCollections(@QueryParam("fromViews") Boolean includeViewCollections) {
      Set<String> favoriteCollectionIds = collectionFacade.getFavoriteCollectionsIds();
      List<Collection> collections = collectionFacade.getCollections();
      collections.forEach(collection -> collection.setFavorite(favoriteCollectionIds.contains(collection.getId())));

      if (includeViewCollections != null && includeViewCollections && !isManager()) {
         collections.addAll(viewFacade.getViewsCollections());
      }

      return collections;
   }

   @GET
   @Deprecated
   @Path("{collectionId}/attributes")
   public Set<Attribute> getCollectionAttributes(@PathParam("collectionId") String collectionId) {
      return getCollection(collectionId).getAttributes();
   }

   @PUT
   @Path("{collectionId}/attributes/{attributeId}/default")
   public Response setDefaultAttribute(@PathParam("collectionId") String collectionId, @PathParam("attributeId") String attributeId) {
      collectionFacade.setDefaultAttribute(collectionId, attributeId);

      return Response.ok().link(getParentUri(attributeId), "parent").build();
   }

   @POST
   @Path("{collectionId}/attributes")
   public Set<Attribute> createCollectionAttributes(@PathParam("collectionId") String collectionId, List<Attribute> attributes) {
      return new HashSet<>(collectionFacade.createCollectionAttributes(collectionId, attributes));
   }

   @PUT
   @Path("{collectionId}/attributes/{attributeId}")
   public Attribute updateCollectionAttribute(@PathParam("collectionId") String collectionId, @PathParam("attributeId") String attributeId, Attribute attribute) {
      return collectionFacade.updateCollectionAttribute(collectionId, attributeId, attribute);
   }

   @DELETE
   @Path("{collectionId}/attributes/{attributeId}")
   public Response deleteCollectionAttribute(@PathParam("collectionId") String collectionId, @PathParam("attributeId") String attributeId) {
      if (attributeId == null) {
         throw new BadRequestException("attributeId");
      }

      collectionFacade.deleteCollectionAttribute(collectionId, attributeId);

      return Response.ok().link(getParentUri(attributeId), "parent").build();
   }

   @GET
   @Path("{collectionId}/permissions")
   public Permissions getCollectionPermissions(@PathParam("collectionId") String collectionId) {
      return collectionFacade.getCollectionPermissions(collectionId);
   }

   @PUT
   @Path("{collectionId}/permissions/users")
   public Set<Permission> updateUserPermission(@PathParam("collectionId") String collectionId, Set<Permission> userPermission) {
      return collectionFacade.updateUserPermissions(collectionId, userPermission);
   }

   @DELETE
   @Path("{collectionId}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("collectionId") String collectionId, @PathParam("userId") String userId) {
      collectionFacade.removeUserPermission(collectionId, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{collectionId}/permissions/groups")
   public Set<Permission> updateGroupPermission(@PathParam("collectionId") String collectionId, Set<Permission> groupPermission) {
      return collectionFacade.updateGroupPermissions(collectionId, groupPermission);
   }

   @DELETE
   @Path("{collectionId}/permissions/groups/{groupId}")
   public Response removeGroupPermission(@PathParam("collectionId") String collectionId, @PathParam("groupId") String groupId) {
      collectionFacade.removeGroupPermission(collectionId, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

   @POST
   @Path("{collectionId}/favorite")
   public Response addFavoriteCollection(@PathParam("collectionId") String collectionId) {
      collectionFacade.addFavoriteCollection(collectionId);

      return Response.ok().build();
   }

   @DELETE
   @Path("{collectionId}/favorite")
   public Response removeFavoriteCollection(@PathParam("collectionId") String collectionId) {
      collectionFacade.removeFavoriteCollection(collectionId);

      return Response.ok().build();
   }

}
