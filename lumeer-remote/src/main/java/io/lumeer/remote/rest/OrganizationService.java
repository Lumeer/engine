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
import io.lumeer.api.model.Organization;
import io.lumeer.core.facade.OrganizationFacade;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
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

   @GET
   public List<JsonOrganization> getOrganizations() {
      return organizationFacade.getOrganizations().stream()
            .map(JsonOrganization::new)
            .collect(Collectors.toList());
   }

   @GET
   @Path("{organizationCode}")
   public JsonOrganization getOrganization(@PathParam("organizationCode") String organizationCode) {
      Organization organization = organizationFacade.getOrganization(organizationCode);
      return new JsonOrganization(organization);
   }

   @DELETE
   @Path("{organizationCode}")
   public Response deleteOrganization(@PathParam("organizationCode") String organizationCode) {
      organizationFacade.deleteOrganization(organizationCode);

      return Response.ok().link(getParentUri(organizationCode), "parent").build();
   }

   @POST
   public Response createOrganization(JsonOrganization organization) {
      Organization storedOrganization = organizationFacade.createOrganization(organization);

      URI resourceUri = getResourceUri(storedOrganization);
      return Response.created(resourceUri).build();
   }

   @PUT
   @Path("{organizationCode}")
   public Response editOrganization(@PathParam("organizationCode") String organizationCode, JsonOrganization organization) {
      Organization storedOrganization = organizationFacade.editOrganization(organizationCode, organization);

      JsonOrganization jsonOrganization = new JsonOrganization(storedOrganization);
      return Response.ok(jsonOrganization).build();
   }
}
