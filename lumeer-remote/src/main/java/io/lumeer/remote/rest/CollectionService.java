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
   @Deprecated
   @Path("{collectionCode}/attributes")
   public List<JsonAttribute> getCollectionAttributes(@PathParam("collectionCode") String collectionCode) {
      List<Attribute> attributes = getCollection(collectionCode).getAttributes();
      return JsonAttribute.convert(attributes);
   }

   @GET
   public List<JsonCollection> getCollections(@QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
      Pagination pagination = new Pagination(page, pageSize);

      List<Collection> collections = collectionFacade.getCollections(pagination);
      return JsonCollection.convert(collections);
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
