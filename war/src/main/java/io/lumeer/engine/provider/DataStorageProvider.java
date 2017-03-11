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

import io.lumeer.engine.api.cache.CacheManager;
import io.lumeer.engine.api.cache.CacheProvider;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageFactory;
import io.lumeer.engine.controller.ConfigurationFacade;
import io.lumeer.engine.controller.OrganisationFacade;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class DataStorageProvider {

   private static final String SYSTEM_CONNECTION = "/SYSTEM/"; // organisation cannot have / in its name

   private Map<String, DataStorage> connections = new ConcurrentHashMap<>();

   @Inject
   private DataStorageFactory dataStorageFactory;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private OrganisationFacade organisationFacade;

   @Inject
   private CacheManager cacheManager;

   public DataStorage getUserStorage() {
      return connections.computeIfAbsent(organisationFacade.getOrganisationId(),
            k -> dataStorageFactory.getStorage(cacheManager.getCacheProvider("userDataStorage"), configurationFacade.getDataStorage(), configurationFacade.getDataStorageDatabase(), configurationFacade.getDataStorageUseSsl()));
   }

   public DataStorage getSystemStorage() {
      return connections.computeIfAbsent(SYSTEM_CONNECTION,
            k -> dataStorageFactory.getStorage(cacheManager.getCacheProvider("systemDataStorage"), configurationFacade.getSystemDataStorage(), configurationFacade.getSystemDataStorageDatabase(), configurationFacade.getSystemDataStorageUseSsl()));
   }

   @PreDestroy
   public void closeConnections() {
      connections.forEach((k, v) -> v.disconnect());
   }
}
