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
package io.lumeer.engine.api.cache;

/**
 * Provides a mean of obtaining a named cache and thus keeping a list of multiple caches.
 */
public interface CacheProvider {

   /**
    * Initializes the cache provider. Without calling this method, the class behavior is unknown.
    *
    * @param namespace
    *       Namespace of the cache provider.
    * @param cacheManager
    *       Parent cache manager.
    */
   void init(final String namespace, final CacheManager cacheManager);

   /**
    * Obtains a fresh cache of given type.
    *
    * @param cacheName
    *       Name of the cache to obtain.
    * @param <T>
    *       Type of values stored in the cache.
    * @return A fresh cache of given type.
    */
   <T> Cache<T> getCache(final String cacheName);
}
