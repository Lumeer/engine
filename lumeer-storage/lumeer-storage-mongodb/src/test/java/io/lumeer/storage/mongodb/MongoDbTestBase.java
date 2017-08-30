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

import static io.lumeer.storage.mongodb.EmbeddedMongoDb.*;

import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.storage.mongodb.model.MorphiaView;

import com.mongodb.client.MongoDatabase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

public abstract class MongoDbTestBase {

   private static Morphia morphia = new Morphia().mapPackage(MorphiaView.class.getPackage().getName());
   private static EmbeddedMongoDb embeddedMongoDb;

   static {
      morphia.getMapper().getOptions().setStoreEmpties(true);
   }

   protected MongoDbStorage mongoDbStorage;

   protected MongoDatabase database;
   protected AdvancedDatastore datastore;

   @BeforeClass
   public static void startEmbeddedMongoDb() {
      embeddedMongoDb = new EmbeddedMongoDb();
      embeddedMongoDb.start();
   }

   @AfterClass
   public static void stopEmbeddedMongoDb() {
      if (embeddedMongoDb != null) {
         embeddedMongoDb.stop();
      }
   }

   @Before
   public void connectMongoDbStorage() {
      mongoDbStorage = new MongoDbStorage(morphia);
      mongoDbStorage.connect(new StorageConnection(HOST, PORT, USER, PASSWORD), NAME, SSL);

      database = mongoDbStorage.getDatabase();
      datastore = mongoDbStorage.getDataStore();

      database.drop();
   }

   @After
   public void disconnectMongoDbStorage() {
      if (mongoDbStorage != null) {
         mongoDbStorage.disconnect();
      }
   }
}
