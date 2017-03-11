/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.api.cache;

/**
 * Allows obtaining various cache providers so that individual classes can create and maintain their own cache
 * and we are still able to distinguish between caches from multiple instances of the same class.
 * Normally, all instances of a class share the same caches. However, there might be multiple groups of instances
 * that fulfill a different purpose and need to distinguish between their caches.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
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
