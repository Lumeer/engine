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
package io.lumeer.engine.util;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageFactory;
import io.lumeer.engine.controller.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.provider.DataStorageProvider;

import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

/**
 * This class uses CDI to alias Java EE resources, such as the persistence context, to CDI beans
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
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
