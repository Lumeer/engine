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

import io.lumeer.api.model.geocoding.Coordinates;
import io.lumeer.api.model.geocoding.Location;
import io.lumeer.core.facade.GeoCodingFacade;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * This service is independent of the rest of the application and can become a microservice in the future.
 */
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("geocoding")
public class GeoCodingService extends AbstractService {

   @Inject
   private GeoCodingFacade geoCodingFacade;

   @GET
   @Path("coordinates")
   public Map<String, Coordinates> findCoordinates(@QueryParam("query") final Set<String> queries) {
      if (queries == null || queries.size() == 0) {
         throw new BadRequestException("No query has been provided");
      }

      return geoCodingFacade.findCoordinates(queries);
   }

   @GET
   @Path("locations")
   public List<Location> findLocations(
         @QueryParam("query") final String query,
         @QueryParam("limit") final Integer limit) {
      if (query == null || query.length() == 0) {
         throw new BadRequestException("No query has been provided");
      }

      return geoCodingFacade.findLocationsByQuery(query, limit);
   }

   @GET
   @Path("locations/{coordinates}")
   public Location findLocationByCoordinates(@PathParam("coordinates") String coordinates) {
      return geoCodingFacade.findLocationByCoordinates(new Coordinates(coordinates));
   }
}
