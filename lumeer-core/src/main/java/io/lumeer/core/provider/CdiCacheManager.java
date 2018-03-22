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
package io.lumeer.core.provider;

import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;
import io.lumeer.engine.api.cache.CacheManager;
import io.lumeer.engine.api.cache.CacheProvider;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CdiCacheManager implements CacheManager, Serializable {

   @Inject
   private CacheFactory cacheFactory;

   private Map<String, Map<String, Cache>> caches = new ConcurrentHashMap<>();

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Override
   public CacheProvider getCacheProvider(final String namespace) {
      final CacheProvider provider = new DefaultCacheProvider();
      provider.init(namespace, this);

      return provider;
   }

   public <T> Cache<T> getCache(final String cacheName) {
      final String key = getKey();
      final Map<String, Cache> localCaches = caches.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
      final Cache<T> cache = localCaches.computeIfAbsent(cacheName, k -> cacheFactory.getCache());

      return cache;
   }

   public String getKey() {
      if (!workspaceKeeper.getOrganization().isPresent()) {
         return "system";
      }
      String key = workspaceKeeper.getOrganization().get().getId();

      if (!workspaceKeeper.getProject().isPresent()) {
         return key;
      }

      return key + "/" + workspaceKeeper.getProject().get().getId();
   }
}
