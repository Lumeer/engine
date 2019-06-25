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

import io.lumeer.api.model.geocoding.GeoCodingResult;
import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class GeoCodingCache {

   private static final int CACHE_SIZE = 10000;

   @Inject
   private CacheFactory cacheFactory;

   private Cache<GeoCodingResult> cache;
   private Deque<String> cachedQueries;

   @PostConstruct
   public void initCache() {
      cache = cacheFactory.getCache();
      cachedQueries = new ConcurrentLinkedDeque<>();
   }

   public GeoCodingResult getResult(final String query) {
      return cache.get(query);
   }

   public void updateResult(final String query, final GeoCodingResult result) {
      this.cache.set(query, result);
      this.cachedQueries.push(query);

      removeLastCacheEntryIfFull();
   }

   private void removeLastCacheEntryIfFull() {
      if (cachedQueries.size() < CACHE_SIZE) {
         return;
      }

      var query = cachedQueries.pollLast();
      if (query != null) {
         cache.remove(query);
      }
   }
}
