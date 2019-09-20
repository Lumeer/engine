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

import io.lumeer.api.model.geocoding.Coordinates;
import io.lumeer.core.client.opensearch.OpenSearchClient;
import io.lumeer.core.client.opensearch.OpenSearchResult;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class MapQuestClient implements OpenSearchClient {

   private static final String GEOCODING_URL = "https://www.mapquestapi.com/geocoding/v1/";
   private static final String OPEN_SEARCH_URL = "https://open.mapquestapi.com/nominatim/v1/";

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private Logger log;

   private String mapQuestKey;

   @PostConstruct
   public void init() {
      mapQuestKey = this.configurationProducer.get("mapquest_key");
   }

   public MapQuestResponse batchGeoCode(final Set<String> locations) {
      if (mapQuestKey == null || "".equals(mapQuestKey)) {
         return null;
      }

      final Client client = ClientBuilder.newBuilder().build();
      final Response response = client.target(GEOCODING_URL + "batch")
                                      .queryParam("key", mapQuestKey)
                                      .queryParam("location", locations.toArray(new Object[0]))
                                      .request(MediaType.APPLICATION_JSON)
                                      .header("Content-Type", MediaType.APPLICATION_JSON)
                                      .buildGet()
                                      .invoke();
      return decodeResponse(client, response, new GenericType<MapQuestResponse>() {});
   }

   @Override
   public List<OpenSearchResult> search(final String query, final int limit, final String language) {
      final Client client = ClientBuilder.newBuilder().build();
      final Response response = client.target(OPEN_SEARCH_URL).path("search.php")
                                      .queryParam("addressdetails", "1")
                                      .queryParam("format", "json")
                                      .queryParam("key", mapQuestKey)
                                      .queryParam("limit", limit)
                                      .queryParam("osm_type", "N")
                                      .queryParam("q", query)
                                      .request(MediaType.APPLICATION_JSON)
                                      .header("Accept-Language", language)
                                      .header("Content-Type", MediaType.APPLICATION_JSON)
                                      .buildGet()
                                      .invoke();
      return decodeResponse(client, response, new GenericType<List<OpenSearchResult>>() {});
   }

   @Override
   public OpenSearchResult reverse(final Coordinates coordinates, final String language) {
      final Client client = ClientBuilder.newBuilder().build();
      final Response response = client.target(OPEN_SEARCH_URL).path("reverse.php")
                                      .queryParam("addressdetails", "1")
                                      .queryParam("format", "json")
                                      .queryParam("key", mapQuestKey)
                                      .queryParam("lat", coordinates.getLatitude())
                                      .queryParam("lon", coordinates.getLongitude())
                                      .queryParam("osm_type", "N")
                                      .request(MediaType.APPLICATION_JSON)
                                      .header("Accept-Language", language)
                                      .header("Content-Type", MediaType.APPLICATION_JSON)
                                      .buildGet()
                                      .invoke();
      return decodeResponse(client, response, new GenericType<OpenSearchResult>() {});
   }

   private <T> T decodeResponse(final Client client, Response response, GenericType<T> type) {
      try {
         if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new IOException("MapQuest returned HTTP error code " + response.getStatus() + ": " + response.getStatusInfo().getReasonPhrase());
         }
         return response.readEntity(type);
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to communicate with MapQuest API:", e);
         return null;
      } finally {
         client.close();
      }
   }
}
