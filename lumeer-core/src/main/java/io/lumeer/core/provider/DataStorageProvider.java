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
package io.lumeer.core.provider;

import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.facade.ConfigurationFacade;
import io.lumeer.core.facade.SystemConfigurationFacade;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.DataStorageFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DataStorageProvider {

   private static final String SYSTEM_CONNECTION = "/SYSTEM/"; // organisation cannot have / in its name

   private Map<String, DataStorage> connections = new ConcurrentHashMap<>();

   @Inject
   private DataStorageFactory dataStorageFactory;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private SystemConfigurationFacade systemConfigurationFacade;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   public DataStorage getUserStorage() {
      String code = workspaceKeeper.getOrganization().isPresent() ? workspaceKeeper.getOrganization().get().getCode() : "Default";
      return connections.computeIfAbsent(code,
            k -> dataStorageFactory.getStorage(configurationFacade.getDataStorage(), configurationFacade.getDataStorageDatabase(), configurationFacade.getDataStorageUseSsl()));
   }

   public DataStorage getSystemStorage() {
      return connections.computeIfAbsent(SYSTEM_CONNECTION,
            k -> dataStorageFactory.getStorage(systemConfigurationFacade.getSystemDataStorage(), systemConfigurationFacade.getSystemDataStorageDatabase(), systemConfigurationFacade.getSystemDataStorageUseSsl()));
   }

   @PreDestroy
   public void closeConnections() {
      connections.forEach((k, v) -> v.disconnect());
   }
}
