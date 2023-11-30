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

import io.lumeer.api.model.ResourceVariable;
import io.lumeer.core.facade.ResourceVariableFacade;
import io.lumeer.remote.rest.annotation.HealthCheck;

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/organizations/{organizationId:[0-9a-fA-F]{24}}/variables")
public class ResourceVariableService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PostConstruct
   public void init() {
      workspaceKeeper.setOrganizationId(organizationId);
   }

   @Inject
   private ResourceVariableFacade resourceVariableFacade;

   @POST
   @HealthCheck
   public ResourceVariable create(ResourceVariable variable) {
      checkOrganizationId(variable);
      return resourceVariableFacade.create(variable);
   }

   private void checkOrganizationId(ResourceVariable variable) {
      if (!organizationId.equals(variable.getOrganizationId())) {
         throw new BadRequestException("Invalid organizationId in object body '" + variable.getOrganizationId() + "'");
      }
   }

   @PUT
   @Path("{variableId:[0-9a-fA-F]{24}}")
   @HealthCheck
   public ResourceVariable update(@PathParam("variableId") String variableId, ResourceVariable variable) {
      checkOrganizationId(variable);
      return resourceVariableFacade.update(variableId, variable);
   }

   @GET
   @Path("{variableId:[0-9a-fA-F]{24}}")
   public ResourceVariable getVariable(@PathParam("variableId") String variableId) {
      return resourceVariableFacade.getVariable(variableId);
   }

   @DELETE
   @Path("{variableId:[0-9a-fA-F]{24}}")
   public Response delete(@PathParam("variableId") String variableId) {
      resourceVariableFacade.delete(variableId);

      return Response.ok().link(getParentUri(variableId), "parent").build();
   }

   @GET
   @Path("projects/{projectId:[0-9a-fA-F]{24}}")
   public List<ResourceVariable> getVariables(@PathParam("projectId") String projectId) {
      return resourceVariableFacade.getInProject(projectId);
   }

}
