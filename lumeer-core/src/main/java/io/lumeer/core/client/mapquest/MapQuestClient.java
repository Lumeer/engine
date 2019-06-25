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
package io.lumeer.core.client.mapquest;

import io.lumeer.api.model.geocoding.GeoCodingProvider;
import io.lumeer.api.model.geocoding.GeoCodingResult;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class MapQuestClient {

   private static final String MAPQUEST_URL = "https://www.mapquestapi.com/geocoding/v1/";

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private Logger log;

   private String mapQuestKey;

   @PostConstruct
   public void init() {
      mapQuestKey = this.configurationProducer.get("mapquest_key");
   }

   public List<GeoCodingResult> batchGeoCode(Set<String> locations) {
      final Client client = ClientBuilder.newBuilder().build();
      final Response response = client.target(MAPQUEST_URL + "batch")
                                      .queryParam("key", mapQuestKey)
                                      .queryParam("location", locations.toArray(new Object[0]))
                                      .request(MediaType.APPLICATION_JSON)
                                      .header("Content-Type", MediaType.APPLICATION_JSON)
                                      .buildGet()
                                      .invoke();

      try {
         if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new IOException("MapQuest returned HTTP error code " + response.getStatus() + ": " + response.getStatusInfo().getReasonPhrase());
         }
         return convertResults(response.readEntity(MapQuestResponse.class));
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to communicate with MapQuest API:", e);
         return Collections.emptyList();
      } finally {
         client.close();
      }
   }

   private List<GeoCodingResult> convertResults(MapQuestResponse response) {
      return response.getResults().stream()
                     .map(result -> new GeoCodingResult(GeoCodingProvider.MAPQUEST, result.getProvidedLocation().getLocation(), result.getLocations()))
                     .collect(Collectors.toList());
   }
}
