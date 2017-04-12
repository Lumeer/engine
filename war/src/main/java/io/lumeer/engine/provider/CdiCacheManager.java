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
package io.lumeer.engine.provider;

import io.lumeer.engine.api.cache.Cache;
import io.lumeer.engine.api.cache.CacheFactory;
import io.lumeer.engine.api.cache.CacheManager;
import io.lumeer.engine.api.cache.CacheProvider;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class CdiCacheManager implements CacheManager, Serializable {

   @Inject
   private CacheFactory cacheFactory;

   private Map<String, Map<String, Cache>> caches = new ConcurrentHashMap<>();

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Override
   public CacheProvider getCacheProvider(final String namespace) {
      final CacheProvider provider = new DefaultCacheProvider();
      provider.init(namespace, this);

      return provider;
   }

   public <T> Cache<T> getCache(final String name) {
      final String key = organizationFacade.getOrganizationId() + "/" + projectFacade.getCurrentProjectId();
      final Map<String, Cache> localCaches = caches.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
      final Cache<T> cache = localCaches.computeIfAbsent(name, k -> cacheFactory.getCache());

      return cache;
   }

}
