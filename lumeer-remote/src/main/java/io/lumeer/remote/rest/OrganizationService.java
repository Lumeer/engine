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

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.core.facade.OrganizationFacade;

import java.net.URI;
import java.util.List;
import java.util.Set;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations")
public class OrganizationService extends AbstractService {

   @Inject
   private OrganizationFacade organizationFacade;

   @POST
   public Response createOrganization(JsonOrganization organization) {
      Organization storedOrganization = organizationFacade.createOrganization(organization);

      URI resourceUri = getResourceUri(storedOrganization.getCode());
      return Response.created(resourceUri).build();
   }

   @PUT
   @Path("{organizationCode}")
   public Response updateOrganization(@PathParam("organizationCode") String organizationCode, JsonOrganization organization) {
      Organization storedOrganization = organizationFacade.updateOrganization(organizationCode, organization);

      return Response.ok(JsonOrganization.convert(storedOrganization)).build();
   }

   @DELETE
   @Path("{organizationCode}")
   public Response deleteOrganization(@PathParam("organizationCode") String organizationCode) {
      organizationFacade.deleteOrganization(organizationCode);

      return Response.ok().link(getParentUri(organizationCode), "parent").build();
   }

   @GET
   @Path("{organizationCode}")
   public JsonOrganization getOrganization(@PathParam("organizationCode") String organizationCode) {
      Organization organization = organizationFacade.getOrganization(organizationCode);
      return JsonOrganization.convert(organization);
   }

   @GET
   public List<JsonOrganization> getOrganizations() {
      List<Organization> organizations = organizationFacade.getOrganizations();
      return JsonOrganization.convert(organizations);
   }

   @GET
   @Path("{organizationCode}/permissions")
   public JsonPermissions getOrganizationPermissions(@PathParam("organizationCode") String organizationCode) {
      Permissions permissions = organizationFacade.getOrganizationPermissions(organizationCode);
      return JsonPermissions.convert(permissions);
   }

   @PUT
   @Path("{organizationCode}/permissions/users")
   public Set<JsonPermission> updateUserPermission(@PathParam("organizationCode") String organizationCode, JsonPermission userPermission) {
      Set<Permission> storedUserPermissions = organizationFacade.updateUserPermissions(organizationCode, userPermission);
      return JsonPermission.convert(storedUserPermissions);
   }

   @DELETE
   @Path("{organizationCode}/permissions/users/{user}")
   public Response removeUserPermission(@PathParam("organizationCode") String organizationCode, @PathParam("user") String user) {
      organizationFacade.removeUserPermission(organizationCode, user);

      return Response.ok().link(getParentUri("users", user), "parent").build();
   }

   @PUT
   @Path("{organizationCode}/permissions/groups")
   public Set<JsonPermission> updateGroupPermission(@PathParam("organizationCode") String organizationCode, JsonPermission groupPermission) {
      Set<Permission> storedGroupPermissions = organizationFacade.updateGroupPermissions(organizationCode, groupPermission);
      return JsonPermission.convert(storedGroupPermissions);
   }

   @DELETE
   @Path("{organizationCode}/permissions/groups/{group}")
   public Response removeGroupPermission(@PathParam("organizationCode") String organizationCode, @PathParam("group") String group) {
      organizationFacade.removeGroupPermission(organizationCode, group);

      return Response.ok().link(getParentUri("groups", group), "parent").build();
   }
}
