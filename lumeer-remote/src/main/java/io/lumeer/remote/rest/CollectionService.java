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

import java.net.URI;
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
   public Response createCollection(JsonCollection collection) {
      Collection storedCollection = collectionFacade.createCollection(collection);

      URI resourceUri = getResourceUri(storedCollection.getCode());
      return Response.created(resourceUri).build();
   }

   @PUT
   @Path("{collectionCode}")
   public Response updateCollection(@PathParam("collectionCode") String collectionCode, JsonCollection collection) {
      Collection storedCollection = collectionFacade.updateCollection(collectionCode, collection);

      return Response.ok(JsonCollection.convert(storedCollection)).build();
   }

   @DELETE
   @Path("{collectionCode}")
   public Response deleteCollection(@PathParam("collectionCode") String collectionCode) {
      collectionFacade.deleteCollection(collectionCode);

      return Response.ok().link(getParentUri(collectionCode), "parent").build();
   }

   @GET
   @Path("{collectionCode}")
   public JsonCollection getCollection(@PathParam("collectionCode") String collectionCode) {
      Collection collection = collectionFacade.getCollection(collectionCode);
      return JsonCollection.convert(collection);
   }

   @GET
   public List<JsonCollection> getCollections(@QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
      Pagination pagination = new Pagination(page, pageSize);

      List<Collection> collections = collectionFacade.getCollections(pagination);
      return JsonCollection.convert(collections);
   }

   @GET
   @Path("names")
   public Set<String> getCollectionNames() {
      return collectionFacade.getCollectionNames();
   }

   @GET
   @Deprecated
   @Path("{collectionCode}/attributes")
   public Set<JsonAttribute> getCollectionAttributes(@PathParam("collectionCode") String collectionCode) {
      Set<Attribute> attributes = getCollection(collectionCode).getAttributes();
      return JsonAttribute.convert(attributes);
   }

   @PUT
   @Path("{collectionCode}/attributes/{attributeFullName}")
   public JsonAttribute updateCollectionAttribute(@PathParam("collectionCode") String collectionCode, @PathParam("attributeFullName") String attributeFullName, JsonAttribute attribute) {
      Attribute storedAttribute = collectionFacade.updateCollectionAttribute(collectionCode, attributeFullName, attribute);

      return JsonAttribute.convert(storedAttribute);
   }

   @DELETE
   @Path("{collectionCode}/attributes/{attributeFullName}")
   public Response deleteCollectionAttribute(@PathParam("collectionCode") String collectionCode, @PathParam("attributeFullName") String attributeFullName) {
      if (attributeFullName == null) {
         throw new BadRequestException("attributeFullName");
      }

      collectionFacade.deleteCollectionAttribute(collectionCode, attributeFullName);

      return Response.ok().link(getParentUri(attributeFullName), "parent").build();
   }

   @GET
   @Path("{collectionCode}/permissions")
   public JsonPermissions getCollectionPermissions(@PathParam("collectionCode") String code) {
      Permissions permissions = collectionFacade.getCollectionPermissions(code);
      return JsonPermissions.convert(permissions);
   }

   @PUT
   @Path("{collectionCode}/permissions/users")
   public Set<JsonPermission> updateUserPermission(@PathParam("collectionCode") String code, JsonPermission userPermission) {
      Set<Permission> storedUserPermissions = collectionFacade.updateUserPermissions(code, userPermission);
      return JsonPermission.convert(storedUserPermissions);
   }

   @DELETE
   @Path("{collectionCode}/permissions/users/{user}")
   public Response removeUserPermission(@PathParam("collectionCode") String code, @PathParam("user") String user) {
      collectionFacade.removeUserPermission(code, user);

      return Response.ok().link(getParentUri("users", user), "parent").build();
   }

   @PUT
   @Path("{collectionCode}/permissions/groups")
   public Set<JsonPermission> updateGroupPermission(@PathParam("collectionCode") String code, JsonPermission groupPermission) {
      Set<Permission> storedGroupPermissions = collectionFacade.updateGroupPermissions(code, groupPermission);
      return JsonPermission.convert(storedGroupPermissions);
   }

   @DELETE
   @Path("{collectionCode}/permissions/groups/{group}")
   public Response removeGroupPermission(@PathParam("collectionCode") String code, @PathParam("group") String group) {
      collectionFacade.removeGroupPermission(code, group);

      return Response.ok().link(getParentUri("groups", group), "parent").build();
   }

}
