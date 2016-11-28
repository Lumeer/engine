/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
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
package io.lumeer.engine.util;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.engine.controller.configuration.DefaultConfigurationProducer;
import io.lumeer.mongodb.MongoDbStorage;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class uses CDI to alias Java EE resources, such as the persistence context, to CDI beans
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Resources {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Resource
   @Produces
   private ManagedExecutorService managedExecutorService;

   @Produces
   @Dependent
   public Logger produceLog(InjectionPoint injectionPoint) {
      return Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
   }

   /**
    * Produces system storage for user data etc.
    *
    * @return System data storage.
    */
   @Produces
   @SystemDataStorage
   @RequestScoped
   public DataStorage getSystemDataStorage() {
      final MongoDbStorage storage = new MongoDbStorage();
      final Map<String, String> defaultConfiguration = defaultConfigurationProducer.getDefaultConfiguration();

      storage.connect(new StorageConnection(
            defaultConfiguration.get(LumeerConst.SYSTEM_DB_HOST_PROPERTY),
            Integer.valueOf(defaultConfiguration.get(LumeerConst.SYSTEM_DB_PORT_PROPERTY)),
            defaultConfiguration.get(LumeerConst.SYSTEM_DB_USER_PROPERTY),
            defaultConfiguration.get(LumeerConst.SYSTEM_DB_PASSWORD_PROPERTY)),
            defaultConfiguration.get(LumeerConst.SYSTEM_DB_NAME_PROPERTY));

      return storage;
   }

}
