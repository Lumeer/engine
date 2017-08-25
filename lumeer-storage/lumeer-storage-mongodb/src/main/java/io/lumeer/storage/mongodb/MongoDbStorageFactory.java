/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.storage.mongodb;

import io.lumeer.engine.api.cache.CacheProvider;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageFactory;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.storage.mongodb.model.MorphiaView;

import org.mongodb.morphia.Morphia;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoDbStorageFactory implements DataStorageFactory {

   private Morphia morphia;

   @PostConstruct
   public void init() {
      morphia = new Morphia().mapPackage(MorphiaView.class.getPackage().getName());
      morphia.getMapper().getOptions().setStoreEmpties(true);
   }

   @Override
   public DataStorage getStorage(final CacheProvider cacheProvider, final List<StorageConnection> connections, final String database, final Boolean useSsl) {
      final DataStorage storage = new MongoDbStorage(morphia);
      storage.setCacheProvider(cacheProvider);
      storage.connect(connections, database, useSsl);

      return storage;
   }
}
