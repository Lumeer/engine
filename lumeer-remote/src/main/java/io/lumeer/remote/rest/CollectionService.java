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
import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.ImportedCollection;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Rule;
import io.lumeer.core.facade.AuditFacade;
import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.core.facade.ImportFacade;
import io.lumeer.remote.rest.annotation.HealthCheck;

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
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/collections")
public class CollectionService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private AuditFacade auditFacade;

   @Inject
   private ImportFacade importFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @POST
   @HealthCheck
   public Collection createCollection(Collection collection) {
      return collectionFacade.createCollection(collection);
   }

   @PUT
   @Path("{collectionId:[0-9a-fA-F]{24}}")
   @HealthCheck
   public Collection updateCollection(@PathParam("collectionId") String collectionId, Collection collection) {
      return collectionFacade.updateCollection(collectionId, collection);
   }

   @DELETE
   @Path("{collectionId:[0-9a-fA-F]{24}}")
   public Response deleteCollection(@PathParam("collectionId") String collectionId) {
      collectionFacade.deleteCollection(collectionId);

      return Response.ok().link(getParentUri(collectionId), "parent").build();
   }

   @GET
   @Path("{collectionId:[0-9a-fA-F]{24}}")
   public Collection getCollection(@PathParam("collectionId") String collectionId) {
      return collectionFacade.getCollection(collectionId);
   }

   @GET
   public List<Collection> getCollections() {
      return collectionFacade.getAllCollections();
   }

   @POST
   @Path("{collectionId:[0-9a-fA-F]{24}}/import")
   public Collection importDocuments(@PathParam("collectionId") String collectionId, @QueryParam("format") String format, ImportedCollection importedCollection) {
      return importFacade.importDocuments(collectionId, format, importedCollection);
   }

   @GET
   @Deprecated
   @Path("{collectionId:[0-9a-fA-F]{24}}/attributes")
   public Set<Attribute> getCollectionAttributes(@PathParam("collectionId") String collectionId) {
      return getCollection(collectionId).getAttributes();
   }

   @PUT
   @Path("{collectionId:[0-9a-fA-F]{24}}/attributes/{attributeId}/default")
   public Response setDefaultAttribute(@PathParam("collectionId") String collectionId, @PathParam("attributeId") String attributeId) {
      collectionFacade.setDefaultAttribute(collectionId, attributeId);

      return Response.ok().link(getParentUri(attributeId), "parent").build();
   }

   @POST
   @Path("{collectionId:[0-9a-fA-F]{24}}/attributes")
   @HealthCheck
   public Set<Attribute> createCollectionAttributes(@PathParam("collectionId") String collectionId, List<Attribute> attributes) {
      return new HashSet<>(collectionFacade.createCollectionAttributes(collectionId, attributes));
   }

   @PUT
   @Path("{collectionId:[0-9a-fA-F]{24}}/attributes/{attributeId}")
   @HealthCheck
   public Attribute updateCollectionAttribute(@PathParam("collectionId") String collectionId, @PathParam("attributeId") String attributeId, Attribute attribute) {
      return collectionFacade.updateCollectionAttribute(collectionId, attributeId, attribute);
   }

   @DELETE
   @Path("{collectionId:[0-9a-fA-F]{24}}/attributes/{attributeId}")
   public Response deleteCollectionAttribute(@PathParam("collectionId") String collectionId, @PathParam("attributeId") String attributeId) {
      if (attributeId == null) {
         throw new BadRequestException("attributeId");
      }

      collectionFacade.deleteCollectionAttribute(collectionId, attributeId);

      return Response.ok().link(getParentUri(attributeId), "parent").build();
   }

   @PUT
   @Path("{collectionId:[0-9a-fA-F]{24}}/rule/{ruleId}")
   public Collection upsertRule(@PathParam("collectionId") String collectionId, @PathParam("ruleId") final String ruleId, Rule rule) {
      return collectionFacade.upsertRule(collectionId, ruleId, rule);
   }

   @POST
   @Path("{collectionId:[0-9a-fA-F]{24}}/rule/{ruleId}")
   public void runRule(@PathParam("collectionId") String collectionId, @PathParam("ruleId") final String ruleId) {
      final Collection collection = collectionFacade.getCollection(collectionId);
      collectionFacade.runRule(collection, ruleId);
   }

   @PUT
   @Path("{collectionId:[0-9a-fA-F]{24}}/purpose")
   public Collection updatePurpose(@PathParam("collectionId") String collectionId, CollectionPurpose purpose) {
      return collectionFacade.updatePurpose(collectionId, purpose);
   }

   @GET
   @Path("{collectionId:[0-9a-fA-F]{24}}/permissions")
   public Permissions getCollectionPermissions(@PathParam("collectionId") String collectionId) {
      return collectionFacade.getCollectionPermissions(collectionId);
   }

   @PUT
   @Path("{collectionId:[0-9a-fA-F]{24}}/permissions/users")
   public Set<Permission> updateUserPermission(@PathParam("collectionId") String collectionId, Set<Permission> userPermission) {
      return collectionFacade.updateUserPermissions(collectionId, userPermission);
   }

   @DELETE
   @Path("{collectionId:[0-9a-fA-F]{24}}/permissions/users/{userId:[0-9a-fA-F]{24}}")
   public Response removeUserPermission(@PathParam("collectionId") String collectionId, @PathParam("userId") String userId) {
      collectionFacade.removeUserPermission(collectionId, userId);

      return Response.ok().link(getParentUri("users", userId), "parent").build();
   }

   @PUT
   @Path("{collectionId:[0-9a-fA-F]{24}}/permissions/groups")
   public Set<Permission> updateGroupPermission(@PathParam("collectionId") String collectionId, Set<Permission> groupPermission) {
      return collectionFacade.updateGroupPermissions(collectionId, groupPermission);
   }

   @DELETE
   @Path("{collectionId:[0-9a-fA-F]{24}}/permissions/groups/{groupId:[0-9a-fA-F]{24}}")
   public Response removeGroupPermission(@PathParam("collectionId") String collectionId, @PathParam("groupId") String groupId) {
      collectionFacade.removeGroupPermission(collectionId, groupId);

      return Response.ok().link(getParentUri("groups", groupId), "parent").build();
   }

   @POST
   @Path("{collectionId:[0-9a-fA-F]{24}}/favorite")
   public Response addFavoriteCollection(@PathParam("collectionId") String collectionId) {
      collectionFacade.addFavoriteCollection(collectionId);

      return Response.ok().build();
   }

   @DELETE
   @Path("{collectionId:[0-9a-fA-F]{24}}/favorite")
   public Response removeFavoriteCollection(@PathParam("collectionId") String collectionId) {
      collectionFacade.removeFavoriteCollection(collectionId);

      return Response.ok().build();
   }

   @GET
   @Path("{collectionId:[0-9a-fA-F]{24}}/audit")
   public List<AuditRecord> getAuditLogs(@PathParam("collectionId") String collectionId) {
      return auditFacade.getAuditRecordsForCollection(collectionId);
   }

}
