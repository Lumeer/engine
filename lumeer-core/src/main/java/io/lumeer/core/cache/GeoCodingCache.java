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
package io.lumeer.core.cache;

import io.lumeer.api.model.geocoding.Coordinates;
import io.lumeer.api.model.geocoding.Location;
import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GeoCodingCache {

   private static final int QUERY_LOCATIONS_CACHE_SIZE = 10000;
   private static final int COORDINATES_LOCATION_CACHE_SIZE = 10000;
   private static final int QUERY_COORDINATES_CACHE_SIZE = 10000;

   @Inject
   private CacheFactory cacheFactory;

   private Cache<List<Location>> queryLocationsCache;
   private Deque<String> queryLocationsCacheKeys;

   private Cache<Location> coordinatesLocationCache;
   private Deque<String> coordinatesLocationCacheKeys;

   private Cache<Coordinates> queryCoordinatesCache;
   private Deque<String> queryCoordinatesCacheKeys;

   @PostConstruct
   public void initCache() {
      queryLocationsCache = cacheFactory.getCache();
      queryLocationsCacheKeys = new ConcurrentLinkedDeque<>();

      coordinatesLocationCache = cacheFactory.getCache();
      coordinatesLocationCacheKeys = new ConcurrentLinkedDeque<>();

      queryCoordinatesCache = cacheFactory.getCache();
      queryCoordinatesCacheKeys = new ConcurrentLinkedDeque<>();
   }

   public List<Location> getQueryLocations(final String query, final String language) {
      return queryLocationsCache.get(GeoCodingCache.createKey(query, language));
   }

   public void updateQueryLocations(final String query, final List<Location> locations, final String language) {
      var key = GeoCodingCache.createKey(query, language);

      queryLocationsCache.set(key, locations);
      queryLocationsCacheKeys.push(key);

      if (queryLocationsCacheKeys.size() > QUERY_LOCATIONS_CACHE_SIZE) {
         GeoCodingCache.removeLastCacheEntry(queryLocationsCache, queryLocationsCacheKeys);
      }
   }

   public Location getCoordinatesLocation(final Coordinates coordinates, final String language) {
      return coordinatesLocationCache.get(GeoCodingCache.createKey(coordinates.toString(), language));
   }

   public void updateCoordinatesLocation(final Coordinates coordinates, final Location location, final String language) {
      var key = GeoCodingCache.createKey(coordinates.toString(), language);

      coordinatesLocationCache.set(key, location);
      coordinatesLocationCacheKeys.push(key);

      if (queryLocationsCacheKeys.size() > QUERY_LOCATIONS_CACHE_SIZE) {
         GeoCodingCache.removeLastCacheEntry(coordinatesLocationCache, coordinatesLocationCacheKeys);
      }
   }

   public Coordinates getQueryCoordinates(final String query) {
      return queryCoordinatesCache.get(query);
   }

   public void updateQueryCoordinates(final String query, final Coordinates coordinates) {
      queryCoordinatesCache.set(query, coordinates);
      queryCoordinatesCacheKeys.push(query);

      if (queryCoordinatesCacheKeys.size() > QUERY_COORDINATES_CACHE_SIZE) {
         GeoCodingCache.removeLastCacheEntry(queryCoordinatesCache, queryCoordinatesCacheKeys);
      }
   }

   private static void removeLastCacheEntry(final Cache cache, final Deque<String> cachedKeys) {
      var key = cachedKeys.pollLast();
      if (key != null) {
         cache.remove(key);
      }
   }

   private static String createKey(final String query, final String language) {
      return query + "_" + language;
   }
}
