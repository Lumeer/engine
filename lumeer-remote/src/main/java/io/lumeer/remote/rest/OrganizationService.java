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
