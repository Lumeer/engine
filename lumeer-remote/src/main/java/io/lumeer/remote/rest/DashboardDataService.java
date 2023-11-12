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

import io.lumeer.api.model.DashboardData;
import io.lumeer.core.facade.DashboardDataFacade;

import java.util.List;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("organizations/{organizationId:[0-9a-fA-F]{24}}/projects/{projectId:[0-9a-fA-F]{24}}/dashboard")
public class DashboardDataService extends AbstractService {

   @PathParam("organizationId")
   private String organizationId;

   @PathParam("projectId")
   private String projectId;

   @Inject
   private DashboardDataFacade dashboardDataFacade;

   @PostConstruct
   public void init() {
      workspaceKeeper.setWorkspaceIds(organizationId, projectId);
   }

   @GET
   @Path("data")
   public List<DashboardData> getAll() {
      return dashboardDataFacade.getAll();
   }

   @GET
   @Path("data/{type}/{typeId}")
   public DashboardData getByType(@PathParam("type") String type, @PathParam("typeId") String typeId) {
      return dashboardDataFacade.getByType(type, typeId);
   }

   @POST
   @Path("data")
   public DashboardData update(DashboardData dashboardData) {
      return dashboardDataFacade.update(dashboardData);
   }

   @POST
   @Path("data/{type}/delete")
   public Response delete(@PathParam("type") String type, final Set<String> ids) {
      dashboardDataFacade.deleteTypes(type, ids);

      return Response.ok().build();
   }

}
