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
package io.lumeer.storage.mongodb.dao.system;

import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataStorage;

import com.mongodb.client.MongoDatabase;
import org.mongodb.morphia.AdvancedDatastore;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class SystemScopedDao {

   protected MongoDatabase database;
   protected AdvancedDatastore datastore;

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @PostConstruct
   public void init() {
      this.database = (MongoDatabase) dataStorage.getDatabase();
      this.datastore = (AdvancedDatastore) dataStorage.getDataStore();
   }

   void setDatabase(final MongoDatabase database) {
      this.database = database;
   }

   void setDatastore(final AdvancedDatastore datastore) {
      this.datastore = datastore;
   }

}
