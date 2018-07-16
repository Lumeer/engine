/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.core.facade.CollectionFacade;

import java.util.HashSet;
import java.util.List;
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
import javax.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationCode}/projects/{projectCode}/collections")
public class CollectionService extends AbstractService {

   @PathParam("organizationCode")
   private String organizationCode;

   @PathParam("projectCode")
   private String projectCode;

   @Inject
   private CollectionFacade collectionFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspace(organizationCode, projectCode);
   }

   @POST
   public Collection createCollection(JsonCollection collection) {
      Collection storedCollection = collectionFacade.createCollection(collection);

      JsonCollection returnCollection = JsonCollection.convert(storedCollection);
      returnCollection.setFavorite(false);

      return returnCollection;
   }

   @PUT
   @Path("{collectionId}")
   public Collection updateCollection(@PathParam("collectionId") String collectionId, JsonCollection collection) {
      Collection storedCollection = collectionFacade.updateCollection(collectionId, collection);

      JsonCollection returnCollection = JsonCollection.convert(storedCollection);
      returnCollection.setFavorite(collectionFacade.isFavorite(returnCollection.getId()));

      return returnCollection;
   }

   @DELETE
   @Path("{collectionId}")
   public Response deleteCollection(@PathParam("collectionId") String collectionId) {
      collectionFacade.deleteCollection(collectionId);

      return Response.ok().link(getParentUri(collectionId), "parent").build();
   }

   @GET
   @Path("{collectionId}")
   public JsonCollection getCollection(@PathParam("collectionId") String collectionId) {
      Collection collection = collectionFacade.getCollection(collectionId);

      JsonCollection returnCollection = JsonCollection.convert(collection);
      returnCollection.setFavorite(collectionFacade.isFavorite(returnCollection.getId()));

      return returnCollection;
   }

   @GET
   public List<JsonCollection> getCollections(@QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
      Pagination pagination = new Pagination(page, pageSize);

      Set<String> favoriteCollectionIds = collectionFacade.getFavoriteCollectionsIds();
      return collectionFacade.getCollections(pagination).stream()
                             .map(coll -> {
                                JsonCollection collection = JsonCollection.convert(coll);
                                collection.setFavorite(favoriteCollectionIds.contains(collection.getId()));
                                return collection;
                             })
                             .collect(Collectors.toList());
   }

   @GET
   @Path("info/names")
   public Set<String> getCollectionNames() {
      return collectionFacade.getCollectionNames();
   }

   @GET
   @Deprecated
   @Path("{collectionId}/attributes")
   public Set<JsonAttribute> getCollectionAttributes(@PathParam("collectionId") String collectionId) {
      Set<Attribute> attributes = getCollection(collectionId).getAttributes();
      return JsonAttribute.convert(attributes);
   }

   @PUT
   @Path("{collectionId}/attributes/{attributeId}/default")
   public Response setDefaultAttribute(@PathParam("collectionId") String collectionId, @PathParam("attributeId") String attributeId) {
      collectionFacade.setDefaultAttribute(collectionId, attributeId);

      return Response.ok().link(getParentUri(attributeId), "parent").build();
   }

   @POST
   @Path("{collectionId}/attributes")
   public Set<JsonAttribute> createCollectionAttributes(@PathParam("collectionId") String collectionId, List<JsonAttribute> attributes) {
      Set<Attribute> storedAttributes = new HashSet<>(collectionFacade.createCollectionAttributes(collectionId, attributes));

      return JsonAttribute.convert(storedAttributes);
   }

   @PUT
   @Path("{collectionId}/attributes/{attributeId}")
   public JsonAttribute updateCollectionAttribute(@PathParam("collectionId") String collectionId, @PathParam("attributeId") String attributeId, JsonAttribute attribute) {
      Attribute storedAttribute = collectionFacade.updateCollectionAttribute(collectionId, attributeId, attribute);

      return JsonAttribute.convert(storedAttribute);
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
   public JsonPermissions getCollectionPermissions(@PathParam("collectionId") String collectionId) {
      Permissions permissions = collectionFacade.getCollectionPermissions(collectionId);
      return JsonPermissions.convert(permissions);
   }

   @PUT
   @Path("{collectionId}/permissions/users")
   public Set<JsonPermission> updateUserPermission(@PathParam("collectionId") String collectionId, JsonPermission... userPermission) {
      Set<Permission> storedUserPermissions = collectionFacade.updateUserPermissions(collectionId, userPermission);
      return JsonPermission.convert(storedUserPermissions);
   }

   @DELETE
   @Path("{collectionId}/permissions/users/{userId}")
   public Response removeUserPermission(@PathParam("collectionId") String collectionId, @PathParam("userId") String userId) {
      collectionFacade.removeUserPermission(collectionId, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{collectionId}/permissions/groups")
   public Set<JsonPermission> updateGroupPermission(@PathParam("collectionId") String collectionId, JsonPermission... groupPermission) {
      Set<Permission> storedGroupPermissions = collectionFacade.updateGroupPermissions(collectionId, groupPermission);
      return JsonPermission.convert(storedGroupPermissions);
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
