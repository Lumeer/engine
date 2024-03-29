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
package io.lumeer.core.util;

import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.provider.DataStorageProvider;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.DataStorageFactory;

import java.util.logging.Logger;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

/**
 * This class uses CDI to alias Java EE resources, such as the persistence context, to CDI beans
 *
 */
public class Resources {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private DataStorageFactory dataStorageFactory;

   @Inject
   private DataStorageProvider dataStorageProvider;

   @Resource
   @Produces
   private ManagedExecutorService managedExecutorService;

   @Produces
   @Dependent
   @Default
   public Logger produceLog(InjectionPoint injectionPoint) {
      return produceLog(injectionPoint.getMember().getDeclaringClass().getName());
   }

   public static Logger produceLog(final String className) {
      return Logger.getLogger(className);
   }

   @Produces
   @SystemDataStorage
   @ApplicationScoped
   public DataStorage getSystemDataStorage() {
      return dataStorageProvider.getSystemStorage();
   }

   @Produces
   @UserDataStorage
   @RequestScoped
   public DataStorage getDataStorage() {
      return dataStorageProvider.getUserStorage();
   }

}
