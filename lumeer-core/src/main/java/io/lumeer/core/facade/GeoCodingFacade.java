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
package io.lumeer.core.facade;

import io.lumeer.api.model.geocoding.Coordinates;
import io.lumeer.api.model.geocoding.Location;
import io.lumeer.core.cache.GeoCodingCache;
import io.lumeer.core.client.mapquest.MapQuestClient;
import io.lumeer.core.client.opensearch.OpenSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class GeoCodingFacade {

   private static final int LOCATIONS_LIMIT = 10;

   @Inject
   private GeoCodingCache geoCodingCache;

   @Inject
   private MapQuestClient mapQuestClient;

   public Map<String, Coordinates> findCoordinates(final Set<String> queries) {
      var coordinatesMap = getCachedCoordinates(queries);
      var unresolvedQueries = filterUnresolvedQueries(queries, coordinatesMap);

      if (unresolvedQueries.size() > 0) {
         getFreshCoordinates(unresolvedQueries).forEach((query, coordinates) -> {
            geoCodingCache.updateQueryCoordinates(query, coordinates);
            coordinatesMap.put(query, coordinates);
         });
      }

      return coordinatesMap;
   }

   public Location findLocationByCoordinates(final Coordinates coordinates, final String language) {
      var cachedLocation = geoCodingCache.getCoordinatesLocation(coordinates, language);
      if (cachedLocation != null) {
         return cachedLocation;
      }

      var result = mapQuestClient.reverse(coordinates, language);
      if (result == null) {
         return null;
      }

      var location = result.toLocation();
      geoCodingCache.updateCoordinatesLocation(coordinates, location, language);

      return location;
   }

   public List<Location> findLocationsByQuery(final String query, final Integer limit, final String language) {
      var cachedLocations = geoCodingCache.getQueryLocations(query, language);
      if (cachedLocations != null) {
         return cachedLocations;
      }

      int locationsLimit = limit != null ? limit : LOCATIONS_LIMIT;
      // osm_type filtering does not work so all 3 types are returned and then filtered
      int searchLimit = locationsLimit * 3;

      var results = mapQuestClient.search(query, searchLimit, language);
      if (results == null) {
         return new ArrayList<>();
      }

      var locations = results.stream()
                             .filter(result -> result.getOsmType().equals("node") || result.getOsmType().equals("way"))
                             .limit(locationsLimit)
                             .map(OpenSearchResult::toLocation)
                             .collect(Collectors.toList());
      geoCodingCache.updateQueryLocations(query, locations, language);

      return locations;
   }

   private Map<String, Coordinates> getCachedCoordinates(final Set<String> queries) {
      Map<String, Coordinates> coordinatesMap = new HashMap<>();
      queries.forEach(query -> {
         if (query != null && !query.isEmpty()) {
            coordinatesMap.put(query, geoCodingCache.getQueryCoordinates(query));
         }
      });
      return coordinatesMap;
   }

   private Set<String> filterUnresolvedQueries(final Set<String> queries, final Map<String, Coordinates> coordinatesMap) {
      return queries.stream()
                    .filter(query -> coordinatesMap.get(query) == null)
                    .collect(Collectors.toSet());
   }

   private Map<String, Coordinates> getFreshCoordinates(final Set<String> queries) {
      var response = mapQuestClient.batchGeoCode(queries);
      if (response == null) {
         return Collections.emptyMap();
      }

      return response.getResults().stream()
                     .filter(result -> result.getLocations().size() > 0)
                     .collect(Collectors.toMap(result -> result.getProvidedLocation().getLocation(), result -> {
                        var latLng = result.getLocations().get(0).getLatLng();
                        return new Coordinates(latLng.getLat(), latLng.getLng());
                     }));
   }

}
