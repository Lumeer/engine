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
 * Allows obtaining various cache providers so that individual classes can create and maintain their own cache
 * and we are still able to distinguish between caches from multiple instances of the same class.
 * Normally, all instances of a class share the same caches. However, there might be multiple groups of instances
 * that fulfill a different purpose and need to distinguish between their caches.
 */
public interface CacheManager {

   /**
    * Gets a new cache provider for the given namespace. The caches in the namespace are shared.
    *
    * @param namespace
    *       Namespace of the cache provider.
    * @return A cache provider with the given namespace.
    */
   CacheProvider getCacheProvider(final String namespace);

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
